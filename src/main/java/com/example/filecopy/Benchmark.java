package com.example.filecopy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Простой бенчмарк-раннер: прогоняет каждую стратегию на одном файле и печатает таблицу.
 *
 * ВАЖНО про честность замера:
 *  - делаем прогревочный прогон (warmup) перед измерением — иначе первый прогон
 *    читает с диска, а следующие из page cache ОС, и цифры несравнимы;
 *  - это «грубый» замер через nanoTime. Для копирования файла (операция в секунды)
 *    он терпим. Для микробенчмарков (как heap vs direct на Этапе 1) нужен JMH.
 */
public final class Benchmark {

    private final List<CopyStrategy> strategies;

    public Benchmark(List<CopyStrategy> strategies) {
        this.strategies = strategies;
    }

    public void run(Path src, Path dst) throws IOException {
        long sizeBytes = Files.size(src);
        System.out.printf("Файл: %s (%.1f МБ)%n%n", src, sizeBytes / 1024.0 / 1024.0);
        System.out.printf("%-12s %12s %14s%n", "стратегия", "время, мс", "скорость, МБ/с");
        System.out.println("-".repeat(40));

        for (CopyStrategy s : strategies) {
            // прогрев: один холостой прогон, чтобы файл попал в кэш ОС
            s.copy(src, dst, CopyStrategy.ProgressListener.NOOP);

            long start = System.nanoTime();
            s.copy(src, dst, CopyStrategy.ProgressListener.NOOP);
            long elapsedNs = System.nanoTime() - start;

            double ms = elapsedNs / 1_000_000.0;
            double mbPerSec = (sizeBytes / 1024.0 / 1024.0) / (elapsedNs / 1_000_000_000.0);
            System.out.printf("%-12s %12.1f %14.1f%n", s.name(), ms, mbPerSec);
        }

        System.out.println("\nПримечание: цифры зависят от диска, кэша ОС и размера файла.");
        System.out.println("transferTo обычно выигрывает по нагрузке на CPU; по времени —");
        System.out.println("разница может упираться в скорость диска. Это нормальный результат.");
    }
}
