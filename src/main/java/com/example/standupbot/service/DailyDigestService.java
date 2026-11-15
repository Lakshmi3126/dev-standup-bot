package com.example.standupbot.service;

import com.example.standupbot.dto.DailyDigestData;
import com.example.standupbot.entity.Member;
import com.example.standupbot.entity.Standup;
import com.example.standupbot.entity.Team;
import com.example.standupbot.repository.MemberRepository;
import com.example.standupbot.repository.StandupRepository;
import org.springframework.stereotype.Service;
import com.example.standupbot.entity.Leave;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DailyDigestService {

    private final MemberRepository memberRepository;
    private final StandupRepository standupRepository;
    private final LeaveService leaveService;

    public DailyDigestService(
            MemberRepository memberRepository,
            StandupRepository standupRepository, LeaveService leaveService) {

        this.memberRepository = memberRepository;
        this.standupRepository = standupRepository;
        this.leaveService=leaveService;
    }

    public DailyDigestData buildDailyDigest(
            Team team,
            LocalDate date) {

        List<Member> members =
                memberRepository.findByTeamId(team.getId());

        List<Standup> standups =
                standupRepository
                        .findByTeamIdAndStandupDateOrderByStandupDateAsc(
                                team.getId(),
                                date
                        );

        Map<Long, Member> membersById =
                members.stream()
                        .collect(Collectors.toMap(
                                Member::getId,
                                Function.identity()
                        ));

        List<DailyDigestData.MemberUpdate> onTime =
                new ArrayList<>();

        List<DailyDigestData.MemberUpdate> late =
                new ArrayList<>();

        Set<Long> submittedMemberIds =
                new HashSet<>();

        for (Standup standup : standups) {

            if (standup.getSubmittedAt() == null) {
                continue;
            }

            Member member =
                    membersById.get(standup.getMemberId());

            if (member == null) {
                continue;
            }

            submittedMemberIds.add(member.getId());

            DailyDigestData.MemberUpdate update =
                    new DailyDigestData.MemberUpdate(
                            member.getId(),
                            member.getName(),
                            standup.getYesterday(),
                            standup.getToday(),
                            standup.getBlockers()
                    );

            if (standup.getStatus()
                    == Standup.Status.LATE) {

                late.add(update);

            } else {

                onTime.add(update);
            }
        }

        Set<Long> onLeaveMemberIds =
        leaveService.getTeamLeaves(team.getId(), date)
                .stream()
                .map(leave -> leave.getMember().getId())
                .collect(Collectors.toSet());

        List<String> onLeave =
                members.stream()
                        .filter(member ->
                                onLeaveMemberIds.contains(member.getId()))
                        .map(Member::getName)
                        .toList();

        List<String> missing =
                members.stream()
                        .filter(member ->
                                !submittedMemberIds.contains(member.getId())
                        && !onLeaveMemberIds.contains(member.getId()))
                        .map(Member::getName)
                        .toList();

        return new DailyDigestData(
                team.getId(),
                team.getName(),
                date,
                onTime,
                late,
                missing,
                onLeave
        );
    }

    public DailyDigestData generateInitialDigest(
            Team team,
            LocalDate date) {

        return buildDailyDigest(team, date);
    }
}
