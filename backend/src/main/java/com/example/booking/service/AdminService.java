package com.example.booking.service;

import com.example.booking.dto.admin.AdminSummaryResponse;
import com.example.booking.entity.BookingStatus;
import com.example.booking.entity.Role;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.GameStatsRepository;
import com.example.booking.repository.RoomRepository;
import com.example.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    /** Сколько человек показываем в «топе» броней. */
    private static final int TOP_BOOKERS = 5;

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final GameStatsRepository gameStatsRepository;

    @Transactional(readOnly = true)
    public AdminSummaryResponse summary() {
        LocalDateTime since = LocalDateTime.now().minusDays(30);

        List<AdminSummaryResponse.TopUser> topBookers = bookingRepository.countByUser().stream()
                .limit(TOP_BOOKERS)
                .map(c -> new AdminSummaryResponse.TopUser(c.getUserId(), c.getUserName(), c.getBookings()))
                .toList();

        // Итоги по играм берём нативным запросом: в базе могут остаться
        // значения game_type, которых в enum уже нет, и материализация
        // сущностей на них ломает всю страницу.
        List<AdminSummaryResponse.GameTotal> games = new ArrayList<>();
        int scoreSum = 0;
        for (Object[] row : gameStatsRepository.gameTypeTotals()) {
            games.add(new AdminSummaryResponse.GameTotal(
                    String.valueOf(row[0]), asLong(row[1]), asLong(row[2]), asLong(row[3])));
            scoreSum += asLong(row[3]);
        }

        return AdminSummaryResponse.builder()
                .users(userRepository.count())
                .admins(userRepository.countByRole(Role.ADMIN))
                .rooms(roomRepository.count())
                .activeRooms(roomRepository.countByIsActiveTrue())
                .bookings(bookingRepository.count())
                .activeBookings(bookingRepository.countByStatus(BookingStatus.ACTIVE))
                .bookingsLast30Days(
                        bookingRepository.countByStartTimeGreaterThanEqualAndStartTimeLessThan(since, LocalDateTime.now()))
                .gameScoreSum(scoreSum)
                .topBookers(topBookers)
                .games(games)
                .build();
    }

    /** PostgreSQL отдаёт bigint как Long, но на всякий случай приводим к long. */
    private long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}
