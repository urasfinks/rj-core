package ru.jamsys.core.component.manager;

public interface ManagerItemBuilder<T> {

    T build(String index, Class<?> classItem);

}
