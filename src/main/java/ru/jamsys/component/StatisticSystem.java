package ru.jamsys.component;

import com.sun.management.OperatingSystemMXBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.AbstractCoreComponent;
import ru.jamsys.statistic.SystemStatisticData;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

@Component
@Lazy
public class StatisticSystem extends AbstractCoreComponent {

    private final StatisticAggregator statisticAggregator;
    private final String nameScheduler = "StatisticSystem";
    Scheduler scheduler;

    public StatisticSystem(Scheduler scheduler, StatisticAggregator statisticAggregator) {
        this.statisticAggregator = statisticAggregator;
        this.scheduler = scheduler;
        scheduler.add(nameScheduler, this::flushStatistic, 1000);
    }

    public volatile double cpuUsage;

    boolean first = true;

    OperatingSystemMXBean operatingSystemMXBean;
    RuntimeMXBean runtimeMXBean;
    int availableProcessors;
    long prevUpTime;
    long prevProcessCpuTime;

    public void runFirst() {
        operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        availableProcessors = operatingSystemMXBean.getAvailableProcessors();
        prevUpTime = runtimeMXBean.getUptime();
        prevProcessCpuTime = operatingSystemMXBean.getProcessCpuTime();
    }

    public void runSecond() {
        operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long upTime = runtimeMXBean.getUptime();
        long processCpuTime = operatingSystemMXBean.getProcessCpuTime();
        long elapsedCpu = processCpuTime - prevProcessCpuTime;
        long elapsedTime = upTime - prevUpTime;
        cpuUsage = Math.min(99F, elapsedCpu / (elapsedTime * 10000F * availableProcessors));
        statisticAggregator.add(new SystemStatisticData(cpuUsage));
    }

    @Override
    public void flushStatistic() {
        if (first) {
            runFirst();
        } else {
            runSecond();
        }
        first = !first;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        scheduler.remove(nameScheduler, this::flushStatistic);
    }
}
