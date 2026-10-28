package com.example.booking.dto.game;

import lombok.Builder;

import java.util.List;

@Builder
public record GamesProfileResponse(
        int totalScore,
        List<GameStatsResponse> games
) {
}
