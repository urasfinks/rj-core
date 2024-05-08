package ru.jamsys.core.component.promise;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.extension.KeepAliveComponent;
import ru.jamsys.core.statistic.AvgMetric;
import ru.jamsys.core.statistic.time.TimeEnvelopeNano;
import ru.jamsys.core.util.UtilRisc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Lazy
public class PromiseTaskTime implements KeepAliveComponent {

    ConcurrentLinkedDeque<TimeEnvelopeNano<String>> queue = new ConcurrentLinkedDeque<>();
    Map<String, Map<String, Object>> statistic = new HashMap<>();

    public TimeEnvelopeNano<String> add(String index) {
        TimeEnvelopeNano<String> timeEnvelopeMs = new TimeEnvelopeNano<>(index);
        queue.add(timeEnvelopeMs);
        return timeEnvelopeMs;
    }

    @Override
    public void keepAlive(AtomicBoolean isThreadRun) {
        Map<String, AvgMetric> tmp = new HashMap<>();
        UtilRisc.forEach(isThreadRun, queue, (TimeEnvelopeNano<String> timeEnvelope) -> {
            String index = timeEnvelope.getValue();
            if (!tmp.containsKey(index)) {
                tmp.put(index, new AvgMetric());
            }
            tmp.get(index).add(timeEnvelope.getOffsetLastActivityNano());
            if (timeEnvelope.isStop()) {
                queue.remove(timeEnvelope);
            }
        });
        tmp.forEach((String index, AvgMetric metric) -> statistic.put(index, metric.flush("")));
    }

}
