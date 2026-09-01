package com.example.gymcrm.bdd;

import com.example.gymcrm.integration.jms.JmsQueueProperties;
import com.example.gymcrm.web.dto.TraineeDeletionReportRequest;
import com.example.gymcrm.web.dto.TrainerWorkloadRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class GymCrmStepDefinitions {
    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final JmsTemplate jmsTemplate;
    private final JmsQueueProperties queues;
    private final ScenarioState state;

    public GymCrmStepDefinitions(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            JmsTemplate jmsTemplate,
            JmsQueueProperties queues,
            ScenarioState state) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.jmsTemplate = jmsTemplate;
        this.queues = queues;
        this.state = state;
    }

    @Given("a registered trainee")
    public void aRegisteredTrainee() throws Exception {
        state.setTrainee(registerTrainee("Ann" + state.suffix(), "Trainee"));
    }

    @Given("two registered trainees")
    public void twoRegisteredTrainees() throws Exception {
        state.setTrainee(registerTrainee("First" + state.suffix(), "Trainee"));
        state.setSecondTrainee(registerTrainee("Second" + state.suffix(), "Trainee"));
    }

    @Given("a registered trainer")
    public void aRegisteredTrainer() throws Exception {
        MvcResult result = perform(post("/api/v1/trainers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "firstName", "Coach" + state.suffix(),
                        "lastName", "Trainer",
                        "specialization", "YOGA"))));
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        JsonNode body = jsonTree(result);
        state.setTrainer(new ScenarioState.Account(body.path("username").asText(), body.path("password").asText()));
    }

    @Given("the trainee is deactivated")
    public void theTraineeIsDeactivated() throws Exception {
        String authorization = login(state.trainee());
        perform(patch("/api/v1/trainees/{username}/status", state.trainee().username())
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("active", false))));
    }

    @When("the trainee logs in with the correct password")
    public void theTraineeLogsInWithTheCorrectPassword() throws Exception {
        loginRaw(state.trainee().username(), state.trainee().password());
    }

    @When("the trainee logs in with password {string}")
    public void theTraineeLogsInWithPassword(String password) throws Exception {
        loginRaw(state.trainee().username(), password);
    }

    @When("the trainee logs in with an empty password")
    public void theTraineeLogsInWithAnEmptyPassword() throws Exception {
        loginRaw(state.trainee().username(), "");
    }

    @When("an unauthenticated client gets {string}")
    public void anUnauthenticatedClientGets(String path) throws Exception {
        perform(get(path));
    }

    @When("the first trainee requests the second trainee profile")
    public void theFirstTraineeRequestsTheSecondTraineeProfile() throws Exception {
        perform(get("/api/v1/trainees/{username}", state.secondTrainee().username())
                .header(HttpHeaders.AUTHORIZATION, login(state.trainee())));
    }

    @When("the trainee adds a training")
    public void theTraineeAddsATraining() throws Exception {
        addTraining(login(state.trainee()), state.trainee().username(), 45);
    }

    @When("the trainer adds a training")
    public void theTrainerAddsATraining() throws Exception {
        addTraining(login(state.trainer()), state.trainee().username(), 45);
    }

    @When("the trainer adds a training for trainee {string}")
    public void theTrainerAddsATrainingForTrainee(String traineeUsername) throws Exception {
        addTraining(login(state.trainer()), traineeUsername, 45);
    }

    @When("the trainer adds a training lasting {int} minutes")
    public void theTrainerAddsATrainingLastingMinutes(int minutes) throws Exception {
        addTraining(login(state.trainer()), state.trainee().username(), minutes);
    }

    @When("the trainer cancels training {int}")
    public void theTrainerCancelsTraining(Integer trainingId) throws Exception {
        perform(delete("/api/v1/trainings/{trainingId}", trainingId)
                .header(HttpHeaders.AUTHORIZATION, login(state.trainer())));
    }

    @When("the trainer cancels the last training")
    public void theTrainerCancelsTheLastTraining() throws Exception {
        String authorization = login(state.trainer());
        MvcResult list = perform(get("/api/v1/trainers/{username}/trainings", state.trainer().username())
                .header(HttpHeaders.AUTHORIZATION, authorization));
        long trainingId = jsonTree(list).get(0).path("trainingId").asLong();
        state.setLastTrainingId(trainingId);
        perform(delete("/api/v1/trainings/{trainingId}", trainingId)
                .header(HttpHeaders.AUTHORIZATION, authorization));
    }

    @When("the trainee deletes their profile")
    public void theTraineeDeletesTheirProfile() throws Exception {
        perform(delete("/api/v1/trainees/{username}", state.trainee().username())
                .header(HttpHeaders.AUTHORIZATION, login(state.trainee())));
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(Integer status) {
        assertThat(state.lastResponse().getResponse().getStatus()).isEqualTo(status);
    }

    @Then("the response contains an access token")
    public void theResponseContainsAnAccessToken() throws Exception {
        assertThat(jsonTree(state.lastResponse()).path("accessToken").asText()).isNotBlank();
        assertThat(jsonTree(state.lastResponse()).path("tokenType").asText()).isEqualTo("Bearer");
    }

    @Then("a workload event with action {string} is published for the trainer")
    public void aWorkloadEventWithActionIsPublishedForTheTrainer(String action) {
        TrainerWorkloadRequest event = receive(queues.trainerWorkload(), TrainerWorkloadRequest.class);
        if ("DELETE".equals(action) && event.action() == TrainerWorkloadRequest.WorkloadAction.ADD) {
            event = receive(queues.trainerWorkload(), TrainerWorkloadRequest.class);
        }
        assertThat(event.action().name()).isEqualTo(action);
        assertThat(event.trainerUsername()).isEqualToIgnoringCase(state.trainer().username());
    }

    @Then("a trainee deletion report is published for the trainee")
    public void aTraineeDeletionReportIsPublishedForTheTrainee() {
        TraineeDeletionReportRequest event = receive(
                queues.traineeDeletionReport(),
                TraineeDeletionReportRequest.class);
        assertThat(event.traineeUsername()).isEqualToIgnoringCase(state.trainee().username());
    }

    private ScenarioState.Account registerTrainee(String firstName, String lastName) throws Exception {
        MvcResult result = perform(post("/api/v1/trainees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "firstName", firstName,
                        "lastName", lastName,
                        "dateOfBirth", "2000-01-01",
                        "address", "Cucumber address"))));
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        JsonNode body = jsonTree(result);
        return new ScenarioState.Account(body.path("username").asText(), body.path("password").asText());
    }

    private void addTraining(String authorization, String traineeUsername, int minutes) throws Exception {
        perform(post("/api/v1/trainings")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "traineeUsername", traineeUsername,
                        "trainerUsername", state.trainer().username(),
                        "trainingName", "Cucumber Yoga " + state.suffix(),
                        "trainingDate", "2026-08-09",
                        "durationMinutes", minutes))));
    }

    private String login(ScenarioState.Account account) throws Exception {
        MvcResult previous = state.lastResponse();
        loginRaw(account.username(), account.password());
        assertThat(state.lastResponse().getResponse().getStatus()).isEqualTo(200);
        String token = "Bearer " + jsonTree(state.lastResponse()).path("accessToken").asText();
        state.setLastResponse(previous);
        return token;
    }

    private void loginRaw(String username, String password) throws Exception {
        perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("username", username, "password", password))));
    }

    private MvcResult perform(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(request).andReturn();
        state.setLastResponse(result);
        return result;
    }

    private <T> T receive(String destination, Class<T> type) {
        long previousTimeout = jmsTemplate.getReceiveTimeout();
        jmsTemplate.setReceiveTimeout(3000);
        try {
            Object payload = jmsTemplate.receiveAndConvert(destination);
            assertThat(payload).isInstanceOf(type);
            return type.cast(payload);
        } finally {
            jmsTemplate.setReceiveTimeout(previousTimeout);
        }
    }

    private JsonNode jsonTree(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
