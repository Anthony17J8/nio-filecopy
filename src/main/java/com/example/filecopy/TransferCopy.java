package com.example.filecopy;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Стратегия В — zero-copy через FileChannel.transferTo().
 * Данные НЕ заходят в твой код — на поддерживающих ОС перемещаются прямо
 * между буферами ядра (системный вызов sendfile на Linux). Минимум копий и
 * переключений контекста → ниже нагрузка на CPU.
 * <p>
 * Нюанс: transferTo за ОДИН вызов может перелить не весь файл, поэтому — в цикле.
 * transferTo индексируется long, так что годится и для файлов > 2 ГБ.
 */
public final class TransferCopy implements CopyStrategy {

    @Override
    public long copy(Path src, Path dst, ProgressListener progress) throws IOException {
        try (FileChannel in = FileChannel.open(src, StandardOpenOption.READ);
             FileChannel out = FileChannel.open(dst,
                     StandardOpenOption.WRITE,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING)) {

            long size = in.size();
            long position = 0;
            while (position < size) {
                long transferred = in.transferTo(position, size - position, out);
                position += transferred;
                progress.onProgress(position, size);
            }

            return position;
        }
    }

    @Override
    public String name() {
        return "transfer";
    }
}
