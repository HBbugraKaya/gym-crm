package com.example.gymcrm.report.bdd;

import com.example.gymcrm.report.jms.JmsQueueProperties;
import com.example.gymcrm.report.service.TraineeReportService;
import com.example.gymcrm.report.web.dto.TraineeDeletionReportRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class ReportStepDefinitions {
    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final JmsTemplate jmsTemplate;
    private final JmsQueueProperties queues;
    private final TraineeReportService traineeReportService;
    private final ReportScenarioState state;

    public ReportStepDefinitions(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            JmsTemplate jmsTemplate,
            JmsQueueProperties queues,
            TraineeReportService traineeReportService,
            ReportScenarioState state) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.jmsTemplate = jmsTemplate;
        this.queues = queues;
        this.traineeReportService = traineeReportService;
        this.state = state;
    }

    @When("a client with role {string} records a deletion for {string}")
    public void clientRecordsDeletion(String role, String traineeUsername) throws Exception {
        perform(post("/api/v1/trainee-deletion-reports")
                .with(jwt().authorities(new SimpleGrantedAuthority(role)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(deletionJson(traineeUsername)));
    }

    @When("an unauthenticated client records a deletion for {string}")
    public void unauthenticatedRecordsDeletion(String traineeUsername) throws Exception {
        perform(post("/api/v1/trainee-deletion-reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(deletionJson(traineeUsername)));
    }

    @When("a client with role {string} lists deletion reports")
    public void clientListsDeletionReports(String role) throws Exception {
        perform(get("/api/v1/trainee-deletion-reports")
                .with(jwt().authorities(new SimpleGrantedAuthority(role))));
    }

    @When("a client with role {string} records a deletion with an empty username")
    public void clientRecordsDeletionWithEmptyUsername(String role) throws Exception {
        perform(post("/api/v1/trainee-deletion-reports")
                .with(jwt().authorities(new SimpleGrantedAuthority(role)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(deletionJson("")));
    }

    @When("a trainee deletion message arrives for {string}")
    public void traineeDeletionMessageArrives(String traineeUsername) {
        jmsTemplate.convertAndSend(
                queues.traineeDeletionReport(),
                new TraineeDeletionReportRequest(traineeUsername, "Jms", "Runner", true));
    }

    @When("an invalid trainee deletion message arrives")
    public void invalidTraineeDeletionMessageArrives() {
        jmsTemplate.convertAndSend(
                queues.traineeDeletionReport(),
                new TraineeDeletionReportRequest("", "Jms", "Runner", true));
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(Integer status) {
        assertThat(state.lastResponse().getResponse().getStatus()).isEqualTo(status);
    }

    @Then("a deletion report exists for {string}")
    public void aDeletionReportExistsFor(String traineeUsername) {
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                assertThat(traineeReportService.findAll())
                        .anyMatch(entry -> traineeUsername.equalsIgnoreCase(entry.traineeUsername())));
    }

    @Then("the message is moved to the trainee deletion report DLQ")
    public void theMessageIsMovedToTheTraineeDeletionReportDlq() {
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                assertThat(receive(queues.traineeDeletionReportDlq()))
                        .isInstanceOf(TraineeDeletionReportRequest.class));
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

    private String deletionJson(String traineeUsername) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "traineeUsername", traineeUsername,
                "traineeFirstName", "Runner",
                "traineeLastName", "One",
                "isActive", true));
    }
}
