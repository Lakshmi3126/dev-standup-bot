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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private MemberService memberService;

    private Team team;

    @BeforeEach
    void setUp() {

        team = new Team();

        team.setId(1L);
        team.setName("Development Team");
        team.setTimezone("Asia/Kolkata");
    }

    @Test
    void createsMemberSuccessfully() {

        CreateMemberRequest request = new CreateMemberRequest();

        request.setName("Keerthi");
        request.setEmail("keerthi@example.com");
        request.setSlackUserId("U12345");

        when(teamRepository.findById(1L))
                .thenReturn(Optional.of(team));

        when(memberRepository.existsByTeamIdAndEmail(
                1L,
                "keerthi@example.com"
        )).thenReturn(false);

        Member savedMember = new Member();

        savedMember.setId(10L);
        savedMember.setTeam(team);
        savedMember.setName("Keerthi");
        savedMember.setEmail("keerthi@example.com");
        savedMember.setSlackUserId("U12345");

        when(memberRepository.save(any(Member.class)))
                .thenReturn(savedMember);

        MemberResponse response =
                memberService.createMember(1L, request);

        assertEquals(10L, response.getId());
        assertEquals(1L, response.getTeamId());
        assertEquals("Keerthi", response.getName());
        assertEquals("keerthi@example.com", response.getEmail());
        assertEquals("U12345", response.getSlackUserId());

        verify(memberRepository).save(any(Member.class));
    }

    @Test
    void rejectsDuplicateMemberEmail() {

        CreateMemberRequest request = new CreateMemberRequest();

        request.setName("Another User");
        request.setEmail("keerthi@example.com");

        when(teamRepository.findById(1L))
                .thenReturn(Optional.of(team));

        when(memberRepository.existsByTeamIdAndEmail(
                1L,
                "keerthi@example.com"
        )).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> memberService.createMember(1L, request)
        );

        verify(memberRepository, never())
                .save(any(Member.class));
    }

    @Test
    void rejectsMemberWhenTeamDoesNotExist() {

        CreateMemberRequest request = new CreateMemberRequest();

        request.setName("Keerthi");
        request.setEmail("keerthi@example.com");

        when(teamRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> memberService.createMember(1L, request)
        );

        verify(memberRepository, never())
                .save(any(Member.class));
    }

    @Test
    void updatesMemberSuccessfully() {

        UpdateMemberRequest request = new UpdateMemberRequest();

        request.setName("Updated Keerthi");
        request.setEmail("updated@example.com");
        request.setSlackUserId("U99999");

        Member existingMember = new Member();

        existingMember.setId(10L);
        existingMember.setTeam(team);
        existingMember.setName("Keerthi");
        existingMember.setEmail("keerthi@example.com");
        existingMember.setSlackUserId("U12345");

        when(memberRepository.findByIdAndTeamId(10L, 1L))
                .thenReturn(Optional.of(existingMember));

        when(memberRepository.existsByTeamIdAndEmailAndIdNot(
                1L,
                "updated@example.com",
                10L
        )).thenReturn(false);

        when(memberRepository.save(existingMember))
                .thenReturn(existingMember);

        MemberResponse response =
                memberService.updateMember(1L, 10L, request);

        assertEquals(
                "Updated Keerthi",
                response.getName()
        );

        assertEquals(
                "updated@example.com",
                response.getEmail()
        );

        assertEquals(
                "U99999",
                response.getSlackUserId()
        );

        verify(memberRepository)
                .save(existingMember);
    }

    @Test
    void rejectsDuplicateEmailDuringUpdate() {

        UpdateMemberRequest request = new UpdateMemberRequest();

        request.setName("Updated Keerthi");
        request.setEmail("existing@example.com");
        request.setSlackUserId("U99999");

        Member existingMember = new Member();

        existingMember.setId(10L);
        existingMember.setTeam(team);

        when(memberRepository.findByIdAndTeamId(10L, 1L))
                .thenReturn(Optional.of(existingMember));

        when(memberRepository.existsByTeamIdAndEmailAndIdNot(
                1L,
                "existing@example.com",
                10L
        )).thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> memberService.updateMember(1L, 10L, request)
        );

        verify(memberRepository, never())
                .save(any(Member.class));
    }

    // ---------------------------------------------------------
    // TEST 6: MEMBER FROM WRONG TEAM
    // ---------------------------------------------------------

    @Test
    void rejectsMemberFromWrongTeam() {

        Long teamId = 1L;
        Long memberId = 10L;

        when(memberRepository.findByIdAndTeamId(
                memberId,
                teamId
        )).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> memberService.getMemberById(
                        teamId,
                        memberId
                )
        );

        verify(memberRepository)
                .findByIdAndTeamId(memberId, teamId);
    }
}