package ru.jamsys.core.extension.property.repository;

import lombok.Getter;
import ru.jamsys.core.extension.property.Property;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class PropertyRepositoryMap<T> implements PropertyRepository {

    private final Class<T> cls;

    private final Map<String, T> mapRepository = new LinkedHashMap<>();

    private final List<String> propertyNotNull = new ArrayList<>();

    public PropertyRepositoryMap(Class<T> cls) {
        this.cls = cls;
    }

    public T getRepositoryItem(String key) {
        return getMapRepository().get(key);
    }

    @Override
    public Map<String, String> getRepository() {
        Map<String, String> result = new LinkedHashMap<>();
        UtilRisc.forEach(null, mapRepository, (s, t) -> {
            result.put(s, t == null ? null : String.valueOf(t));
        });
        return result;
    }

    @Override
    public void setRepository(String key, String value) {
        @SuppressWarnings("unchecked")
        T apply = (T) Property.convertType.get(cls).apply(value);
        mapRepository.put(key, apply);
    }

    @Override
    public PropertyRepository checkNotNull() {
        UtilRisc.forEach(null, mapRepository, (key, value) -> {
            if (propertyNotNull.contains(key) && value == null) {
                throw new RuntimeException(getClass().getName() + " key: " + key + "; value is null");
            }
        });
        return this;
    }

}
