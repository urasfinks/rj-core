package ru.jamsys.message;

public enum MessageHandle {
    @SuppressWarnings("unused")
    CREATE, //Создано кем-то
    @SuppressWarnings("unused")
    PUT, //Куда-то вставлено
    @SuppressWarnings("unused")
    EXECUTE, //Выполняется кем-то
    @SuppressWarnings("unused")
    COMPLETE, //Исполнено кем-то
    @SuppressWarnings("unused")
    ON_RECEIVE //Получено кем-то
}
