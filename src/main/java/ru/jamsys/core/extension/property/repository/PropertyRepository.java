package ru.jamsys.core.extension.property.repository;

import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.List;

public interface PropertyRepository<T> {

    void init(PropertyDispatcher<T> propertyDispatcher);

    List<PropertyEnvelopeRepository<T>> getListPropertyEnvelopeRepository(); // Репозиторий значений

    void append(String repositoryPropertyKey, PropertyDispatcher<T> propertyDispatcher);

    void updateRepository(String repositoryPropertyKey, PropertyDispatcher<T> propertyDispatcher);

    default void checkNotNull() {
        UtilRisc.forEach(null, getListPropertyEnvelopeRepository(), fieldData -> {
            if (fieldData.isNotNull() && fieldData.getValue() == null) {
                throw new RuntimeException(UtilJson.toStringPretty(this, "--"));
            }
        });
    }

    default PropertyEnvelopeRepository<T> getByFieldNameConstants(String FieldNameConstants) { //@FieldNameConstants -> FileByteProperty.Fields.folder
        for (PropertyEnvelopeRepository<T> propertyEnvelopeRepository : getListPropertyEnvelopeRepository()) {
            if (propertyEnvelopeRepository.getFieldNameConstants().equals(FieldNameConstants)) {
                return propertyEnvelopeRepository;
            }
        }
        return null;
    }

    default PropertyEnvelopeRepository<T> getByRepositoryPropertyKey(String repositoryPropertyKey) {
        for (PropertyEnvelopeRepository<T> propertyEnvelopeRepository : getListPropertyEnvelopeRepository()) {
            if (propertyEnvelopeRepository.getRepositoryPropertyKey().equals(repositoryPropertyKey)) {
                return propertyEnvelopeRepository;
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    default PropertyEnvelopeRepository<T> getByPropertyKey(String propertyKey) {
        for (PropertyEnvelopeRepository<T> propertyEnvelopeRepository : getListPropertyEnvelopeRepository()) {
            if (propertyEnvelopeRepository.getPropertyKey().equals(propertyKey)) {
                return propertyEnvelopeRepository;
            }
        }
        return null;
    }

}
