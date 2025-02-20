package ru.jamsys.core.extension.property.repository;

import ru.jamsys.core.flat.util.UtilRisc;

import java.util.Map;

public interface PropertyRepository {

    Map<String, String> getRepository();

    void setRepository(String key, String value);

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
