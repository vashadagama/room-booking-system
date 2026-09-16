package com.example.booking.config;

import com.example.booking.entity.Room;
import com.example.booking.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoomRepository roomRepository;

    @Override
    public void run(String... args) {
        if (roomRepository.count() == 0) {
            roomRepository.save(Room.builder()
                    .name("Mars")
                    .capacity(8)
                    .location("Floor 2, East Wing")
                    .description("Large meeting room with city view")
                    .equipment("projector,whiteboard,conference-phone")
                    .isActive(true)
                    .build());

            roomRepository.save(Room.builder()
                    .name("Venus")
                    .capacity(4)
                    .location("Floor 2, West Wing")
                    .description("Small room for quick syncs")
                    .equipment("tv,whiteboard")
                    .isActive(true)
                    .build());

            roomRepository.save(Room.builder()
                    .name("Saturn")
                    .capacity(12)
                    .location("Floor 3, North Wing")
                    .description("Conference room for large meetings")
                    .equipment("projector,whiteboard,video-conference")
                    .isActive(true)
                    .build());
        }
    }
}