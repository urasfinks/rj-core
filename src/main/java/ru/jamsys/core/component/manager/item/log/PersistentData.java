package ru.jamsys.core.component.manager.item.log;

import ru.jamsys.core.extension.ByteSerialization;

import java.util.Map;

public interface PersistentData extends ByteSerialization {

    String getBody();

    Map<String, Object> getHeader();

    long getTimeAdd();

    void print();

}
