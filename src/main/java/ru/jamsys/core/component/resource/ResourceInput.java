package ru.jamsys.core.component.resource;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface ResourceInput<T> {

    // Считать из ресурса
    T read();

}
