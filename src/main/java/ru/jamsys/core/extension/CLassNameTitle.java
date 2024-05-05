package ru.jamsys.core.extension;

public interface CLassNameTitle {

    default String getClassNameTitle(String index) {
        return getClassNameTitle(getClass(), index);
    }

    default String getClassNameTitle(Class<?> cls, String index) {
        return cls.getName() + "::" + index;
    }

}
