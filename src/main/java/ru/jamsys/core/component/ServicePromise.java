package ru.jamsys.core.component;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.ManagerExpiration;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.component.manager.item.Expiration;
import ru.jamsys.core.extension.ClassName;
import ru.jamsys.core.extension.KeepAliveComponent;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseImpl;
import ru.jamsys.core.promise.PromiseTask;
import ru.jamsys.core.statistic.AvgMetric;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.timer.Timer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Lazy
public class ServicePromise implements ClassName, KeepAliveComponent, StatisticsFlushComponent {

    Broker<Promise> broker;

    private final Expiration<PromiseTask> promiseTaskRetry;

    ConcurrentLinkedDeque<Timer> queueTimerNano = new ConcurrentLinkedDeque<>();

    Map<String, Map<String, Object>> timeStatisticNano = new HashMap<>();

    @Getter
    Map<String, Map<String, Object>> timeStatisticMs = new HashMap<>();

    public ServicePromise(ManagerBroker managerBroker, ApplicationContext applicationContext, ManagerExpiration managerExpiration) {
        this.broker = managerBroker.initAndGet(getClassName(applicationContext), Promise.class, promise
                -> promise.timeOut(getClassName("onPromiseTaskExpired")));
        promiseTaskRetry = managerExpiration.get("PromiseTaskRetry", PromiseTask.class, this::onPromiseTaskRetry);
    }

    public Promise get(String index, long timeout) {
        PromiseImpl promise = new PromiseImpl(index, timeout);
        promise.setRegisterInBroker(broker.add(promise, timeout));
        return promise;
    }

    public void finish(DisposableExpirationMsImmutableEnvelope<Promise> fin) {
        broker.remove(fin);
    }

    public void addRetryDelay(PromiseTask promiseTask) {
        promiseTaskRetry.add(new ExpirationMsImmutableEnvelope<>(promiseTask, promiseTask.getRetryDelayMs()));
    }

    public Timer registrationTimer(String index) {
        Timer timer = new Timer(index);
        queueTimerNano.add(timer);
        return timer;
    }

    private void onPromiseTaskRetry(DisposableExpirationMsImmutableEnvelope<PromiseTask> env) {
        PromiseTask promiseTask = env.getValue();
        if (promiseTask != null) {
            promiseTask.start();
        }
    }

    @Override
    public void keepAlive(AtomicBoolean isThreadRun) {
        Map<String, AvgMetric> mapMetricNano = new HashMap<>();
        Map<String, AvgMetric> mapMetricMs = new HashMap<>();
        UtilRisc.forEach(isThreadRun, queueTimerNano, (Timer timeEnvelope) -> {
            String index = timeEnvelope.getIndex();
            mapMetricNano.computeIfAbsent(index, _ -> new AvgMetric()).add(timeEnvelope.getNano());
            mapMetricMs.computeIfAbsent(index, _ -> new AvgMetric()).add(timeEnvelope.getMs());
            if (timeEnvelope.isStop()) {
                queueTimerNano.remove(timeEnvelope);
            }
        });
        mapMetricNano.forEach((String index, AvgMetric metric) -> timeStatisticNano.put(index, metric.flush("")));
        mapMetricMs.forEach((String index, AvgMetric metric) -> timeStatisticMs.put(index, metric.flush("")));
        //System.out.println(UtilJson.toStringPretty(timeStatisticMs, ""));
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isThreadRun) {
        List<Statistic> result = new ArrayList<>();

        UtilRisc.forEach(isThreadRun, timeStatisticNano, (s, stringObjectMap) -> {
            result.add(new Statistic(parentTags, parentFields)
                    .addField(s, stringObjectMap.get("Avg"))
                    .addTag("unit", "nano")
            );
        });
        UtilRisc.forEach(isThreadRun, timeStatisticMs, (s, stringObjectMap) -> {
            result.add(new Statistic(parentTags, parentFields)
                    .addField(s, stringObjectMap.get("Avg"))
                    .addTag("unit", "ms")
            );
        });
        return result;
    }
}
