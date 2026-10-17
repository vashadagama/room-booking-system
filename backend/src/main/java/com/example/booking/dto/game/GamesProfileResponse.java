package com.example.booking.dto.game;

import lombok.Builder;

import java.util.List;

/**
 * Блок «Игры» в профиле: статистика по каждой игре плюс общая сумма.
 */
@Builder
public record GamesProfileResponse(
        int totalScore,
        List<GameStatsResponse> games
) {
}
