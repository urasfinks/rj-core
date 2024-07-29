package ru.jamsys.core.extension.property;

import ru.jamsys.core.extension.property.item.PropertiesRepository;

import java.util.LinkedHashMap;
import java.util.Map;

public class PropertiesRepositoryMap implements PropertiesRepository {

    private final Map<String, String> mapProp = new LinkedHashMap<>();

    @Override
    public Map<String, String> getPropValue() {
        return mapProp;
    }

    @Override
    public void setPropValue(String prop, String value) {
        mapProp.put(prop, value);
    }

}
