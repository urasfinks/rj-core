package ru.jamsys.core.extension;

import org.springframework.context.ApplicationContext;

public class ClassNameImpl implements ClassName {

    public static String getClassNameStatic(Class<?> cls) {
        return new ClassNameImpl().getClassName(cls);
    }

    public static String getClassNameStatic(Class<?> cls, String index) {
        return new ClassNameImpl().getClassName(cls, index);
    }

    public static String getClassNameStatic(Class<?> cls, String index, ApplicationContext applicationContext) {
        return new ClassNameImpl().getClassName(cls, index, applicationContext);
    }

}
