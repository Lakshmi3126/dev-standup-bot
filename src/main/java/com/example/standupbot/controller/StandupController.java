package com.example.standupbot.controller;

import com.example.standupbot.dto.StandupResponse;
import com.example.standupbot.dto.SubmitStandupRequest;
import com.example.standupbot.service.StandupService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class StandupController {

    private final StandupService standupService;

    public StandupController(StandupService standupService) {
        this.standupService = standupService;
    }

    // SUBMIT STANDUP
    // POST /api/teams/{teamId}/standups
    @PostMapping("/{teamId}/standups")
    public ResponseEntity<StandupResponse> submitStandup(
            @PathVariable Long teamId,
            @Valid @RequestBody SubmitStandupRequest request) {

        StandupResponse response =
                standupService.submitStandup(teamId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // GET MEMBER STANDUPS
    // GET /api/teams/{teamId}/members/{memberId}/standups
    @GetMapping("/{teamId}/members/{memberId}/standups")
    public ResponseEntity<List<StandupResponse>> getMemberStandups(
            @PathVariable Long teamId,
            @PathVariable Long memberId) {

        List<StandupResponse> standups =
                standupService.getMemberStandups(teamId, memberId);

        return ResponseEntity.ok(standups);
    }

    // GET STANDUPS BY DATE RANGE
    // GET /api/teams/{teamId}/standups?from=...&to=...
    @GetMapping("/{teamId}/standups")
    public ResponseEntity<List<StandupResponse>> getStandupsByDateRange(
            @PathVariable Long teamId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to) {

        List<StandupResponse> standups =
                standupService.getStandupsByDateRange(
                        teamId, from, to);

        return ResponseEntity.ok(standups);
    }

    // GET TODAY'S STANDUPS
    // GET /api/teams/{teamId}/standups/today
    @GetMapping("/{teamId}/standups/today")
    public ResponseEntity<List<StandupResponse>> getTodayStandups(
            @PathVariable Long teamId) {

        List<StandupResponse> standups =
                standupService.getTodayStandups(teamId);

        return ResponseEntity.ok(standups);
    }
}