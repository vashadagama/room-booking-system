package com.example.booking.dto.admin;

import lombok.Builder;

import java.util.List;

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