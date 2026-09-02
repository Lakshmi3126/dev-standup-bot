package com.example.standupbot.service;

import com.example.standupbot.dto.CreateTeamRequest;
import com.example.standupbot.dto.TeamResponse;
import com.example.standupbot.dto.UpdateTeamRequest;
import com.example.standupbot.entity.Team;
import com.example.standupbot.exception.ConflictException;
import com.example.standupbot.exception.InvalidTimezoneException;
import com.example.standupbot.exception.ResourceNotFoundException;
import com.example.standupbot.repository.MemberRepository;
import com.example.standupbot.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final MemberRepository memberRepository;

    public TeamService(
            TeamRepository teamRepository,
            MemberRepository memberRepository
    ) {
        this.teamRepository = teamRepository;
        this.memberRepository = memberRepository;
    }

    // =========================
    // CREATE TEAM
    // =========================

    public TeamResponse createTeam(CreateTeamRequest request) {

        validateTimezone(request.getTimezone());

        Team team = new Team();

        team.setName(request.getName());
        team.setTimezone(request.getTimezone());
        team.setDeadline(request.getDeadline());

        // Stored in database for the notification/Slack module.
        // These values are never returned in TeamResponse.
        team.setWebhookUrl(request.getWebhookUrl());
        team.setSlackBotToken(request.getSlackBotToken());

        LocalDateTime now = LocalDateTime.now();

        team.setCreatedAt(now);
        team.setUpdatedAt(now);

        Team savedTeam = teamRepository.save(team);

        return toResponse(savedTeam);
    }

    // =========================
    // GET ALL TEAMS
    // =========================

    public List<TeamResponse> getAllTeams() {

        return teamRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================
    // GET TEAM BY ID
    // =========================

    public TeamResponse getTeamById(Long teamId) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Team not found with id: " + teamId
                        )
                );

        return toResponse(team);
    }

    // =========================
    // UPDATE TEAM
    // =========================

    public TeamResponse updateTeam(
            Long teamId,
            UpdateTeamRequest request
    ) {

        validateTimezone(request.getTimezone());

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Team not found with id: " + teamId
                        )
                );

        team.setName(request.getName());
        team.setTimezone(request.getTimezone());
        team.setDeadline(request.getDeadline());
        team.setWebhookUrl(request.getWebhookUrl());

        /*
         * IMPORTANT:
         *
         * slackBotToken is intentionally NOT updated here.
         *
         * The project specification allows the token
         * during team creation, but PUT does not rotate it.
         */

        team.setUpdatedAt(LocalDateTime.now());

        Team updatedTeam = teamRepository.save(team);

        return toResponse(updatedTeam);
    }

    // =========================
    // DELETE TEAM
    // =========================

    public void deleteTeam(Long teamId) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Team not found with id: " + teamId
                        )
                );

        /*
         * A team cannot be deleted while it has members.
         *
         * We return HTTP 409 Conflict through ConflictException.
         * There is NO cascade deletion.
         */

        if (memberRepository.existsByTeamId(teamId)) {
            throw new ConflictException(
                    "Cannot delete team with existing members"
            );
        }

        teamRepository.delete(team);
    }

    // =========================
    // VALIDATE TIMEZONE
    // =========================

    private void validateTimezone(String timezone) {

        try {

            ZoneId.of(timezone);

        } catch (Exception exception) {

            throw new InvalidTimezoneException(
                    "Invalid IANA timezone: " + timezone
            );
        }
    }

    // =========================
    // ENTITY -> RESPONSE DTO
    // =========================

    private TeamResponse toResponse(Team team) {

        TeamResponse response = new TeamResponse();

        response.setId(team.getId());
        response.setName(team.getName());
        response.setTimezone(team.getTimezone());
        response.setDeadline(team.getDeadline());

        /*
         * IMPORTANT:
         *
         * Do NOT return:
         *
         * - webhookUrl
         * - slackBotToken
         *
         * These are sensitive/write-only values.
         */

        return response;
    }
}