package com.example.gymcrm.report.web;

import com.example.gymcrm.report.service.TraineeReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class ReportSecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TraineeReportService traineeReportService;

    @Test
    void traineeRoleCanRecordDeletion() throws Exception {
        mockMvc.perform(post("/api/v1/trainee-deletion-reports")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRAINEE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "traineeUsername": "runner.one",
                                  "traineeFirstName": "Runner",
                                  "traineeLastName": "One",
                                  "isActive": true
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void trainerRoleCannotRecordDeletion() throws Exception {
        mockMvc.perform(post("/api/v1/trainee-deletion-reports")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRAINER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "traineeUsername": "runner.one",
                                  "traineeFirstName": "Runner",
                                  "traineeLastName": "One",
                                  "isActive": true
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedApplicationRolesCannotReadDeletionReports() throws Exception {
        mockMvc.perform(get("/api/v1/trainee-deletion-reports")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRAINEE"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/trainee-deletion-reports")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRAINER"))))
                .andExpect(status().isForbidden());
    }
}
