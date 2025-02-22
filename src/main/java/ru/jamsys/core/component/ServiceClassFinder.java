package ru.jamsys.core.component;

import com.google.common.reflect.ClassPath;
import lombok.Getter;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.core.extension.annotation.IgnoreClassFinder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.property.PropertySubscriber;
import ru.jamsys.core.extension.property.repository.PropertyRepositoryMap;
import ru.jamsys.core.flat.util.Util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ServiceClassFinder {

    private final List<Class<?>> availableClass;

    // Используется для однозначности имён на графиках в ClassNameTitle
    @Getter
    private final Map<Class<?>, String> uniqueClassName = new HashMap<>();

    private final ExceptionHandler exceptionHandler;

    private final PropertyRepositoryMap<Boolean> ignoredClassMap = new PropertyRepositoryMap<>(Boolean.class);

    private final ApplicationContext applicationContext;

    public ServiceClassFinder(
            ApplicationContext applicationContext,
            ExceptionHandler exceptionHandler,
            ServiceProperty serviceProperty
    ) {
        this.applicationContext = applicationContext;
        this.exceptionHandler = exceptionHandler;

        @SuppressWarnings("SameParameterValue")
        String pkg = "ru.jamsys";
        availableClass = getAvailableClass(pkg);

        fillUniqueClassName();

        PropertySubscriber ignoredClassAgent = new PropertySubscriber(
                serviceProperty,
                (_, _, property) -> {
                    Util.logConsoleJson(getClass(), "onUpdate IgnoreClassFinder: " + property.get());
                    availableClass.clear();
                    availableClass.addAll(getAvailableClass(pkg));
                    fillUniqueClassName();
                },
                ignoredClassMap,
                "run.args.IgnoreClassFinder"
        );

        ignoredClassAgent.addSubscriptionRegexp("run\\.args\\.IgnoreClassFinder.*");
    }

    private void fillUniqueClassName() {
        Map<String, Integer> countDuplicateSimpleName = new HashMap<>();
        availableClass.forEach((Class<?> cls) -> {
            String simpleName = cls.getSimpleName();
            if (!simpleName.isEmpty()) {
                int cur = countDuplicateSimpleName.computeIfAbsent(simpleName, _ -> 0);
                countDuplicateSimpleName.put(simpleName, cur + 1);
            }
        });
        availableClass.forEach((Class<?> cls) -> {
            String simpleName = cls.getSimpleName();
            if (!simpleName.isEmpty()) {
                if (countDuplicateSimpleName.get(simpleName) > 1) {
                    uniqueClassName.put(cls, cls.getName());
                    System.err.println("Duplicate class name: " + cls.getName());
                } else {
                    uniqueClassName.put(cls, cls.getSimpleName());
                }
            }
        });
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
                            exceptionHandler.handler(e);
                        }
                    }
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unused")
    public <T> List<Class<T>> getTypeSuperclass(Class<?> cls, Class<T> fnd) {
        return new ArrayList<>(getActualType(new Type[]{cls.getGenericSuperclass()}, fnd));
    }

    public <T> List<Class<T>> getTypeInterface(Class<?> cls, Class<T> fnd) {
        return new ArrayList<>(getActualType(cls.getGenericInterfaces(), fnd));
    }

    public <T> T instanceOf(Class<T> cls) {
        if (availableClass.contains(cls)) {
            return applicationContext.getBean(cls);
        }
        return null;
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


    public static boolean instanceOf(Class<?> cls, Class<?> interfaceRef) {
        return interfaceRef.isAssignableFrom(cls); //!cls.equals(interfaceRef) &&
    }

    public void removeAvailableClass(Class<?> cls, String cause) {
        Util.logConsole(
                getClass(),
                "removeAvailableClass: " + cls.getName() + "; cause: " + cause,
                true
        );
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
                        if (instanceOf(annotation.annotationType(), IgnoreClassFinder.class)) {
                            findUnusedAnnotation = true;
                            break;
                        }
                    }
                    Boolean s = ignoredClassMap.getRepositoryItem(aClass.getName());
                    if (s != null) {
                        findUnusedAnnotation = s;
                    }
                    if (findUnusedAnnotation) {
                        return;
                    }
                    listClass.add(aClass);
                } catch (Throwable th) {
                    throw new ForwardException(th);
                }
            });

        } catch (Throwable th) {
            throw new ForwardException(th);
        }
        return listClass;
    }

}
