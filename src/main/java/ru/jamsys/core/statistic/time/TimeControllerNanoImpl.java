package ru.jamsys.core.statistic.time;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimeControllerNanoImpl implements TimeControllerNano {

    private long lastActivityNano = System.nanoTime(); //Nano time потоко не безопасен + ресурсоёмкий

    private Long timeStopNano = null;

}
