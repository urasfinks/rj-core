package ru.jamsys.extension;

public interface ComponentItemBuilder<T> {
    T build(String key);
}
