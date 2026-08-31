package com.example.standupbot.service;

import com.example.standupbot.dto.CreateTeamRequest;
import com.example.standupbot.dto.TeamResponse;
import com.example.standupbot.dto.UpdateTeamRequest;
import com.example.standupbot.entity.Team;
import com.example.standupbot.exception.InvalidTimezoneException;
import com.example.standupbot.exception.ResourceNotFoundException;
import com.example.standupbot.repository.TeamRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    // CREATE TEAM
    public TeamResponse createTeam(CreateTeamRequest request) {

        // Make sure the timezone is a valid IANA timezone.
        validateTimezone(request.getTimezone());

        Team team = new Team();

        team.setName(request.getName());
        team.setTimezone(request.getTimezone());
        team.setDeadline(request.getDeadline());

        // These values are stored in the database because they are
        // required later by the notification module.
        // They are NEVER included in TeamResponse.
        team.setWebhookUrl(request.getWebhookUrl());
        team.setSlackBotToken(request.getSlackBotToken());

        LocalDateTime now = LocalDateTime.now();
        team.setCreatedAt(now);
        team.setUpdatedAt(now);

        Team savedTeam = teamRepository.save(team);

        return toResponse(savedTeam);
    }

    // GET ALL TEAMS
    public List<TeamResponse> getAllTeams() {

        return teamRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // GET TEAM BY ID
    public TeamResponse getTeamById(Long teamId) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Team not found with id: " + teamId
                        )
                );

        return toResponse(team);
    }

    // UPDATE TEAM
    public TeamResponse updateTeam(
            Long teamId,
            UpdateTeamRequest request) {

        // Validate timezone before updating the team.
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

        /*
         * slackBotToken is intentionally NOT updated here.
         *
         * The project specification does not currently define
         * Slack bot token rotation through PUT.
         */

        team.setWebhookUrl(request.getWebhookUrl());

        team.setUpdatedAt(LocalDateTime.now());

        Team updatedTeam = teamRepository.save(team);

        return toResponse(updatedTeam);
    }

    // DELETE TEAM
    public void deleteTeam(Long teamId) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Team not found with id: " + teamId
                        )
                );

        /*
         * The project specification has not yet decided whether
         * deleting a team with members/standups should:
         *
         * 1. return 409 Conflict, or
         * 2. cascade the deletion.
         *
         * Therefore, that rule is intentionally not implemented here yet.
         */

        teamRepository.delete(team);
    }

    // VALIDATE TIMEZONE
    private void validateTimezone(String timezone) {

        try {
            /*
             * ZoneId.of() validates timezone IDs such as:
             *
             * Asia/Kolkata
             * Europe/London
             * America/New_York
             */
            ZoneId.of(timezone);

        } catch (Exception exception) {

            throw new InvalidTimezoneException(
                    "Invalid IANA timezone: " + timezone
            );
        }
    }

    // ENTITY -> RESPONSE DTO
    private TeamResponse toResponse(Team team) {

        TeamResponse response = new TeamResponse();

        response.setId(team.getId());
        response.setName(team.getName());
        response.setTimezone(team.getTimezone());
        response.setDeadline(team.getDeadline());

        /*
         * IMPORTANT:
         *
         * Do NOT add:
         *
         * response.setWebhookUrl(...)
         * response.setSlackBotToken(...)
         *
         * These are write-only secrets and must never appear
         * in API responses.
         */

        return response;
    }
}