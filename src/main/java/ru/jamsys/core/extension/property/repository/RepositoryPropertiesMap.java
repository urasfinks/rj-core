package ru.jamsys.core.extension.property.repository;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class RepositoryPropertiesMap<T> implements RepositoryProperties {

    @Setter
    private String ns;

    private final Map<String, RepositoryMapValue<T>> mapRepository2 = new LinkedHashMap<>();


    private final Class<T> cls;

    public RepositoryPropertiesMap(Class<T> cls) {
        this.cls = cls;
    }

    @Override
    public Map<String, RepositoryMapValue<?>> getMapRepository() {
        return new LinkedHashMap<>(mapRepository2);
    }

    @Override
    public void setProperty(String prop, String value) {
        RepositoryMapValue<?> repositoryMapValue = mapRepository2.computeIfAbsent(prop, s
                -> new RepositoryMapValue<>(cls, value, "ns: " + ns + "; key: " + s)
        );
        repositoryMapValue.setValue(value);
    }

}
