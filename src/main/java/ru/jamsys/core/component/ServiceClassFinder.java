package ru.jamsys.core.component;

import com.google.common.reflect.ClassPath;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.annotation.ServiceClassFinderIgnore;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyEnvelope;
import ru.jamsys.core.extension.property.repository.RepositoryProperty;
import ru.jamsys.core.flat.util.UtilLog;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

// Несёт информацию о загруженных классах ядра.
// Есть аннотация ServiceClassFinderIgnore, при которой класс ядра не попадает в availableClass
// Так сделаны плагины для HttpHandler, то есть плагины есть, но они все помечены аннотацией ServiceClassFinderIgnore
// Для того, что бы в проекте добавить плагин, надо убрать ServiceClassFinderIgnore с плагина
// Это можно сделать при помощи .properties:
// App.ServiceClassFinder.ignore.ru.jamsys.core.plugin.promise.DeeplinkSchemaApple=false
// По умолчанию включенный ServiceClassFinderIgnore подразумевает настройку в namespace = App.ServiceClassFinder.ignore
// полное_имя_класса = true, то есть игнорирование включено, если поставить false => игнорирование выключено

@Component
// @Lazy не должен быть ленивым, должен сразу инициализироваться так как является корневым для множества начальных
// компонентов
public class ServiceClassFinder implements CascadeKey {

    // Список доступных классов для создания через ядро
    private final List<Class<?>> availableClass = new ArrayList<>();

    private final RepositoryProperty<Boolean> ignoredClassMap = new RepositoryProperty<>(Boolean.class);

    public static String pkg = "ru.jamsys";

    public ServiceClassFinder() {
        PropertyDispatcher<Boolean> propertyDispatcher = new PropertyDispatcher<>(
                (_, _, newValue) -> {
                    UtilLog.printInfo("onUpdate ServiceClassFinderIgnore: " + newValue);
                    availableClass.clear();
                    availableClass.addAll(getAvailableClass(pkg));
                },
                ignoredClassMap,
                getCascadeKey("ignore")
        )
                .addSubscriptionRegexp(getCascadeKey("ignore.*"));
        propertyDispatcher.run();
        availableClass.clear();
        availableClass.addAll(getAvailableClass(pkg));
    }

    private <T> List<Class<T>> getActualType(Type[] genericInterfaces, Class<T> fnd) {
        List<Class<T>> result = new ArrayList<>();
        if (genericInterfaces != null) {
            for (Type type : genericInterfaces) {
                if (type instanceof ParameterizedType) {
                    Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
                    for (Type actualType : actualTypeArguments) {
                        String typeClass = actualType.getTypeName();
                        try {
                            Class<?> aClass = Class.forName(typeClass);
                            if (instanceOf(aClass, fnd)) {
                                @SuppressWarnings("unchecked")
                                Class<T> tmp = (Class<T>) aClass;
                                result.add(tmp);
                            }
                        } catch (Exception e) {
                            App.error(e);
                        }
                    }
                }
            }
        }
        return result;
    }

    public <T> T instanceOf(Class<T> cls) {
        if (availableClass.contains(cls)) {
            return App.get(cls);
        }
        return null;
    }

    public static boolean instanceOf(Class<?> cls, Class<?> interfaceRef) {
        return interfaceRef.isAssignableFrom(cls); //!cls.equals(interfaceRef) &&
    }

    public <T> List<Class<T>> findByInstance(Class<T> cls) {
        List<Class<T>> result = new ArrayList<>();
        for (Class<?> availableClass : availableClass) {
            if (instanceOf(availableClass, cls)) {
                @SuppressWarnings("unchecked")
                Class<T> tmp = (Class<T>) availableClass;
                result.add(tmp);
            }
        }
        return result;
    }

    public void removeAvailableClass(Class<?> cls, String cause) {
        UtilLog.error(null)
                .addHeader("description", "removeAvailableClass")
                .addHeader("classRemove", cls.getName())
                .addHeader("cause", cause)
                .print();
        availableClass.remove(cls);
    }

    private List<Class<?>> getAvailableClass(String packageName) {
        List<Class<?>> listClass = new ArrayList<>();
        try {
            ClassPath.from(getClass().getClassLoader()).getTopLevelClassesRecursive(packageName).forEach(info -> {
                try {
                    Class<?> aClass = Class.forName(info.getName());
                    if (Modifier.isAbstract(aClass.getModifiers())) {
                        return;
                    }
                    if (Modifier.isInterface(aClass.getModifiers())) {
                        return;
                    }
                    boolean findUnusedAnnotation = false;
                    for (Annotation annotation : aClass.getAnnotations()) {
                        if (instanceOf(annotation.annotationType(), ServiceClassFinderIgnore.class)) {
                            findUnusedAnnotation = true;
                            break;
                        }
                    }
                    PropertyEnvelope<Boolean> propertyEnvelope = ignoredClassMap.getByRepositoryPropertyKey(aClass.getName());
                    Boolean s = null;
                    if (propertyEnvelope != null) {
                        s = propertyEnvelope.getValue();
                    }
                    if (s != null) {
                        findUnusedAnnotation = s;
                    }
                    if (findUnusedAnnotation) {
                        return;
                    }
                    listClass.add(aClass);
                } catch (Throwable th) {
                    throw new ForwardException(info, th);
                }
            });
        } catch (Throwable th) {
            throw new ForwardException(packageName, th);
        }
        return listClass;
    }

    @SuppressWarnings("unused")
    public <T> List<Class<T>> getTypeSuperclass(Class<?> cls, Class<T> fnd) {
        return new ArrayList<>(getActualType(new Type[]{cls.getGenericSuperclass()}, fnd));
    }

    @SuppressWarnings("unused")
    public <T> List<Class<T>> getTypeInterface(Class<?> cls, Class<T> fnd) {
        return new ArrayList<>(getActualType(cls.getGenericInterfaces(), fnd));
    }

    @SuppressWarnings("unused")
    // Вернуть список доступных классов за исключением
    public <T> List<Class<T>> findByInstanceExclude(Class<T> inst, Class<?> exclude) {
        List<Class<T>> result = new ArrayList<>();
        for (Class<?> cls : availableClass) {
            if (instanceOf(cls, inst)) {
                @SuppressWarnings("unchecked")
                Class<T> tmp = (Class<T>) cls;
                if (!instanceOf(exclude, tmp)) {
                    result.add(tmp);
                }
            }
        }
        return result;
    }

}
