package ru.jamsys.core.i360.entity;

// Преобразователь контекста, должен быть идентпотентен (результат должен быть неизменяемым с течением времени)
// для решения обратных связей нужен другой экземпляр адаптера
public interface Adapter extends Entity {

    EntityChain transform(EntityChain entityChain);

}
