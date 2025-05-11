package ru.jamsys.core.extension.batch.writer;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.flat.util.UtilByte;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractAsyncFileReader {

    // Класс данных полезной нагрузки, что бы не пересериализовывать данные туда и обратно
    @Getter
    @Setter
    public static class DataPayload {

        private byte[] bytes;

        private Object object;

        public DataPayload(byte[] bytes, Object object) {
            this.bytes = bytes;
            this.object = object;
        }

    }

    // Сегментированные по позиции чтения данные из файла
    @Getter
    public static class FileReaderResult {

        private final ConcurrentHashMap<Long, DataPayload> mapData = new ConcurrentHashMap<>(); // key: position;

        private final AtomicInteger size = new AtomicInteger(0); // Счётчик оставшихся позиций

        @Setter
        private volatile boolean error = false; // Ошибка чтения данных

        @Setter
        private volatile boolean finishState = false; // Встретили -1 длину данных в bin

        public void add(long position, byte[] bytes, Object object) {
            mapData.computeIfAbsent(position, _ -> {
                size.incrementAndGet();
                return new DataPayload(bytes, object);
            });
        }

        public void remove(long position) {
            if (mapData.remove(position) != null) {
                size.decrementAndGet();
            }
        }

        public int size() {
            return size.get();
        }

    }

    // Может одновременно вестись запись в этот файл
    public static void read(String filePath, FileReaderResult fileReaderResult) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(filePath, "r")) {
            long currentPosition = 0;
            while (true) {
                byte[] lengthBytes = new byte[4]; // Читаем длину следующего блока (4 байта)
                if (file.read(lengthBytes) < 4) {
                    // EOF в процессе записи — файл может быть пуст или частично записан
                    break;
                }

                int length = UtilByte.bytesToInt(lengthBytes);

                if (length == -1) {
                    // Маркер конца файла
                    fileReaderResult.setFinishState(true);
                    break;
                }

                if (length < 0) {
                    fileReaderResult.setError(true);
                    App.error(new IOException("Invalid block in file '" + filePath + "' length encountered: " + length));
                    break;
                }

                long nextPosition = file.getFilePointer() + length;

                byte[] lengthBytesData = new byte[length];

                if (file.read(lengthBytesData) != length) {
                    fileReaderResult.setError(true);
                    App.error(new IOException("Unexpected end of file '" + filePath + "' while skipping content"));
                    break;
                }

                // Только если блок считан корректно — добавляем позицию
                fileReaderResult.add(currentPosition, lengthBytesData, null);
                currentPosition = nextPosition;
            }
        }
    }

}
