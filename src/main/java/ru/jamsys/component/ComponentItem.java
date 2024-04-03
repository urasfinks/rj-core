package ru.jamsys.component;

public interface ComponentItem<T> {
    T createComponentItem(String key);
}
