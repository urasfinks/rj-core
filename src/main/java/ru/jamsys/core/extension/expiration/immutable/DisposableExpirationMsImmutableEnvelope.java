package ru.jamsys.core.extension.expiration.immutable;

// Одноразовое использование немутабельной обёртки протухания
// Предположим есть 2 процесса, которые одновременно имеют ссылки на этот объект
//      1) Занимается очисткой протухших сообщений
//      2) Занимается логикой в которой используется этот объект
// Таким образом мы решаем синхронизацию двух процессов, если мы объект посчитали протухшим и выполнили onExpired
// что бы второй процесс уже не смог получить данные обёртки

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.concurrent.atomic.AtomicBoolean;

public class DisposableExpirationMsImmutableEnvelope<T> extends ExpirationMsImmutableEnvelope<T> {

    AtomicBoolean usage = new AtomicBoolean(false);

    public DisposableExpirationMsImmutableEnvelope(T value, long keepAliveOnInactivityMs) {
        super(value, keepAliveOnInactivityMs);
    }

    public DisposableExpirationMsImmutableEnvelope(T value, long keepAliveOnInactivityMs, long lastActivityMs) {
        super(value, keepAliveOnInactivityMs, lastActivityMs);
    }

    public boolean isNeutralized() {
        return usage.get();
    }

    // Нельзя, что бы JSON сериализатор портил логику одноразовой обёртки
    @JsonIgnore
    @Override
    public T getValue() {
        if (usage.compareAndSet(false, true)) {
            return super.getValue();
        }
        return null;
    }

    @SuppressWarnings("all")
    public boolean doNeutralized() { //Нейтрализовать
        return usage.compareAndSet(false, true);
    }

    public ExpirationMsImmutableEnvelope<T> revert() {
        return new ExpirationMsImmutableEnvelope<>(super.getValue(), getInactivityTimeoutMs(), getLastActivityMs());
    }

    public static <T> DisposableExpirationMsImmutableEnvelope<T> convert(ExpirationMsImmutableEnvelope<T> input) {
        return new DisposableExpirationMsImmutableEnvelope<>(input.getValue(), input.getInactivityTimeoutMs(), input.getLastActivityMs());
    }

}
