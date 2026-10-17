package com.example.booking.dto.game;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

/**
 * Результат победы в пасьянсе.
 *
 * Как и в сапёре, клиент присылает только факты: сколько секунд шла
 * партия и сколько сделано ходов. Очки считает сервер.
 */
@Builder
public record SolitaireResultRequest(
        @Min(0) @Max(100_000) int seconds,
        @Min(0) @Max(10_000) int moves
) {
}
