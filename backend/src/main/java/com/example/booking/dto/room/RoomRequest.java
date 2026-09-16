package com.example.booking.dto.room;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RoomRequest {

    @NotBlank(message = "Room name is required")
    @Size(max = 100)
    private String name;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    @NotBlank(message = "Location is required")
    @Size(max = 200)
    private String location;

    @Size(max = 500)
    private String description;

    private String equipment;
}