package ru.jamsys.core.extension.log;

import ru.jamsys.core.extension.ByteCodec;

import java.util.Map;

public interface DataPersistent extends ByteCodec {

    String getBodyString();

    Map<String, Object> getHeader();

    long getTimeAdd();

    void print();

}
