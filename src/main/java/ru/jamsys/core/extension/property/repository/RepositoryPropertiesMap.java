package ru.jamsys.core.extension.property.repository;

import java.util.LinkedHashMap;
import java.util.Map;

public class RepositoryPropertiesMap implements RepositoryProperties {

    private final Map<String, String> mapProperties = new LinkedHashMap<>();

    @Override
    public Map<String, String> getProperties() {
        return mapProperties;
    }

    @Override
    public void setProperty(String prop, String value) {
        mapProperties.put(prop, value);
    }

}
