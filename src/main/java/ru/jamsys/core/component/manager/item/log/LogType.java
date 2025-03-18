package ru.jamsys.core.component.manager.item.log;

import ru.jamsys.core.extension.CamelNormalization;

public enum LogType implements CamelNormalization {
    INFO, // Инфомрация, что действие совершено
    DEBUG, // Информация, что действие совершено + отладочная информация с ньюансами
    ERROR; // Действие совершенно с ошибками

    public static LogType valueOfOrdinal(int ordinal){
        LogType[] constants = LogType.class.getEnumConstants();
        if (ordinal >= 0 && ordinal < constants.length) {
            return constants[ordinal]; // Возвращаем константу по индексу
        }
        return null; // Если ordinal недопустим
    }
}
