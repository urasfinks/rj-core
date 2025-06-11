package ru.jamsys.core.extension.property.repository;

import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.PropertyEnvelope;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.Map;

public class RepositoryPropertyBuilder<T, X extends RepositoryPropertyAnnotationField<T>> {

    X repositoryProperty;

    public RepositoryPropertyBuilder(X repositoryProperty, String ns) {
        this.repositoryProperty = repositoryProperty;
        this.repositoryProperty.init(ns, false);
    }

    public RepositoryPropertyBuilder<T, X> apply(String fieldNameConstants, String value) {
        PropertyEnvelope<T> byFieldNameConstants = repositoryProperty.getByFieldNameConstants(fieldNameConstants);
        byFieldNameConstants.setValue(value);
        return this;
    }

    // Для тех случаев, когда надо запихнуть данные в репозиторий, не простых объектов, которые сериализовать через
    // строку не получается, либо накладно.
    public RepositoryPropertyBuilder<T, X> applyWithoutCheck(String fieldNameConstants, Object value) {
        PropertyEnvelope<T> byFieldNameConstants = repositoryProperty.getByFieldNameConstants(fieldNameConstants);
        byFieldNameConstants.setValueWithoutCheck(value);
        return this;
    }

    // Применить значения из ServiceProperty
    public RepositoryPropertyBuilder<T, X> applyServiceProperty() {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        UtilRisc.forEach(null, repositoryProperty.getListPropertyEnvelopeRepository(), tPropertyEnvelope -> {
            if (serviceProperty.contains(tPropertyEnvelope.getPropertyKey())) {
                tPropertyEnvelope.setValue(serviceProperty
                        .computeIfAbsent(tPropertyEnvelope.getPropertyKey(), null)
                        .get()
                );
            }
        });
        return this;
    }

    // Применить значения из ServiceProperty если в репозитории null
    public RepositoryPropertyBuilder<T, X> applyServicePropertyOnlyNull() {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        UtilRisc.forEach(null, repositoryProperty.getListPropertyEnvelopeRepository(), tPropertyEnvelope -> {
            if (tPropertyEnvelope.getValue() == null && serviceProperty.contains(tPropertyEnvelope.getPropertyKey())) {
                tPropertyEnvelope.setValue(serviceProperty
                        .computeIfAbsent(tPropertyEnvelope.getPropertyKey(), null)
                        .get()
                );
            }
        });
        return this;
    }

    // Применить значения из Map
    public RepositoryPropertyBuilder<T, X> applyMap(Map<String, String> map) {
        map.forEach((key, value) -> repositoryProperty.getByFieldNameConstants(key).setValue(value));
        return this;
    }

    public X build() {
        // Применяем все установленные значения в репозиторий
        UtilRisc.forEach(null, repositoryProperty.getListPropertyEnvelopeRepository(), tPropertyEnvelope -> {
            tPropertyEnvelope.apply();
        });
        repositoryProperty.checkNotNull();
        repositoryProperty.checkRegexp();
        return repositoryProperty;
    }

}
