package ru.jamsys.core.component;

import com.sun.management.OperatingSystemMXBean;
import org.springframework.stereotype.Component;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.statistic.Statistic;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ServiceSystemStatistic implements StatisticsFlushComponent {

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

    public void runSecond(Statistic statistic) {
        operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long upTime = runtimeMXBean.getUptime();
        long processCpuTime = operatingSystemMXBean.getProcessCpuTime();
        long elapsedCpu = processCpuTime - prevProcessCpuTime;
        long elapsedTime = upTime - prevUpTime;
        cpuUsage = Math.min(99F, elapsedCpu / (elapsedTime * 10000F * availableProcessors));
        statistic.addField("cpu", cpuUsage);
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean threadRun) {
        List<Statistic> result = new ArrayList<>();
        if (first) {
            runFirst();
        } else {
            Statistic statistic = new Statistic(parentTags, parentFields);
            runSecond(statistic);
            result.add(statistic);
        }
        first = !first;
        Statistic statistic = new Statistic(parentTags, parentFields);
        statistic.addField("heapSize", Runtime.getRuntime().totalMemory());
        statistic.addField("heapSizeMax", Runtime.getRuntime().maxMemory());
        statistic.addField("heapSizeFree", Runtime.getRuntime().freeMemory());
        result.add(statistic);
        return result;
    }
}
