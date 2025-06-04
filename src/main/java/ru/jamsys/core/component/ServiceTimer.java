package ru.jamsys.core.component;

import org.springframework.stereotype.Component;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.extension.log.StatDataHeader;
import ru.jamsys.core.extension.statistic.AvgMetric;
import ru.jamsys.core.extension.statistic.timer.nano.TimerNanoEnvelope;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

// Сервис времён
// Основная фишка сервиса, что он снимает метрики в не зависимости от их остановки, то есть пока метрика существует,
// он снимает её время. Если будет рост времени метрики, мы увидим на графиках как её время растёт на срезах в 3 секунды

@Component
public class ServiceTimer implements StatisticsFlushComponent {

    Map<String, LongSummaryStatistics> timeStatisticNano = new HashMap<>();

    ConcurrentLinkedDeque<TimerNanoEnvelope<String>> timerQueue = new ConcurrentLinkedDeque<>();

    private final ConcurrentLinkedDeque<StatDataHeader> timerStatisticQueue = new ConcurrentLinkedDeque<>();

    // Глобальная регистрация времени исполнения
    public TimerNanoEnvelope<String> get(String index) {
        TimerNanoEnvelope<String> timer = new TimerNanoEnvelope<>(index);
        timerQueue.add(timer);
        return timer;
    }

    public void helper(AtomicBoolean threadRun) {
        // Убираем сборку метрик по времени в отдельный поток, что бы не нагружать flushAndGetStatistic
        // helper будем вызывать 1 раз в 3 секунды или больше
        Map<String, AvgMetric> mapMetricNano = new HashMap<>();
        UtilRisc.forEach(threadRun, timerQueue, (TimerNanoEnvelope<String> timer) -> {
            mapMetricNano.computeIfAbsent(timer.getValue(), _ -> new AvgMetric())
                    .add(timer.getOffsetLastActivityNano());
            if (timer.isStop()) {
                timerQueue.remove(timer);
            }
        });
        mapMetricNano.forEach((index, metric) -> {
            LongSummaryStatistics flush = metric.flushLongSummaryStatistics();
            timeStatisticNano.put(index, flush);
            timerStatisticQueue.add(new StatDataHeader(getClass(), index)
                    .addHeader("sum", flush.getSum())
            );
        });
    }

    @Override
    public List<StatDataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        List<StatDataHeader> result = new ArrayList<>();
        while (!timerStatisticQueue.isEmpty()) {
            result.add(timerStatisticQueue.pollFirst());
        }
        return result;
    }

}
