package com.example.filecopy;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Общий контракт для всех способов копирования файла.
 * Три реализации: StreamCopy (java.io), BufferCopy (NIO buffer), TransferCopy (zero-copy).
 *
 * Единый интерфейс позволяет переключать стратегию флагом CLI и честно их сравнивать
 * в бенчмарке — логика вызова одинаковая, отличается только нутро copy().
 */
public interface CopyStrategy {

    /**
     * Копирует src в dst.
     *
     * @param src      исходный файл (должен существовать)
     * @param dst      файл назначения (будет создан/перезаписан)
     * @param progress колбэк прогресса; вызывается по мере копирования с числом
     *                 уже скопированных байт. Может быть NOOP, если прогресс не нужен.
     * @return общее число скопированных байт
     */
    long copy(Path src, Path dst, ProgressListener progress) throws IOException;

    /** Короткое имя стратегии для CLI и вывода (например, "stream"). */
    String name();

    /** Колбэк прогресса. Реализуй для прогресс-бара или передай NOOP. */
    @FunctionalInterface
    interface ProgressListener {
        void onProgress(long bytesCopied, long totalBytes);

        ProgressListener NOOP = (copied, total) -> { };
    }
}
