package ru.jamsys.core.extension;

import org.springframework.context.ApplicationContext;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ClassFinderComponent;

public interface ClassName {

    default String getClassName(String index) {
        return getClassName(getClass(), index);
    }

    default String getClassName() {
        return getClassName(getClass());
    }

    default String getClassName(String index, ApplicationContext applicationContext) {
        return getClassName(getClass(), index, applicationContext);
    }

    default String getClassName(ApplicationContext applicationContext) {
        return getClassName(getClass(), null, applicationContext);
    }

    default String getClassName(Class<?> cls, String index) {
        return getClassName(cls, index, App.context);
    }

    default String getClassName(Class<?> cls) {
        return getClassName(cls, null, App.context);
    }

    default String getClassName(Class<?> cls, String index, ApplicationContext applicationContext) {
        ClassFinderComponent classFinderComponent = applicationContext.getBean(ClassFinderComponent.class);
        String clsName = cls.getName();
        //  Не конкурентная проверка
        if (classFinderComponent.getUniqueClassName().containsKey(cls)) {
            clsName = classFinderComponent.getUniqueClassName().get(cls);
        }
        return index == null ? clsName : (clsName + "::" + index);
    }

}
