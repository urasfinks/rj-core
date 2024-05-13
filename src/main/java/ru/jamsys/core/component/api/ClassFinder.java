package ru.jamsys.core.component.api;

import lombok.Getter;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.extension.IgnoreClassFinder;

import javax.tools.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
public class ClassFinder {

    private final List<Class<?>> availableClass;

    // Используется для однозначности имён на графиках в CLassNameTitle
    @Getter
    private final Map<Class<?>, String> uniqueClassName = new HashMap<>();

    private final ExceptionHandler exceptionHandler;

    public ClassFinder(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        @SuppressWarnings("SameParameterValue")
        String pkg = "ru.jamsys";
        availableClass = getAvailableClass(pkg);
        fillUniqueClassName();
    }

    private void fillUniqueClassName() {
        Map<String, Integer> countDuplicateSimpleName = new HashMap<>();
        availableClass.forEach((Class<?> cls) -> {
            String simpleName = cls.getSimpleName();
            if (!simpleName.isEmpty()) {
                // Не конкурентная проверка
                if (!countDuplicateSimpleName.containsKey(simpleName)) {
                    countDuplicateSimpleName.put(simpleName, 0);
                }
                countDuplicateSimpleName.put(simpleName, countDuplicateSimpleName.get(simpleName) + 1);
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
                    if (actualTypeArguments != null) {
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

    public <T> List<Class<T>> findByInstance(Class<T> inst) {
        List<Class<T>> result = new ArrayList<>();
        for (Class<?> cls : availableClass) {
            if (instanceOf(cls, inst)) {
                @SuppressWarnings("unchecked")
                Class<T> tmp = (Class<T>) cls;
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

    private List<Class<?>> getAvailableClass(String packageName) {
        List<Class<?>> listClass = new ArrayList<>();
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

            StandardLocation location = StandardLocation.CLASS_PATH;
            Set<JavaFileObject.Kind> kinds = new HashSet<>();
            kinds.add(JavaFileObject.Kind.CLASS);
            Iterable<JavaFileObject> list = fileManager.list(location, packageName, kinds, true);

            String packageToDir = "/" + packageName.replace(".", "/") + "/";

            for (JavaFileObject classFile : list) {
                String pathClass = classFile.getName();
                String className = pathClass.substring(pathClass.indexOf(packageToDir) + packageToDir.length());
                className = packageName + "." + className.substring(0, className.length() - 6).replace("/", ".");
                Class<?> aClass = Class.forName(className);
                if (Modifier.isAbstract(aClass.getModifiers())) {
                    continue;
                }
                if (Modifier.isInterface(aClass.getModifiers())) {
                    continue;
                }
                boolean findUnusedAnnotation = false;
                for (Annotation annotation : aClass.getAnnotations()) {
                    if (instanceOf(annotation.annotationType(), IgnoreClassFinder.class)) {
                        findUnusedAnnotation = true;
                        break;
                    }
                }
                if (findUnusedAnnotation) {
                    continue;
                }
                listClass.add(aClass);
            }
        } catch (Exception e) {
            exceptionHandler.handler(e);
        }
        return listClass;
    }

}
