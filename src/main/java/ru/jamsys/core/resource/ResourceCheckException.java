package ru.jamsys.core.resource;

import java.util.function.Function;

public interface ResourceCheckException {

    Function<Throwable, Boolean> getFatalException();

}
