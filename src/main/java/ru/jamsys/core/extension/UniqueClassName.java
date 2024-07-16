package ru.jamsys.core.extension;

import org.springframework.context.ApplicationContext;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceClassFinder;

public interface UniqueClassName {

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
        ServiceClassFinder serviceClassFinder = applicationContext.getBean(ServiceClassFinder.class);
        String clsName = cls.getName();
        //  Не конкурентная проверка
        if (serviceClassFinder.getUniqueClassName().containsKey(cls)) {
            clsName = serviceClassFinder.getUniqueClassName().get(cls);
        }
        return index == null ? clsName : (clsName + "." + index);
    }

}
