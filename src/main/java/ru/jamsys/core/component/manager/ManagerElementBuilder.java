package ru.jamsys.core.component.manager;

// E - element
// CA - CustomArgument

public interface ManagerElementBuilder<E, CA> {

    E build(String index, Class<?> classItem, CA customArgument);

}
