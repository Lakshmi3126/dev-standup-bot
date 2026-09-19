package com.example.standupbot.repository;

import com.example.standupbot.entity.DigestLog;
import com.example.standupbot.entity.DigestLogStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DigestLogRepository
        extends JpaRepository<DigestLog, Long> {

    Optional<DigestLog> findByTeamIdAndDigestDate(
            Long teamId,
            LocalDate digestDate
    );

    Optional<DigestLog> findByTeamIdAndDigestDateAndStatus(
            Long teamId,
            LocalDate digestDate,
            DigestLogStatus status
    );
}