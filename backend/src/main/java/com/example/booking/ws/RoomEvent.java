package com.example.booking.ws;

import java.time.LocalDate;

/**
 * События предметной области, о которых нужно сообщить подключённым клиентам.
 * Публикуются сервисами, слушаются в {@link WsEventListener} после коммита транзакции.
 */
public sealed interface RoomEvent {

    /** Список комнат изменился: добавили, переименовали или деактивировали. */
    record RoomsChanged() implements RoomEvent {}

    /** Расписание конкретной комнаты на конкретный день изменилось. */
    record BookingsChanged(Long roomId, LocalDate date) implements RoomEvent {}
}