package com.example.standupbot.controller;

import com.example.standupbot.dto.TeamResponse;
import com.example.standupbot.exception.ConflictException;
import com.example.standupbot.exception.InvalidTimezoneException;
import com.example.standupbot.exception.ResourceNotFoundException;
import com.example.standupbot.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeamControllerTest {

    private TeamService teamService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        teamService = mock(TeamService.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TeamController(teamService))
                .setControllerAdvice(new com.example.standupbot.exception.GlobalExceptionHandler())
                .build();
    }

    @Test
    void invalidTimezoneReturns400() throws Exception {

        doThrow(new InvalidTimezoneException("Invalid IANA timezone"))
                .when(teamService)
                .createTeam(any());

        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Test Team",
                                    "timezone": "Invalid/Timezone",
                                    "deadline": "09:00",
                                    "webhookUrl": "https://example.com/webhook"
                                }
                                """)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void deletingTeamWithMembersReturns409() throws Exception {

        doThrow(new ConflictException("Cannot delete team with existing members"))
                .when(teamService)
                .deleteTeam(1L);

        mockMvc.perform(delete("/api/teams/1"))
                .andExpect(status().isConflict());
    }

    @Test
    void deletingNonexistentTeamReturns404() throws Exception {

        doThrow(new ResourceNotFoundException("Team not found with id: 999"))
                .when(teamService)
                .deleteTeam(999L);

        mockMvc.perform(delete("/api/teams/999"))
                .andExpect(status().isNotFound());
    }
}