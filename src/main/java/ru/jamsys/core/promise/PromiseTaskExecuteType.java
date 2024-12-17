package ru.jamsys.core.promise;

import lombok.Getter;
import ru.jamsys.core.extension.CamelNormalization;

@Getter
public enum PromiseTaskExecuteType implements CamelNormalization {

    WAIT, // Ждём когда все предыдущие запущенные задачи будут исполнены
    IO, //Запускаем эту задачу в VirtualThread
    COMPUTE, //Запускаем эту задачу в RealThread
    EXTERNAL_WAIT_IO, // complete будет вызван сторонними средствами
    EXTERNAL_WAIT_COMPUTE, // complete будет вызван сторонними средствами

    // Результат задачи не интересен, ошибка или успешное выполнение не повлияет на жизненный цикл обещания
    // Запуск будет осуществлён в VirtualThread
    // Задача не помещается в setRunningTasks.add(), поэтому любой WAIT не поймёт, что есть запущенная задача
    ASYNC_NO_WAIT_IO,

    // Результат задачи не интересен, ошибка или успешное выполнение не повлияет на жизненный цикл обещания
    // Запуск будет осуществлён в RealThread
    // Задача не помещается в setRunningTasks.add(), поэтому любой WAIT не поймёт, что есть запущенная задача
    ASYNC_NO_WAIT_COMPUTE

}
