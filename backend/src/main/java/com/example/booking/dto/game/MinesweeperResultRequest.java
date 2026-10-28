package com.example.booking.dto.game;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

@Builder
public record MinesweeperResultRequest(
        @Min(0) @Max(10_000) int seconds,
        @Min(0) @Max(20) int correctFlags
) {
}
