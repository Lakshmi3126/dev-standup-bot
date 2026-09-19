package com.example.standupbot.service;

import com.example.standupbot.dto.DailyDigestData;
import com.example.standupbot.entity.DigestLog;
import com.example.standupbot.entity.Member;
import com.example.standupbot.entity.Standup;
import com.example.standupbot.entity.Team;
import com.example.standupbot.entity.DigestLogStatus;
import com.example.standupbot.notification.NotificationService;
import com.example.standupbot.notification.SlackMessageFormatter;
import com.example.standupbot.repository.MemberRepository;
import com.example.standupbot.repository.StandupRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
public class StandUpAutomationService {

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
            SlackMessageFormatter slackMessageFormatter) {

        this.memberRepository = memberRepository;
        this.standupRepository = standupRepository;
        this.digestLogService = digestLogService;
        this.notificationService = notificationService;
        this.slackMessageFormatter = slackMessageFormatter;
    }

    /**
     * Finds all team members who have not submitted
     * today's standup.
     */
    public List<Member> findMissingMembers(
            Team team,
            LocalDate today) {

        List<Member> members =
                memberRepository.findByTeamId(team.getId());

        List<Standup> standups =
                standupRepository
                        .findByTeamIdAndStandupDateOrderByStandupDateAsc(
                                team.getId(),
                                today
                        );

        Set<Long> submittedMemberIds =
                standups.stream()
                        .filter(s -> s.getSubmittedAt() != null)
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
     * Creates the PENDING DigestLog before attempting
     * Slack delivery.
     *
     * Successful delivery changes the log to SENT.
     * Failed delivery changes the log to FAILED.
     */
        public DailyDigestData processDeadline(
            Team team,
            LocalDate today) {

        Optional<DigestLog> existingLog =
                digestLogService.findToday(
                        team.getId(),
                        today
                );

        if (existingLog.isPresent()) {

                DigestLog log = existingLog.get();

                DailyDigestData digest =
                        digestLogService.createPendingDigest(
                                team,
                                today
                        );

                if (log.getStatus() == DigestLogStatus.SENT ||
                        log.getStatus() == DigestLogStatus.PENDING) {

                return digest;
                }

                return sendDigest(team, digest, log);
        }

        DailyDigestData digest =
                digestLogService.createPendingDigest(
                        team,
                        today
                );

        DigestLog log =
                digestLogService.findToday(
                        team.getId(),
                        today
                ).orElseThrow(() ->
                        new IllegalStateException(
                                "DigestLog was not created"
                        )
                );

        return sendDigest(team, digest, log);
        }
        private DailyDigestData sendDigest(
        Team team,
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

    return digest;
}
}