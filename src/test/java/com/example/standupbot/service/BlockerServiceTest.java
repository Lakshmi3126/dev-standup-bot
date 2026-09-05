package com.example.standupbot.service;

import com.example.standupbot.entity.Blocker;
import com.example.standupbot.entity.Standup;
import com.example.standupbot.repository.BlockerRepository;
import com.example.standupbot.repository.StandupRepository;
import com.example.standupbot.repository.MemberRepository;
import com.example.standupbot.entity.Member;
import com.example.standupbot.dto.BlockerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlockerServiceTest {

    @Mock
    private BlockerRepository blockerRepository;

    @Mock
    private StandupRepository standupRepository;

    private Clock clock;
    private BlockerService blockerService;

    @Mock
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(
                Instant.parse("2026-08-31T04:00:00Z"),
                java.time.ZoneOffset.UTC);

        blockerService = new BlockerService(
                blockerRepository,
                standupRepository,
                memberRepository,
                clock);
    }

    @Test
    void shouldCreateFirstBlocker() {
        Standup standup = new Standup();
        standup.setId(100L);
        standup.setTeamId(1L);
        standup.setMemberId(10L);
        standup.setStandupDate(LocalDate.of(2026, 8, 31));
        standup.setBlockers("API is broken");
        standup.setSubmittedAt(
                Instant.parse("2026-08-31T04:00:00Z"));

        when(standupRepository
                .findTopByTeamIdAndMemberIdAndStandupDateLessThanOrderByStandupDateDesc(
                        1L, 10L, LocalDate.of(2026, 8, 31)))
                .thenReturn(null);

        when(blockerRepository
                .findTopByMemberIdOrderByLastReportedAtDesc(10L))
                .thenReturn(Optional.empty());

        blockerService.processStandup(standup);

        ArgumentCaptor<Blocker> captor =
                ArgumentCaptor.forClass(Blocker.class);

        verify(blockerRepository).save(captor.capture());

        Blocker saved = captor.getValue();

        assertEquals(10L, saved.getMemberId());
        assertEquals(100L, saved.getStandupId());
        assertEquals("API is broken", saved.getDescription());
        assertEquals(1, saved.getConsecutiveDays());
        assertEquals(Blocker.Status.ACTIVE, saved.getStatus());
    }
    @Test
    void shouldIncrementStreakForSameBlockerOnNextWorkingDay() {
        Standup previousStandup = new Standup();
        previousStandup.setId(100L);
        previousStandup.setTeamId(1L);
        previousStandup.setMemberId(10L);
        previousStandup.setStandupDate(LocalDate.of(2026, 8, 31)); // Monday
        previousStandup.setBlockers("API is broken");

        Standup currentStandup = new Standup();
        currentStandup.setId(101L);
        currentStandup.setTeamId(1L);
        currentStandup.setMemberId(10L);
        currentStandup.setStandupDate(LocalDate.of(2026, 9, 1)); // Tuesday
        currentStandup.setBlockers("api is broken"); // different case
        currentStandup.setSubmittedAt(
                Instant.parse("2026-09-01T04:00:00Z"));

        Blocker existingBlocker = new Blocker();
        existingBlocker.setMemberId(10L);
        existingBlocker.setStandupId(100L);
        existingBlocker.setDescription("API is broken");
        existingBlocker.setConsecutiveDays(1);
        existingBlocker.setStatus(Blocker.Status.ACTIVE);

        when(standupRepository
                .findTopByTeamIdAndMemberIdAndStandupDateLessThanOrderByStandupDateDesc(
                        1L, 10L, LocalDate.of(2026, 9, 1)))
                .thenReturn(previousStandup);

        when(blockerRepository
                .findTopByMemberIdOrderByLastReportedAtDesc(10L))
                .thenReturn(Optional.of(existingBlocker));

        blockerService.processStandup(currentStandup);

        verify(blockerRepository).save(existingBlocker);

        assertEquals(2, existingBlocker.getConsecutiveDays());
        assertEquals(Blocker.Status.ACTIVE, existingBlocker.getStatus());
        assertEquals("api is broken", existingBlocker.getDescription());
        assertEquals(101L, existingBlocker.getStandupId());
        }
        @Test
    void shouldResetStreakWhenBlockerChanges() {
        Standup previousStandup = new Standup();
        previousStandup.setId(100L);
        previousStandup.setTeamId(1L);
        previousStandup.setMemberId(10L);
        previousStandup.setStandupDate(LocalDate.of(2026, 8, 31));
        previousStandup.setBlockers("API is broken");

        Standup currentStandup = new Standup();
        currentStandup.setId(101L);
        currentStandup.setTeamId(1L);
        currentStandup.setMemberId(10L);
        currentStandup.setStandupDate(LocalDate.of(2026, 9, 1));
        currentStandup.setBlockers("Database is down");
        currentStandup.setSubmittedAt(
                Instant.parse("2026-09-01T04:00:00Z"));

        Blocker existingBlocker = new Blocker();
        existingBlocker.setMemberId(10L);
        existingBlocker.setStandupId(100L);
        existingBlocker.setDescription("API is broken");
        existingBlocker.setConsecutiveDays(2);
        existingBlocker.setStatus(Blocker.Status.ACTIVE);

        when(standupRepository
                .findTopByTeamIdAndMemberIdAndStandupDateLessThanOrderByStandupDateDesc(
                        1L, 10L, LocalDate.of(2026, 9, 1)))
                .thenReturn(previousStandup);

        when(blockerRepository
                .findTopByMemberIdOrderByLastReportedAtDesc(10L))
                .thenReturn(Optional.of(existingBlocker));

        blockerService.processStandup(currentStandup);

        verify(blockerRepository).save(existingBlocker);

        assertEquals(1, existingBlocker.getConsecutiveDays());
        assertEquals(Blocker.Status.ACTIVE, existingBlocker.getStatus());
        assertEquals("Database is down", existingBlocker.getDescription());
        assertEquals(101L, existingBlocker.getStandupId());
    }
    @Test
    void shouldMarkBlockerUnresolvedAfterThreeConsecutiveDays() {
        Standup previousStandup = new Standup();
        previousStandup.setId(101L);
        previousStandup.setTeamId(1L);
        previousStandup.setMemberId(10L);
        previousStandup.setStandupDate(LocalDate.of(2026, 9, 1));
        previousStandup.setBlockers("API is broken");

        Standup currentStandup = new Standup();
        currentStandup.setId(102L);
        currentStandup.setTeamId(1L);
        currentStandup.setMemberId(10L);
        currentStandup.setStandupDate(LocalDate.of(2026, 9, 2));
        currentStandup.setBlockers("api is broken");
        currentStandup.setSubmittedAt(
                Instant.parse("2026-09-02T04:00:00Z"));

        Blocker existingBlocker = new Blocker();
        existingBlocker.setMemberId(10L);
        existingBlocker.setStandupId(101L);
        existingBlocker.setDescription("API is broken");
        existingBlocker.setConsecutiveDays(2);
        existingBlocker.setStatus(Blocker.Status.ACTIVE);

        when(standupRepository
                .findTopByTeamIdAndMemberIdAndStandupDateLessThanOrderByStandupDateDesc(
                        1L, 10L, LocalDate.of(2026, 9, 2)))
                .thenReturn(previousStandup);

        when(blockerRepository
                .findTopByMemberIdOrderByLastReportedAtDesc(10L))
                .thenReturn(Optional.of(existingBlocker));

        blockerService.processStandup(currentStandup);

        verify(blockerRepository).save(existingBlocker);

        assertEquals(3, existingBlocker.getConsecutiveDays());
        assertEquals(Blocker.Status.UNRESOLVED, existingBlocker.getStatus());
        assertEquals("api is broken", existingBlocker.getDescription());
        assertEquals(102L, existingBlocker.getStandupId());
    }
    @Test
    void shouldContinueStreakFromFridayToMonday() {
        Standup previousStandup = new Standup();
        previousStandup.setId(100L);
        previousStandup.setTeamId(1L);
        previousStandup.setMemberId(10L);
        previousStandup.setStandupDate(LocalDate.of(2026, 9, 4)); // Friday
        previousStandup.setBlockers("API is broken");

        Standup currentStandup = new Standup();
        currentStandup.setId(101L);
        currentStandup.setTeamId(1L);
        currentStandup.setMemberId(10L);
        currentStandup.setStandupDate(LocalDate.of(2026, 9, 7)); // Monday
        currentStandup.setBlockers("API is broken");
        currentStandup.setSubmittedAt(
                Instant.parse("2026-09-07T04:00:00Z"));

        Blocker existingBlocker = new Blocker();
        existingBlocker.setMemberId(10L);
        existingBlocker.setStandupId(100L);
        existingBlocker.setDescription("API is broken");
        existingBlocker.setConsecutiveDays(1);
        existingBlocker.setStatus(Blocker.Status.ACTIVE);

        when(standupRepository
                .findTopByTeamIdAndMemberIdAndStandupDateLessThanOrderByStandupDateDesc(
                        1L, 10L, LocalDate.of(2026, 9, 7)))
                .thenReturn(previousStandup);

        when(blockerRepository
                .findTopByMemberIdOrderByLastReportedAtDesc(10L))
                .thenReturn(Optional.of(existingBlocker));

        blockerService.processStandup(currentStandup);

        verify(blockerRepository).save(existingBlocker);

        assertEquals(2, existingBlocker.getConsecutiveDays());
        assertEquals(Blocker.Status.ACTIVE, existingBlocker.getStatus());
    }
    @Test
    void shouldResetStreakWhenAWorkingDayIsMissed() {
        Standup previousStandup = new Standup();
        previousStandup.setId(100L);
        previousStandup.setTeamId(1L);
        previousStandup.setMemberId(10L);
        previousStandup.setStandupDate(LocalDate.of(2026, 8, 31)); // Monday
        previousStandup.setBlockers("API is broken");

        Standup currentStandup = new Standup();
        currentStandup.setId(101L);
        currentStandup.setTeamId(1L);
        currentStandup.setMemberId(10L);
        currentStandup.setStandupDate(LocalDate.of(2026, 9, 2)); // Wednesday
        currentStandup.setBlockers("API is broken");
        currentStandup.setSubmittedAt(
                Instant.parse("2026-09-02T04:00:00Z"));

        Blocker existingBlocker = new Blocker();
        existingBlocker.setMemberId(10L);
        existingBlocker.setStandupId(100L);
        existingBlocker.setDescription("API is broken");
        existingBlocker.setConsecutiveDays(2);
        existingBlocker.setStatus(Blocker.Status.ACTIVE);

        when(standupRepository
                .findTopByTeamIdAndMemberIdAndStandupDateLessThanOrderByStandupDateDesc(
                        1L, 10L, LocalDate.of(2026, 9, 2)))
                .thenReturn(previousStandup);

        when(blockerRepository
                .findTopByMemberIdOrderByLastReportedAtDesc(10L))
                .thenReturn(Optional.of(existingBlocker));

        blockerService.processStandup(currentStandup);

        verify(blockerRepository).save(existingBlocker);

        assertEquals(1, existingBlocker.getConsecutiveDays());
        assertEquals(Blocker.Status.ACTIVE, existingBlocker.getStatus());
    }
    @Test
    void shouldGetMemberBlockers() {

        Member member = new Member();
        member.setId(10L);

        Blocker blocker = new Blocker();
        blocker.setId(100L);
        blocker.setMemberId(10L);
        blocker.setStandupId(50L);
        blocker.setDescription("API is broken");
        blocker.setFirstReportedAt(
                Instant.parse("2026-09-01T04:00:00Z"));
        blocker.setLastReportedAt(
                Instant.parse("2026-09-03T04:00:00Z"));
        blocker.setConsecutiveDays(3);
        blocker.setStatus(Blocker.Status.UNRESOLVED);

        when(memberRepository.findByIdAndTeamId(10L, 1L))
                .thenReturn(Optional.of(member));

        when(blockerRepository.findByMemberIdOrderByLastReportedAtDesc(10L))
                .thenReturn(List.of(blocker));

        List<com.example.standupbot.dto.BlockerResponse> result =
                blockerService.getMemberBlockers(1L, 10L);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).id());
        assertEquals(10L, result.get(0).memberId());
        assertEquals("API is broken", result.get(0).description());
        assertEquals(3, result.get(0).consecutiveDays());
        assertEquals(Blocker.Status.UNRESOLVED, result.get(0).status());
    }
    
    @Test
    void shouldGetTeamBlockers() {

        Member member1 = new Member();
        member1.setId(10L);

        Member member2 = new Member();
        member2.setId(20L);

        Blocker blocker1 = new Blocker();
        blocker1.setId(100L);
        blocker1.setMemberId(10L);
        blocker1.setStandupId(50L);
        blocker1.setDescription("API is broken");
        blocker1.setFirstReportedAt(
                Instant.parse("2026-09-01T04:00:00Z"));
        blocker1.setLastReportedAt(
                Instant.parse("2026-09-03T04:00:00Z"));
        blocker1.setConsecutiveDays(3);
        blocker1.setStatus(Blocker.Status.UNRESOLVED);

        Blocker blocker2 = new Blocker();
        blocker2.setId(101L);
        blocker2.setMemberId(20L);
        blocker2.setStandupId(51L);
        blocker2.setDescription("Database is down");
        blocker2.setFirstReportedAt(
                Instant.parse("2026-09-03T04:00:00Z"));
        blocker2.setLastReportedAt(
                Instant.parse("2026-09-03T04:00:00Z"));
        blocker2.setConsecutiveDays(1);
        blocker2.setStatus(Blocker.Status.ACTIVE);

        when(memberRepository.findByTeamId(1L))
                .thenReturn(List.of(member1, member2));

        when(blockerRepository.findByMemberIdOrderByLastReportedAtDesc(10L))
                .thenReturn(List.of(blocker1));

        when(blockerRepository.findByMemberIdOrderByLastReportedAtDesc(20L))
                .thenReturn(List.of(blocker2));

        List<BlockerResponse> result =
                blockerService.getTeamBlockers(1L);

        assertEquals(2, result.size());
        assertEquals(100L, result.get(0).id());
        assertEquals(101L, result.get(1).id());
    }
}


