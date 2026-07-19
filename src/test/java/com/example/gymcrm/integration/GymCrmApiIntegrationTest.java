package com.example.gymcrm.integration;

import com.example.gymcrm.GymCrmApplication;
import com.example.gymcrm.web.dto.RegistrationResponse;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GymCrmApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GymCrmApiIntegrationTest {
    private static final AtomicLong UNIQUE_SEQUENCE = new AtomicLong(System.nanoTime());

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataSource dataSource;

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
        assertThat(apiDocs.path("components").path("securitySchemes").path("basicAuth").path("scheme").asText())
                .isEqualTo("basic");
        assertThat(apiDocs.path("paths").path("/api/v1/trainees").path("post").has("security")).isFalse();
        assertThat(apiDocs.path("paths").path("/api/v1/trainees/{username}").path("get")
                .path("security").toString()).contains("basicAuth");

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/training-types"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE,
                        "Basic realm=\"gym-crm\", charset=\"UTF-8\""));
    }

    @Test
    void trainerAssignmentTrainingListsTrainingTypesAndHardDeleteWorkEndToEnd() throws Exception {
        String suffix = uniqueSuffix();
        RegistrationResponse trainee = registerTrainee("Runner" + suffix, "Trainee");
        RegistrationResponse trainer = registerTrainer("Coach" + suffix, "Trainer", "YOGA");
        String traineeAuthorization = basicAuth(trainee);
        String trainerAuthorization = basicAuth(trainer);

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

        mockMvc.perform(get("/api/v1/trainees/{username}/trainings", trainee.username())
                        .header(HttpHeaders.AUTHORIZATION, traineeAuthorization)
                        .queryParam("periodFrom", trainingDate.minusDays(1).toString())
                        .queryParam("periodTo", trainingDate.plusDays(1).toString())
                        .queryParam("trainerName", "Coach" + suffix)
                        .queryParam("trainingType", "YOGA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].trainingName").value(trainingName))
                .andExpect(jsonPath("$[0].trainingType").value("YOGA"))
                .andExpect(jsonPath("$[0].durationMinutes").value(55));

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

        assertThat(countTrainings(trainingName)).isEqualTo(1);
        mockMvc.perform(delete("/api/v1/trainees/{username}", trainee.username())
                        .header(HttpHeaders.AUTHORIZATION, traineeAuthorization))
                .andExpect(status().isOk());

        assertThat(countTrainees(trainee.username())).isZero();
        assertThat(countTrainings(trainingName)).isZero();
        assertThat(countUsers(trainer.username())).isEqualTo(1);
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

    private String basicAuth(RegistrationResponse registration) {
        return basicAuth(registration.username(), registration.password());
    }

    private String basicAuth(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
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
