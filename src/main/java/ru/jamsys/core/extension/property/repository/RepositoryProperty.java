package ru.jamsys.core.extension.property.repository;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyEnvelope;
import ru.jamsys.core.flat.util.UtilRisc;

// Произвольный репозиторий Property. Работает также в связке с PropertyDispatcher

@Getter
public class RepositoryProperty<T> extends AbstractRepositoryProperty<T> {

    private final Class<T> cls;

    public RepositoryProperty(Class<T> cls) {
        this.cls = cls;
    }

    @Override
    public void init(String ns, boolean sync) {
        if (getInit().compareAndSet(false, true)) {
            if (sync) {
                UtilRisc.forEach(null, getListPropertyEnvelopeRepository(), tPropertyEnvelopeRepository -> {
                    tPropertyEnvelopeRepository.syncPropertyValue();
                });
            }
        }
    }

    //TODO: поменять местами аргументы
    @Override
    public void append(String repositoryPropertyKey, String ns) {
        PropertyEnvelope<T> tPropertyEnvelope = new PropertyEnvelope<>(
                this,
                null,
                cls,
                null,
                repositoryPropertyKey,
                CascadeKey.complexLinear(ns, repositoryPropertyKey),
                null,
                "AdditionalProperties",
                null,
                false,
                true
        )
                .syncPropertyValue();
        if (!getListPropertyEnvelopeRepository().contains(tPropertyEnvelope)) {
            getListPropertyEnvelopeRepository().add(tPropertyEnvelope);
        }
    }

    @Override
    public void updateRepository(String repositoryPropertyKey, PropertyDispatcher<T> propertyDispatcher) {
        PropertyEnvelope<T> propertyEnvelope = getByRepositoryPropertyKey(repositoryPropertyKey);
        if (propertyEnvelope == null) {
            append(repositoryPropertyKey, propertyDispatcher.getNs());
        } else {
            propertyEnvelope.syncPropertyValue();
        }
    }

    @Override
    @JsonValue
    public Object getJsonValue() {
        return new HashMapBuilder<String, Object>()
                .append("cls", cls)
                .append("listPropertyEnvelopeRepository", getListPropertyEnvelopeRepository())
                .append("init", getInit().get())
                ;
    }

}
