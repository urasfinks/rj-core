package ru.jamsys.core.component;

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
import ru.jamsys.core.extension.property.PropertyEnvelope;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseImpl;
import ru.jamsys.core.promise.PromiseTask;
import ru.jamsys.core.statistic.AvgMetric;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.timer.Timer;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Lazy
public class ServicePromise implements ClassName, KeepAliveComponent, StatisticsFlushComponent {

    public static Set<Promise> queueMultipleCompleteSet = Util.getConcurrentHashSet();

    Broker<Promise> broker;

    private final Expiration<PromiseTask> promiseTaskRetry;

    ConcurrentLinkedDeque<PropertyEnvelope<Timer, String, String>> queueTimer = new ConcurrentLinkedDeque<>();

    Map<String, LongSummaryStatistics> timeStatisticNano = new HashMap<>();

    private final ConcurrentLinkedDeque<Statistic> toStatistic = new ConcurrentLinkedDeque<>();

    public ServicePromise(ManagerBroker managerBroker, ApplicationContext applicationContext, ManagerExpiration managerExpiration) {
        this.broker = managerBroker.initAndGet(getClassName("run", applicationContext), Promise.class, promise
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

    public PropertyEnvelope<Timer, String, String> registrationTimer(String index) {
        PropertyEnvelope<Timer, String, String> timer = new PropertyEnvelope<>(new Timer(index));
        queueTimer.add(timer);
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
        Map<String, PropertyEnvelope<AvgMetric, String, String>> mapMetricNano = new HashMap<>();
        UtilRisc.forEach(isThreadRun, queueTimer, (PropertyEnvelope<Timer, String, String> timerEnvelope) -> {
            Timer timer = timerEnvelope.getValue();
            mapMetricNano.computeIfAbsent(timer.getIndex(), _ -> {
                PropertyEnvelope<AvgMetric, String, String> envelope = new PropertyEnvelope<>(new AvgMetric());
                envelope.setProperty(timerEnvelope.getMapProperty());
                return envelope;
            }).getValue().add(timer.getNano());
            if (timer.isStop()) {
                queueTimer.remove(timerEnvelope);
            }
        });
        mapMetricNano.forEach((index, envelope) -> {
            AvgMetric metric = envelope.getValue();
            LongSummaryStatistics flush = metric.flush();
            timeStatisticNano.put(index, flush);
            toStatistic.add(new Statistic()
                    .addTags(envelope.getMapProperty())
                    .addField(index, flush.getSum())
            );
        });
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isThreadRun) {
        List<Statistic> result = new ArrayList<>();
        while (!toStatistic.isEmpty()) {
            Statistic statistic = toStatistic.pollFirst();
            if (statistic != null) {
                statistic.addTags(parentTags).addFields(parentFields);
                result.add(statistic);
            }
        }
        return result;
    }

}
