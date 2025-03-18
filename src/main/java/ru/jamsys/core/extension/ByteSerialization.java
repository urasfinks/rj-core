package ru.jamsys.core.extension;

public interface ByteSerialization {

    byte[] toByte() throws Exception;

    void toObject(byte[] bytes) throws Exception;

}
