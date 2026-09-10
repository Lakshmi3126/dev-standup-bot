package com.example.standupbot.controller;

import com.example.standupbot.dto.StandupResponse;
import com.example.standupbot.dto.SubmitStandupRequest;
import com.example.standupbot.entity.Standup;
import com.example.standupbot.service.StandupService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StandupController.class)
class StandupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StandupService standupService;

    @TestConfiguration
    static class TestConfig {

        @Bean
        StandupService standupService() {
            return Mockito.mock(StandupService.class);
        }
    }

    @Test
    void shouldSubmitStandup() throws Exception {

        StandupResponse response = new StandupResponse(
                100L,
                1L,
                10L,
                LocalDate.of(2026, 9, 4),
                "Completed login API",
                "Work on authentication",
                "None",
                Instant.parse("2026-09-04T04:00:00Z"),
                Standup.Status.ON_TIME
        );

        when(standupService.submitStandup(
                eq(1L),
                any(SubmitStandupRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                post("/api/teams/1/standups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": 10,
                                  "yesterday": "Completed login API",
                                  "today": "Work on authentication",
                                  "blockers": "None"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.teamId").value(1))
                .andExpect(jsonPath("$.memberId").value(10))
                .andExpect(jsonPath("$.standupDate").value("2026-09-04"))
                .andExpect(jsonPath("$.status").value("ON_TIME"));
    }

    @Test
    void shouldRejectInvalidSubmitRequest() throws Exception {

        mockMvc.perform(
                post("/api/teams/1/standups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": 10,
                                  "yesterday": "",
                                  "today": "",
                                  "blockers": "None"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetMemberStandups() throws Exception {

        StandupResponse response = new StandupResponse(
                100L,
                1L,
                10L,
                LocalDate.of(2026, 9, 4),
                "Yesterday work",
                "Today work",
                "None",
                Instant.parse("2026-09-04T04:00:00Z"),
                Standup.Status.ON_TIME
        );

        when(standupService.getMemberStandups(1L, 10L))
                .thenReturn(List.of(response));

        mockMvc.perform(
                get("/api/teams/1/members/10/standups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].memberId").value(10));
    }

    @Test
    void shouldGetStandupsByDateRange() throws Exception {

        StandupResponse response = new StandupResponse(
                100L,
                1L,
                10L,
                LocalDate.of(2026, 9, 4),
                "Yesterday work",
                "Today work",
                "None",
                Instant.parse("2026-09-04T04:00:00Z"),
                Standup.Status.ON_TIME
        );

        when(standupService.getStandupsByDateRange(
                1L,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 4)))
                .thenReturn(List.of(response));

        mockMvc.perform(
                get("/api/teams/1/standups")
                        .param("from", "2026-09-01")
                        .param("to", "2026-09-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(100));
    }

    @Test
    void shouldGetTodayStandups() throws Exception {

        StandupResponse response = new StandupResponse(
                100L,
                1L,
                10L,
                LocalDate.of(2026, 9, 4),
                "Yesterday work",
                "Today work",
                "None",
                Instant.parse("2026-09-04T04:00:00Z"),
                Standup.Status.ON_TIME
        );

        when(standupService.getTodayStandups(1L))
                .thenReturn(List.of(response));

        mockMvc.perform(
                get("/api/teams/1/standups/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(100));
    }
}