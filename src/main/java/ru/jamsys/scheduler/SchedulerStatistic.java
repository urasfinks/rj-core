package ru.jamsys.scheduler;

import lombok.Data;

import java.util.Set;

@Data
public class SchedulerStatistic {

    String name = getClass().getSimpleName();

    Set<String> set;

    public SchedulerStatistic(Set<String> set) {
        this.set = set;
    }
}
