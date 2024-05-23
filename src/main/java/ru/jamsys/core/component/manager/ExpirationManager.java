package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.item.Expiration;

@Component
public class ExpirationManager extends AbstractManager<Expiration<?>> {

    public <T> ManagerElement<Expiration<T>> get(String index, Class<T> classItem) {
        return new ManagerElement<>(index, classItem, this);
    }

    @Override
    public Expiration<?> build(String index, Class<?> classItem) {
        return new Expiration<>(index, classItem);
    }

}
