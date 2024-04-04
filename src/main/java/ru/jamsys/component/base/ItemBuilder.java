package ru.jamsys.component.base;

public interface ItemBuilder<T> {
    T build(String key);
}
