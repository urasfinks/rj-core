package ru.jamsys.thread.generator;

import ru.jamsys.thread.task.Task;

public interface Generator {

    String getCronTemplate();

    Task getTask();

    int getId(); // Для сортировки запуска. Если KeepAlive вызывать последним, то всегда будем получать недостаток потоков

}
