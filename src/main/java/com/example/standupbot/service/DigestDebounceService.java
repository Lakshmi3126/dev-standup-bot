package com.example.standupbot.service;

import com.example.standupbot.dto.DailyDigestData;
import com.example.standupbot.entity.DigestLog;
import com.example.standupbot.entity.Team;
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

    /*
     * One pending update per team/date.
     */
    private final Map<String, ScheduledFuture<?>>
            pendingUpdates =
            new ConcurrentHashMap<>();

    public DigestDebounceService(
            TaskScheduler taskScheduler,
            DailyDigestService dailyDigestService,
            DigestLogService digestLogService) {

        this.taskScheduler = taskScheduler;
        this.dailyDigestService = dailyDigestService;
        this.digestLogService = digestLogService;
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
     * Regenerates the latest digest after the debounce.
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

            /*
             * P4 will receive this digest and send
             * the updated Slack message.
             *
             * No Slack implementation is placed here.
             */

            DigestLog log =
                    digestLogService.findToday(
                            team.getId(),
                            date
                    ).orElse(null);

            if (log != null) {

                digestLogService.recordUpdate(log);
            }

        } finally {

            pendingUpdates.remove(key);
        }
    }

    /**
     * Used when the previous digest delivery failed.
     */
    public void sendFailedDigest(
            Team team,
            LocalDate date,
            DailyDigestData digest,
            DigestLog log) {

        /*
         * P4 will send the digest.
         *
         * We don't implement Slack here.
         */

        System.out.println(
                "Digest delivery previously failed for team "
                        + team.getId()
                        + ". Regenerated digest."
        );
    }
}