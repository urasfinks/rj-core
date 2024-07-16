package ru.jamsys.core.extension;

import org.springframework.context.ApplicationContext;

public class UniqueClassNameImpl implements UniqueClassName {

    public static String getClassNameStatic(Class<?> cls) {
        return new UniqueClassNameImpl().getClassName(cls);
    }

    public static String getClassNameStatic(Class<?> cls, String index) {
        return new UniqueClassNameImpl().getClassName(cls, index);
    }

    public static String getClassNameStatic(Class<?> cls, String index, ApplicationContext applicationContext) {
        return new UniqueClassNameImpl().getClassName(cls, index, applicationContext);
    }

}
