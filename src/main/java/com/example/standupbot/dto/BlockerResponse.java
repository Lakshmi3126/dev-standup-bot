package com.example.standupbot.dto;

import com.example.standupbot.entity.Blocker;

import java.time.Instant;

public record BlockerResponse(
        Long id,
        Long memberId,
        Long standupId,
        String description,
        Instant firstReportedAt,
        Instant lastReportedAt,
        int consecutiveDays,
        Blocker.Status status
) {
}