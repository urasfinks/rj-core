package ru.jamsys.core.resource;

import java.util.function.Function;

public interface ResourceCheckException {

    // TODO: исправить просто на checkFatalException без передачи функции
    Function<Throwable, Boolean> getFatalException();

}
