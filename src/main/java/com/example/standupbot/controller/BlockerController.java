package com.example.standupbot.controller;

import com.example.standupbot.dto.BlockerResponse;
import com.example.standupbot.service.BlockerService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BlockerController {

    private final BlockerService blockerService;

    public BlockerController(BlockerService blockerService) {
        this.blockerService = blockerService;
    }

    @GetMapping("/api/teams/{teamId}/blockers")
    public ResponseEntity<List<BlockerResponse>> getTeamBlockers(
            @PathVariable Long teamId) {

        return ResponseEntity.ok(
                blockerService.getTeamBlockers(teamId)
        );
    }

    @GetMapping("/api/teams/{teamId}/members/{memberId}/blockers")
    public ResponseEntity<List<BlockerResponse>> getMemberBlockers(
            @PathVariable Long teamId,
            @PathVariable Long memberId) {

        return ResponseEntity.ok(
                blockerService.getMemberBlockers(teamId, memberId)
        );
    }
}