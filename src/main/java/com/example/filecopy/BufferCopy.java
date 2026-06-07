package com.example.filecopy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Стратегия Б — NIO через ByteBuffer.
 * Данные проходят ЧЕРЕЗ твой буфер в пользовательском пространстве:
 * диск → буфер ядра → твой ByteBuffer → буфер ядра → диск.
 * <p>
 * Это ровно тот цикл, который ты заполнял в скелете BufferCopyBenchmark.java.
 * Перенеси его сюда, добавив вызовы progress.onProgress(...) как в StreamCopy.
 */
public final class BufferCopy implements CopyStrategy {

    private static final int BUFFER_SIZE = 8192;

    @Override
    public long copy(Path src, Path dst, ProgressListener progress) throws IOException {
        try (FileChannel in = FileChannel.open(src, StandardOpenOption.READ);
             FileChannel out = FileChannel.open(dst,
                     StandardOpenOption.WRITE,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING)) {   // ← обнуляем, чтобы не было "хвоста"

            long total = in.size();
            long copied = 0;
            ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);


            while (in.read(buf) != -1) {
                buf.flip();                       // переключить на чтение
                while (buf.hasRemaining()) {
                    copied += out.write(buf);     // write может записать НЕ ВСЁ
                }
                buf.clear();                      // готовим к следующему read
                progress.onProgress(copied, total);
            }
            return copied;
        }
    }

    @Override
    public String name() {
        return "buffer";
    }
}
