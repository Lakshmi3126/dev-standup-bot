package com.example.standupbot.service;

import com.example.standupbot.dto.DailyDigestData;
import com.example.standupbot.entity.DigestLog;
import com.example.standupbot.entity.DigestLogStatus;
import com.example.standupbot.entity.Team;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class LateSubmissionService {

    private final DigestLogService digestLogService;
    private final DailyDigestService dailyDigestService;
    private final DigestDebounceService digestDebounceService;

    public LateSubmissionService(
            DigestLogService digestLogService,
            DailyDigestService dailyDigestService,
            DigestDebounceService digestDebounceService) {

        this.digestLogService = digestLogService;
        this.dailyDigestService = dailyDigestService;
        this.digestDebounceService = digestDebounceService;
    }

    /**
     * Called after a late standup is submitted.
     */
    public void handleLateSubmission(
            Team team,
            LocalDate date) {

        Optional<DigestLog> optionalLog =
                digestLogService.findToday(
                        team.getId(),
                        date
                );

        /*
         * No digest exists.
         *
         * Nothing to do.
         */
        if (optionalLog.isEmpty()) {
            return;
        }

        DigestLog log =
                optionalLog.get();

        /*
         * Digest is currently being sent.
         *
         * Do nothing.
         */
        if (log.getStatus()
                == DigestLogStatus.PENDING) {

            return;
        }

        /*
         * Digest delivery previously failed.
         *
         * Treat it as unsent and regenerate.
         */
        if (log.getStatus()
                == DigestLogStatus.FAILED) {

            DailyDigestData digest =
                    dailyDigestService.buildDailyDigest(
                            team,
                            date
                    );

            digestDebounceService.sendFailedDigest(
                    team,
                    date,
                    digest,
                    log
            );

            return;
        }

        /*
         * Digest was already sent.
         *
         * Schedule an updated digest using
         * the debounce mechanism.
         */
        if (log.getStatus()
                == DigestLogStatus.SENT) {

            digestDebounceService.scheduleUpdatedDigest(
                    team,
                    date
            );
        }
    }
}