package com.example.booking.controller;

import com.example.booking.dto.admin.AdminSummaryResponse;
import com.example.booking.dto.room.RoomResponse;
import com.example.booking.service.AdminService;
import com.example.booking.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final RoomService roomService;

    @GetMapping("/rooms")
    public List<RoomResponse> rooms() {
        return roomService.getAllIncludingInactive();
    }

    @GetMapping("/summary")
    public AdminSummaryResponse summary() {
        return adminService.summary();
    }
}