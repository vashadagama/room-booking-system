package com.example.booking.service;

import com.example.booking.dto.room.RoomRequest;
import com.example.booking.dto.room.RoomResponse;
import com.example.booking.entity.Room;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.RoomRepository;
import com.example.booking.ws.WsEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final WsEventPublisher wsEvents;

    public List<RoomResponse> getAllActive() {
        return roomRepository.findAllByIsActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Для админки: нужны и отключённые комнаты, их больше не видно в списке. */
    public List<RoomResponse> getAllIncludingInactive() {
        return roomRepository.findAll()
                .stream()
                .map(this::toResponse)
                .sorted(Comparator.comparing(RoomResponse::getName))
                .toList();
    }

    public RoomResponse getById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
        return toResponse(room);
    }

    @Transactional
    public RoomResponse create(RoomRequest request) {
        Room room = Room.builder()
                .name(request.getName())
                .capacity(request.getCapacity())
                .location(request.getLocation())
                .description(request.getDescription())
                .equipment(request.getEquipment())
                .isActive(true)
                .build();

        RoomResponse response = toResponse(roomRepository.save(room));
        wsEvents.roomsChanged();
        return response;
    }

    @Transactional
    public RoomResponse update(Long id, RoomRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
        room.setName(request.getName());
        room.setCapacity(request.getCapacity());
        room.setLocation(request.getLocation());
        room.setDescription(request.getDescription());
        room.setEquipment(request.getEquipment());
        RoomResponse response = toResponse(roomRepository.save(room));
        wsEvents.roomsChanged();
        return response;
    }

    @Transactional
    public void deactivate(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
        room.setIsActive(false);
        roomRepository.save(room);
        wsEvents.roomsChanged();
    }

    private RoomResponse toResponse(Room room) {
        return RoomResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .capacity(room.getCapacity())
                .location(room.getLocation())
                .description(room.getDescription())
                .equipment(room.getEquipment())
                .isActive(room.getIsActive())
                .createdAt(room.getCreatedAt())
                .build();
    }
}