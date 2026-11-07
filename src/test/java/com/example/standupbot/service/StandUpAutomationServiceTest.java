
package com.example.standupbot.service;

import com.example.standupbot.dto.DailyDigestData;
import com.example.standupbot.entity.DigestLog;
import com.example.standupbot.entity.DigestLogStatus;
import com.example.standupbot.entity.Member;
import com.example.standupbot.entity.Standup;
import com.example.standupbot.entity.Team;
import com.example.standupbot.notification.NotificationService;
import com.example.standupbot.notification.SlackMessageFormatter;
import com.example.standupbot.repository.MemberRepository;
import com.example.standupbot.repository.StandupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StandUpAutomationServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private StandupRepository standupRepository;

    @Mock
    private DigestLogService digestLogService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SlackMessageFormatter slackMessageFormatter;

    private StandUpAutomationService service;
    private Team team;

    @BeforeEach
    void setUp() {
        service = new StandUpAutomationService(
                memberRepository,
                standupRepository,
                digestLogService,
                notificationService,
                slackMessageFormatter,
                Clock.fixed(
                        Instant.parse("2026-09-10T04:30:00Z"),
                        java.time.ZoneOffset.UTC
                )
        );

        team = new Team();
        team.setId(1L);
        team.setName("Engineering");
        team.setWebhookUrl("webhook-url");
    }

    @Test
    void findMissingMembers_shouldReturnMembersWhoDidNotSubmit() {
        Member alice = new Member();
        alice.setId(1L);
        alice.setName("Alice");

        Member bob = new Member();
        bob.setId(2L);
        bob.setName("Bob");

        Standup aliceStandup = new Standup();
        aliceStandup.setMemberId(1L);
        aliceStandup.setSubmittedAt(Instant.now());

        LocalDate date = LocalDate.of(2026, 9, 10);

        when(memberRepository.findByTeamId(1L))
                .thenReturn(List.of(alice, bob));

        when(standupRepository
                .findByTeamIdAndStandupDateOrderByStandupDateAsc(
                        1L, date))
                .thenReturn(List.of(aliceStandup));

        List<Member> result =
                service.findMissingMembers(team, date);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getId());
        assertEquals("Bob", result.get(0).getName());
    }

    @Test
    void findMissingMembers_shouldReturnEmptyWhenEveryoneSubmitted() {
        Member alice = new Member();
        alice.setId(1L);
        alice.setName("Alice");

        Member bob = new Member();
        bob.setId(2L);
        bob.setName("Bob");

        Standup aliceStandup = new Standup();
        aliceStandup.setMemberId(1L);
        aliceStandup.setSubmittedAt(Instant.now());

        Standup bobStandup = new Standup();
        bobStandup.setMemberId(2L);
        bobStandup.setSubmittedAt(Instant.now());

        LocalDate date = LocalDate.of(2026, 9, 10);

        when(memberRepository.findByTeamId(1L))
                .thenReturn(List.of(alice, bob));

        when(standupRepository
                .findByTeamIdAndStandupDateOrderByStandupDateAsc(
                        1L, date))
                .thenReturn(List.of(aliceStandup, bobStandup));

        List<Member> result =
                service.findMissingMembers(team, date);

        assertTrue(result.isEmpty());
    }

    @Test
    void processDeadline_shouldRecoverStalePendingDigest() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        DailyDigestData digest = createDigest(date);
        DigestLog log = mock(DigestLog.class);

        when(digestLogService.findToday(1L, date))
                .thenReturn(Optional.of(log));
        when(log.getStatus()).thenReturn(DigestLogStatus.PENDING);
        when(log.getCreatedAt())
                .thenReturn(Instant.parse("2026-09-10T04:20:00Z"));
        when(digestLogService.createPendingDigest(team, date))
                .thenReturn(digest);
        when(slackMessageFormatter.formatDailyDigest(digest))
                .thenReturn("Daily Standup Digest");

        DailyDigestData result = service.processDeadline(team, date);

        assertSame(digest, result);
        verify(notificationService).sendChannelMessage(
                "webhook-url",
                "Daily Standup Digest"
        );
        verify(digestLogService).markSent(log, null);
    }

    @Test
    void processDeadline_shouldSendDigestAndMarkSent() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        DailyDigestData digest = createDigest(date, "Bob");

        DigestLog log = mock(DigestLog.class);

        when(digestLogService.findToday(1L, date))
                .thenReturn(Optional.of(log));

        when(log.getStatus())
                .thenReturn(DigestLogStatus.FAILED);

        when(digestLogService.createPendingDigest(team, date))
                .thenReturn(digest);

        when(slackMessageFormatter.formatDailyDigest(digest))
                .thenReturn("Daily Standup Digest");

        DailyDigestData result =
                service.processDeadline(team, date);

        assertSame(digest, result);

        verify(notificationService).sendChannelMessage(
                "webhook-url",
                "Daily Standup Digest"
        );

        verify(digestLogService).markSent(log, null);
        verify(digestLogService, never()).markFailed(any());
    }

    @Test
    void processDeadline_shouldMarkFailedWhenDeliveryFails() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        DailyDigestData digest = createDigest(date);

        DigestLog log = mock(DigestLog.class);

        when(digestLogService.findToday(1L, date))
                .thenReturn(Optional.of(log));

        when(log.getStatus())
                .thenReturn(DigestLogStatus.FAILED);

        when(digestLogService.createPendingDigest(team, date))
                .thenReturn(digest);

        when(slackMessageFormatter.formatDailyDigest(digest))
                .thenReturn("Daily Standup Digest");

        doThrow(new RuntimeException("Slack failed"))
                .when(notificationService)
                .sendChannelMessage(
                        "webhook-url",
                        "Daily Standup Digest"
                );

        assertThrows(
                RuntimeException.class,
                () -> service.processDeadline(team, date)
        );

        verify(digestLogService).markFailed(log);
        verify(digestLogService, never()).markSent(any(), any());
    }

    @Test
    void processDeadline_shouldNotSendWhenDigestAlreadySent() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        DigestLog log = mock(DigestLog.class);

        when(digestLogService.findToday(1L, date))
                .thenReturn(Optional.of(log));

        when(log.getStatus())
                .thenReturn(DigestLogStatus.SENT);

        DailyDigestData result =
                service.processDeadline(team, date);

        assertNull(result);

        verify(digestLogService, never())
                .createPendingDigest(team, date);

        verify(notificationService, never())
                .sendChannelMessage(anyString(), anyString());
    }

    @Test
    void processDeadline_shouldNotSendWhenDigestIsPending() {
        LocalDate date = LocalDate.of(2026, 9, 10);
        DigestLog log = mock(DigestLog.class);

        when(digestLogService.findToday(1L, date))
                .thenReturn(Optional.of(log));

        when(log.getStatus())
                .thenReturn(DigestLogStatus.PENDING);

        when(log.getCreatedAt())
        .thenReturn(Instant.parse("2026-09-10T04:29:00Z"));

        DailyDigestData result =
                service.processDeadline(team, date);

        assertNull(result);

        verify(digestLogService, never())
                .createPendingDigest(team, date);

        verify(notificationService, never())
                .sendChannelMessage(anyString(), anyString());
    }

    private DailyDigestData createDigest(
            LocalDate date,
            String... missingMembers) {

        return new DailyDigestData(
                1L,
                "Engineering",
                date,
                List.of(),
                List.of(),
                List.of(missingMembers)
        );
    }
}
