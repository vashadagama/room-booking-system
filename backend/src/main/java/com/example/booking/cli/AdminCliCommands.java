package com.example.booking.cli;

import com.example.booking.entity.Booking;
import com.example.booking.entity.Role;
import com.example.booking.entity.Room;
import com.example.booking.entity.User;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.GameStatsRepository;
import com.example.booking.repository.RoomRepository;
import com.example.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
@Profile("cli")
@RequiredArgsConstructor
public class AdminCliCommands {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final GameStatsRepository gameStatsRepository;

    @Command(name = "promote", description = "Выдать пользователю роль ADMIN: promote --username <логин>")
    @Transactional
    public String promote(
            @Option(longName = "username", description = "Логин пользователя", required = true) String username) {
        if (username.isBlank()) {
            return "Укажите логин: promote --username ivan";
        }
        User user = userRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден: " + username));

        if (user.getRole() == Role.ADMIN) {
            return user.getUsername() + " уже администратор";
        }
        user.setRole(Role.ADMIN);
        userRepository.save(user);
        return user.getUsername() + " теперь администратор. Токен нужно перевыпустить: войти заново.";
    }

    @Command(name = "report", description = "Показать аномалии в данных")
    @Transactional(readOnly = true)
    public String report() {
        LocalDateTime now = LocalDateTime.now();
        List<String> problems = new ArrayList<>();

        List<Booking> stale = bookingRepository.findFinishedButActive(now);
        if (!stale.isEmpty()) {
            problems.add("броней с прошедшим временем, но со статусом ACTIVE: " + stale.size()
                    + " (статус COMPLETED не выставляется нигде — их надо закрыть)");
        }

        List<Booking> inInactive = bookingRepository.findActiveInInactiveRooms();
        if (!inInactive.isEmpty()) {
            problems.add("активных броней в отключённых комнатах: " + inInactive.size()
                    + " (бронь нельзя ни отменить, ни продлить)");
        }

        List<String> duplicates = roomRepository.findDuplicateNames();
        if (!duplicates.isEmpty()) {
            problems.add("комнат с одинаковым именем: " + String.join(", ", duplicates));
        }

        Set<Long> booked = bookingRepository.findAll().stream().map(Booking::getUserId).collect(Collectors.toSet());
        Set<Long> played = new HashSet<>(gameStatsRepository.distinctUserIds());
        List<String> idle = userRepository.findAll().stream()
                .filter(u -> !booked.contains(u.getId()) && !played.contains(u.getId()))
                .map(User::getUsername)
                .toList();
        if (!idle.isEmpty()) {
            problems.add("пользователей без единой брони и без игр: " + idle.size()
                    + " (" + String.join(", ", idle) + ")");
        }

        if (problems.isEmpty()) {
            return "Аномалий не найдено.";
        }
        return "Найдено проблем: " + problems.size() + System.lineSeparator()
                + problems.stream().map(p -> " - " + p).collect(Collectors.joining(System.lineSeparator()));
    }

    @Command(name = "export", description = "Выгрузить данные в CSV: export [--from 2026-01-01] [--to 2026-12-31]")
    @Transactional(readOnly = true)
    public String export(
            @Option(longName = "from", defaultValue = "1970-01-01", description = "Начало периода, ГГГГ-ММ-ДД") String from,
            @Option(longName = "to", defaultValue = "2999-12-31", description = "Конец периода, ГГГГ-ММ-ДД") String to,
            @Option(longName = "out", defaultValue = "exports", description = "Каталог для файлов") String out) {
        LocalDate start = LocalDate.parse(from);
        LocalDate end = LocalDate.parse(to);
        Path dir = Path.of(out).toAbsolutePath();

        List<Booking> bookings = bookingRepository.findInPeriod(start.atStartOfDay(), end.plusDays(1).atStartOfDay());

        CsvWriter.write(dir.resolve("bookings.csv"),
                List.of("id", "room_id", "room_name", "user_id", "user_name", "title",
                        "start_time", "end_time", "status", "created_at"),
                bookings.stream().map(b -> List.of(
                        b.getId(), b.getRoom().getId(), b.getRoom().getName(), b.getUserId(),
                        b.getUserName(), b.getTitle(), b.getStartTime(), b.getEndTime(),
                        b.getStatus(), b.getCreatedAt())).toList());

        CsvWriter.write(dir.resolve("users.csv"),
                List.of("id", "username", "first_name", "last_name", "role", "created_at"),
                userRepository.findAll().stream().map(u -> List.of(
                        u.getId(), u.getUsername(), u.getFirstName(), u.getLastName(),
                        u.getRole(), u.getCreatedAt())).toList());

        List<Object[]> stats = gameStatsRepository.rawStats();
        CsvWriter.write(dir.resolve("game-stats.csv"),
                List.of("user_id", "game_type", "wins", "losses", "score", "best_game", "updated_at"),
                stats.stream().map(row -> List.of(
                        row[0], row[1], row[2], row[3], row[4], row[5], row[6])).toList());

        return "Выгружено в " + dir + ": броней " + bookings.size()
                + ", пользователей " + userRepository.count()
                + ", записей об играх " + stats.size();
    }

    @Command(name = "backup", description = "Сделать снимок базы в JSON: backup [--out backups]")
    @Transactional(readOnly = true)
    public String backup(
            @Option(longName = "out", defaultValue = "backups", description = "Каталог для файла") String out) {
        String stamp = LocalDateTime.now().toString().replace(':', '-').substring(0, 19);
        Path file = Path.of(out).toAbsolutePath().resolve("backup-" + stamp + ".json");

        Map<String, Object> snapshot = new TreeMap<>();
        snapshot.put("rooms", roomRepository.findAll().stream().map(this::roomLine).toList());
        snapshot.put("users", userRepository.findAll().stream().map(this::userLine).toList());
        snapshot.put("bookings", bookingRepository.findAll().stream().map(this::bookingLine).toList());
        snapshot.put("gameStats", gameStatsRepository.rawStats().stream().map(this::statsLine).toList());

        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, toJson(snapshot));
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось записать снимок: " + file, e);
        }

        return "Снимок записан: " + file + " (" + file.toFile().length() / 1024 + " КБ)";
    }

    private Map<String, Object> roomLine(Room room) {
        Map<String, Object> row = new TreeMap<>();
        row.put("id", room.getId());
        row.put("name", room.getName());
        row.put("capacity", room.getCapacity());
        row.put("location", room.getLocation());
        row.put("description", room.getDescription());
        row.put("equipment", room.getEquipment());
        row.put("isActive", room.getIsActive());
        row.put("createdAt", room.getCreatedAt());
        return row;
    }

    private Map<String, Object> userLine(User user) {
        Map<String, Object> row = new TreeMap<>();
        row.put("id", user.getId());
        row.put("username", user.getUsername());
        row.put("firstName", user.getFirstName());
        row.put("lastName", user.getLastName());
        row.put("role", user.getRole());
        row.put("createdAt", user.getCreatedAt());
        return row;
    }

    private Map<String, Object> bookingLine(Booking booking) {
        Map<String, Object> row = new TreeMap<>();
        row.put("id", booking.getId());
        row.put("roomId", booking.getRoom().getId());
        row.put("roomName", booking.getRoom().getName());
        row.put("userId", booking.getUserId());
        row.put("userName", booking.getUserName());
        row.put("title", booking.getTitle());
        row.put("startTime", booking.getStartTime());
        row.put("endTime", booking.getEndTime());
        row.put("status", booking.getStatus());
        row.put("createdAt", booking.getCreatedAt());
        return row;
    }

    private Map<String, Object> statsLine(Object[] stats) {
        Map<String, Object> row = new TreeMap<>();
        row.put("userId", stats[0]);
        row.put("gameType", stats[1]);
        row.put("wins", stats[2]);
        row.put("losses", stats[3]);
        row.put("score", stats[4]);
        row.put("bestGame", stats[5]);
        row.put("updatedAt", stats[6]);
        return row;
    }

    private String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append(jsonString(String.valueOf(entry.getKey()))).append(':').append(toJson(entry.getValue()));
            }
            return sb.append('}').toString();
        }
        if (value instanceof Iterable<?> list) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : list) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append(toJson(item));
            }
            return sb.append(']').toString();
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return jsonString(String.valueOf(value));
    }

    private String jsonString(String value) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}