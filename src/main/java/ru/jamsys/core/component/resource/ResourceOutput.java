package ru.jamsys.core.component.resource;

// Ресурсом может являться пул ресурсов

@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface ResourceOutput<T> {

    // Записать в ресурс
    boolean write(T data);

}
