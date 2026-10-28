package com.example.booking.repository;

import com.example.booking.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findAllByIsActiveTrue();

    boolean existsByName(String name);

    long countByIsActiveTrue();

    @Query("SELECT r.name FROM Room r GROUP BY r.name HAVING COUNT(r) > 1")
    List<String> findDuplicateNames();
}
