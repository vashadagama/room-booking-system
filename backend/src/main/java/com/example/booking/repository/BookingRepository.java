package com.example.booking.repository;

import com.example.booking.entity.Booking;
import com.example.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findAllByUserId(Long userId);

    List<Booking> findAllByRoomIdAndStatus(Long roomId, BookingStatus status);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.room.id = :roomId
              AND b.status = 'ACTIVE'
              AND b.startTime < :endTime
              AND b.endTime > :startTime
            """)
    List<Booking> findOverlappingBookings(
            @Param("roomId") Long roomId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("""
            SELECT b FROM Booking b
            WHERE b.room.id = :roomId
              AND b.status = 'ACTIVE'
              AND b.startTime >= :dayStart
              AND b.endTime <= :dayEnd
            """)
    List<Booking> findByRoomAndDay(
            @Param("roomId") Long roomId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd
    );

    /** Брони за период — нужно и выгрузке в CSV, и сводке админки. */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.startTime >= :from AND b.startTime < :to
            ORDER BY b.startTime
            """)
    List<Booking> findInPeriod(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /** Сколько броней у каждого пользователя — для топа активности. */
    @Query("""
            SELECT b.userId AS userId, b.userName AS userName, COUNT(b) AS bookings
            FROM Booking b
            GROUP BY b.userId, b.userName
            ORDER BY COUNT(b) DESC
            """)
    List<BookingCount> countByUser();

    interface BookingCount {
        Long getUserId();

        String getUserName();

        long getBookings();
    }

    long countByStatus(BookingStatus status);

    long countByStartTimeGreaterThanEqualAndStartTimeLessThan(LocalDateTime from, LocalDateTime to);

    /** Активные брони, которые уже закончились: их статус не обновили. */
    @Query("SELECT b FROM Booking b WHERE b.status = 'ACTIVE' AND b.endTime < :now")
    List<Booking> findFinishedButActive(@Param("now") LocalDateTime now);

    /** Активные брони на отключённых комнатах — бронировать в них уже нельзя. */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.status = 'ACTIVE' AND b.room.isActive = false
            """)
    List<Booking> findActiveInInactiveRooms();
}
