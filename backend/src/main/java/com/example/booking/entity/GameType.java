package com.example.booking.entity;

/**
 * Игры, в которых считаются очки. Значение попадает в БД как строка,
 * поэтому переименование enum'а сломает уже сохранённую статистику.
 */
public enum GameType {
    MINESWEEPER,
    SOLITAIRE
}
