package com.example.standupbot.service;

import com.example.standupbot.dto.StandupResponse;
import com.example.standupbot.dto.SubmitStandupRequest;
import com.example.standupbot.entity.Member;
import com.example.standupbot.entity.Standup;
import com.example.standupbot.entity.Team;
import com.example.standupbot.exception.DuplicateSubmissionException;
import com.example.standupbot.exception.ResourceNotFoundException;
import com.example.standupbot.exception.TeamMemberMismatchException;
import com.example.standupbot.repository.MemberRepository;
import com.example.standupbot.repository.StandupRepository;
import com.example.standupbot.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StandupServiceTest {

    @Mock
    private StandupRepository standupRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private MemberRepository memberRepository;

    private Clock clock;
    private StandupService standupService;

    private final ZoneId teamZone = ZoneId.of("Asia/Kolkata");

    @BeforeEach
    void setUp() {
        Instant fixedInstant = Instant.parse("2026-08-31T04:00:00Z");
        clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));

        standupService = new StandupService(
                standupRepository,
                teamRepository,
                memberRepository,
                clock);
    }

    @Test
    void shouldSubmitStandupOnTime() {
        Team team = createTeam(1L, "Asia/Kolkata", LocalTime.of(10, 0));
        Member member = createMember(10L, team);

        SubmitStandupRequest request = new SubmitStandupRequest(
                10L,
                "Completed login API",
                "Work on authentication",
                "None");

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(memberRepository.findById(10L)).thenReturn(Optional.of(member));
        when(standupRepository
                .existsByTeamIdAndMemberIdAndStandupDate(
                        1L, 10L, LocalDate.of(2026, 8, 31)))
                .thenReturn(false);

        when(standupRepository.saveAndFlush(any(Standup.class)))
                .thenAnswer(invocation -> {
                    Standup standup = invocation.getArgument(0);
                    standup.setId(100L);
                    return standup;
                });

        StandupResponse response =
                standupService.submitStandup(1L, request);

        assertEquals(100L, response.id());
        assertEquals(1L, response.teamId());
        assertEquals(10L, response.memberId());
        assertEquals(
                LocalDate.of(2026, 8, 31),
                response.standupDate());
        assertEquals(Standup.Status.ON_TIME, response.status());
        assertEquals(
                Instant.parse("2026-08-31T04:00:00Z"),
                response.submittedAt());

        verify(standupRepository).saveAndFlush(any(Standup.class));
    }

    @Test
    void shouldMarkStandupLate() {
        Instant lateInstant =
                Instant.parse("2026-08-31T05:00:00Z");

        clock = Clock.fixed(lateInstant, ZoneId.of("UTC"));

        standupService = new StandupService(
                standupRepository,
                teamRepository,
                memberRepository,
                clock);

        Team team = createTeam(1L, "Asia/Kolkata", LocalTime.of(10, 0));
        Member member = createMember(10L, team);

        SubmitStandupRequest request = new SubmitStandupRequest(
                10L,
                "Completed login API",
                "Work on authentication",
                "None");

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(memberRepository.findById(10L)).thenReturn(Optional.of(member));
        when(standupRepository
                .existsByTeamIdAndMemberIdAndStandupDate(
                        1L, 10L, LocalDate.of(2026, 8, 31)))
                .thenReturn(false);

        when(standupRepository.saveAndFlush(any(Standup.class)))
                .thenAnswer(invocation -> {
                    Standup standup = invocation.getArgument(0);
                    standup.setId(101L);
                    return standup;
                });

        StandupResponse response =
                standupService.submitStandup(1L, request);

        assertEquals(Standup.Status.LATE, response.status());
        assertEquals(
                LocalDate.of(2026, 8, 31),
                response.standupDate());
    }

    @Test
    void shouldRejectDuplicateStandup() {
        Team team = createTeam(1L, "Asia/Kolkata", LocalTime.of(10, 0));
        Member member = createMember(10L, team);

        SubmitStandupRequest request = new SubmitStandupRequest(
                10L,
                "Completed login API",
                "Work on authentication",
                "None");

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(memberRepository.findById(10L)).thenReturn(Optional.of(member));
        when(standupRepository
                .existsByTeamIdAndMemberIdAndStandupDate(
                        1L, 10L, LocalDate.of(2026, 8, 31)))
                .thenReturn(true);

        assertThrows(
                DuplicateSubmissionException.class,
                () -> standupService.submitStandup(1L, request));

        verify(standupRepository, never())
                .saveAndFlush(any(Standup.class));
    }

    @Test
    void shouldRejectUnknownTeam() {
        SubmitStandupRequest request = new SubmitStandupRequest(
                10L,
                "Completed login API",
                "Work on authentication",
                "None");

        when(teamRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> standupService.submitStandup(1L, request));

        verifyNoInteractions(memberRepository);
        verifyNoInteractions(standupRepository);
    }

    @Test
    void shouldRejectUnknownMember() {
        Team team = createTeam(1L, "Asia/Kolkata", LocalTime.of(10, 0));

        SubmitStandupRequest request = new SubmitStandupRequest(
                10L,
                "Completed login API",
                "Work on authentication",
                "None");

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(memberRepository.findById(10L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> standupService.submitStandup(1L, request));

        verifyNoInteractions(standupRepository);
    }

    @Test
    void shouldRejectMemberFromDifferentTeam() {
        Team requestedTeam =
                createTeam(1L, "Asia/Kolkata", LocalTime.of(10, 0));

        Team memberTeam =
                createTeam(2L, "Asia/Kolkata", LocalTime.of(10, 0));

        Member member = createMember(10L, memberTeam);

        SubmitStandupRequest request = new SubmitStandupRequest(
                10L,
                "Completed login API",
                "Work on authentication",
                "None");

        when(teamRepository.findById(1L))
                .thenReturn(Optional.of(requestedTeam));

        when(memberRepository.findById(10L))
                .thenReturn(Optional.of(member));

        assertThrows(
                TeamMemberMismatchException.class,
                () -> standupService.submitStandup(1L, request));

        verifyNoInteractions(standupRepository);
    }

    @Test
    void shouldMarkStandupOnTimeAtExactDeadline() {
    Instant deadlineInstant =
            Instant.parse("2026-08-31T04:30:00Z");

    clock = Clock.fixed(deadlineInstant, ZoneId.of("UTC"));

    standupService = new StandupService(
            standupRepository,
            teamRepository,
            memberRepository,
            clock);

    Team team = createTeam(1L, "Asia/Kolkata", LocalTime.of(10, 0));
    Member member = createMember(10L, team);

    SubmitStandupRequest request = new SubmitStandupRequest(
            10L,
            "Completed login API",
            "Work on authentication",
            "None");

    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
    when(memberRepository.findById(10L)).thenReturn(Optional.of(member));
    when(standupRepository
            .existsByTeamIdAndMemberIdAndStandupDate(
                    1L, 10L, LocalDate.of(2026, 8, 31)))
            .thenReturn(false);

    when(standupRepository.saveAndFlush(any(Standup.class)))
            .thenAnswer(invocation -> {
                Standup standup = invocation.getArgument(0);
                standup.setId(102L);
                return standup;
            });

    StandupResponse response =
            standupService.submitStandup(1L, request);

    assertEquals(Standup.Status.ON_TIME, response.status());
    }

    @Test
    void shouldUseTeamTimezoneForStandupDate() {
    Instant instant =
            Instant.parse("2026-08-31T23:30:00Z");

    clock = Clock.fixed(instant, ZoneId.of("UTC"));

    standupService = new StandupService(
            standupRepository,
            teamRepository,
            memberRepository,
            clock);

    Team team = createTeam(1L, "Asia/Kolkata", LocalTime.of(10, 0));
    Member member = createMember(10L, team);

    SubmitStandupRequest request = new SubmitStandupRequest(
            10L,
            "Completed login API",
            "Work on authentication",
            "None");

    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
    when(memberRepository.findById(10L)).thenReturn(Optional.of(member));

    when(standupRepository
            .existsByTeamIdAndMemberIdAndStandupDate(
                    1L, 10L, LocalDate.of(2026, 9, 1)))
            .thenReturn(false);

    when(standupRepository.saveAndFlush(any(Standup.class)))
            .thenAnswer(invocation -> {
                Standup standup = invocation.getArgument(0);
                standup.setId(103L);
                return standup;
            });

    StandupResponse response =
            standupService.submitStandup(1L, request);

    assertEquals(
            LocalDate.of(2026, 9, 1),
            response.standupDate());

    assertEquals(
            Instant.parse("2026-08-31T23:30:00Z"),
            response.submittedAt());
    }

    @Test
    void shouldGetMemberStandups() {
    Team team = createTeam(1L, "Asia/Kolkata", LocalTime.of(10, 0));
    Member member = createMember(10L, team);

    Standup standup = new Standup();
    standup.setId(100L);
    standup.setTeamId(1L);
    standup.setMemberId(10L);
    standup.setStandupDate(LocalDate.of(2026, 8, 31));
    standup.setYesterday("Yesterday work");
    standup.setToday("Today work");
    standup.setBlockers("None");
    standup.setSubmittedAt(
            Instant.parse("2026-08-31T04:00:00Z"));
    standup.setStatus(Standup.Status.ON_TIME);

    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
    when(memberRepository.findById(10L))
            .thenReturn(Optional.of(member));

    when(standupRepository
            .findByTeamIdAndMemberIdOrderByStandupDateDesc(1L, 10L))
            .thenReturn(java.util.List.of(standup));

    var result = standupService.getMemberStandups(1L, 10L);

    assertEquals(1, result.size());
    assertEquals(100L, result.get(0).id());
    assertEquals(10L, result.get(0).memberId());
    }

    @Test
    void shouldGetStandupsByDateRange() {
    Team team = createTeam(1L, "Asia/Kolkata", LocalTime.of(10, 0));

    Standup standup = new Standup();
    standup.setId(100L);
    standup.setTeamId(1L);
    standup.setMemberId(10L);
    standup.setStandupDate(LocalDate.of(2026, 8, 25));
    standup.setYesterday("Yesterday work");
    standup.setToday("Today work");
    standup.setStatus(Standup.Status.ON_TIME);

    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

    when(standupRepository
            .findByTeamIdAndStandupDateBetweenOrderByStandupDateAsc(
                    1L,
                    LocalDate.of(2026, 8, 20),
                    LocalDate.of(2026, 8, 31)))
            .thenReturn(java.util.List.of(standup));

    var result = standupService.getStandupsByDateRange(
            1L,
            LocalDate.of(2026, 8, 20),
            LocalDate.of(2026, 8, 31));

    assertEquals(1, result.size());
    assertEquals(100L, result.get(0).id());
    }

    private Team createTeam(
            Long id,
            String timezone,
            LocalTime deadline) {

        Team team = new Team();
        team.setId(id);
        team.setName("Test Team");
        team.setTimezone(timezone);
        team.setDeadline(deadline);

        return team;
    }

    @Test
    void shouldGetTodayStandupsUsingTeamTimezone() {
    Instant instant =
            Instant.parse("2026-08-31T23:30:00Z");

    clock = Clock.fixed(instant, ZoneId.of("UTC"));

    standupService = new StandupService(
            standupRepository,
            teamRepository,
            memberRepository,
            clock);

    Team team = createTeam(1L, "Asia/Kolkata", LocalTime.of(10, 0));

    Standup standup = new Standup();
    standup.setId(100L);
    standup.setTeamId(1L);
    standup.setMemberId(10L);
    standup.setStandupDate(LocalDate.of(2026, 9, 1));
    standup.setStatus(Standup.Status.ON_TIME);

    when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

    when(standupRepository
            .findByTeamIdAndStandupDateOrderByStandupDateAsc(
                    1L,
                    LocalDate.of(2026, 9, 1)))
            .thenReturn(java.util.List.of(standup));

    var result = standupService.getTodayStandups(1L);

    assertEquals(1, result.size());
    assertEquals(100L, result.get(0).id());
    }

    private Member createMember(Long id, Team team) {
        Member member = new Member();
        member.setId(id);
        member.setTeam(team);
        member.setName("Test Member");
        member.setEmail("test@example.com");

        return member;
    }
}