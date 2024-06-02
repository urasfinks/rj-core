package ru.jamsys.core.extension;

public interface ByteItem {

    byte[] getByteInstance() throws Exception;

    void instanceFromByte(byte[] bytes) throws Exception;

}
