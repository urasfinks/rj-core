package ru.jamsys.statistic;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class SystemStatistic extends AbstractStatistic {

    double cpu;

    public SystemStatistic(double cpu) {
        this.cpu = cpu;
    }

}
