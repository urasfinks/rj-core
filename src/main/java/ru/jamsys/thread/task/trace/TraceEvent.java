package ru.jamsys.thread.task.trace;

import ru.jamsys.extension.EnumName;

public enum TraceEvent implements EnumName {
    CREATE,
    EXECUTE,
    TRANSIT,
    ERROR
}
