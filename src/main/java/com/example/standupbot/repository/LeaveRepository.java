
package com.example.standupbot.repository;

import com.example.standupbot.entity.Leave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRepository extends JpaRepository<Leave, Long> {

    // Check whether a member is on leave on a particular date.
    boolean existsByMember_IdAndCancelledFalseAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long memberId, LocalDate date1, LocalDate date2
    );

    // Get all active leaves for a team on a particular date.
    List<Leave> findByMember_Team_IdAndCancelledFalseAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long teamId, LocalDate date1, LocalDate date2
    );

    // Get a member's leave records.
    List<Leave> findByMember_IdOrderByStartDateDesc(Long memberId);
}
