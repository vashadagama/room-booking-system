package com.example.booking.dto.game;

import lombok.Builder;

@Builder
public record GameStatsResponse(
        String game,
        int wins,
        int losses,
        int games,
        int score,
        int bestGame
) {
}
