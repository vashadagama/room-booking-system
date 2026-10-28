package com.example.booking.ws;

import java.time.LocalDate;

public sealed interface RoomEvent {
    record RoomsChanged() implements RoomEvent {}
    record BookingsChanged(Long roomId, LocalDate date) implements RoomEvent {}
}