package ru.jamsys.core.extension.property.repository;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class PropertyRepositoryList<T> implements PropertyRepository<T> {

    private final Class<T> cls;

    final List<PropertyEnvelopeRepository<T>> listPropertyEnvelopeRepository = new ArrayList<>();

    private final AtomicBoolean init = new AtomicBoolean(false);

    @JsonIgnore
    private PropertyDispatcher<T> propertyDispatcher;

    public PropertyRepositoryList(Class<T> cls) {
        this.cls = cls;
    }

    @Override
    public void init(PropertyDispatcher<T> propertyDispatcher) {
        this.propertyDispatcher = propertyDispatcher;
        if (init.compareAndSet(false, true)) {
            UtilRisc.forEach(null, listPropertyEnvelopeRepository, tPropertyEnvelopeRepository -> {
                tPropertyEnvelopeRepository
                        .setPropertyKey(propertyDispatcher.getPropertyKey(tPropertyEnvelopeRepository.getRepositoryPropertyKey()))
                        .setServiceProperty(propertyDispatcher.getServiceProperty())
                        .syncPropertyValue();
            });
        }
    }

    @Override
    public void append(String repositoryPropertyKey, PropertyDispatcher<T> propertyDispatcher) {
        PropertyEnvelopeRepository<T> tPropertyEnvelopeRepository = new PropertyEnvelopeRepository<>(
                cls,
                repositoryPropertyKey,
                null,
                false
        )
                .setDescription("AdditionalProperties")
                .setPropertyKey(propertyDispatcher.getPropertyKey(repositoryPropertyKey))
                .setServiceProperty(propertyDispatcher.getServiceProperty())
                .setPropertyRepository(this)
                .setDynamic(true)
                .syncPropertyValue();
        if (!listPropertyEnvelopeRepository.contains(tPropertyEnvelopeRepository)) {
            listPropertyEnvelopeRepository.add(tPropertyEnvelopeRepository);
        }
    }

    @Override
    public void updateRepository(String repositoryPropertyKey, PropertyDispatcher<T> propertyDispatcher) {
        this.propertyDispatcher = propertyDispatcher;
        PropertyEnvelopeRepository<T> propertyEnvelopeRepository = getByRepositoryPropertyKey(repositoryPropertyKey);
        if (propertyEnvelopeRepository == null) {
            append(repositoryPropertyKey, propertyDispatcher);
        } else {
            propertyEnvelopeRepository.syncPropertyValue();
        }
    }

}
