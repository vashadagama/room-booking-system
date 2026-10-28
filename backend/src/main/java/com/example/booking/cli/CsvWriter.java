package com.example.booking.cli;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class CsvWriter {

    private CsvWriter() {
    }

    static void write(Path file, List<String> headers, List<? extends List<?>> rows) {
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                out.write(String.join(",", headers.stream().map(CsvWriter::escape).toList()));
                out.write("\n");
                for (List<?> row : rows) {
                    out.write(String.join(",", row.stream().map(String::valueOf).map(CsvWriter::escape).toList()));
                    out.write("\n");
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось записать " + file, e);
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.indexOf(',') >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0;
        if (!needsQuotes) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}