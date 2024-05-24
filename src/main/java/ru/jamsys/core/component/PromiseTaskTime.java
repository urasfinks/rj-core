package ru.jamsys.core.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.ExpirationManager;
import ru.jamsys.core.component.manager.ManagerElement;
import ru.jamsys.core.component.manager.item.Expiration;
import ru.jamsys.core.extension.ClassName;
import ru.jamsys.core.extension.KeepAliveComponent;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.promise.Promise;
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
public class PromiseTaskTime implements KeepAliveComponent, ClassName {

    private final ManagerElement<Expiration<PromiseTask>> promiseTaskRetry;

    private final ManagerElement<Expiration<Promise>> promiseTaskExpired;

    ConcurrentLinkedDeque<TimeEnvelopeNano<String>> queue = new ConcurrentLinkedDeque<>();

    Map<String, Map<String, Object>> statistic = new HashMap<>();

    public PromiseTaskTime(ApplicationContext applicationContext) {
        ExpirationManager expirationManager = applicationContext.getBean(ExpirationManager.class);
        promiseTaskRetry = expirationManager.get("PromiseTaskRetry", PromiseTask.class, this::onPromiseTaskRetry);
        promiseTaskExpired = expirationManager.get("PromiseTaskExpired", Promise.class, this::onPromiseTaskExpired);
    }

    private void onPromiseTaskRetry(DisposableExpirationMsImmutableEnvelope<PromiseTask> env) {
        PromiseTask promiseTask = env.getValue();
        if (promiseTask != null) {
            promiseTask.start();
        }
    }

    private void onPromiseTaskExpired(DisposableExpirationMsImmutableEnvelope<Promise> env) {
        Promise promise = env.getValue();
        if (promise != null) {
            promise.timeOut(getClassName("onPromiseTaskExpired"));
        }
    }

    public DisposableExpirationMsImmutableEnvelope<PromiseTask> addRetryDelay(PromiseTask promiseTask) {
        return promiseTaskRetry.get().add(new ExpirationMsImmutableEnvelope<>(promiseTask, promiseTask.getRetryDelayMs()));
    }

    public DisposableExpirationMsImmutableEnvelope<Promise> addExpiration(Promise promise) {
        return promiseTaskExpired.get().add(new DisposableExpirationMsImmutableEnvelope<>(promise, promise.getKeepAliveOnInactivityMs()));
    }

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
            // Не конкурентная проверка
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
