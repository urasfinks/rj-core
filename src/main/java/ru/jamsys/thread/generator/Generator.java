package ru.jamsys.thread.generator;

import ru.jamsys.statistic.TimeEnvelope;
import ru.jamsys.thread.task.AbstractTask;

public interface Generator {

    String getCronTemplate();

    TimeEnvelope<AbstractTask> getTaskTimeEnvelope();

    int getId(); // Для сортировки запуска. Если KeepAlive вызывать последним, то всегда будем получать недостаток потоков

}
