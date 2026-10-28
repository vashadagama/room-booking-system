package com.example.booking.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class WsEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void roomsChanged() {
        publisher.publishEvent(new RoomEvent.RoomsChanged());
    }

    public void bookingsChanged(Long roomId, LocalDate date) {
        publisher.publishEvent(new RoomEvent.BookingsChanged(roomId, date));
    }
}