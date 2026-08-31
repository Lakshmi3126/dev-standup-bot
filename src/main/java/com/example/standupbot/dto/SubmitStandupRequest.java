package com.example.standupbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitStandupRequest(
        @NotNull
        Long memberId,

        @NotBlank
        String yesterday,

        @NotBlank
        String today,

        String blockers
) {
}