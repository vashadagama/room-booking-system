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
}