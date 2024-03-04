package ru.jamsys.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class TaskStatistic extends AbstractStatisticTaskComponent {

    public TaskStatistic(ApplicationContext applicationContext) {
        super(applicationContext);
    }
}

