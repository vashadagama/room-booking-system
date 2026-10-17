package com.example.booking.security;

import com.example.booking.exception.TooManyAttemptsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Счётчик неудачных попыток входа: не больше {@code maxAttempts} за окно
 * {@code windowMinutes}. Хранится в памяти процесса — этого достаточно для
 * одного инстанса, но при нескольких репликах счётчики не общие.
 *
 * Ключ — имя пользователя. Учти: это защищает от перебора, но позволяет
 * заблокировать конкретный аккаунт, засыпая его 5-ю попытками.
 */
@Component
public class LoginRateLimiter {

    private final Map<String, Attempts> failedByUser = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration window;

    public LoginRateLimiter(
            @Value("${security.login.max-attempts:5}") int maxAttempts,
            @Value("${security.login.window-minutes:10}") long windowMinutes) {
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofMinutes(windowMinutes);
    }

    public void checkNotBlocked(String username) {
        Attempts attempts = failedByUser.get(username);
        if (attempts == null) return;

        if (attempts.expired()) {
            failedByUser.remove(username, attempts);
            return;
        }

        if (attempts.count >= maxAttempts) {
            long minutesLeft = Math.max(1,
                    Duration.between(Instant.now(), attempts.expiresAt()).toMinutes() + 1);
            throw new TooManyAttemptsException(
                    "Too many failed login attempts. Try again in " + minutesLeft + " minute(s).");
        }
    }

    public void recordFailure(String username) {
        Instant now = Instant.now();
        failedByUser.compute(username, (key, existing) -> {
            if (existing == null || existing.expired()) {
                return new Attempts(1, now.plus(window));
            }
            return new Attempts(existing.count + 1, existing.expiresAt());
        });
    }

    public void reset(String username) {
        failedByUser.remove(username);
    }

    private record Attempts(int count, Instant expiresAt) {
        boolean expired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}