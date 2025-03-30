package ru.jamsys.core.extension.broker.persist;

// Позиция данных в файле и размер

public interface FileDataPosition {

    FileDataPosition setFileDataPosition(long position);

    long getFileDataPosition();

    FileDataPosition setFileDataLength(int length);

    int getFileDataLength();

}
