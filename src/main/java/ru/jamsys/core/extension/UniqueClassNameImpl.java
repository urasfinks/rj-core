package ru.jamsys.core.extension;

import org.springframework.context.ApplicationContext;

public class UniqueClassNameImpl implements UniqueClassName {

    public static String getClassNameStatic(Class<?> cls) {
        return new UniqueClassNameImpl().getClassName(cls);
    }

    public static String getClassNameStatic(Class<?> cls, String postfix) {
        return new UniqueClassNameImpl().getClassName(cls, postfix);
    }

    public static String getClassNameStatic(Class<?> cls, String postfix, ApplicationContext applicationContext) {
        return new UniqueClassNameImpl().getClassName(cls, postfix, applicationContext);
    }

}
