package com.example.gymcrm.integration;

import com.example.gymcrm.config.AppConfig;
import com.example.gymcrm.web.config.WebConfig;
import com.example.gymcrm.web.dto.RegistrationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {AppConfig.class, WebConfig.class})
@TestPropertySource(properties = {
        "gym.db.url=jdbc:h2:mem:gymcrm-integration;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "gym.hibernate.ddl-auto=create-drop"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GymCrmApiIntegrationTest {
    private static final AtomicLong UNIQUE_SEQUENCE = new AtomicLong(System.nanoTime());

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private ObjectMapper objectMapper;

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpMockMvc() {
        objectMapper = JsonMapper.builder().findAndAddModules().build();
        jdbcTemplate = new JdbcTemplate(dataSource);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(new com.example.gymcrm.web.filter.TransactionIdFilter())
                .build();
    }

    @Test
    void traineeRegistrationLoginProfilePasswordAndStatusLifecycleUsesRealPersistence() throws Exception {
        String suffix = uniqueSuffix();
        RegistrationResponse trainee = registerTrainee("Lifecycle" + suffix, "Trainee");
        String oldAuthorization = basicAuth(trainee);

        mockMvc.perform(get("/api/v1/auth/login").header(HttpHeaders.AUTHORIZATION, oldAuthorization))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/trainees/{username}", trainee.username())
                        .header(HttpHeaders.AUTHORIZATION, oldAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(trainee.username()))
                .andExpect(jsonPath("$.active").value(true));

        String newPassword = "Changed-" + suffix;
        mockMvc.perform(put("/api/v1/users/{username}/password", trainee.username())
                        .header(HttpHeaders.AUTHORIZATION, oldAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("newPassword", newPassword))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/auth/login").header(HttpHeaders.AUTHORIZATION, oldAuthorization))
                .andExpect(status().isUnauthorized());

        String newAuthorization = basicAuth(trainee.username(), newPassword);
        mockMvc.perform(get("/api/v1/auth/login").header(HttpHeaders.AUTHORIZATION, newAuthorization))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/users/{username}/status", trainee.username())
                        .header(HttpHeaders.AUTHORIZATION, newAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("active", false))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/trainees/{username}", trainee.username())
                        .header(HttpHeaders.AUTHORIZATION, newAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch("/api/v1/users/{username}/status", trainee.username())
                        .header(HttpHeaders.AUTHORIZATION, newAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("active", false))))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/v1/users/{username}/status", trainee.username())
                        .header(HttpHeaders.AUTHORIZATION, newAuthorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("active", true))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/trainees/{username}", trainee.username())
                        .header(HttpHeaders.AUTHORIZATION, newAuthorization))
                .andExpect(status().isOk());

        assertThat(countUsers(trainee.username())).isZero();
        assertThat(countTrainees(trainee.username())).isZero();
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

    @Test
    void httpProtocolErrorsUsePreciseStatusesAndTransactionIds() throws Exception {
        mockMvc.perform(get("/api/v1/auth/login"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.transactionId").isNotEmpty());

        mockMvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(post("/api/v1/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));

        mockMvc.perform(post("/api/v1/trainees")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not-json"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
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
