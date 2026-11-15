
package com.example.standupbot.controller;

import com.example.standupbot.entity.Leave;
import com.example.standupbot.entity.Member;
import com.example.standupbot.entity.Team;
import com.example.standupbot.service.LeaveService;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaveController.class)
class LeaveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LeaveService leaveService;

    @TestConfiguration
    static class TestConfig {

        @Bean
        LeaveService leaveService() {
            return Mockito.mock(LeaveService.class);
        }
    }

    private Leave createLeave(boolean cancelled) {

        Team team = new Team();
        team.setId(1L);

        Member member = new Member();
        member.setId(10L);
        member.setTeam(team);

        Leave leave = new Leave();
        leave.setId(100L);
        leave.setMember(member);
        leave.setStartDate(LocalDate.of(2026, 11, 16));
        leave.setEndDate(LocalDate.of(2026, 11, 18));
        leave.setCancelled(cancelled);

        return leave;
    }

    @Test
    void shouldAddLeave() throws Exception {

        when(leaveService.addLeave(
                eq(1L),
                eq(10L),
                any(LocalDate.class),
                any(LocalDate.class)))
                .thenReturn(createLeave(false));

        mockMvc.perform(
                post("/api/teams/1/members/10/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "2026-11-16",
                                  "endDate": "2026-11-18"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.memberId").value(10))
                .andExpect(jsonPath("$.startDate").value("2026-11-16"))
                .andExpect(jsonPath("$.endDate").value("2026-11-18"))
                .andExpect(jsonPath("$.cancelled").value(false));
    }

    @Test
    void shouldRejectInvalidLeaveRequest() throws Exception {

        mockMvc.perform(
                post("/api/teams/1/members/10/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": null,
                                  "endDate": null
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetMemberLeaveHistory() throws Exception {

        when(leaveService.getMemberLeaveHistory(1L, 10L))
                .thenReturn(List.of(createLeave(false)));

        mockMvc.perform(
                get("/api/teams/1/members/10/leaves"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].memberId").value(10));
    }

    @Test
    void shouldUpdateLeave() throws Exception {

        when(leaveService.updateLeave(
                eq(1L),
                eq(10L),
                eq(100L),
                any(LocalDate.class),
                any(LocalDate.class)))
                .thenReturn(createLeave(false));

        mockMvc.perform(
                put("/api/teams/1/members/10/leaves/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDate": "2026-11-16",
                                  "endDate": "2026-11-18"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.cancelled").value(false));
    }

    @Test
    void shouldCancelLeave() throws Exception {

        when(leaveService.cancelLeave(1L, 10L, 100L))
                .thenReturn(createLeave(true));

        mockMvc.perform(
                delete("/api/teams/1/members/10/leaves/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.cancelled").value(true));
    }
}
