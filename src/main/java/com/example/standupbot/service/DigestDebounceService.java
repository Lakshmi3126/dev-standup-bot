package com.example.standupbot.service;

import com.example.standupbot.dto.DailyDigestData;
import com.example.standupbot.entity.DigestLog;
import com.example.standupbot.entity.Team;
import com.example.standupbot.notification.NotificationService;
import com.example.standupbot.notification.SlackMessageFormatter;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class DigestDebounceService {

    private static final long DEBOUNCE_MINUTES = 5;

    private final TaskScheduler taskScheduler;
    private final DailyDigestService dailyDigestService;
    private final DigestLogService digestLogService;
    private final NotificationService notificationService;
    private final SlackMessageFormatter slackMessageFormatter;

    private final Map<String, ScheduledFuture<?>>
            pendingUpdates =
            new ConcurrentHashMap<>();

    public DigestDebounceService(
            TaskScheduler taskScheduler,
            DailyDigestService dailyDigestService,
            DigestLogService digestLogService,
            NotificationService notificationService,
            SlackMessageFormatter slackMessageFormatter) {

        this.taskScheduler = taskScheduler;
        this.dailyDigestService = dailyDigestService;
        this.digestLogService = digestLogService;
        this.notificationService = notificationService;
        this.slackMessageFormatter = slackMessageFormatter;
    }

    /**
     * Schedule an updated digest.
     *
     * If another late submission arrives before the
     * 5-minute period ends, the previous task is cancelled
     * and a new 5-minute timer starts.
     */
    public void scheduleUpdatedDigest(
            Team team,
            LocalDate date) {

        String key =
                team.getId() + "-" + date;

        ScheduledFuture<?> existing =
                pendingUpdates.get(key);

        if (existing != null) {
            existing.cancel(false);
        }

        Instant executionTime =
                Instant.now()
                        .plus(
                                DEBOUNCE_MINUTES,
                                ChronoUnit.MINUTES
                        );

        ScheduledFuture<?> future =
                taskScheduler.schedule(
                        () -> processUpdatedDigest(
                                team,
                                date,
                                key
                        ),
                        executionTime
                );

        pendingUpdates.put(key, future);
    }

    /**
     * Regenerates and sends the latest digest after
     * the debounce period.
     */
    private void processUpdatedDigest(
            Team team,
            LocalDate date,
            String key) {

        try {

            DailyDigestData digest =
                    dailyDigestService.buildDailyDigest(
                            team,
                            date
                    );

            DigestLog log =
                    digestLogService.findToday(
                            team.getId(),
                            date
                    ).orElse(null);

            if (log == null) {
                return;
            }

            String message =
                    slackMessageFormatter.formatDailyDigest(
                            digest
                    );

            /*
             * Send the updated digest through the
             * existing P4 NotificationService.
             */
            notificationService.sendChannelMessage(
                    team.getWebhookUrl(),
                    message
            );

            /*
             * Increment update_count only AFTER
             * successful Slack delivery.
             */
            digestLogService.recordUpdate(log);

        } finally {

            pendingUpdates.remove(key);
        }
    }

    /**
     * Retries a digest whose previous delivery failed.
     */
    public void sendFailedDigest(
            Team team,
            LocalDate date,
            DailyDigestData digest,
            DigestLog log) {

        String message =
                slackMessageFormatter.formatDailyDigest(
                        digest
                );

        try {

            notificationService.sendChannelMessage(
                    team.getWebhookUrl(),
                    message
            );

            digestLogService.markSent(
                    log,
                    null
            );

        } catch (Exception e) {

            digestLogService.markFailed(log);

            throw e;
        }
    }
}