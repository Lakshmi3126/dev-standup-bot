package com.example.standupbot.service;

import com.example.standupbot.entity.Member;
import com.example.standupbot.entity.Standup;
import com.example.standupbot.entity.Team;
import com.example.standupbot.repository.MemberRepository;
import com.example.standupbot.repository.StandupRepository;
import com.example.standupbot.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class StandUpService {

    private final StandupRepository standupRepository;
    private final TeamRepository teamRepository;
    private final MemberRepository memberRepository;
    private final LateSubmissionService lateSubmissionService;

    public StandUpService(
            StandupRepository standupRepository,
            TeamRepository teamRepository,
            MemberRepository memberRepository,
            LateSubmissionService lateSubmissionService) {

        this.standupRepository = standupRepository;
        this.teamRepository = teamRepository;
        this.memberRepository = memberRepository;
        this.lateSubmissionService = lateSubmissionService;
    }

    public Standup submitStandup(
            Long teamId,
            Long memberId,
            String yesterday,
            String today,
            String blockers) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Team not found: " + teamId));

        Member member = memberRepository.findByIdAndTeamId(memberId, teamId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Member not found: " + memberId));

        ZoneId zoneId = ZoneId.of(team.getTimezone());

        ZonedDateTime submittedTime = ZonedDateTime.now(zoneId);

        LocalDate date = submittedTime.toLocalDate();
        LocalTime currentTime = submittedTime.toLocalTime();

        Standup.Status status;

        if (currentTime.isAfter(team.getDeadline())) {
            status = Standup.Status.LATE;
        } else {
            status = Standup.Status.ON_TIME;
        }

        Standup standup = new Standup();

        standup.setTeamId(team.getId());
        standup.setMemberId(member.getId());
        standup.setStandupDate(date);

        standup.setYesterday(yesterday);
        standup.setToday(today);
        standup.setBlockers(blockers);

        standup.setSubmittedAt(Instant.now());
        standup.setStatus(status);
        standup.setCreatedAt(Instant.now());

        Standup savedStandup = standupRepository.save(standup);

        // If the standup was submitted after the deadline,
        // trigger the late-submission workflow.
        if (status == Standup.Status.LATE) {
            lateSubmissionService.handleLateSubmission(team, date);
        }

        return savedStandup;
    }
}