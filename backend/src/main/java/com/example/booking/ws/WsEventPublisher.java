package com.example.booking.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Тонкая обёртка над {@link ApplicationEventPublisher}, чтобы сервисам
 * не приходилось знать про механику событий и WebSocket.
 */
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