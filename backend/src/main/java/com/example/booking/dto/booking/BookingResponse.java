package com.example.booking.dto.booking;

import com.example.booking.entity.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BookingResponse {
    private Long id;
    private Long roomId;
    private String roomName;
    private Long userId;
    private String userName;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BookingStatus status;
    private LocalDateTime createdAt;
}