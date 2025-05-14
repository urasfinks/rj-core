package ru.jamsys.core.extension.batch.writer;

import ru.jamsys.core.App;
import ru.jamsys.core.flat.util.UtilByte;

import java.io.IOException;
import java.io.RandomAccessFile;

public abstract class AbstractAsyncFileReader {

    // Может одновременно вестись запись в этот файл
    public static void read(String filePath, DataFromFile dataFromFile) throws IOException {
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
                    dataFromFile.setFinishState(true);
                    break;
                }

                if (length < 0) {
                    dataFromFile.setError(true);
                    App.error(new IOException("Invalid block in file '" + filePath + "' length encountered: " + length));
                    break;
                }

                long nextPosition = file.getFilePointer() + length;

                byte[] lengthBytesData = new byte[length];

                if (file.read(lengthBytesData) != length) {
                    dataFromFile.setError(true);
                    App.error(new IOException("Unexpected end of file '" + filePath + "' while skipping content"));
                    break;
                }

                // Только если блок считан корректно — добавляем позицию
                dataFromFile.add(currentPosition, lengthBytesData, null);
                currentPosition = nextPosition;
            }
        }
    }

}
