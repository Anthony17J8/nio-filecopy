package com.example.filecopy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Проверяет КОРРЕКТНОСТЬ: копия должна быть побайтово равна оригиналу.
 * Сравниваем по SHA-256 — надёжнее, чем длина или беглый просмотр.
 *
 * Параметризованный тест прогоняет ВСЕ три стратегии. Пока BufferCopy и
 * TransferCopy не реализованы (бросают UnsupportedOperationException),
 * их ветки будут падать — это нормально, тест «зазеленеет» по мере реализации.
 */
class CopyStrategyTest {

    // Источник стратегий для параметризованного теста
    static Stream<CopyStrategy> strategies() {
        return Stream.of(new StreamCopy(), new BufferCopy(), new TransferCopy());
    }

    @ParameterizedTest(name = "{0} копирует байт-в-байт")
    @MethodSource("strategies")
    void copiesIdentically(CopyStrategy strategy, @TempDir Path dir) throws Exception {
        // arrange: создаём исходный файл со случайными данными (~2 МБ)
        Path src = dir.resolve("src.bin");
        Path dst = dir.resolve("dst-" + strategy.name() + ".bin");
        byte[] data = new byte[2 * 1024 * 1024];
        new Random(42).nextBytes(data);
        Files.write(src, data);

        // act
        long copied = strategy.copy(src, dst, CopyStrategy.ProgressListener.NOOP);

        // assert: размер, число скопированных байт и контрольная сумма совпадают
        assertEquals(Files.size(src), Files.size(dst), "размеры различаются");
        assertEquals(data.length, copied, "вернулось неверное число байт");
        assertArrayEquals(sha256(src), sha256(dst), "содержимое различается (SHA-256)");
    }

    @Test
    void overwritesLongerFileWithoutTail(@TempDir Path dir) throws Exception {
        // регрессия на грабли с TRUNCATE_EXISTING:
        // если dst был длиннее src, "хвост" не должен остаться
        Path src = dir.resolve("short.bin");
        Path dst = dir.resolve("long.bin");
        Files.write(src, "short".getBytes());
        Files.write(dst, "this is a much longer existing file".getBytes());

        new StreamCopy().copy(src, dst, CopyStrategy.ProgressListener.NOOP);

        assertEquals(Files.size(src), Files.size(dst), "остался хвост старого файла");
        assertArrayEquals(sha256(src), sha256(dst));
    }

    private static byte[] sha256(Path file) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(Files.readAllBytes(file));
    }
}
