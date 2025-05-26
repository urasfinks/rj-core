package ru.jamsys.core.extension.log;

import ru.jamsys.core.extension.CamelNormalization;

public enum LogType implements CamelNormalization {
    INFO, // Инфомрация, что действие совершено
    DEBUG, // Информация, что действие совершено + отладочная информация с ньюансами
    ERROR // Действие совершенно с ошибками
}
