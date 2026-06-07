package com.example.filecopy;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Стратегия А — классический java.io через буферизованные потоки.
 * Это BASELINE: с ним сравниваем выигрыш NIO-стратегий.
 *
 * РЕАЛИЗОВАНА ПОЛНОСТЬЮ — служит образцом. Обрати внимание, как в цикл
 * встроен колбэк прогресса. В BufferCopy и TransferCopy сделаешь так же.
 */
public final class StreamCopy implements CopyStrategy {

    private static final int BUFFER_SIZE = 8192;

    @Override
    public long copy(Path src, Path dst, ProgressListener progress) throws IOException {
        long total = Files.size(src);
        long copied = 0;

        try (InputStream in = new BufferedInputStream(new FileInputStream(src.toFile()));
             OutputStream out = new BufferedOutputStream(new FileOutputStream(dst.toFile()))) {

            byte[] buf = new byte[BUFFER_SIZE];
            int n;
            while ((n = in.read(buf)) != -1) {   // -1 = конец файла
                out.write(buf, 0, n);            // пишем РОВНО n байт, не buf.length
                copied += n;
                progress.onProgress(copied, total);
            }
        }
        return copied;
    }

    @Override
    public String name() {
        return "stream";
    }
}
