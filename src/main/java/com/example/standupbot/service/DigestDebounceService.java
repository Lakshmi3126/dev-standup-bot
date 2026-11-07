package com.example.standupbot.service;

import com.example.standupbot.dto.DailyDigestData;
import com.example.standupbot.entity.DigestLog;
import com.example.standupbot.entity.Team;
import com.example.standupbot.notification.NotificationService;
import com.example.standupbot.notification.SlackMessageFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class DigestDebounceService {

    private static final Logger log =
            LoggerFactory.getLogger(DigestDebounceService.class);

    private static final Duration DEBOUNCE_DELAY =
            Duration.ofMinutes(5);

    private static final int MAX_DELIVERY_ATTEMPTS = 3;

    private final Clock clock;
    private final TaskScheduler taskScheduler;
    private final DailyDigestService dailyDigestService;
    private final DigestLogService digestLogService;
    private final NotificationService notificationService;
    private final SlackMessageFormatter slackMessageFormatter;

    private final Map<String, PendingUpdate> pendingUpdates =
            new ConcurrentHashMap<>();

    private final Map<String, Object> updateLocks =
        new ConcurrentHashMap<>();

private Object getUpdateLock(String key) {
    return updateLocks.computeIfAbsent(key, ignored -> new Object());
}

    public DigestDebounceService(
            TaskScheduler taskScheduler,
            DailyDigestService dailyDigestService,
            DigestLogService digestLogService,
            NotificationService notificationService,
            SlackMessageFormatter slackMessageFormatter,
            Clock clock) {

        this.taskScheduler = taskScheduler;
        this.dailyDigestService = dailyDigestService;
        this.digestLogService = digestLogService;
        this.notificationService = notificationService;
        this.slackMessageFormatter = slackMessageFormatter;
        this.clock = clock;
    }

    public void scheduleUpdatedDigest(Team team, LocalDate date) {
        String key = team.getId() + "-" + date;

        PendingUpdate pendingUpdate = new PendingUpdate();

        synchronized (getUpdateLock(key)) {
            PendingUpdate previous = pendingUpdates.put(key, pendingUpdate);

            if (previous != null && previous.future != null) {
                previous.future.cancel(false);
            }
        }

        scheduleAttempt(
                team, date, key, pendingUpdate, 1, DEBOUNCE_DELAY);
    }

    private void scheduleAttempt(
            Team team,
            LocalDate date,
            String key,
            PendingUpdate pendingUpdate,
            int attempt,
            Duration delay) {

        synchronized (getUpdateLock(key)) {
            if (pendingUpdates.get(key) != pendingUpdate) {
                return;
            }

            Instant executionTime = Instant.now(clock).plus(delay);

            ScheduledFuture<?> future = taskScheduler.schedule(
                    () -> processUpdatedDigest(
                            team, date, key, pendingUpdate, attempt),
                    executionTime
            );

            if (future != null) {
                pendingUpdate.future = future;
            } else {
                pendingUpdates.remove(key, pendingUpdate);
                log.error(
                        "Could not schedule updated digest for team {} on {}",
                        team.getId(),
                        date
                );
            }
        }
    }

    private void processUpdatedDigest(
            Team team,
            LocalDate date,
            String key,
            PendingUpdate pendingUpdate,
            int attempt) {

        synchronized (getUpdateLock(key)) {
            if (pendingUpdates.get(key) != pendingUpdate) {
                return;
            }
        }

        DailyDigestData digest;
        DigestLog digestLog;
        String message;

        try {
            digest = dailyDigestService.buildDailyDigest(team, date);

            digestLog = digestLogService
                    .findToday(team.getId(), date)
                    .orElse(null);

            if (digestLog == null) {
                removePendingUpdate(key, pendingUpdate);
                return;
            }

            message = slackMessageFormatter.formatDailyDigest(digest);

        } catch (Exception exception) {
            removePendingUpdate(key, pendingUpdate);

            log.error(
                    "Could not prepare updated digest for team {} on {}",
                    team.getId(),
                    date,
                    exception
            );
            return;
        }

        synchronized (getUpdateLock(key)) {
            if (pendingUpdates.get(key) != pendingUpdate) {
                return;
            }

            try {
                notificationService.sendChannelMessage(
                        team.getWebhookUrl(),
                        message
                );

            } catch (Exception exception) {

                if (attempt < MAX_DELIVERY_ATTEMPTS) {
                    Duration retryDelay = Duration.ofMinutes(attempt);

                    log.warn(
                            "Updated digest delivery failed for team {} on {}; retrying attempt {} of {}",
                            team.getId(),
                            date,
                            attempt + 1,
                            MAX_DELIVERY_ATTEMPTS,
                            exception
                    );

                    scheduleAttempt(
                            team,
                            date,
                            key,
                            pendingUpdate,
                            attempt + 1,
                            retryDelay
                    );

                } else {
                    pendingUpdates.remove(key, pendingUpdate);

                    log.error(
                            "Updated digest delivery exhausted retries for team {} on {}",
                            team.getId(),
                            date,
                            exception
                    );
                }

                return;
            }

            try {
                digestLogService.recordUpdate(digestLog);

            } catch (Exception exception) {
                log.error(
                        "Slack accepted updated digest for team {} on {}, but update tracking failed",
                        team.getId(),
                        date,
                        exception
                );

            } finally {
                pendingUpdates.remove(key, pendingUpdate);
            }
        }
    }

    private void removePendingUpdate(
            String key,
            PendingUpdate pendingUpdate) {

        synchronized (getUpdateLock(key)) {
            pendingUpdates.remove(key, pendingUpdate);
        }
    }

    public void sendFailedDigest(
            Team team,
            LocalDate date,
            DailyDigestData digest,
            DigestLog logEntry) {

        String message =
                slackMessageFormatter.formatDailyDigest(digest);

        try {
            notificationService.sendChannelMessage(
                    team.getWebhookUrl(),
                    message
            );

            digestLogService.markSent(logEntry, null);

        } catch (Exception exception) {
            digestLogService.markFailed(logEntry);
            throw exception;
        }
    }

    private static class PendingUpdate {
        private ScheduledFuture<?> future;
    }
}
