package ru.jamsys.component;

import com.sun.management.OperatingSystemMXBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.StatisticsCollector;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Lazy
public class SystemStatistic extends AbstractComponent implements StatisticsCollector {

    public volatile double cpuUsage;

    boolean first = true;

    OperatingSystemMXBean operatingSystemMXBean;
    RuntimeMXBean runtimeMXBean;
    int availableProcessors;
    long prevUpTime;
    long prevProcessCpuTime;

    public SystemStatistic(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    public void runFirst() {
        operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        availableProcessors = operatingSystemMXBean.getAvailableProcessors();
        prevUpTime = runtimeMXBean.getUptime();
        prevProcessCpuTime = operatingSystemMXBean.getProcessCpuTime();
    }

    public void runSecond(List<Statistic> list, Map<String, String> parentTags, Map<String, Object> parentFields) {
        operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long upTime = runtimeMXBean.getUptime();
        long processCpuTime = operatingSystemMXBean.getProcessCpuTime();
        long elapsedCpu = processCpuTime - prevProcessCpuTime;
        long elapsedTime = upTime - prevUpTime;
        cpuUsage = Math.min(99F, elapsedCpu / (elapsedTime * 10000F * availableProcessors));
        list.add(new Statistic(parentTags, parentFields).addField("cpu", cpuUsage));
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isRun) {
        List<Statistic> result = new ArrayList<>();
        if (first) {
            runFirst();
        } else {
            runSecond(result, parentTags, parentFields);
        }
        first = !first;
        return result;
    }
}
