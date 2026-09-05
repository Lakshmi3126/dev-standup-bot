package com.example.standupbot.service;

import com.example.standupbot.entity.Blocker;
import com.example.standupbot.entity.Standup;
import com.example.standupbot.repository.BlockerRepository;
import com.example.standupbot.repository.StandupRepository;
import org.springframework.stereotype.Service;
import com.example.standupbot.repository.MemberRepository;
import com.example.standupbot.dto.BlockerResponse;
import java.time.Clock;
import com.example.standupbot.entity.Member;
import java.util.ArrayList;
import java.util.List;

@Service
public class BlockerService {
    private final BlockerRepository blockerRepository;
    private final StandupRepository standupRepository;
    private final MemberRepository memberRepository;
    private final Clock clock;

    public BlockerService(
            BlockerRepository blockerRepository,
            StandupRepository standupRepository,
            MemberRepository memberRepository,
            Clock clock) {

        this.blockerRepository = blockerRepository;
        this.standupRepository = standupRepository;
        this.memberRepository = memberRepository;
        this.clock = clock;
    }

    public void processStandup(Standup standup) {

        if (standup == null || standup.getBlockers() == null
                || standup.getBlockers().isBlank()) {
            return;
        }

        Standup previousStandup =
                standupRepository
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
                .findTopByMemberIdOrderByLastReportedAtDesc(
                        standup.getMemberId())
                .orElse(null);

        if (blocker == null) {
            blocker = new Blocker();

            blocker.setMemberId(standup.getMemberId());
            blocker.setStandupId(standup.getId());
            blocker.setDescription(currentBlocker);
            blocker.setFirstReportedAt(standup.getSubmittedAt());
            blocker.setLastReportedAt(standup.getSubmittedAt());
            blocker.setConsecutiveDays(1);
            blocker.setStatus(Blocker.Status.ACTIVE);

            blockerRepository.save(blocker);
            return;
        }

        if (sameAsPrevious && consecutiveDay) {
            blocker.setStandupId(standup.getId());
            blocker.setDescription(currentBlocker);
            blocker.setLastReportedAt(standup.getSubmittedAt());
            blocker.setConsecutiveDays(blocker.getConsecutiveDays() + 1);
            if (blocker.getConsecutiveDays() >= 3) {
                blocker.setStatus(Blocker.Status.UNRESOLVED);
            } else {
                blocker.setStatus(Blocker.Status.ACTIVE);
            }

            blockerRepository.save(blocker);
            return;
        }
        blocker.setStandupId(standup.getId());
        blocker.setDescription(currentBlocker);
        blocker.setFirstReportedAt(standup.getSubmittedAt());
        blocker.setLastReportedAt(standup.getSubmittedAt());
        blocker.setConsecutiveDays(1);
        blocker.setStatus(Blocker.Status.ACTIVE);

        blockerRepository.save(blocker);
    }

    public List<BlockerResponse> getMemberBlockers(Long teamId, Long memberId) {
        memberRepository.findByIdAndTeamId(memberId, teamId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        return blockerRepository.findByMemberIdOrderByLastReportedAtDesc(memberId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<BlockerResponse> getTeamBlockers(Long teamId) {
        List<Member> members = memberRepository.findByTeamId(teamId);

        List<BlockerResponse> blockers = new ArrayList<>();

        for (Member member : members) {
            blockers.addAll(
                    blockerRepository
                            .findByMemberIdOrderByLastReportedAtDesc(member.getId())
                            .stream()
                            .map(this::toResponse)
                            .toList()
            );
        }

        return blockers;
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
            java.time.LocalDate previousDate,
            java.time.LocalDate currentDate) {

        if (previousDate == null || currentDate == null) {
            return false;
        }

        java.time.LocalDate expectedDate = previousDate.plusDays(1);

        while (expectedDate.getDayOfWeek().getValue() >= 6) {
            expectedDate = expectedDate.plusDays(1);
        }

        return currentDate.equals(expectedDate);
    }
}