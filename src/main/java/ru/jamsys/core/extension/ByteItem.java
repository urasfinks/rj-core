package ru.jamsys.core.extension;

import java.io.InputStream;

public interface ByteItem {

    byte[] getByteInstance() throws Exception;

    void instanceFromByte(InputStream fis) throws Exception;

}
