
package com.example.standupbot.service;

import com.example.standupbot.dto.BlockerResponse;
import com.example.standupbot.entity.Blocker;
import com.example.standupbot.entity.Member;
import com.example.standupbot.entity.Standup;
import com.example.standupbot.exception.ResourceNotFoundException;
import com.example.standupbot.notification.BlockerAlertContent;
import com.example.standupbot.notification.NotificationService;
import com.example.standupbot.notification.SlackMessageFormatter;
import com.example.standupbot.repository.BlockerRepository;
import com.example.standupbot.repository.MemberRepository;
import com.example.standupbot.repository.StandupRepository;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class BlockerService {

    private final BlockerRepository blockerRepository;
    private final StandupRepository standupRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final SlackMessageFormatter slackMessageFormatter;
    private final Clock clock;

    public BlockerService(
            BlockerRepository blockerRepository,
            StandupRepository standupRepository,
            MemberRepository memberRepository,
            NotificationService notificationService,
            SlackMessageFormatter slackMessageFormatter,
            Clock clock) {

        this.blockerRepository = blockerRepository;
        this.standupRepository = standupRepository;
        this.memberRepository = memberRepository;
        this.notificationService = notificationService;
        this.slackMessageFormatter = slackMessageFormatter;
        this.clock = clock;
    }

    public void processStandup(Standup standup) {

        if (standup == null) {
            return;
        }

        // No blocker reported: resolve the latest active/unresolved blocker.
        if (standup.getBlockers() == null
                || standup.getBlockers().isBlank()) {

           List<Blocker> activeBlockers = blockerRepository
        .findByMemberIdAndTeamIdOrderByLastReportedAtDesc(
                standup.getMemberId(),
                standup.getTeamId())
        .stream()
        .filter(blocker ->
                blocker.getStatus() == Blocker.Status.ACTIVE
                || blocker.getStatus() == Blocker.Status.UNRESOLVED)
        .toList();

for (Blocker blocker : activeBlockers) {
    blocker.setStatus(Blocker.Status.RESOLVED);
}

blockerRepository.saveAll(activeBlockers);

            return;
        }

        Standup previousStandup = standupRepository
                .findTopByTeamIdAndMemberIdAndStandupDateLessThanOrderByStandupDateDesc(
                        standup.getTeamId(),
                        standup.getMemberId(),
                        standup.getStandupDate());

        String currentBlocker = standup.getBlockers().trim();

        boolean sameAsPrevious =
                previousStandup != null
                        && previousStandup.getBlockers() != null
                        && currentBlocker.equalsIgnoreCase(
                                previousStandup.getBlockers().trim());

        boolean consecutiveDay =
                previousStandup != null
                        && isConsecutiveWorkingDay(
                                previousStandup.getStandupDate(),
                                standup.getStandupDate());

        Blocker blocker = blockerRepository
                .findByMemberIdAndTeamIdOrderByLastReportedAtDesc(
                        standup.getMemberId(),
                        standup.getTeamId())
                .stream()
                .filter(existing ->
                        existing.getStatus() != Blocker.Status.RESOLVED)
                .findFirst()
                .orElse(null);

        // First blocker reported.
        if (blocker == null) {
            createNewBlocker(standup, currentBlocker);
            return;
        }

        // Same blocker reported on consecutive working days.
        if (sameAsPrevious && consecutiveDay) {

            blocker.setStandupId(standup.getId());
            blocker.setDescription(currentBlocker);
            blocker.setLastReportedAt(standup.getSubmittedAt());
            blocker.setConsecutiveDays(
                    blocker.getConsecutiveDays() + 1);

            if (blocker.getConsecutiveDays() >= 3) {
                blocker.setStatus(Blocker.Status.UNRESOLVED);
            } else {
                blocker.setStatus(Blocker.Status.ACTIVE);
            }

            blockerRepository.save(blocker);

            if (blocker.getConsecutiveDays() == 3) {
                sendUnresolvedBlockerAlert(
                        standup, currentBlocker, blocker);
            }

            return;
        }

        // Different blocker or a break in consecutive working days.
        blocker.setStandupId(standup.getId());
        blocker.setDescription(currentBlocker);
        blocker.setFirstReportedAt(standup.getSubmittedAt());
        blocker.setLastReportedAt(standup.getSubmittedAt());
        blocker.setConsecutiveDays(1);
        blocker.setStatus(Blocker.Status.ACTIVE);

        blockerRepository.save(blocker);

        // A changed blocker is a new issue and needs an immediate alert.
        if (!sameAsPrevious) {
            sendBlockerAlert(standup, currentBlocker);
        }
    }

    private void createNewBlocker(
            Standup standup,
            String currentBlocker) {

        Blocker blocker = new Blocker();

        blocker.setMemberId(standup.getMemberId());
        blocker.setStandupId(standup.getId());
        blocker.setDescription(currentBlocker);
        blocker.setFirstReportedAt(standup.getSubmittedAt());
        blocker.setLastReportedAt(standup.getSubmittedAt());
        blocker.setConsecutiveDays(1);
        blocker.setStatus(Blocker.Status.ACTIVE);

        blockerRepository.save(blocker);

        sendBlockerAlert(standup, currentBlocker);
    }

    private void sendBlockerAlert(
            Standup standup,
            String currentBlocker) {

        Member member = memberRepository
                .findByIdAndTeamId(
                        standup.getMemberId(),
                        standup.getTeamId())
                .orElse(null);

        if (member == null || member.getTeam() == null) {
            return;
        }

        BlockerAlertContent content = new BlockerAlertContent(
                member.getTeam().getName(),
                member.getName(),
                currentBlocker,
                1
        );

        String message =
                slackMessageFormatter.formatBlockerAlert(content);

        notificationService.sendChannelMessage(
                member.getTeam().getWebhookUrl(),
                message
        );
    }

    private void sendUnresolvedBlockerAlert(
            Standup standup,
            String currentBlocker,
            Blocker blocker) {

        Member member = memberRepository
                .findByIdAndTeamId(
                        standup.getMemberId(),
                        standup.getTeamId())
                .orElse(null);

        if (member == null || member.getTeam() == null) {
            return;
        }

        BlockerAlertContent content = new BlockerAlertContent(
                member.getTeam().getName(),
                member.getName(),
                currentBlocker,
                blocker.getConsecutiveDays()
        );

        String message =
                slackMessageFormatter.formatUnresolvedBlockerAlert(content);

        notificationService.sendChannelMessage(
                member.getTeam().getWebhookUrl(),
                message
        );
    }

    public List<BlockerResponse> getTeamBlockers(Long teamId) {

        return blockerRepository
                .findByTeamIdOrderByLastReportedAtDesc(teamId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<BlockerResponse> getMemberBlockers(
            Long teamId,
            Long memberId) {

        memberRepository.findByIdAndTeamId(memberId, teamId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Member not found"));

        return blockerRepository
                .findByMemberIdAndTeamIdOrderByLastReportedAtDesc(
                        memberId,
                        teamId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private BlockerResponse toResponse(Blocker blocker) {

        return new BlockerResponse(
                blocker.getId(),
                blocker.getMemberId(),
                blocker.getStandupId(),
                blocker.getDescription(),
                blocker.getFirstReportedAt(),
                blocker.getLastReportedAt(),
                blocker.getConsecutiveDays(),
                blocker.getStatus()
        );
    }

    private boolean isConsecutiveWorkingDay(
            LocalDate previousDate,
            LocalDate currentDate) {

        if (previousDate == null || currentDate == null) {
            return false;
        }

        LocalDate expectedDate = previousDate.plusDays(1);

        while (expectedDate.getDayOfWeek().getValue() >= 6) {
            expectedDate = expectedDate.plusDays(1);
        }

        return currentDate.equals(expectedDate);
    }
}
