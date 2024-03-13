package ru.jamsys.thread.generator;

import ru.jamsys.thread.task.Task;

public interface Generator {

    String getCronTemplate();

    Task getTask();

}
