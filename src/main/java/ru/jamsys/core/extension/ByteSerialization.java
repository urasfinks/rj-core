package ru.jamsys.core.extension;

public interface ByteSerialization {

    // Это некоторое число, которое вы можете использовать в своих интересах, которое не относится к данным.
    // Прямо сейчас это реализуется для persist транзакций, что мы их обработали.
    // Будем это флаг (число) менять прямо в файле при помощи RandomAccessFile
    short getSubscriberStatusRead();

    void setSubscriberStatusRead(short subscriberStatusRead);

    byte[] toByte() throws Exception;

    void toObject(byte[] bytes) throws Exception;

}
