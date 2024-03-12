package ru.jamsys.task.generator;

import ru.jamsys.task.Task;

public interface Generator {

    String getCronTemplate();

    Task getTask();

}
