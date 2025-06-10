package ru.jamsys.core.extension.property.repository;

import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyEnvelope;

import java.util.Map;
import java.util.function.Consumer;

public class RepositoryPropertyBuilder<T, X extends RepositoryPropertyAnnotationField<T>> {

    X x;

    public RepositoryPropertyBuilder(X x) {
        this.x = x;
    }

    public RepositoryPropertyBuilder<T, X> apply(Consumer<X> consumer) {
        consumer.accept(x);
        return this;
    }

    // Применить значения из ServiceProperty
    public RepositoryPropertyBuilder<T, X> applyServiceProperty(String ns) {
        PropertyDispatcher<T> propertyDispatcher = new PropertyDispatcher<>(null, x, ns);
        propertyDispatcher.run();
        propertyDispatcher.shutdown();
        return this;
    }

    // Применить значения из Map
    public RepositoryPropertyBuilder<T, X> applyMap(Map<String, Object> map) {
        map.forEach((key, value) -> {
            PropertyEnvelope<T> propertyEnvelope = x.getByRepositoryPropertyKey(key);
            try {
                propertyEnvelope.getField().set(x, value);
            } catch (Throwable th) {
                throw new ForwardException(this, th);
            }
        });
        return this;
    }

    public X build() {
        return x;
    }

}
