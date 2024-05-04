package ru.jamsys.core.extension;

public interface ComponentItemBuilder<T> {
    T build(String key);
}
