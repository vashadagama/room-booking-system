package com.example.booking.ws;

import com.example.booking.dto.booking.BookingResponse;
import com.example.booking.dto.room.RoomResponse;

import java.time.LocalDate;
import java.util.List;

public record WsMessage(
        String type,
        List<RoomResponse> rooms,
        Long roomId,
        LocalDate date,
        List<BookingResponse> bookings
) {
    public static final String CONNECTED = "CONNECTED";
    public static final String ROOMS_CHANGED = "ROOMS_CHANGED";
    public static final String BOOKINGS_CHANGED = "BOOKINGS_CHANGED";

    public static WsMessage connected() {
        return new WsMessage(CONNECTED, null, null, null, null);
    }

    public static WsMessage roomsChanged(List<RoomResponse> rooms) {
        return new WsMessage(ROOMS_CHANGED, rooms, null, null, null);
    }

    public static WsMessage bookingsChanged(Long roomId, LocalDate date, List<BookingResponse> bookings) {
        return new WsMessage(BOOKINGS_CHANGED, null, roomId, date, bookings);
    }
}