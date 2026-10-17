package com.example.booking.service;

import com.example.booking.entity.GameStats;
import com.example.booking.entity.GameType;
import com.example.booking.repository.GameStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Очки игрока.
 *
 * Считаются только здесь и только по правилам конкретной игры. Клиент
 * присылает факты (сколько секунд шла партия, сколько флажков стояло
 * верно), а цифры получает здесь: пересчитывать очки на клиенте нельзя,
 * клиенту нельзя доверять.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameStatsService {

    // Очки за сапёр: база минус штраф за время плюс бонус за верные флажки.
    private static final int MINESWEEPER_BASE = 100;
    private static final int MINESWEEPER_TIME_PENALTY_PER_SECOND = 1;
    private static final int MINESWEEPER_FLAG_BONUS = 5;
    private static final int MINESWEEPER_MIN_SCORE = 10;
    /** Потолок одной партии: 100 базы плюс максимум 10 флажков по 5. */
    private static final int MINESWEEPER_MAX_SCORE = MINESWEEPER_BASE + 10 * MINESWEEPER_FLAG_BONUS;
    private static final int MINESWEEPER_MAX_FLAGS = 10;
    private static final int MINESWEEPER_MAX_SECONDS = 10_000;

    // Очки за пасьянс: победа плюс награда за скорость и за аккуратность.
    private static final int SOLITAIRE_WIN_BASE = 300;
    private static final int SOLITAIRE_SPEED_PENALTY_PER_SECOND = 1;
    /** Выше этого времени скорость уже не учитывается. */
    private static final int SOLITAIRE_SPEED_WINDOW_SECONDS = 300;
    private static final int SOLITAIRE_MOVES_BONUS_CAP = 200;
    private static final int SOLITAIRE_MIN_SCORE = 50;
    private static final int SOLITAIRE_MAX_SCORE = 800;
    private static final int SOLITAIRE_MAX_SECONDS = 100_000;
    private static final int SOLITAIRE_MAX_MOVES = 10_000;

    private final GameStatsRepository gameStatsRepository;

    @Transactional
    public int recordMinesweeperWin(Long userId, String username, int seconds, int correctFlags) {
        int safeSeconds = Math.max(0, Math.min(seconds, MINESWEEPER_MAX_SECONDS));
        int safeFlags = Math.max(0, Math.min(correctFlags, MINESWEEPER_MAX_FLAGS));

        int points = MINESWEEPER_BASE
                - safeSeconds * MINESWEEPER_TIME_PENALTY_PER_SECOND
                + safeFlags * MINESWEEPER_FLAG_BONUS;
        points = Math.max(MINESWEEPER_MIN_SCORE, Math.min(MINESWEEPER_MAX_SCORE, points));

        GameStats stats = findOrCreate(userId, GameType.MINESWEEPER);
        stats.setWins(stats.getWins() + 1);
        stats.setScore(stats.getScore() + points);
        stats.setBestGame(Math.max(stats.getBestGame(), points));
        gameStatsRepository.save(stats);

        log.info("Игра: {} ({}) сапёр — {} очков за {} с, всего {}",
                username, userId, points, safeSeconds, stats.getScore());
        return stats.getScore();
    }

    @Transactional
    public int recordSolitaireWin(Long userId, String username, int seconds, int moves) {
        int safeSeconds = Math.max(0, Math.min(seconds, SOLITAIRE_MAX_SECONDS));
        int safeMoves = Math.max(0, Math.min(moves, SOLITAIRE_MAX_MOVES));

        int points = SOLITAIRE_WIN_BASE
                + Math.max(0, SOLITAIRE_SPEED_WINDOW_SECONDS - safeSeconds)
                + Math.min(safeMoves, SOLITAIRE_MOVES_BONUS_CAP);
        points = Math.max(SOLITAIRE_MIN_SCORE, Math.min(SOLITAIRE_MAX_SCORE, points));

        GameStats stats = findOrCreate(userId, GameType.SOLITAIRE);
        stats.setWins(stats.getWins() + 1);
        stats.setScore(stats.getScore() + points);
        stats.setBestGame(Math.max(stats.getBestGame(), points));
        gameStatsRepository.save(stats);

        log.info("Игра: {} ({}) пасьянс — {} очков за {} с и {} ходов, всего {}",
                username, userId, points, safeSeconds, safeMoves, stats.getScore());
        return stats.getScore();
    }

    /** Статистика по всем играм; игры, в которых ещё не играли, показываем нулями. */
    @Transactional(readOnly = true)
    public List<GameStats> statsFor(Long userId) {
        List<GameStats> stats = new ArrayList<>(gameStatsRepository.findAllByUserId(userId));
        for (GameType type : GameType.values()) {
            boolean present = stats.stream().anyMatch(s -> s.getGameType() == type);
            if (!present) {
                stats.add(GameStats.builder().userId(userId).gameType(type).build());
            }
        }
        stats.sort(Comparator.comparingInt(s -> s.getGameType().ordinal()));
        return stats;
    }

    @Transactional(readOnly = true)
    public int totalScore(Long userId) {
        return gameStatsRepository.findAllByUserId(userId).stream()
                .mapToInt(GameStats::getScore)
                .sum();
    }

    private GameStats findOrCreate(Long userId, GameType type) {
        return gameStatsRepository.findByUserIdAndGameType(userId, type)
                .orElseGet(() -> GameStats.builder().userId(userId).gameType(type).build());
    }
}