package ru.jamsys.core.extension.async.writer;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ConcurrentLinkedDeque;

// Сегментированные по позиции чтения данные из файла
@Getter
public class FileReaderResult implements DataFromFile {

    private final ConcurrentLinkedDeque<DataPayload> queue = new ConcurrentLinkedDeque<>(); // key: position;

    @Setter
    private volatile boolean error = false; // Ошибка чтения данных

    @Setter
    private volatile boolean finishState = false; // Встретили -1 длину данных в bin

    @Override
    public void add(DataPayload dataPayload) {
        queue.add(dataPayload);
    }

    public void remove(long position) {
        queue.removeIf(dataPayload -> dataPayload.getPosition() == position);
    }

}
