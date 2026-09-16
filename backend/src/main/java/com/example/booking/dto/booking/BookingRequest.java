package com.example.booking.dto.booking;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingRequest {

    @NotNull(message = "Room ID is required")
    private Long roomId;

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    // Пока авторизации нет — передаём вручную.
    // В лабе 7 заменим на данные из JWT.
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "User name is required")
    private String userName;
}