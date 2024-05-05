package ru.jamsys.core.extension;

import org.springframework.context.ApplicationContext;
import ru.jamsys.core.App;
import ru.jamsys.core.component.api.ClassFinder;

public interface CLassNameTitle {

    default String getClassNameTitle(String index) {
        return getClassNameTitle(getClass(), index);
    }

    default String getClassNameTitle(String index, ApplicationContext applicationContext) {
        return getClassNameTitle(getClass(), index, applicationContext);
    }

    default String getClassNameTitle(Class<?> cls, String index, ApplicationContext applicationContext) {
        ClassFinder classFinder = applicationContext.getBean(ClassFinder.class);
        String clsName = cls.getName();
        if (classFinder.getUniqueClassName().containsKey(cls)) {
            clsName = classFinder.getUniqueClassName().get(cls);
        }
        return clsName + "::" + index;
    }

    default String getClassNameTitle(Class<?> cls, String index) {
        return getClassNameTitle(cls, index, App.context);
    }

}
