package ru.jamsys.core.extension.raw.writer;

public interface StatusCodeModel {

    // Код в диапазоне [0-15]
    int getByteIndex();

    String getDescription();

}
