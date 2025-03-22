package ru.jamsys.core.extension.raw.writer;

public interface SubscriberStatusReadModel {

    // Код в диапазоне [0-15]
    int getByteIndex();

    String getDescription();

}
