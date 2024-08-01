package ru.jamsys.core.extension.property.repository;

import java.util.Map;

public interface RepositoryProperties {

    Map<String, String> getProperties();

    void setProperty(String prop, String value);

}
