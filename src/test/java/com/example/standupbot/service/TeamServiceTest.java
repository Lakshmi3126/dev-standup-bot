package com.example.standupbot.service;

import com.example.standupbot.dto.CreateTeamRequest;
import com.example.standupbot.dto.TeamResponse;
import com.example.standupbot.entity.Team;
import com.example.standupbot.exception.ConflictException;
import com.example.standupbot.exception.InvalidTimezoneException;
import com.example.standupbot.exception.ResourceNotFoundException;
import com.example.standupbot.repository.MemberRepository;
import com.example.standupbot.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private TeamService teamService;

    private CreateTeamRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new CreateTeamRequest();

        validRequest.setName("Development Team");
        validRequest.setTimezone("Asia/Kolkata");
        validRequest.setDeadline(LocalTime.of(10, 0));
        validRequest.setWebhookUrl("https://example.com/webhook");
        validRequest.setSlackBotToken("secret-token");
    }

    // =========================================================
    // TEST 1: INVALID TIMEZONE
    // =========================================================

    @Test
    void createTeamWithInvalidTimezoneThrowsException() {

        validRequest.setTimezone("Invalid/Timezone");

        assertThrows(
                InvalidTimezoneException.class,
                () -> teamService.createTeam(validRequest)
        );

        verify(teamRepository, never()).save(any(Team.class));
    }

    // =========================================================
    // TEST 2: DELETE TEAM WITH MEMBERS
    // =========================================================

    @Test
    void deleteTeamWithMembersThrowsConflictException() {

        Long teamId = 1L;

        Team team = new Team();
        team.setId(teamId);
        team.setName("Development Team");

        when(teamRepository.findById(teamId))
                .thenReturn(Optional.of(team));

        when(memberRepository.existsByTeamId(teamId))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> teamService.deleteTeam(teamId)
        );

        verify(teamRepository, never()).delete(any(Team.class));
    }

    // =========================================================
    // TEST 3: DELETE NONEXISTENT TEAM
    // =========================================================

    @Test
    void deleteNonexistentTeamThrowsResourceNotFoundException() {

        Long teamId = 999L;

        when(teamRepository.findById(teamId))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> teamService.deleteTeam(teamId)
        );

        verify(memberRepository, never()).existsByTeamId(anyLong());
        verify(teamRepository, never()).delete(any(Team.class));
    }

    // =========================================================
    // TEST 4: SENSITIVE DATA IS NOT RETURNED
    // =========================================================

    @Test
    void teamResponseDoesNotExposeSensitiveFields() {

        Team team = new Team();

        team.setId(1L);
        team.setName("Development Team");
        team.setTimezone("Asia/Kolkata");
        team.setDeadline(LocalTime.of(10, 0));

        team.setWebhookUrl("https://example.com/webhook");
        team.setSlackBotToken("super-secret-token");

        when(teamRepository.save(any(Team.class)))
                .thenReturn(team);

        TeamResponse response =
                teamService.createTeam(validRequest);

        assertNotNull(response);

        assertEquals(1L, response.getId());
        assertEquals("Development Team", response.getName());
        assertEquals("Asia/Kolkata", response.getTimezone());
        assertEquals(LocalTime.of(10, 0), response.getDeadline());

        /*
         * TeamResponse does not contain:
         *
         * webhookUrl
         * slackBotToken
         *
         * Therefore these sensitive values cannot be
         * returned through the API response.
         */
        assertFalse(
                response.getClass()
                        .getDeclaredFields()
                        == null
        );

        assertNull(
                findField(response, "webhookUrl")
        );

        assertNull(
                findField(response, "slackBotToken")
        );
    }

    // =========================================================
    // HELPER METHOD
    // =========================================================

    private java.lang.reflect.Field findField(
            TeamResponse response,
            String fieldName
    ) {
        try {
            return response.getClass()
                    .getDeclaredField(fieldName);
        } catch (NoSuchFieldException exception) {
            return null;
        }
    }
}