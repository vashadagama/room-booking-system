package com.example.booking.dto.admin;

import lombok.Builder;

import java.util.List;

/**
 * Сводка для админки. Считается одним запросом на каждый счётчик,
 * чтобы страница не делала десяток отдельных вызовов.
 */
@Builder
public record AdminSummaryResponse(
        long users,
        long admins,
        long rooms,
        long activeRooms,
        long bookings,
        long activeBookings,
        long bookingsLast30Days,
        int gameScoreSum,
        List<TopUser> topBookers,
        List<GameTotal> games
) {
    public record TopUser(Long userId, String userName, long bookings) {
    }

    public record GameTotal(String game, long players, long wins, long score) {
    }
}