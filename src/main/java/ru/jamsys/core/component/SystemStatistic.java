package ru.jamsys.core.component;

import com.sun.management.OperatingSystemMXBean;
import org.springframework.stereotype.Component;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.extension.log.StatDataHeader;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
@Component
public class SystemStatistic implements StatisticsFlushComponent {

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
    }

    @Override
    public List<StatDataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        List<StatDataHeader> result = new ArrayList<>();
        if (first) {
            runFirst();
        } else {
            runSecond();
        }
        first = !first;
        result.add(new StatDataHeader(getClass(), null)
                .addHeader("cpu", cpuUsage)
                .addHeader("heapSize", Runtime.getRuntime().totalMemory())
                .addHeader("heapSizeMax", Runtime.getRuntime().maxMemory())
                .addHeader("heapSizeFree", Runtime.getRuntime().freeMemory()));
        return result;
    }

}
