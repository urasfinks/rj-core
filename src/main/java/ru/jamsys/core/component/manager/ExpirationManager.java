package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.item.Expiration;

@Component
public class ExpirationManager extends AbstractManager2<Expiration<?>> {

    public <T> EnvelopManagerObject<Expiration<T>> get(String index, Class<T> classItem) {
        return new EnvelopManagerObject<>(index, classItem, this);
    }

    @Override
    public Expiration<?> build(String index, Class<?> classItem) {
        return new Expiration<>(index, classItem);
    }

}
