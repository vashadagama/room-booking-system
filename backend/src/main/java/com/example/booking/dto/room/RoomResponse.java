package com.example.booking.dto.room;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RoomResponse {
    private Long id;
    private String name;
    private Integer capacity;
    private String location;
    private String description;
    private String equipment;
    private Boolean isActive;
    private LocalDateTime createdAt;
}