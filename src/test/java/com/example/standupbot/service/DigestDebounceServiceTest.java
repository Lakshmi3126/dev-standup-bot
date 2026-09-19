package com.example.standupbot.service;

import com.example.standupbot.dto.DailyDigestData;
import com.example.standupbot.entity.DigestLog;
import com.example.standupbot.entity.Team;
import com.example.standupbot.notification.NotificationService;
import com.example.standupbot.notification.SlackMessageFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DigestDebounceServiceTest {

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private DailyDigestService dailyDigestService;

    @Mock
    private DigestLogService digestLogService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SlackMessageFormatter slackMessageFormatter;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    @Mock
    private DigestLog digestLog;

    private DigestDebounceService service;

    private Team team;

    private final LocalDate date =
            LocalDate.of(2026, 9, 10);

    @BeforeEach
    void setUp() {

        service = new DigestDebounceService(
                taskScheduler,
                dailyDigestService,
                digestLogService,
                notificationService,
                slackMessageFormatter
        );

        team = new Team();
        team.setId(1L);
        team.setName("Engineering");
        team.setWebhookUrl("webhook-url");

        when(taskScheduler.schedule(
                any(Runnable.class),
                any(Instant.class)))
                .thenReturn(scheduledFuture);
    }

    @Test
    void shouldScheduleUpdatedDigest() {

        service.scheduleUpdatedDigest(team, date);

        verify(taskScheduler).schedule(
                any(Runnable.class),
                any(Instant.class)
        );
    }

    @Test
    void shouldCancelPreviousUpdateWhenAnotherSubmissionArrives() {

        service.scheduleUpdatedDigest(team, date);

        service.scheduleUpdatedDigest(team, date);

        verify(scheduledFuture)
                .cancel(false);

        verify(taskScheduler, times(2))
                .schedule(
                        any(Runnable.class),
                        any(Instant.class)
                );
    }

    @Test
    void shouldSendUpdatedDigestAndRecordUpdateAfterSuccessfulDelivery() {

        DailyDigestData digest =
                new DailyDigestData(
                        1L,
                        "Engineering",
                        date,
                        List.of(),
                        List.of(),
                        List.of("Bob")
                );

        when(dailyDigestService.buildDailyDigest(team, date))
                .thenReturn(digest);

        when(digestLogService.findToday(1L, date))
                .thenReturn(Optional.of(digestLog));

        when(slackMessageFormatter.formatDailyDigest(digest))
                .thenReturn("Updated digest");

        ArgumentCaptor<Runnable> runnableCaptor =
                ArgumentCaptor.forClass(Runnable.class);

        service.scheduleUpdatedDigest(team, date);

        verify(taskScheduler).schedule(
                runnableCaptor.capture(),
                any(Instant.class)
        );

        /*
         * Simulate the debounce timer finishing.
         */
        runnableCaptor.getValue().run();

        verify(notificationService)
                .sendChannelMessage(
                        "webhook-url",
                        "Updated digest"
                );

        /*
         * update_count must be incremented
         * only after successful delivery.
         */
        verify(digestLogService)
                .recordUpdate(digestLog);
    }

    @Test
    void shouldNotRecordUpdateWhenUpdatedDigestDeliveryFails() {

        DailyDigestData digest =
                new DailyDigestData(
                        1L,
                        "Engineering",
                        date,
                        List.of(),
                        List.of(),
                        List.of()
                );

        when(dailyDigestService.buildDailyDigest(team, date))
                .thenReturn(digest);

        when(digestLogService.findToday(1L, date))
                .thenReturn(Optional.of(digestLog));

        when(slackMessageFormatter.formatDailyDigest(digest))
                .thenReturn("Updated digest");

        doThrow(new RuntimeException("Slack failed"))
                .when(notificationService)
                .sendChannelMessage(
                        "webhook-url",
                        "Updated digest"
                );

        ArgumentCaptor<Runnable> runnableCaptor =
                ArgumentCaptor.forClass(Runnable.class);

        service.scheduleUpdatedDigest(team, date);

        verify(taskScheduler).schedule(
                runnableCaptor.capture(),
                any(Instant.class)
        );

        /*
         * The scheduled task should fail because
         * Slack delivery failed.
         */
        try {
            runnableCaptor.getValue().run();
        } catch (RuntimeException ignored) {
            // Expected
        }

        /*
         * update_count must NOT be incremented
         * when delivery fails.
         */
        verify(digestLogService, never())
                .recordUpdate(digestLog);
    }
}