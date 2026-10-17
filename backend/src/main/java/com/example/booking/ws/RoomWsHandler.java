package com.example.booking.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Держит открытые соединения и рассылает им сообщения.
 *
 * Соединения публичные: список комнат и расписание и так доступны всем
 * через REST без токена, поэтому сокет ничего дополнительно не раскрывает.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RoomWsHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("WS: подключение {} (всего {})", session.getId(), sessions.size());
        send(session, WsMessage.connected());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("WS: отключение {} (осталось {})", session.getId(), sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WS: ошибка соединения {}: {}", session.getId(), exception.getMessage());
        sessions.remove(session.getId());
        closeQuietly(session);
    }

    public void broadcast(WsMessage message) {
        String json = serialize(message);

        for (WebSocketSession session : sessions.values()) {
            sendRaw(session, json);
        }
    }

    /**
     * Пингуем раз в минуту и попутно вычищаем мёртвые соединения.
     * Без этого вкладка, закрытая без корректного закрытия, осталась бы
     * в реестре навсегда.
     */
    @Scheduled(fixedDelay = 60_000)
    public void heartbeat() {
        sessions.values().removeIf(session -> {
            if (!session.isOpen()) {
                sessions.remove(session.getId());
                closeQuietly(session);
                return true;
            }
            try {
                session.sendMessage(new PingMessage());
                return false;
            } catch (IOException e) {
                sessions.remove(session.getId());
                closeQuietly(session);
                return true;
            }
        });
    }

    public int sessionCount() {
        return sessions.size();
    }

    private void send(WebSocketSession session, WsMessage message) {
        sendRaw(session, serialize(message));
    }

    private void sendRaw(WebSocketSession session, String json) {
        if (!session.isOpen()) {
            sessions.remove(session.getId());
            return;
        }
        try {
            // WebSocketSession не потокобезопасен для одновременной записи,
            // а рассылка и пинг могут попасть в один момент из разных потоков.
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException e) {
            sessions.remove(session.getId());
            closeQuietly(session);
        }
    }

    private String serialize(WsMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (RuntimeException e) {
            // Jackson 3 не бросает проверяемых исключений, но оборачивать
            // всё равно полезно: молча уронить рассылку нельзя.
            throw new IllegalStateException("Не удалось сериализовать WS-сообщение: " + message.type(), e);
        }
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException ignored) {
            // закрытие уже не удалось — ничего не сделать
        }
    }
}