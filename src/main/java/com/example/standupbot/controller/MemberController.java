package com.example.standupbot.controller;

import com.example.standupbot.dto.CreateMemberRequest;
import com.example.standupbot.dto.MemberResponse;
import com.example.standupbot.dto.UpdateMemberRequest;
import com.example.standupbot.service.MemberService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams/{teamId}/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    // CREATE MEMBER
    // POST /api/teams/{teamId}/members
    @PostMapping
    public ResponseEntity<MemberResponse> createMember(
            @PathVariable Long teamId,
            @Valid @RequestBody CreateMemberRequest request) {

        MemberResponse response =
                memberService.createMember(teamId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // GET ALL MEMBERS OF A TEAM
    // GET /api/teams/{teamId}/members
    @GetMapping
    public ResponseEntity<List<MemberResponse>> getAllMembers(
            @PathVariable Long teamId) {

        List<MemberResponse> members =
                memberService.getAllMembers(teamId);

        return ResponseEntity.ok(members);
    }

    // GET ONE MEMBER
    // GET /api/teams/{teamId}/members/{memberId}
    @GetMapping("/{memberId}")
    public ResponseEntity<MemberResponse> getMemberById(
            @PathVariable Long teamId,
            @PathVariable Long memberId) {

        MemberResponse response =
                memberService.getMemberById(teamId, memberId);

        return ResponseEntity.ok(response);
    }

    // UPDATE MEMBER
    // PUT /api/teams/{teamId}/members/{memberId}
    @PutMapping("/{memberId}")
    public ResponseEntity<MemberResponse> updateMember(
            @PathVariable Long teamId,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberRequest request) {

        MemberResponse response =
                memberService.updateMember(
                        teamId,
                        memberId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    // DELETE MEMBER
    // DELETE /api/teams/{teamId}/members/{memberId}
    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> deleteMember(
            @PathVariable Long teamId,
            @PathVariable Long memberId) {

        memberService.deleteMember(teamId, memberId);

        return ResponseEntity.noContent().build();
    }
}