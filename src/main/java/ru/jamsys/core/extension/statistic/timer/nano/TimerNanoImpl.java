package ru.jamsys.core.extension.statistic.timer.nano;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimerNanoImpl implements TimerNano {

    private long lastActivityNano = System.nanoTime(); //Nano time потоко не безопасен + ресурсоёмкий

    private Long timeStopNano = null;

}
