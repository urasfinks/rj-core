package ru.jamsys.core.extension;

// Хотелось выстроить на Dashboard имена сервисов без пересечения имён
// Как-то всё вышло из-под контроля и появилась зависимость на ApplicationContext
// Что хотелось бы сделать:
// 1) Убрать ApplicationContext
// 2) Сделать каскад, что бы нельзя было на пустом месте создать ключ
// Каждый UniqueClass должен иметь метод getKey() и getParentClass()
// Получается все должны быть унаследованы от App и далее

import ru.jamsys.core.App;

public interface CascadeName {

    String getKey();

    CascadeName getParentCascadeName();

    default String getCascadeName(String ns) {
        return getCascadeName() + "." + ns;
    }

    default String getCascadeName(String ns, Class<?> classItem) {
        return getCascadeName() + "[" + ns + "]<" + App.getUniqueClassName(classItem) + ">";
    }

    default String getCascadeName() {
        String key = getKey();
        if (key == null) {
            return getParentCascadeName().getCascadeName() + "." + App.getUniqueClassName(getClass());
        } else {
            return getParentCascadeName().getCascadeName() + "." + App.getUniqueClassName(getClass()) + "." + key;
        }
    }

}
