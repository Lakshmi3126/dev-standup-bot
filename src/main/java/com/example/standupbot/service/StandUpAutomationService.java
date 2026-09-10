package com.example.standupbot.service;

import com.example.standupbot.dto.DailyDigestData;
import com.example.standupbot.entity.Member;
import com.example.standupbot.entity.Standup;
import com.example.standupbot.entity.Team;
import com.example.standupbot.repository.MemberRepository;
import com.example.standupbot.repository.StandupRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StandUpAutomationService {

    private final MemberRepository memberRepository;
    private final StandupRepository standupRepository;
    private final DigestLogService digestLogService;

    public StandUpAutomationService(
            MemberRepository memberRepository,
            StandupRepository standupRepository,
            DigestLogService digestLogService) {

        this.memberRepository = memberRepository;
        this.standupRepository = standupRepository;
        this.digestLogService = digestLogService;
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
     * Builds the daily digest and creates
     * the PENDING DigestLog before Slack delivery.
     */
    public DailyDigestData processDeadline(
            Team team,
            LocalDate today) {

        DailyDigestData digest =
                digestLogService.createPendingDigest(
                        team,
                        today
                );

        return digest;
    }
}