package ru.jamsys.core.promise;

import lombok.Getter;
import ru.jamsys.core.extension.EnumName;

@Getter
public enum PromiseTaskExecuteType implements EnumName {

    WAIT, // Ждём когда все предыдущие запущенные задачи будут исполнены
    IO, //Запускаем эту задачу в VirtualThread
    COMPUTE, //Запускаем эту задачу в RealThread
    EXTERNAL_WAIT, // Внешняя задача, complete будет вызван сторонними средствами

    // Результат задачи не интересен, ошибка или успешное выполнение не повлияет на жизненный цикл обещания
    // Запуск будет осуществлён в VirtualThread
    ASYNC_NO_WAIT_IO,

    // Результат задачи не интересен, ошибка или успешное выполнение не повлияет на жизненный цикл обещания
    // Запуск будет осуществлён в RealThread
    ASYNC_NO_WAIT_COMPUTE

}
