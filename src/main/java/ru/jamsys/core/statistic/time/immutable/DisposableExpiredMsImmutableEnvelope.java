package ru.jamsys.core.statistic.time.immutable;

// Одноразовое использование немутабельной обёртки протухания
// Предположим есть 2 процесса, которые одновременно имеют ссылки на этот объект
//      1) Занимается очисткой протухших сообщений
//      2) Занимается логикой в которой используется этот объект
// Таким образом мы решаем синхронизацию двух процессов, если мы объект посчитали протухшим и выполнили onExpired
// что бы второй процесс уже не смог получить данные обёртки

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class DisposableExpiredMsImmutableEnvelope<T> extends ExpiredMsImmutableEnvelope<T> {

    AtomicBoolean usage = new AtomicBoolean(false);

    public DisposableExpiredMsImmutableEnvelope(T value, long keepAliveOnInactivityMs) {
        super(value, keepAliveOnInactivityMs);
    }

    public DisposableExpiredMsImmutableEnvelope(T value, long keepAliveOnInactivityMs, long lastActivityMs) {
        super(value, keepAliveOnInactivityMs, lastActivityMs);
    }

    public @Nullable T concurrentUse() {
        if (usage.compareAndSet(false, true)) {
            return getValue();
        }
        return null;
    }

    public static <T> DisposableExpiredMsImmutableEnvelope<T> convert(ExpiredMsImmutableEnvelope<T> input) {
        return new DisposableExpiredMsImmutableEnvelope<>(input.getValue(), input.getKeepAliveOnInactivityMs(), input.getLastActivityMs());
    }

}
