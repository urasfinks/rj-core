package ru.jamsys.thread.generator;

import ru.jamsys.thread.task.AbstractTask;

public interface Generator {

    String getCronTemplate();

    AbstractTask getTask();

    int getId(); // Для сортировки запуска. Если KeepAlive вызывать последним, то всегда будем получать недостаток потоков

}
