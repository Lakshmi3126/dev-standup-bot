
package com.example.standupbot.controller;

import com.example.standupbot.dto.CreateLeaveRequest;
import com.example.standupbot.dto.LeaveResponse;
import com.example.standupbot.entity.Leave;
import com.example.standupbot.service.LeaveService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams/{teamId}/members/{memberId}/leaves")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    // CREATE LEAVE
    @PostMapping
    public ResponseEntity<LeaveResponse> addLeave(
            @PathVariable Long teamId,
            @PathVariable Long memberId,
            @Valid @RequestBody CreateLeaveRequest request) {

        Leave leave = leaveService.addLeave(
                teamId, memberId,
                request.getStartDate(), request.getEndDate()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(leave));
    }

    // GET MEMBER'S LEAVE HISTORY
    @GetMapping
    public ResponseEntity<List<LeaveResponse>> getLeaveHistory(
            @PathVariable Long teamId,
            @PathVariable Long memberId) {

        List<LeaveResponse> response = leaveService
                .getMemberLeaveHistory(teamId, memberId)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    // UPDATE LEAVE
    @PutMapping("/{leaveId}")
    public ResponseEntity<LeaveResponse> updateLeave(
            @PathVariable Long teamId,
            @PathVariable Long memberId,
            @PathVariable Long leaveId,
            @Valid @RequestBody CreateLeaveRequest request) {

        Leave leave = leaveService.updateLeave(
                teamId, memberId, leaveId,
                request.getStartDate(), request.getEndDate()
        );

        return ResponseEntity.ok(toResponse(leave));
    }

    // CANCEL LEAVE
    @DeleteMapping("/{leaveId}")
    public ResponseEntity<LeaveResponse> cancelLeave(
            @PathVariable Long teamId,
            @PathVariable Long memberId,
            @PathVariable Long leaveId) {

        Leave leave = leaveService.cancelLeave(teamId, memberId, leaveId);

        return ResponseEntity.ok(toResponse(leave));
    }

    private LeaveResponse toResponse(Leave leave) {

        LeaveResponse response = new LeaveResponse();
        response.setId(leave.getId());
        response.setMemberId(leave.getMember().getId());
        response.setStartDate(leave.getStartDate());
        response.setEndDate(leave.getEndDate());
        response.setCancelled(leave.isCancelled());

        return response;
    }
}
