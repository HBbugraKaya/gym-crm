package com.example.gymcrm.integration;

import com.example.gymcrm.web.dto.RegistrationResponse;
import com.example.gymcrm.web.dto.TrainerWorkloadRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class GymCrmApiIntegrationTest {
    private static final AtomicLong UNIQUE_SEQUENCE = new AtomicLong(System.nanoTime());

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataSource dataSource;

    @MockitoBean
    private TrainerWorkloadClient trainerWorkloadClient;

    @MockitoBean
    private TraineeDeletionReportClient traineeDeletionReportClient;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpJdbcTemplate() {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void bootInfrastructureExposesActuatorOpenApiAndSecurity() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.applicationProfile.status").value("UP"))
                .andExpect(jsonPath("$.components.trainingTypeCatalog.status").value("UP"));

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application.name").value("spring-boot-gym-crm"))
                .andExpect(jsonPath("$.application.description")
                        .value("Gym trainee, trainer and training management API"));

        MvcResult metrics = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(metrics.getResponse().getContentAsString())
                .contains("gymcrm_profiles_total", "gymcrm_trainings_total");

        MvcResult apiDocsResult = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode apiDocs = jsonTree(apiDocsResult);
        assertThat(apiDocs.path("openapi").asText()).startsWith("3.");
        assertThat(apiDocs.path("info").path("title").asText()).isEqualTo("Gym CRM API");
        assertThat(apiDocs.path("components").path("securitySchemes").path("bearerAuth").path("scheme").asText())
                .isEqualTo("bearer");
        assertThat(apiDocs.path("paths").path("/api/v1/trainees").path("post").has("security")).isFalse();
        assertThat(apiDocs.path("paths").path("/api/v1/trainees/{username}").path("get")
                .path("security").toString()).contains("bearerAuth");

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/training-types"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE,
                        org.hamcrest.Matchers.containsString("Bearer")));

        mockMvc.perform(options("/api/v1/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
    }

    @Test
    void methodSecurityRejectsCrossProfileAndWrongRoleAccess() throws Exception {
        String suffix = uniqueSuffix();
        RegistrationResponse first = registerTrainee("First" + suffix, "Trainee");
        RegistrationResponse second = registerTrainee("Second" + suffix, "Trainee");

        mockMvc.perform(get("/api/v1/trainees/{username}", second.username())
                        .header(HttpHeaders.AUTHORIZATION, bearerAuth(first)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/trainings")
                        .header(HttpHeaders.AUTHORIZATION, bearerAuth(first))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "traineeUsername", first.username(),
                                "trainerUsername", first.username(),
                                "trainingName", "Forbidden",
                                "trainingDate", "2026-07-16",
                                "durationMinutes", 30))))
                .andExpect(status().isForbidden());
    }

    @Test
    void trainerAssignmentTrainingListsTrainingTypesAndHardDeleteWorkEndToEnd() throws Exception {
        String suffix = uniqueSuffix();
        RegistrationResponse trainee = registerTrainee("Runner" + suffix, "Trainee");
        RegistrationResponse trainer = registerTrainer("Coach" + suffix, "Trainer", "YOGA");
        String traineeAuthorization = bearerAuth(trainee);
        String trainerAuthorization = bearerAuth(trainer);

        MvcResult availableResult = mockMvc.perform(
                        get("/api/v1/trainees/{username}/available-trainers", trainee.username())
                                .header(HttpHeaders.AUTHORIZATION, traineeAuthorization))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(usernames(availableResult)).contains(trainer.username());

        mockMvc.perform(put("/api/v1/trainees/{username}/trainers", trainee.username())
                        .header(HttpHeaders.AUTHORIZATION, traineeAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("trainerUsernames", List.of(trainer.username())))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value(trainer.username()))
                .andExpect(jsonPath("$[0].specialization").value("YOGA"));

        mockMvc.perform(get("/api/v1/trainees/{username}", trainee.username())
                        .header(HttpHeaders.AUTHORIZATION, traineeAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainers[0].username").value(trainer.username()));

        mockMvc.perform(get("/api/v1/trainers/{username}", trainer.username())
                        .header(HttpHeaders.AUTHORIZATION, trainerAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialization").value("YOGA"))
                .andExpect(jsonPath("$.trainees[0].username").value(trainee.username()));

        String trainingName = "Integration Yoga " + suffix;
        LocalDate trainingDate = LocalDate.of(2026, 7, 16);
        mockMvc.perform(post("/api/v1/trainings")
                        .header(HttpHeaders.AUTHORIZATION, trainerAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "traineeUsername", trainee.username(),
                                "trainerUsername", trainer.username(),
                                "trainingName", trainingName,
                                "trainingDate", trainingDate,
                                "durationMinutes", 55))))
                .andExpect(status().isOk());

        MvcResult traineeTrainings = mockMvc.perform(
                        get("/api/v1/trainees/{username}/trainings", trainee.username())
                                .header(HttpHeaders.AUTHORIZATION, traineeAuthorization)
                                .queryParam("periodFrom", trainingDate.minusDays(1).toString())
                                .queryParam("periodTo", trainingDate.plusDays(1).toString())
                                .queryParam("trainerName", "Coach" + suffix)
                                .queryParam("trainingType", "YOGA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingName").value(trainingName))
                .andExpect(jsonPath("$[0].trainingType").value("YOGA"))
                .andExpect(jsonPath("$[0].durationMinutes").value(55))
                .andReturn();
        long trainingId = jsonTree(traineeTrainings).get(0).path("trainingId").asLong();

        mockMvc.perform(get("/api/v1/trainers/{username}/trainings", trainer.username())
                        .header(HttpHeaders.AUTHORIZATION, trainerAuthorization)
                        .queryParam("traineeName", "Runner" + suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingName").value(trainingName))
                .andExpect(jsonPath("$[0].traineeName").value("Runner" + suffix + " Trainee"));

        MvcResult typesResult = mockMvc.perform(get("/api/v1/training-types")
                        .header(HttpHeaders.AUTHORIZATION, trainerAuthorization))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode trainingTypes = jsonTree(typesResult);
        assertThat(trainingTypes).hasSize(5);
        assertThat(trainingTypes.findValuesAsText("name"))
                .containsExactlyInAnyOrder("FITNESS", "YOGA", "CARDIO", "STRENGTH", "STRETCHING");

        mockMvc.perform(delete("/api/v1/trainings/{trainingId}", trainingId)
                        .header(HttpHeaders.AUTHORIZATION, trainerAuthorization))
                .andExpect(status().isNoContent());

        verify(trainerWorkloadClient, times(1)).synchronize(
                org.mockito.ArgumentMatchers.argThat(request ->
                        request.action() == TrainerWorkloadRequest.WorkloadAction.ADD));
        verify(trainerWorkloadClient, times(1)).synchronize(
                org.mockito.ArgumentMatchers.argThat(request ->
                        request.action() == TrainerWorkloadRequest.WorkloadAction.DELETE));

        assertThat(countTrainings(trainingName)).isZero();
        mockMvc.perform(delete("/api/v1/trainees/{username}", trainee.username())
                        .header(HttpHeaders.AUTHORIZATION, traineeAuthorization))
                .andExpect(status().isOk());

        verify(traineeDeletionReportClient, times(1)).report(
                org.mockito.ArgumentMatchers.argThat(request ->
                        request.traineeUsername().equals(trainee.username())));

        assertThat(countTrainees(trainee.username())).isZero();
        assertThat(countTrainings(trainingName)).isZero();
        assertThat(countUsers(trainer.username())).isEqualTo(1);
    }

    @Test
    void thirdFailedLoginLocksTheUserAndLogoutRevokesTheJwt() throws Exception {
        String suffix = uniqueSuffix();
        RegistrationResponse trainee = registerTrainee("Secure" + suffix, "Trainee");

        for (int attempt = 0; attempt < 3; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("username", trainee.username(), "password", "wrong-password"))))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", trainee.username(), "password", trainee.password()))))
                .andExpect(status().isLocked());

        RegistrationResponse logoutUser = registerTrainee("Logout" + suffix, "Trainee");
        String authorization = bearerAuth(logoutUser);
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/trainees/{username}", logoutUser.username())
                        .header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void inactiveUsersCannotLoginWhileActiveUsersCan() throws Exception {
        String suffix = uniqueSuffix();
        RegistrationResponse trainee = registerTrainee("Status" + suffix, "Trainee");

        String activeAuthorization = bearerAuth(trainee);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", trainee.username(), "password", trainee.password()))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/trainees/{username}/status", trainee.username())
                        .header(HttpHeaders.AUTHORIZATION, activeAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("active", false))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", trainee.username(), "password", trainee.password()))))
                .andExpect(status().isUnauthorized());
    }

    private RegistrationResponse registerTrainee(String firstName, String lastName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/trainees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "firstName", firstName,
                                "lastName", lastName,
                                "dateOfBirth", "2000-01-01",
                                "address", "Integration test address"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsByteArray(), RegistrationResponse.class);
    }

    private RegistrationResponse registerTrainer(String firstName, String lastName, String specialization)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/trainers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "firstName", firstName,
                                "lastName", lastName,
                                "specialization", specialization))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsByteArray(), RegistrationResponse.class);
    }

    private List<String> usernames(MvcResult result) throws Exception {
        return jsonTree(result).findValuesAsText("username");
    }

    private JsonNode jsonTree(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearerAuth(RegistrationResponse registration) throws Exception {
        return bearerAuth(registration.username(), registration.password());
    }

    private String bearerAuth(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + jsonTree(result).path("accessToken").asText();
    }

    private long countTrainees(String username) {
        return queryCount("""
                select count(*)
                from trainees t
                join users u on u.id = t.user_id
                where lower(u.username) = lower(?)
                """, username);
    }

    private long countUsers(String username) {
        return queryCount("select count(*) from users where lower(username) = lower(?)", username);
    }

    private long countTrainings(String trainingName) {
        return queryCount("select count(*) from trainings where training_name = ?", trainingName);
    }

    private long queryCount(String sql, String value) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class, value);
        return result == null ? 0L : result;
    }

    private String uniqueSuffix() {
        return Long.toUnsignedString(UNIQUE_SEQUENCE.incrementAndGet(), 36);
    }
}
