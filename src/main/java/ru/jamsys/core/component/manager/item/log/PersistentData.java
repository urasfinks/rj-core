package ru.jamsys.core.component.manager.item.log;

import ru.jamsys.core.extension.ByteCodec;

import java.util.Map;

public interface PersistentData extends ByteCodec {

    String getBody();

    Map<String, Object> getHeader();

    long getTimeAdd();

    void print();

}
