package ru.jamsys.core.i360.entity;

import lombok.Getter;
import ru.jamsys.core.i360.Context;

// Преобразователь контекста, должен быть идентпотентен (результат должен быть неизменяемым с течением времени)
// для решения обратных связей нужен другой экземпляр адаптера
@Getter
public class Adapter implements Entity {

    final private String uuid = java.util.UUID.randomUUID().toString();

    public Context transform(Context context) {
        return null;
    }

}
