
package com.example.standupbot.repository;

import com.example.standupbot.entity.Blocker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlockerRepository extends JpaRepository<Blocker, Long> {

    Optional<Blocker> findTopByMemberIdOrderByLastReportedAtDesc(
            Long memberId);

    List<Blocker> findByMemberIdOrderByLastReportedAtDesc(
            Long memberId);

    @Query("""
            SELECT b
            FROM Blocker b, Standup s
            WHERE b.standupId = s.id
              AND b.memberId = :memberId
              AND s.teamId = :teamId
            ORDER BY b.lastReportedAt DESC
            """)
    List<Blocker> findByMemberIdAndTeamIdOrderByLastReportedAtDesc(
            @Param("memberId") Long memberId,
            @Param("teamId") Long teamId);

    @Query("""
            SELECT b
            FROM Blocker b, Standup s
            WHERE b.standupId = s.id
              AND s.teamId = :teamId
            ORDER BY b.lastReportedAt DESC
            """)
    List<Blocker> findByTeamIdOrderByLastReportedAtDesc(
            @Param("teamId") Long teamId);
}
