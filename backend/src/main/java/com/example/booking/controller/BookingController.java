package com.example.booking.controller;

import com.example.booking.dto.booking.BookingRequest;
import com.example.booking.dto.booking.BookingResponse;
import com.example.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping("/my")
    public List<BookingResponse> getMyBookings(@RequestParam Long userId) {
        return bookingService.getMyBookings(userId);
    }

    @GetMapping("/room/{roomId}")
    public List<BookingResponse> getRoomBookings(
            @PathVariable Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookingService.getRoomBookingsForDay(roomId, date);
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request) {
        BookingResponse response = bookingService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id,
                                       @RequestParam Long userId) {
        bookingService.cancel(id, userId);
        return ResponseEntity.noContent().build();
    }
}