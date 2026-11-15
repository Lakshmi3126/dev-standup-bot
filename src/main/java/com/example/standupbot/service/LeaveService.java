
package com.example.standupbot.service;

import com.example.standupbot.entity.Leave;
import com.example.standupbot.entity.Member;
import com.example.standupbot.exception.ResourceNotFoundException;
import com.example.standupbot.exception.ValidationException;
import com.example.standupbot.repository.LeaveRepository;
import com.example.standupbot.repository.MemberRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class LeaveService {

    private final LeaveRepository leaveRepository;
    private final MemberRepository memberRepository;

    public LeaveService(LeaveRepository leaveRepository,
                        MemberRepository memberRepository) {
        this.leaveRepository = leaveRepository;
        this.memberRepository = memberRepository;
    }

    // Add leave for a team member.
    public Leave addLeave(Long teamId, Long memberId,
                          LocalDate startDate, LocalDate endDate) {

        validateDates(startDate, endDate);

        Member member = memberRepository.findByIdAndTeamId(memberId, teamId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Member not found in this team."));

        validateNoOverlappingLeave(memberId, startDate, endDate, null);

        LocalDateTime now = LocalDateTime.now();

        Leave leave = new Leave();
        leave.setMember(member);
        leave.setStartDate(startDate);
        leave.setEndDate(endDate);
        leave.setCancelled(false);
        leave.setCreatedAt(now);
        leave.setUpdatedAt(now);

        return leaveRepository.save(leave);
    }

    // Edit an existing leave.
    public Leave updateLeave(Long teamId, Long memberId, Long leaveId,
                             LocalDate startDate, LocalDate endDate) {

        validateDates(startDate, endDate);

        Leave leave = getLeaveOrThrow(teamId, memberId, leaveId);

        if (leave.isCancelled()) {
            throw new ValidationException("Cannot update a cancelled leave.");
        }

        validateNoOverlappingLeave(memberId, startDate, endDate, leaveId);

        leave.setStartDate(startDate);
        leave.setEndDate(endDate);
        leave.setUpdatedAt(LocalDateTime.now());

        return leaveRepository.save(leave);
    }

    // Cancel leave without deleting its record.
    public Leave cancelLeave(Long teamId, Long memberId, Long leaveId) {

        Leave leave = getLeaveOrThrow(teamId, memberId, leaveId);

        if (leave.isCancelled()) {
            throw new ValidationException("Leave is already cancelled.");
        }

        leave.setCancelled(true);
        leave.setUpdatedAt(LocalDateTime.now());

        return leaveRepository.save(leave);
    }

    // Check whether a member is on leave on a particular date.
    @Transactional(readOnly = true)
    public boolean isMemberOnLeave(Long memberId, LocalDate date) {

        return leaveRepository
                .existsByMember_IdAndCancelledFalseAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        memberId, date, date
                );
    }

    // Get all active leaves for a team on a particular date.
    @Transactional(readOnly = true)
    public List<Leave> getTeamLeaves(Long teamId, LocalDate date) {

        return leaveRepository
                .findByMember_Team_IdAndCancelledFalseAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        teamId, date, date
                );
    }

    // Get leave history of a team member.
    @Transactional(readOnly = true)
    public List<Leave> getMemberLeaveHistory(Long teamId, Long memberId) {

        if (memberRepository.findByIdAndTeamId(memberId, teamId).isEmpty()) {
            throw new ResourceNotFoundException("Member not found in this team.");
        }

        return leaveRepository.findByMember_IdOrderByStartDateDesc(memberId);
    }

    private Leave getLeaveOrThrow(Long teamId, Long memberId, Long leaveId) {

        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Leave not found with ID: " + leaveId));

        if (!leave.getMember().getId().equals(memberId)
                || !leave.getMember().getTeam().getId().equals(teamId)) {
            throw new ResourceNotFoundException("Leave not found for this team member.");
        }

        return leave;
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {

        if (startDate == null || endDate == null) {
            throw new ValidationException("Start date and end date are required.");
        }

        if (endDate.isBefore(startDate)) {
            throw new ValidationException("End date cannot be before start date.");
        }
    }

    private void validateNoOverlappingLeave(
            Long memberId,
            LocalDate startDate,
            LocalDate endDate,
            Long excludedLeaveId) {

        List<Leave> existingLeaves =
                leaveRepository.findByMember_IdOrderByStartDateDesc(memberId);

        boolean overlap = existingLeaves.stream().anyMatch(existing ->
                !existing.isCancelled()
                && (excludedLeaveId == null
                    || !existing.getId().equals(excludedLeaveId))
                && !startDate.isAfter(existing.getEndDate())
                && !endDate.isBefore(existing.getStartDate())
        );

        if (overlap) {
            throw new ValidationException(
                    "This leave overlaps with an existing active leave."
            );
        }
    }
}
