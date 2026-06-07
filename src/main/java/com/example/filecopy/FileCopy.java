package com.example.filecopy;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Точка входа и CLI.
 *
 * Использование:
 *   java -jar filecopy.jar &lt;src&gt; &lt;dst&gt; [--strategy=stream|buffer|transfer] [--benchmark]
 *
 * Примеры:
 *   java -jar filecopy.jar big.bin copy.bin --strategy=transfer
 *   java -jar filecopy.jar big.bin copy.bin --benchmark
 */
public final class FileCopy {

    // Реестр стратегий по имени. Добавишь новую — зарегистрируй здесь.
    private static final Map<String, CopyStrategy> STRATEGIES =
            List.of(new StreamCopy(), new BufferCopy(), new TransferCopy())
                .stream()
                .collect(Collectors.toMap(CopyStrategy::name, Function.identity()));

    public static void main(String[] args) {
        try {
            run(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Ошибка: " + e.getMessage());
            printUsage();
            System.exit(2);
        } catch (NoSuchFileException e) {
            System.err.println("Исходный файл не найден: " + e.getFile());
            System.exit(1);
        } catch (AccessDeniedException e) {
            System.err.println("Нет прав доступа: " + e.getFile());
            System.exit(1);
        } catch (IOException e) {
            // сюда попадёт, например, "диск полон" (No space left on device)
            System.err.println("Ошибка ввода-вывода: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void run(String[] args) throws IOException {
        if (args.length < 2) {
            throw new IllegalArgumentException("нужно указать <src> и <dst>");
        }

        Path src = Path.of(args[0]);
        Path dst = Path.of(args[1]);
        String strategyName = "buffer";   // по умолчанию
        boolean benchmark = false;

        for (int i = 2; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--strategy=")) {
                strategyName = arg.substring("--strategy=".length());
            } else if (arg.equals("--benchmark")) {
                benchmark = true;
            } else {
                throw new IllegalArgumentException("неизвестный аргумент: " + arg);
            }
        }

        // Проверки до старта — внятные сообщения вместо голого stacktrace
        if (!Files.exists(src)) {
            throw new NoSuchFileException(src.toString());
        }
        if (!Files.isRegularFile(src)) {
            throw new IllegalArgumentException("источник не является файлом: " + src);
        }
        if (src.toAbsolutePath().normalize().equals(dst.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("источник и назначение совпадают");
        }

        if (benchmark) {
            new Benchmark(List.copyOf(STRATEGIES.values())).run(src, dst);
            return;
        }

        CopyStrategy strategy = STRATEGIES.get(strategyName);
        if (strategy == null) {
            throw new IllegalArgumentException(
                    "неизвестная стратегия '" + strategyName + "'. Доступны: " + STRATEGIES.keySet());
        }

        System.out.printf("Копирую %s → %s [стратегия: %s]%n", src, dst, strategy.name());
        long start = System.nanoTime();
        long bytes = strategy.copy(src, dst, FileCopy::printProgress);
        double ms = (System.nanoTime() - start) / 1_000_000.0;
        System.out.printf("%nГотово: %d байт за %.1f мс%n", bytes, ms);
    }

    // Простой прогресс-бар в одну строку (перезаписывает себя через \r).
    private static void printProgress(long copied, long total) {
        if (total <= 0) return;
        int percent = (int) (copied * 100 / total);
        int filled = percent / 2;   // шкала из 50 символов
        String bar = "#".repeat(filled) + "-".repeat(50 - filled);
        System.out.printf("\r[%s] %3d%%", bar, percent);
    }

    private static void printUsage() {
        System.err.println("""
                Использование:
                  java -jar filecopy.jar <src> <dst> [--strategy=stream|buffer|transfer] [--benchmark]

                Стратегии:
                  stream    классический java.io (baseline)
                  buffer    NIO через ByteBuffer
                  transfer  zero-copy через transferTo
                """);
    }
}
