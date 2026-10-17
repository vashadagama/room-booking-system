package com.example.booking.dto.game;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

/**
 * Результат победы в сапёре.
 *
 * Клиент присылает только факты: сколько секунд шла партия и сколько
 * флажков стояло верно. Очки считает сервер — доверять цифрам от
 * клиента нельзя.
 */
@Builder
public record MinesweeperResultRequest(
        @Min(0) @Max(10_000) int seconds,
        @Min(0) @Max(20) int correctFlags
) {
}
