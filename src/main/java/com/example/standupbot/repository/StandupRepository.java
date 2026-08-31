package com.example.standupbot.repository;

import com.example.standupbot.entity.Standup;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StandupRepository extends JpaRepository<Standup, Long> {

    boolean existsByTeamIdAndMemberIdAndStandupDate(
            Long teamId,
            Long memberId,
            LocalDate standupDate);

    List<Standup> findByTeamIdAndMemberIdOrderByStandupDateDesc(
            Long teamId,
            Long memberId);

    List<Standup> findByTeamIdAndStandupDateBetweenOrderByStandupDateAsc(
            Long teamId,
            LocalDate from,
            LocalDate to);

    List<Standup> findByTeamIdAndStandupDateOrderByStandupDateAsc(
            Long teamId,
            LocalDate standupDate);
}