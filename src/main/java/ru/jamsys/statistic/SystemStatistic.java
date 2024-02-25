package ru.jamsys.statistic;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class SystemStatistic extends AbstractStatistic {

    double cpu;

    public SystemStatistic(double cpu) {
        this.cpu = cpu;
    }

    @Override
    public List<StatisticEntity> getStatisticEntity() {
         List<StatisticEntity> result = super.getStatisticEntity();
         result.add(new StatisticEntity().addField("cpu", cpu));
         return result;
    }
}
