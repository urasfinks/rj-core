package ru.jamsys.core.extension;

// Компонент будет запускаться автоматически при старте
// В проектах начинайте со 100
public interface LifeCycleComponent extends LifeCycleInterface {
    int getInitializationIndex();
}
