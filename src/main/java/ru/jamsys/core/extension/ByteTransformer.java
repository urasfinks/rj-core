package ru.jamsys.core.extension;

public interface ByteTransformer {

    byte[] getByteInstance() throws Exception;

    void instanceFromByte(byte[] bytes) throws Exception;

}
