package ru.jamsys.core.i360.entity;

import lombok.Getter;
import ru.jamsys.core.i360.Context;

import java.util.Map;

// Преобразователь контекста, должен быть идентпотентен (результат должен быть неизменяемым с течением времени)
// для решения обратных связей нужен другой экземпляр адаптера
@Getter
public class Adapter implements Entity {

    private String uuid;

    public Context transform(Context context) {
        return null;
    }

    public Adapter(Map<String, Object> map) {
    }

}
