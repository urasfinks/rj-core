package ru.jamsys.core.extension.property.repository;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyEnvelope;
import ru.jamsys.core.flat.util.UtilRisc;

// Произвольный репозиторий Property. Работает также в связке с PropertyDispatcher

@Getter
public class RepositoryProperty<T> extends AbstractRepositoryProperty<T> {

    private final Class<T> cls;

    @JsonIgnore
    private PropertyDispatcher<T> propertyDispatcher;

    public RepositoryProperty(Class<T> cls) {
        this.cls = cls;
    }

    @Override
    public void init(PropertyDispatcher<T> propertyDispatcher) {
        this.propertyDispatcher = propertyDispatcher;
        if (getInit().compareAndSet(false, true)) {
            UtilRisc.forEach(null, getListPropertyEnvelopeRepository(), tPropertyEnvelopeRepository -> {
                tPropertyEnvelopeRepository
                        .setPropertyKey(propertyDispatcher.getPropertyKey(tPropertyEnvelopeRepository.getRepositoryPropertyKey()))
                        .setServiceProperty(propertyDispatcher.getServiceProperty())
                        .syncPropertyValue();
            });
        }
    }

    @Override
    public void append(String repositoryPropertyKey, PropertyDispatcher<T> propertyDispatcher) {
        PropertyEnvelope<T> tPropertyEnvelope = new PropertyEnvelope<>(
                cls,
                repositoryPropertyKey,
                null,
                false
        )
                .setDescription("AdditionalProperties")
                .setPropertyKey(propertyDispatcher.getPropertyKey(repositoryPropertyKey))
                .setServiceProperty(propertyDispatcher.getServiceProperty())
                .setRepositoryProperty(this)
                .setDynamic(true)
                .syncPropertyValue();
        if (!getListPropertyEnvelopeRepository().contains(tPropertyEnvelope)) {
            getListPropertyEnvelopeRepository().add(tPropertyEnvelope);
        }
    }

    @Override
    public void updateRepository(String repositoryPropertyKey, PropertyDispatcher<T> propertyDispatcher) {
        this.propertyDispatcher = propertyDispatcher;
        PropertyEnvelope<T> propertyEnvelope = getByRepositoryPropertyKey(repositoryPropertyKey);
        if (propertyEnvelope == null) {
            append(repositoryPropertyKey, propertyDispatcher);
        } else {
            propertyEnvelope.syncPropertyValue();
        }
    }

    @Override
    @JsonValue
    public Object getValue() {
        return new HashMapBuilder<String, Object>()
                .append("cls", cls)
                .append("listPropertyEnvelopeRepository", getListPropertyEnvelopeRepository())
                .append("init", getInit().get())
                ;
    }

}
