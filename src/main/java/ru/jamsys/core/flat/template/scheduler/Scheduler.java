package ru.jamsys.core.flat.template.scheduler;

import com.fasterxml.jackson.annotation.JsonValue;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.property.PropertyDispatcher;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/*
 * Планировщик работает в режиме контракта “не догоняем”:
 * если execute() вызвали спустя долгое время и было пропущено несколько интервалов,
 * планировщик делает только один запуск и пересчитывает следующий.
 *
 * Конкурентный контракт:
 * если execute() вызван параллельно во время выполнения,
 * параллельный вызов будет проигнорирован (drop).
 */

public class Scheduler extends AbstractManagerElement {

    private SchedulerSequence schedulerSequence;
    private boolean executeOnFirstTick;
    private volatile long nextExecute;
    private Consumer<Long> onExecute;
    private final AtomicBoolean sync = new AtomicBoolean(false);
    private final String ns;
    private final String key;

    private volatile CalcTime calcTime = CalcTime.FIXED_RATE;

    private final SchedulerRepositoryProperty repositoryProperty = new SchedulerRepositoryProperty();

    private final PropertyDispatcher<Object> propertyDispatcher;

    public enum CalcTime {
        FIXED_RATE,  // считать от “планового” времени (времени попытки запуска)
        FIXED_DELAY  // считать от времени завершения выполнения
    }

    /*
     * Минимальный интервал (мс) как защита от busy-loop,
     * если SchedulerSequence.next(base) вернёт значение <= base.
     */
    private volatile long minIntervalMillis = 1000;

    public Scheduler(String ns, String key) {
        this.ns = requireNonBlank(ns, "ns");
        this.key = requireNonBlank(key, "key");

        this.propertyDispatcher = new PropertyDispatcher<>(
                null,
                repositoryProperty,
                ns
        );
    }

    public void setup(
            SchedulerSequence schedulerSequence,
            boolean executeOnFirstTick,
            Consumer<Long> onExecute
    ) {
        this.schedulerSequence = Objects.requireNonNull(schedulerSequence, "schedulerSequence");
        this.onExecute = Objects.requireNonNull(onExecute, "onExecute");
        this.executeOnFirstTick = executeOnFirstTick;

        if (!executeOnFirstTick) {
            long now = System.currentTimeMillis();
            this.nextExecute = safeNext(now);
        } else {
            this.nextExecute = 0; // первый execute() выполнит сразу
        }
    }

    @SuppressWarnings("unused")
    public void setCalcTime(CalcTime calcTime) {
        this.calcTime = Objects.requireNonNull(calcTime, "calcTime");
    }

    @SuppressWarnings("unused")
    public void setMinIntervalMillis(long minIntervalMillis) {
        if (minIntervalMillis < 1) {
            throw new IllegalArgumentException("minIntervalMillis must be >= 1");
        }
        this.minIntervalMillis = minIntervalMillis;
    }

    public void execute() {
        final long now = System.currentTimeMillis();

        // Рано или уже выполняется другим потоком
        if (now < nextExecute || !sync.compareAndSet(false, true)) {
            return;
        }

        long after = now;
        try {
            // Выполнение пользовательского кода
            try {
                onExecute.accept(now);
            } catch (Throwable th) {
                App.error(th, this);
            }

            // Определяем базу для расчёта следующего запуска
            after = System.currentTimeMillis();
            long base = (calcTime == CalcTime.FIXED_DELAY) ? after : now;

            nextExecute = safeNext(base);

        } catch (Throwable th) {
            // Аварийный backoff
            App.error(th, this);
            nextExecute = after + minIntervalMillis;

        } finally {
            sync.set(false);
        }
    }

    /**
     * Безопасный расчёт следующего времени выполнения.
     * - Гарантирует next > base
     * - Защищает от busy-loop
     * - Логирует ошибки SchedulerSequence
     */
    private long safeNext(long base) {
        try {
            long candidate = schedulerSequence.next(base);
            if (candidate <= base) {
                return base + minIntervalMillis;
            }
            return candidate;
        } catch (Throwable th) {
            App.error(th, this);
            return base + minIntervalMillis;
        }
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is null/blank");
        }
        return value;
    }

    @JsonValue
    public Object getJsonValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("class", getClass())
                .append("ns", ns)
                .append("key", key)
                // для обратной совместимости
                .append("executeOnInit", executeOnFirstTick)
                .append("executeOnFirstTick", executeOnFirstTick)
                .append("calcTime", calcTime)
                .append("minIntervalMillis", minIntervalMillis)
                .append("nextExecute", nextExecute)
                .append("schedulerSequence", schedulerSequence);
    }

    @Override
    public void runOperation() {
        this.propertyDispatcher.run();
    }

    @Override
    public void shutdownOperation() {
        this.propertyDispatcher.shutdown();
    }

}
