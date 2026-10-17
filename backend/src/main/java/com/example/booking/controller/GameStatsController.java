package com.example.booking.controller;

import com.example.booking.dto.game.GameStatsResponse;
import com.example.booking.dto.game.GamesProfileResponse;
import com.example.booking.dto.game.MinesweeperResultRequest;
import com.example.booking.dto.game.SolitaireResultRequest;
import com.example.booking.entity.GameStats;
import com.example.booking.entity.GameType;
import com.example.booking.security.AuthenticatedUser;
import com.example.booking.service.GameStatsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Очки за игры — блок в профиле и приём результатов.
 *
 * Сама игра живёт во вкладке и считается на клиенте; сюда приходит только
 * итог, из которого сервер сам выводит очки.
 */
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameStatsController {

    private final GameStatsService statsService;

    /** Статистика по всем играм для блока в профиле. */
    @GetMapping("/stats")
    public GamesProfileResponse stats(Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();

        List<GameStatsResponse> games = statsService.statsFor(principal.id()).stream()
                .map(GameStatsController::toResponse)
                .toList();

        return GamesProfileResponse.builder()
                .totalScore(statsService.totalScore(principal.id()))
                .games(games)
                .build();
    }

    /**
     * Победа в сапёре. Очки считает сервер, клиент присылает только
     * время и число верных флажков.
     */
    @PostMapping("/minesweeper")
    public GameStatsResponse submitMinesweeper(Authentication authentication,
                                               @Valid @RequestBody MinesweeperResultRequest request) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();

        statsService.recordMinesweeperWin(principal.id(), principal.username(),
                request.seconds(), request.correctFlags());

        return statsService.statsFor(principal.id()).stream()
                .filter(stats -> stats.getGameType() == GameType.MINESWEEPER)
                .findFirst()
                .map(GameStatsController::toResponse)
                .orElseThrow();
    }

    /**
     * Победа в пасьянсе. Очки считает сервер, клиент присылает только
     * время и число ходов.
     */
    @PostMapping("/solitaire")
    public GameStatsResponse submitSolitaire(Authentication authentication,
                                             @Valid @RequestBody SolitaireResultRequest request) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();

        statsService.recordSolitaireWin(principal.id(), principal.username(),
                request.seconds(), request.moves());

        return statsService.statsFor(principal.id()).stream()
                .filter(stats -> stats.getGameType() == GameType.SOLITAIRE)
                .findFirst()
                .map(GameStatsController::toResponse)
                .orElseThrow();
    }

    private static GameStatsResponse toResponse(GameStats stats) {
        return GameStatsResponse.builder()
                .game(stats.getGameType().name())
                .wins(stats.getWins())
                .losses(stats.getLosses())
                .games(stats.totalGames())
                .score(stats.getScore())
                .bestGame(stats.getBestGame())
                .build();
    }
}