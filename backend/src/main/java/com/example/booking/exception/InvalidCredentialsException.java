package com.example.booking.exception;

/**
 * Неверный логин или пароль.
 * Намеренно не разделяем «нет такого пользователя» и «не тот пароль»,
 * чтобы нельзя было перебором выяснить, какие аккаунты существуют.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}