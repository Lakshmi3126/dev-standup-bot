package com.example.standupbot.service;

import com.example.standupbot.dto.StandupResponse;
import com.example.standupbot.dto.SubmitStandupRequest;
import com.example.standupbot.entity.Member;
import com.example.standupbot.entity.Standup;
import com.example.standupbot.entity.Team;
import com.example.standupbot.exception.DuplicateSubmissionException;
import com.example.standupbot.exception.ResourceNotFoundException;
import com.example.standupbot.exception.TeamMemberMismatchException;
import com.example.standupbot.repository.MemberRepository;
import com.example.standupbot.repository.StandupRepository;
import com.example.standupbot.repository.TeamRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class StandupService {

    private final StandupRepository standupRepository;
    private final TeamRepository teamRepository;
    private final MemberRepository memberRepository;
    private final Clock clock;

    public StandupService(
            StandupRepository standupRepository,
            TeamRepository teamRepository,
            MemberRepository memberRepository,
            Clock clock) {
        this.standupRepository = standupRepository;
        this.teamRepository = teamRepository;
        this.memberRepository = memberRepository;
        this.clock = clock;
    }

    @Transactional
    public StandupResponse submitStandup(
            Long teamId,
            SubmitStandupRequest request) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Team not found"));

       Member member = memberRepository.findById(request.memberId())
        .orElseThrow(() ->
                new ResourceNotFoundException("Member not found"));

        if (!member.getTeam().getId().equals(teamId)) {
            throw new TeamMemberMismatchException(
                "Member does not belong to team");
}

        ZoneId zoneId = ZoneId.of(team.getTimezone());
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(zoneId);

        LocalDate standupDate = now.toLocalDate();
        Instant submittedAt = now.toInstant();
        if (standupRepository.existsByTeamIdAndMemberIdAndStandupDate(
        teamId, member.getId(), standupDate)) {
            throw new DuplicateSubmissionException(
             "Standup already submitted for this team member today");
        }

        Standup.Status status =
                now.toLocalTime().compareTo(team.getDeadline()) <= 0
                        ? Standup.Status.ON_TIME
                        : Standup.Status.LATE;

        Standup standup = new Standup();
        standup.setTeamId(teamId);
        standup.setMemberId(member.getId());
        standup.setStandupDate(standupDate);
        standup.setYesterday(request.yesterday());
        standup.setToday(request.today());
        standup.setBlockers(request.blockers());
        standup.setSubmittedAt(submittedAt);
        standup.setStatus(status);
        standup.setCreatedAt(Instant.now(clock));

        try {
            Standup saved = standupRepository.saveAndFlush(standup);
            return toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateSubmissionException(
                    "Standup already submitted for this team member today");
        }
    }

    @Transactional(readOnly = true)
    public List<StandupResponse> getMemberStandups(
            Long teamId,
            Long memberId) {

        validateMemberBelongsToTeam(teamId, memberId);

        return standupRepository
                .findByTeamIdAndMemberIdOrderByStandupDateDesc(
                        teamId, memberId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StandupResponse> getStandupsByDateRange(
            Long teamId,
            LocalDate from,
            LocalDate to) {

        validateTeamExists(teamId);

        return standupRepository
                .findByTeamIdAndStandupDateBetweenOrderByStandupDateAsc(
                        teamId, from, to)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StandupResponse> getTodayStandups(Long teamId) {

        Team team = validateTeamExists(teamId);

        ZoneId zoneId = ZoneId.of(team.getTimezone());
        LocalDate today = ZonedDateTime.now(clock)
                .withZoneSameInstant(zoneId)
                .toLocalDate();

        return standupRepository
                .findByTeamIdAndStandupDateOrderByStandupDateAsc(
                        teamId, today)
                .stream()
                .map(this::toResponse)
                .toList();
    }

   private Team validateTeamExists(Long teamId) {
    return teamRepository.findById(teamId)
            .orElseThrow(() ->
                    new ResourceNotFoundException("Team not found"));
    }
    private void validateMemberBelongsToTeam(
            Long teamId,
            Long memberId) {

        validateTeamExists(teamId);

        Member member = memberRepository.findById(memberId)
            .orElseThrow(() ->
                    new ResourceNotFoundException("Member not found"));

        if (!member.getTeam().getId().equals(teamId)) {
            throw new TeamMemberMismatchException(
                "Member does not belong to team");
        }
    }

    private StandupResponse toResponse(Standup standup) {
        return new StandupResponse(
                standup.getId(),
                standup.getTeamId(),
                standup.getMemberId(),
                standup.getStandupDate(),
                standup.getYesterday(),
                standup.getToday(),
                standup.getBlockers(),
                standup.getSubmittedAt(),
                standup.getStatus());
    }
}