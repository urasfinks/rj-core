package ru.jamsys;

public interface CoreComponent {

    @SuppressWarnings("unused")
    void shutdown(); //Выключить

    @SuppressWarnings("unused")
    void flushStatistic(); //Сгрузить статистику

    void run();

}
