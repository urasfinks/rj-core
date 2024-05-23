package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.item.Expiration;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
public class ExpirationManager extends AbstractManager<Expiration<?>> {

    Map<String, Consumer<DisposableExpirationMsImmutableEnvelope<?>>> expiredMap = new ConcurrentHashMap<>();

    @SuppressWarnings("all")
    public <T> ManagerElement<Expiration<T>> get(String index, Class<T> classItem, Consumer<DisposableExpirationMsImmutableEnvelope<T>> onExpired) {
        // Интерполяция обобщений, с условием соблюдения сигнатуры. Дальнейшая абстракция по дженерикам не интересна
        final Consumer onExpired2 = onExpired;
        expiredMap.computeIfAbsent(index, _ -> onExpired2);
        return new ManagerElement<>(index, classItem, this);
    }

    @Override
    public Expiration<?> build(String index, Class<?> classItem) {
        return new Expiration<>(index, classItem, expiredMap.get(index));
    }

}
