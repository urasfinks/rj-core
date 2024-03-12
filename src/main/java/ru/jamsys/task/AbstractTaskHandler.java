package ru.jamsys.task;

import ru.jamsys.task.handler.TaskHandler;

public abstract class AbstractTaskHandler<T extends Task> extends TagIndex implements TaskHandler<T> {

}
