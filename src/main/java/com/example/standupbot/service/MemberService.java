package com.example.standupbot.service;

import com.example.standupbot.dto.CreateMemberRequest;
import com.example.standupbot.dto.MemberResponse;
import com.example.standupbot.dto.UpdateMemberRequest;
import com.example.standupbot.entity.Member;
import com.example.standupbot.entity.Team;
import com.example.standupbot.exception.ConflictException;
import com.example.standupbot.exception.ResourceNotFoundException;
import com.example.standupbot.repository.MemberRepository;
import com.example.standupbot.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;

    public MemberService(
            MemberRepository memberRepository,
            TeamRepository teamRepository) {
        this.memberRepository = memberRepository;
        this.teamRepository = teamRepository;
    }

    // CREATE MEMBER
    // A member can only be created if the team already exists.
    public MemberResponse createMember(
            Long teamId,
            CreateMemberRequest request) {

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Team not found with id: " + teamId
                        ));

        // A team cannot have two members with the same email.
        if (memberRepository.existsByTeamIdAndEmail(
                teamId,
                request.getEmail())) {

            throw new ConflictException(
                    "Member with email already exists in this team: "
                            + request.getEmail()
            );
        }

        Member member = new Member();
        member.setTeam(team);
        member.setName(request.getName());
        member.setEmail(request.getEmail());
        member.setSlackUserId(request.getSlackUserId());
        member.setCreatedAt(LocalDateTime.now());

        Member savedMember = memberRepository.save(member);

        return toResponse(savedMember);
    }

    // GET ALL MEMBERS OF A TEAM
    // Only members belonging to this team are returned.
    public List<MemberResponse> getAllMembers(Long teamId) {

        // First make sure the team actually exists.
        validateTeamExists(teamId);

        return memberRepository.findByTeamId(teamId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // GET ONE MEMBER OF A TEAM
    // The member must belong to the specified team.
    public MemberResponse getMemberById(
            Long teamId,
            Long memberId) {

        return memberRepository
                .findByIdAndTeamId(memberId, teamId)
                .map(this::toResponse)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Member not found with id: " + memberId
                        ));
    }

    // UPDATE MEMBER
    // The member must belong to the specified team.
    public MemberResponse updateMember(
            Long teamId,
            Long memberId,
            UpdateMemberRequest request) {

        Member member = memberRepository
                .findByIdAndTeamId(memberId, teamId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Member not found with id: " + memberId
                        ));

        // Prevent changing the email to another member's email.
        if (memberRepository.existsByTeamIdAndEmailAndIdNot(
                teamId,
                request.getEmail(),
                memberId)) {

            throw new ConflictException(
                    "Member with email already exists in this team: "
                            + request.getEmail()
            );
        }

        member.setName(request.getName());
        member.setEmail(request.getEmail());
        member.setSlackUserId(request.getSlackUserId());

        Member updatedMember = memberRepository.save(member);

        return toResponse(updatedMember);
    }

    // DELETE MEMBER
    // The member must belong to the specified team.
    public void deleteMember(
            Long teamId,
            Long memberId) {

        Member member = memberRepository
                .findByIdAndTeamId(memberId, teamId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Member not found with id: " + memberId
                        ));

        memberRepository.delete(member);
    }

    // CHECK WHETHER TEAM EXISTS
    private void validateTeamExists(Long teamId) {

        if (!teamRepository.existsById(teamId)) {
            throw new ResourceNotFoundException(
                    "Team not found with id: " + teamId
            );
        }
    }

    // ENTITY -> RESPONSE DTO
    private MemberResponse toResponse(Member member) {

        MemberResponse response = new MemberResponse();

        response.setId(member.getId());
        response.setTeamId(member.getTeam().getId());
        response.setName(member.getName());
        response.setEmail(member.getEmail());
        response.setSlackUserId(member.getSlackUserId());

        return response;
    }
}