package ru.jamsys.core.component.manager.sub;

// BE - BuildElement
// BA - BuilderArgument

public interface ManagerElementBuilder<BE, BA> {

    BE build(String index, Class<?> classItem, BA builderArgument);

}
