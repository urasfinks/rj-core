package ru.jamsys.core.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.BrokerManager;
import ru.jamsys.core.component.manager.ExpirationManager;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.component.manager.item.Expiration;
import ru.jamsys.core.extension.ClassName;
import ru.jamsys.core.extension.KeepAliveComponent;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseImpl;
import ru.jamsys.core.promise.PromiseTask;
import ru.jamsys.core.statistic.AvgMetric;
import ru.jamsys.core.statistic.expiration.TimeEnvelopeNano;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Lazy
public class PromiseComponent implements ClassName, KeepAliveComponent {

    Broker<Promise> broker;

    private final Expiration<PromiseTask> promiseTaskRetry;

    ConcurrentLinkedDeque<TimeEnvelopeNano<String>> queueTimer = new ConcurrentLinkedDeque<>();

    Map<String, Map<String, Object>> timeStatistic = new HashMap<>();

    public PromiseComponent(BrokerManager brokerManager, ApplicationContext applicationContext, ExpirationManager expirationManager ) {
        this.broker = brokerManager.initAndGet(getClassName(applicationContext), Promise.class, promise
                -> promise.timeOut(getClassName("onPromiseTaskExpired")));
        promiseTaskRetry = expirationManager.get("PromiseTaskRetry", PromiseTask.class, this::onPromiseTaskRetry);
    }

    public Promise get(String index, long timeout) {
        PromiseImpl promise = new PromiseImpl(index, timeout);
        broker.add(promise, timeout);
        return promise;
    }

    public void addRetryDelay(PromiseTask promiseTask) {
        promiseTaskRetry.add(new ExpirationMsImmutableEnvelope<>(promiseTask, promiseTask.getRetryDelayMs()));
    }

    public TimeEnvelopeNano<String> registrationTimer(String index) {
        TimeEnvelopeNano<String> timeEnvelopeMs = new TimeEnvelopeNano<>(index);
        queueTimer.add(timeEnvelopeMs);
        return timeEnvelopeMs;
    }

    private void onPromiseTaskRetry(DisposableExpirationMsImmutableEnvelope<PromiseTask> env) {
        PromiseTask promiseTask = env.getValue();
        if (promiseTask != null) {
            promiseTask.start();
        }
    }

    @Override
    public void keepAlive(AtomicBoolean isThreadRun) {
        Map<String, AvgMetric> tmp = new HashMap<>();
        UtilRisc.forEach(isThreadRun, queueTimer, (TimeEnvelopeNano<String> timeEnvelope) -> {
            String index = timeEnvelope.getValue();
            // Не конкурентная проверка
            if (!tmp.containsKey(index)) {
                tmp.put(index, new AvgMetric());
            }
            tmp.get(index).add(timeEnvelope.getOffsetLastActivityNano());
            if (timeEnvelope.isStop()) {
                queueTimer.remove(timeEnvelope);
            }
        });
        tmp.forEach((String index, AvgMetric metric) -> timeStatistic.put(index, metric.flush("")));
    }

}
