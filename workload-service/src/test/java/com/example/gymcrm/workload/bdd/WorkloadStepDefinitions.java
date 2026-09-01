package com.example.gymcrm.workload.bdd;

import com.example.gymcrm.workload.jms.JmsQueueProperties;
import com.example.gymcrm.workload.web.dto.TrainerWorkloadRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class WorkloadStepDefinitions {
    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final JmsTemplate jmsTemplate;
    private final JmsQueueProperties queues;
    private final WorkloadBddConfiguration.WorkloadDocuments documents;
    private final WorkloadScenarioState state;

    public WorkloadStepDefinitions(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            JmsTemplate jmsTemplate,
            JmsQueueProperties queues,
            WorkloadBddConfiguration.WorkloadDocuments documents,
            WorkloadScenarioState state) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.jmsTemplate = jmsTemplate;
        this.queues = queues;
        this.documents = documents;
        this.state = state;
    }

    @When("an authenticated client records an ADD of {int} minutes for trainer {string} in {word}")
    public void recordAdd(Integer minutes, String trainerUsername, String yearMonth) throws Exception {
        record(trainerUsername, minutes, TrainerWorkloadRequest.WorkloadAction.ADD, yearMonth);
    }

    @When("an authenticated client records a DELETE of {int} minutes for trainer {string} in {word}")
    public void recordDelete(Integer minutes, String trainerUsername, String yearMonth) throws Exception {
        record(trainerUsername, minutes, TrainerWorkloadRequest.WorkloadAction.DELETE, yearMonth);
    }

    @When("an authenticated client gets the monthly workload for {string} in {word}")
    public void getMonthly(String trainerUsername, String yearMonth) throws Exception {
        int[] parts = yearAndMonth(yearMonth);
        perform(get("/api/v1/trainer-workloads/{trainerUsername}", trainerUsername)
                .param("year", String.valueOf(parts[0]))
                .param("month", String.valueOf(parts[1]))
                .with(jwt()));
    }

    @When("an authenticated client gets the summary for {string}")
    public void getSummary(String trainerUsername) throws Exception {
        perform(get("/api/v1/trainer-workloads/{trainerUsername}/summary", trainerUsername)
                .with(jwt()));
    }

    @When("an unauthenticated client gets {string}")
    public void unauthenticatedGet(String path) throws Exception {
        perform(get(path));
    }

    @When("a workload ADD message of {int} minutes arrives for trainer {string} on {word}")
    public void workloadAddMessageArrives(Integer minutes, String trainerUsername, String date) {
        jmsTemplate.convertAndSend(queues.trainerWorkload(), new TrainerWorkloadRequest(
                trainerUsername,
                "Queue",
                "Coach",
                true,
                LocalDate.parse(date),
                minutes,
                TrainerWorkloadRequest.WorkloadAction.ADD));
    }

    @When("an invalid workload message arrives")
    public void invalidWorkloadMessageArrives() {
        jmsTemplate.convertAndSend(queues.trainerWorkload(), new TrainerWorkloadRequest(
                "",
                "Queue",
                "Coach",
                true,
                LocalDate.of(2026, 8, 9),
                30,
                TrainerWorkloadRequest.WorkloadAction.ADD));
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(Integer status) {
        assertThat(state.lastResponse().getResponse().getStatus()).isEqualTo(status);
    }

    @Then("the monthly duration is {int}")
    public void theMonthlyDurationIs(Integer duration) throws Exception {
        assertThat(objectMapper.readTree(state.lastResponse().getResponse().getContentAsByteArray())
                .path("trainingDurationMinutes").asInt()).isEqualTo(duration);
    }

    @Then("the monthly duration for {string} in {word} becomes {int}")
    public void theMonthlyDurationBecomes(String trainerUsername, String yearMonth, Integer duration) {
        int[] parts = yearAndMonth(yearMonth);
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                assertThat(documents.find(trainerUsername))
                        .isPresent()
                        .get()
                        .extracting(workload -> workload.durationFor(parts[0], parts[1]))
                        .isEqualTo(duration));
    }

    @Then("the message is moved to the trainer workload DLQ")
    public void theMessageIsMovedToTheTrainerWorkloadDlq() {
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                assertThat(receive(queues.trainerWorkloadDlq())).isInstanceOf(TrainerWorkloadRequest.class));
    }

    private void record(
            String trainerUsername,
            int minutes,
            TrainerWorkloadRequest.WorkloadAction action,
            String yearMonth) throws Exception {
        int[] parts = yearAndMonth(yearMonth);
        LocalDate trainingDate = LocalDate.of(parts[0], Math.min(Math.max(parts[1], 1), 12), 1);
        perform(post("/api/v1/trainer-workloads")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "trainerUsername", trainerUsername,
                        "trainerFirstName", "Coach",
                        "trainerLastName", "One",
                        "isActive", true,
                        "trainingDate", trainingDate.toString(),
                        "trainingDurationMinutes", minutes,
                        "actionType", action.name()))));
    }

    private static int[] yearAndMonth(String yearMonth) {
        String[] parts = yearMonth.split("-");
        return new int[] {Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }

    private MvcResult perform(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(request).andReturn();
        state.setLastResponse(result);
        return result;
    }

    private Object receive(String destination) {
        long previousTimeout = jmsTemplate.getReceiveTimeout();
        jmsTemplate.setReceiveTimeout(200);
        try {
            return jmsTemplate.receiveAndConvert(destination);
        } finally {
            jmsTemplate.setReceiveTimeout(previousTimeout);
        }
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
