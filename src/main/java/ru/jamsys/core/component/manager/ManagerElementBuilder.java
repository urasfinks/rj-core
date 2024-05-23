package ru.jamsys.core.component.manager;

public interface ManagerElementBuilder<T> {

    T build(String index, Class<?> classItem);

}
