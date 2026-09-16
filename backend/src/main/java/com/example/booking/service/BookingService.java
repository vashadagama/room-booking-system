package com.example.booking.service;

import com.example.booking.dto.booking.BookingRequest;
import com.example.booking.dto.booking.BookingResponse;
import com.example.booking.entity.Booking;
import com.example.booking.entity.BookingStatus;
import com.example.booking.entity.Room;
import com.example.booking.exception.BookingConflictException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    public List<BookingResponse> getMyBookings(Long userId) {
        return bookingRepository.findAllByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<BookingResponse> getRoomBookingsForDay(Long roomId, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
        return bookingRepository.findByRoomAndDay(roomId, dayStart, dayEnd)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BookingResponse create(BookingRequest request) {
        validateTimeRange(request.getStartTime(), request.getEndTime());

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + request.getRoomId()));

        List<Booking> overlapping = bookingRepository.findOverlappingBookings(
                request.getRoomId(), request.getStartTime(), request.getEndTime());

        if (!overlapping.isEmpty()) {
            throw new BookingConflictException("Room is already booked for the selected time slot");
        }

        Booking booking = Booking.builder()
                .room(room)
                .userId(request.getUserId())
                .userName(request.getUserName())
                .title(request.getTitle())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(BookingStatus.ACTIVE)
                .build();

        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public void cancel(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (!booking.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Booking not found for this user");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        if (start.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Start time must be in the future");
        }
    }

    private BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .roomId(booking.getRoom().getId())
                .roomName(booking.getRoom().getName())
                .userId(booking.getUserId())
                .userName(booking.getUserName())
                .title(booking.getTitle())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}