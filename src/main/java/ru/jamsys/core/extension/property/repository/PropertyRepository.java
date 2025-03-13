package ru.jamsys.core.extension.property.repository;

import ru.jamsys.core.flat.util.UtilRisc;

import java.util.Map;

public interface PropertyRepository {

    Map<String, String> getRepository(); // Репозиторий значений

    void setRepository(String propertyName, String value);

    String getDescription(String key);

    PropertyRepository checkNotNull();

    default PropertyRepository isAnyPropertyNullThrow() {
        UtilRisc.forEach(null, getRepository(), (key, value) -> {
            if (value == null) {
                throw new RuntimeException(getClass().getName() + " key: " + key + "; value is null");
            }
        });
        return this;
    }

}
