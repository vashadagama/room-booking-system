package com.example.booking.repository;

import com.example.booking.entity.GameStats;
import com.example.booking.entity.GameType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface GameStatsRepository extends JpaRepository<GameStats, Long> {

    Optional<GameStats> findByUserIdAndGameType(Long userId, GameType gameType);

    List<GameStats> findAllByUserId(Long userId);

    List<GameStats> findAllByOrderByScoreDesc();

    /**
     * Итоги по играм нативным запросом, а не через сущности: в базе
     * остаются строки со значением game_type, которого в enum больше нет
     * (например, BATTLESHIP), и материализация сущности на такой строке
     * роняет весь отчёт. Строкой такие значения проходят нормально —
     * так вдобавок видно, что тип игры выпал из проекта.
     */
    @Query(value = """
            SELECT game_type,
                   COUNT(*),
                   COALESCE(SUM(wins), 0),
                   COALESCE(SUM(score), 0)
            FROM game_stats
            GROUP BY game_type
            ORDER BY game_type
            """, nativeQuery = true)
    List<Object[]> gameTypeTotals();

    /** Кто вообще играл — строкой, чтобы не спотыкаться на неизвестных типах игр. */
    @Query(value = "SELECT DISTINCT user_id FROM game_stats", nativeQuery = true)
    List<Long> distinctUserIds();

    @Query(value = """
            SELECT user_id, game_type, wins, losses, score, best_game, updated_at
            FROM game_stats
            ORDER BY user_id, game_type
            """, nativeQuery = true)
    List<Object[]> rawStats();
}
