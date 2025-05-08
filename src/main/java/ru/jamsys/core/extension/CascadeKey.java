package ru.jamsys.core.extension;

// Хотелось выстроить на Dashboard имена сервисов без пересечения имён
// Как-то всё вышло из-под контроля и появилась зависимость на ApplicationContext
// Что хотелось бы сделать:
// 1) Убрать ApplicationContext
// 2) Сделать каскад, что бы нельзя было на пустом месте создать ключ
// Каждый UniqueClass должен иметь метод getKey() и getParentClass()
// Получается все должны быть унаследованы от App и далее

import ru.jamsys.core.App;

public interface CascadeKey {

    // Если реализация является фабрикой, допустимо возвращать null
    default String getKey() {
        return null;
    }

    // Нельзя возвращать null, если нет родителя, используйте return App.cascadeName;
    default CascadeKey getParentCascadeKey() {
        return App.cascadeName;
    }

    default String getCascadeKey(String ns) {
        return getCascadeKey() + append(ns);
    }

    default String getCascadeKey(String ns, String subClass) {
        return getCascadeKey(ns) + "::" + subClass;
    }

    default String getCascadeKey(String ns, Class<?> classItem) {
        return getCascadeKey(ns) + "<" + App.getUniqueClassName(classItem) + ">";
    }

    default String getCascadeKey() {
        String key = getKey();
        if (key == null) {
            return getParentCascadeKey().getCascadeKey() + append(App.getUniqueClassName(getClass()));
        } else {
            return getParentCascadeKey().getCascadeKey() + append(App.getUniqueClassName(getClass())) + append(key);
        }
    }

    static String append(String ns) {
        return (ns.contains(".") ? ("[" + ns + "]") : ("." + ns));
    }

}
