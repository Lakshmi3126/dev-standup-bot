package com.example.standupbot.repository;

import com.example.standupbot.entity.Blocker;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockerRepository extends JpaRepository<Blocker, Long> {

    Optional<Blocker> findTopByMemberIdOrderByLastReportedAtDesc(
            Long memberId);

    List<Blocker> findByMemberIdOrderByLastReportedAtDesc(
            Long memberId);
}