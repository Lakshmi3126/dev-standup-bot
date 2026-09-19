package com.example.standupbot.service;

import com.example.standupbot.dto.DailyDigestData;
import com.example.standupbot.entity.DigestLog;
import com.example.standupbot.entity.DigestLogStatus;
import com.example.standupbot.entity.Team;
import com.example.standupbot.repository.DigestLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class DigestLogService {

    private final DigestLogRepository digestLogRepository;
    private final DailyDigestService dailyDigestService;

    public DigestLogService(
            DigestLogRepository digestLogRepository,
            DailyDigestService dailyDigestService) {

        this.digestLogRepository = digestLogRepository;
        this.dailyDigestService = dailyDigestService;
    }

    /**
     * Creates the initial digest log.
     *
     * PENDING is stored BEFORE P4 attempts Slack delivery.
     */
    public DailyDigestData createPendingDigest(
            Team team,
            LocalDate date) {

        Optional<DigestLog> existing =
                digestLogRepository.findByTeamIdAndDigestDate(
                        team.getId(),
                        date
                );

        if (existing.isPresent()) {
            return dailyDigestService.buildDailyDigest(
                    team,
                    date
            );
        }

        DailyDigestData digest =
                dailyDigestService.buildDailyDigest(
                        team,
                        date
                );

        DigestLog log =
                DigestLog.pending(
                        team.getId(),
                        date
                );

        digestLogRepository.save(log);

        return digest;
    }

    /**
     * Find today's DigestLog.
     */
    public Optional<DigestLog> findToday(
            Long teamId,
            LocalDate date) {

        return digestLogRepository
                .findByTeamIdAndDigestDate(
                        teamId,
                        date
                );
    }

    /**
     * Mark the digest as successfully sent.
     */
    public void markSent(
            DigestLog log,
            String slackMessageTs) {

        log.setStatus(DigestLogStatus.SENT);
        log.setSentAt(Instant.now());
        log.setLastUpdatedAt(Instant.now());
        log.setSlackMessageTs(slackMessageTs);

        digestLogRepository.save(log);
    }

    /**
     * Mark the digest as failed.
     */
    public void markFailed(
            DigestLog log) {

        log.setStatus(DigestLogStatus.FAILED);
        log.setLastUpdatedAt(Instant.now());

        digestLogRepository.save(log);
    }

    /**
     * Increase the update count when a late submission
     * causes an updated digest.
     */
    public void recordUpdate(
            DigestLog log) {

        int currentCount =
                log.getUpdateCount() == null
                        ? 0
                        : log.getUpdateCount();

        log.setUpdateCount(currentCount + 1);
        log.setLastUpdatedAt(Instant.now());

        digestLogRepository.save(log);
    }
}