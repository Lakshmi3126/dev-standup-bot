
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
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StandUpAutomationService {

    private static final Duration STALE_PENDING_AFTER = Duration.ofMinutes(2);

    private final Clock clock;
    private final MemberRepository memberRepository;
    private final StandupRepository standupRepository;
    private final DigestLogService digestLogService;
    private final NotificationService notificationService;
    private final SlackMessageFormatter slackMessageFormatter;

    public StandUpAutomationService(
            MemberRepository memberRepository,
            StandupRepository standupRepository,
            DigestLogService digestLogService,
            NotificationService notificationService,
            SlackMessageFormatter slackMessageFormatter,
            Clock clock) {

        this.clock = clock;
        this.memberRepository = memberRepository;
        this.standupRepository = standupRepository;
        this.digestLogService = digestLogService;
        this.notificationService = notificationService;
        this.slackMessageFormatter = slackMessageFormatter;
    }

    /**
     * Finds all team members who have not submitted today's standup.
     */
    public List<Member> findMissingMembers(Team team, LocalDate today) {

        List<Member> members =
                memberRepository.findByTeamId(team.getId());

        List<Standup> standups =
                standupRepository
                        .findByTeamIdAndStandupDateOrderByStandupDateAsc(
                                team.getId(),
                                today
                        );

        Set<Long> submittedMemberIds = standups.stream()
                .filter(standup -> standup.getSubmittedAt() != null)
                .map(Standup::getMemberId)
                .collect(Collectors.toSet());

        return members.stream()
                .filter(member ->
                        !submittedMemberIds.contains(member.getId()))
                .toList();
    }

    /**
     * Processes the team's daily deadline.
     *
     * SENT digests are not sent again. FAILED digests and PENDING digests
     * older than the recovery threshold can be retried. A new digest log is
     * created before the initial Slack delivery.
     */
    public DailyDigestData processDeadline(Team team, LocalDate today) {

        Optional<DigestLog> existingLog =
                digestLogService.findToday(team.getId(), today);

        if (existingLog.isPresent()) {
            DigestLog log = existingLog.get();
            DigestLogStatus status = log.getStatus();

            if (status == DigestLogStatus.SENT) {
                return null;
            }

            if (status == DigestLogStatus.PENDING) {
                Instant createdAt = log.getCreatedAt();
                Instant staleBefore =
                        Instant.now(clock).minus(STALE_PENDING_AFTER);

                // A fresh PENDING row may belong to a send currently in flight.
                // A stale row is treated as abandoned (for example, after a crash)
                // and is safe to retry after the recovery window.
                if (createdAt == null || createdAt.isAfter(staleBefore)) {
                    return null;
                }
            }

            // Existing FAILED or stale PENDING digest: rebuild and retry delivery.
            DailyDigestData digest =
                    digestLogService.createPendingDigest(team, today);

            return sendDigest(team, digest, log);
        }

        // No digest log exists: create PENDING before delivery.
        DailyDigestData digest =
                digestLogService.createPendingDigest(team, today);

        DigestLog log = digestLogService
                .findToday(team.getId(), today)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Digest log was not created for team "
                                        + team.getId()
                                        + " on "
                                        + today
                        ));

        return sendDigest(team, digest, log);
    }

    private DailyDigestData sendDigest(
            Team team,
            DailyDigestData digest,
            DigestLog log) {

        try {
            String message =
                    slackMessageFormatter.formatDailyDigest(digest);

            notificationService.sendChannelMessage(
                    team.getWebhookUrl(),
                    message
            );

            digestLogService.markSent(log, null);

            return digest;

        } catch (RuntimeException exception) {
            digestLogService.markFailed(log);
            throw exception;
        }
    }
}
