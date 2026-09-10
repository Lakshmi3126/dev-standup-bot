package com.example.standupbot.controller;

import com.example.standupbot.dto.BlockerResponse;
import com.example.standupbot.entity.Blocker;
import com.example.standupbot.service.BlockerService;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BlockerController.class)
class BlockerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BlockerService blockerService;

    @TestConfiguration
    static class TestConfig {

        @Bean
        BlockerService blockerService() {
            return Mockito.mock(BlockerService.class);
        }
    }

    @Test
    void shouldGetTeamBlockers() throws Exception {

        BlockerResponse response = new BlockerResponse(
                100L,
                10L,
                50L,
                "Login API is blocked",
                Instant.parse("2026-09-02T04:00:00Z"),
                Instant.parse("2026-09-04T04:00:00Z"),
                3,
                Blocker.Status.UNRESOLVED
        );

        when(blockerService.getTeamBlockers(1L))
                .thenReturn(List.of(response));

        mockMvc.perform(
                get("/api/teams/1/blockers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].memberId").value(10))
                .andExpect(jsonPath("$[0].description")
                        .value("Login API is blocked"))
                .andExpect(jsonPath("$[0].consecutiveDays").value(3))
                .andExpect(jsonPath("$[0].status")
                        .value("UNRESOLVED"));
    }

    @Test
    void shouldGetMemberBlockers() throws Exception {

        BlockerResponse response = new BlockerResponse(
                101L,
                10L,
                51L,
                "Database connection blocked",
                Instant.parse("2026-09-04T04:00:00Z"),
                Instant.parse("2026-09-04T04:00:00Z"),
                1,
                Blocker.Status.ACTIVE
        );

        when(blockerService.getMemberBlockers(1L, 10L))
                .thenReturn(List.of(response));

        mockMvc.perform(
                get("/api/teams/1/members/10/blockers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(101))
                .andExpect(jsonPath("$[0].memberId").value(10))
                .andExpect(jsonPath("$[0].status")
                        .value("ACTIVE"));
    }
}