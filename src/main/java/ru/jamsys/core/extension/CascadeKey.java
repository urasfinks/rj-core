package ru.jamsys.core.extension;

// Хотелось выстроить на Dashboard имена сервисов без пересечения имён
// Что хотелось бы сделать:
// 2) Сделать каскад, что бы нельзя было на пустом месте создать ключ
// Каждый UniqueClass должен иметь метод getKey() и getParentClass()
// Получается все должны быть унаследованы от App и далее

import ru.jamsys.core.App;

public interface CascadeKey {

    // Нельзя возвращать null, если нет родителя, используйте return App.cascadeName;
    default CascadeKey getParentCascadeKey() {
        return App.cascadeName;
    }

    default String getCascadeKey(String ns) {
        //return getCascadeKey() + append(ns);
        return complex(getCascadeKey(), ns);
    }

    default String getCascadeKey(String ns, String subClass) {
        return getCascadeKey(ns) + "::" + subClass;
    }

    default String getCascadeKey(String ns, Class<?> classItem) {
        return getCascadeKey(ns) + "<" + App.getUniqueClassName(classItem) + ">";
    }

    default String getCascadeKey() {
        return complex(getParentCascadeKey().getCascadeKey(), App.getUniqueClassName(getClass()));
    }

    static String append(String ns) {
        return (ns.contains(".") ? ("[" + ns + "]") : ("." + ns));
    }

    static String complex(String parent, String child) {
        return parent + append(child);
    }

    static String complexLinear(String parent, String child) {
        return parent == null ? child : (parent + "." + child);
    }

}
