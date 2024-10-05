package ru.jamsys.core.i360.entity;

import ru.jamsys.core.i360.Context;

// Преобразователь контекста, должен быть идентпотентен (результат должен быть неизменяемым с течением времени)
// для решения обратных связей нужен другой экземпляр адаптера
public interface Adapter extends Entity {

    Context transform(Context context);

}
