package com.example.standupbot.controller;

import com.example.standupbot.dto.SubmitStandupRequest;
import com.example.standupbot.entity.Standup;
import com.example.standupbot.service.StandUpService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teams/{teamId}/standups")
public class StandUpController {

    private final StandUpService standUpService;

    public StandUpController(StandUpService standUpService) {
        this.standUpService = standUpService;
    }

    @PostMapping
    public ResponseEntity<Standup> submitStandup(
            @PathVariable Long teamId,
            @Valid @RequestBody SubmitStandupRequest request) {

        Standup savedStandup = standUpService.submitStandup(
                teamId,
                request.memberId(),
                request.yesterday(),
                request.today(),
                request.blockers()
        );

        return ResponseEntity.ok(savedStandup);
    }
}