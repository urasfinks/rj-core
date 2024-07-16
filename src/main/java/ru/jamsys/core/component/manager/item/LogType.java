package ru.jamsys.core.component.manager.item;

import ru.jamsys.core.extension.CamelNormalization;

public enum LogType implements CamelNormalization {
    INFO, // Инфомрация, что действие совершено
    DEBUG, // Информация, что действие совершено + отладочная информация с ньюансами
    ERROR, // Действие совершенно с ошибками
    SYSTEM_EXCEPTION // Несвязанная с Promise ошибка
}
