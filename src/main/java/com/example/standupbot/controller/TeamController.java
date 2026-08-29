package com.example.standupbot.controller;

import com.example.standupbot.dto.CreateTeamRequest;
import com.example.standupbot.dto.TeamResponse;
import com.example.standupbot.dto.UpdateTeamRequest;
import com.example.standupbot.service.TeamService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    // CREATE TEAM
    // POST /api/teams
    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(
            @Valid @RequestBody CreateTeamRequest request) {

        TeamResponse response = teamService.createTeam(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // GET ALL TEAMS
    // GET /api/teams
    @GetMapping
    public ResponseEntity<List<TeamResponse>> getAllTeams() {

        List<TeamResponse> teams = teamService.getAllTeams();

        return ResponseEntity.ok(teams);
    }

    // GET TEAM BY ID
    // GET /api/teams/{teamId}
    @GetMapping("/{teamId}")
    public ResponseEntity<TeamResponse> getTeamById(
            @PathVariable Long teamId) {

        TeamResponse response = teamService.getTeamById(teamId);

        return ResponseEntity.ok(response);
    }

    // UPDATE TEAM
    // PUT /api/teams/{teamId}
    @PutMapping("/{teamId}")
    public ResponseEntity<TeamResponse> updateTeam(
            @PathVariable Long teamId,
            @Valid @RequestBody UpdateTeamRequest request) {

        TeamResponse response =
                teamService.updateTeam(teamId, request);

        return ResponseEntity.ok(response);
    }

    // DELETE TEAM
    // DELETE /api/teams/{teamId}
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable Long teamId) {

        teamService.deleteTeam(teamId);

        return ResponseEntity.noContent().build();
    }
}