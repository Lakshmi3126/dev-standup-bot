package com.example.standupbot.dto;

import com.example.standupbot.entity.Standup;
import java.time.Instant;
import java.time.LocalDate;

public record StandupResponse(
        Long id,
        Long teamId,
        Long memberId,
        LocalDate standupDate,
        String yesterday,
        String today,
        String blockers,
        Instant submittedAt,
        Standup.Status status
) {
}