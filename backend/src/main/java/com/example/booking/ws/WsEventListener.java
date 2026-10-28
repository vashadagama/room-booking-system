package com.example.booking.ws;

import com.example.booking.service.BookingService;
import com.example.booking.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class WsEventListener {

    private final RoomWsHandler handler;
    private final RoomService roomService;
    private final BookingService bookingService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onRoomsChanged(RoomEvent.RoomsChanged event) {
        try {
            handler.broadcast(WsMessage.roomsChanged(roomService.getAllActive()));
        } catch (Exception e) {
            log.error("Не удалось разослать ROOMS_CHANGED", e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onBookingsChanged(RoomEvent.BookingsChanged event) {
        try {
            var bookings = bookingService.getRoomBookingsForDay(event.roomId(), event.date());
            handler.broadcast(WsMessage.bookingsChanged(event.roomId(), event.date(), bookings));
        } catch (Exception e) {
            log.error("Не удалось разослать BOOKINGS_CHANGED для комнаты {}", event.roomId(), e);
        }
    }
}