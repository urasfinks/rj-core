package ru.jamsys.core.extension.async.writer;

import ru.jamsys.core.App;
import ru.jamsys.core.flat.util.UtilByte;

import java.io.IOException;
import java.io.RandomAccessFile;

public abstract class AbstractAsyncFileReader {

    // Может одновременно вестись запись в этот файл
    public static void read(String filePath, DataReader dataReader) throws IOException {
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
                    // Проверяем: действительно ли это конец файла, или за ним есть данные
                    if (file.getFilePointer() == file.length()) {
                        dataReader.setFinishState(true);
                        break;
                    } else {
                        // Продолжаем — это не настоящий конец, файл дописывался
                        continue;
                    }
                }

                if (length < 0) {
                    dataReader.setError(true);
                    App.error(new IOException("Invalid block in file '" + filePath + "' length encountered: " + length));
                    break;
                }

                long nextPosition = file.getFilePointer() + length;

                byte[] lengthBytesData = new byte[length];

                if (file.read(lengthBytesData) != length) {
                    dataReader.setError(true);
                    App.error(new IOException("Unexpected end of file '" + filePath + "' while skipping content"));
                    break;
                }

                // Только если блок считан корректно — добавляем позицию
                dataReader.add(currentPosition, lengthBytesData, null);
                currentPosition = nextPosition;
            }
        }
    }

}
