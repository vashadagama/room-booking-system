package com.example.booking.controller;

import com.example.booking.dto.room.RoomRequest;
import com.example.booking.dto.room.RoomResponse;
import com.example.booking.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public List<RoomResponse> getAll() {
        return roomService.getAllActive();
    }

    @GetMapping("/{id}")
    public RoomResponse getById(@PathVariable Long id) {
        return roomService.getById(id);
    }

    @PostMapping
    public ResponseEntity<RoomResponse> create(@Valid @RequestBody RoomRequest request) {
        RoomResponse response = roomService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public RoomResponse update(@PathVariable Long id,
                               @Valid @RequestBody RoomRequest request) {
        return roomService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        roomService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}