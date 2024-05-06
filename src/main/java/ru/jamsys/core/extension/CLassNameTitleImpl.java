package ru.jamsys.core.extension;

import org.springframework.context.ApplicationContext;

public class CLassNameTitleImpl implements CLassNameTitle {

    public static String getClassNameTitleStatic(Class<?> cls, String index) {
        return new CLassNameTitleImpl().getClassNameTitle(cls, index);
    }

    public static String getClassNameTitleStatic(Class<?> cls, String index, ApplicationContext applicationContext) {
        return new CLassNameTitleImpl().getClassNameTitle(cls, index, applicationContext);
    }

}
