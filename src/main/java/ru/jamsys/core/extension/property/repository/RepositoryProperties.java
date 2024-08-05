package ru.jamsys.core.extension.property.repository;

import java.util.LinkedHashMap;
import java.util.Map;

public interface RepositoryProperties {

    void setNs(String ns);

    Map<String, RepositoryMapValue<?>> getMapRepository();

    void setProperty(String prop, String value);

    default Map<String, Object> unitTestGetProperties() {
        Map<String, Object> result = new LinkedHashMap<>();
        getMapRepository().forEach((s, repositoryElement) -> result.put(s, repositoryElement.getValue()));
        return result;
    }

}
