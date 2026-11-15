
package com.example.standupbot.service;

import com.example.standupbot.entity.Leave;
import com.example.standupbot.entity.Member;
import com.example.standupbot.entity.Team;
import com.example.standupbot.exception.ResourceNotFoundException;
import com.example.standupbot.exception.ValidationException;
import com.example.standupbot.repository.LeaveRepository;
import com.example.standupbot.repository.MemberRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock
    private LeaveRepository leaveRepository;

    @Mock
    private MemberRepository memberRepository;

    private LeaveService leaveService;

    private final LocalDate start = LocalDate.of(2026, 11, 15);
    private final LocalDate end = LocalDate.of(2026, 11, 17);

    private Member member;
    private Team team;

    @BeforeEach
    void setUp() {
        leaveService = new LeaveService(
                leaveRepository, memberRepository);

        team = new Team();
        team.setId(1L);

        member = new Member();
        member.setId(10L);
        member.setTeam(team);
        member.setName("Test Member");
        member.setEmail("test@example.com");
    }

    @Test
    void shouldAddLeaveSuccessfully() {

        when(memberRepository.findByIdAndTeamId(10L, 1L))
                .thenReturn(Optional.of(member));

        when(leaveRepository.findByMember_IdOrderByStartDateDesc(10L))
                .thenReturn(List.of());

        when(leaveRepository.save(any(Leave.class)))
                .thenAnswer(invocation -> {
                    Leave leave = invocation.getArgument(0);
                    leave.setId(100L);
                    return leave;
                });

        Leave result = leaveService.addLeave(1L, 10L, start, end);

        assertEquals(100L, result.getId());
        assertEquals(start, result.getStartDate());
        assertEquals(end, result.getEndDate());
        assertFalse(result.isCancelled());

        verify(leaveRepository).save(any(Leave.class));
    }

    @Test
    void shouldRejectUnknownMember() {

        when(memberRepository.findByIdAndTeamId(10L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> leaveService.addLeave(1L, 10L, start, end));

        verify(leaveRepository, never()).save(any(Leave.class));
    }

    @Test
    void shouldRejectInvalidDateRange() {

        assertThrows(
                ValidationException.class,
                () -> leaveService.addLeave(1L, 10L, end, start));

        verifyNoInteractions(memberRepository);
        verifyNoInteractions(leaveRepository);
    }

    @Test
    void shouldRejectOverlappingLeave() {

        Leave existing = createLeave(100L, start, end, false);

        when(memberRepository.findByIdAndTeamId(10L, 1L))
                .thenReturn(Optional.of(member));

        when(leaveRepository.findByMember_IdOrderByStartDateDesc(10L))
                .thenReturn(List.of(existing));

        assertThrows(
                ValidationException.class,
                () -> leaveService.addLeave(1L, 10L, start, end));

        verify(leaveRepository, never()).save(any(Leave.class));
    }

    @Test
    void shouldCancelLeaveSuccessfully() {

        Leave leave = createLeave(100L, start, end, false);

        when(leaveRepository.findById(100L))
                .thenReturn(Optional.of(leave));

        when(leaveRepository.save(any(Leave.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Leave result = leaveService.cancelLeave(1L, 10L, 100L);

        assertTrue(result.isCancelled());

        verify(leaveRepository).save(leave);
    }

    @Test
    void shouldRejectUpdatingCancelledLeave() {

        Leave leave = createLeave(100L, start, end, true);

        when(leaveRepository.findById(100L))
                .thenReturn(Optional.of(leave));

        assertThrows(
                ValidationException.class,
                () -> leaveService.updateLeave(
                        1L, 10L, 100L, start, end));

        verify(leaveRepository, never()).save(any(Leave.class));
    }

    @Test
    void shouldCheckWhetherMemberIsOnLeave() {

        when(leaveRepository
                .existsByMember_IdAndCancelledFalseAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        10L, start, start))
                .thenReturn(true);

        assertTrue(leaveService.isMemberOnLeave(10L, start));
    }

    @Test
    void shouldReturnFalseWhenMemberIsNotOnLeave() {

        when(leaveRepository
                .existsByMember_IdAndCancelledFalseAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        10L, start, start))
                .thenReturn(false);

        assertFalse(leaveService.isMemberOnLeave(10L, start));
    }

    @Test
    void shouldGetMemberLeaveHistory() {

        when(memberRepository.findByIdAndTeamId(10L, 1L))
                .thenReturn(Optional.of(member));

        Leave leave = createLeave(100L, start, end, false);

        when(leaveRepository.findByMember_IdOrderByStartDateDesc(10L))
                .thenReturn(List.of(leave));

        List<Leave> result =
                leaveService.getMemberLeaveHistory(1L, 10L);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getId());
    }

    private Leave createLeave(
            Long id, LocalDate startDate,
            LocalDate endDate, boolean cancelled) {

        Leave leave = new Leave();
        leave.setId(id);
        leave.setMember(member);
        leave.setStartDate(startDate);
        leave.setEndDate(endDate);
        leave.setCancelled(cancelled);

        return leave;
    }
}
