package ru.jamsys.core.component.manager.sub;

// E - BuildElement (Element)
// EBA - ElementBuilderArgument

public interface ManagerElementBuilder<E, EBA> {

    E build(String index, Class<?> classItem, EBA builderArgument);

}
