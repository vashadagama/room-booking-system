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

    @Query(value = "SELECT DISTINCT user_id FROM game_stats", nativeQuery = true)
    List<Long> distinctUserIds();

    @Query(value = """
            SELECT user_id, game_type, wins, losses, score, best_game, updated_at
            FROM game_stats
            ORDER BY user_id, game_type
            """, nativeQuery = true)
    List<Object[]> rawStats();
}
