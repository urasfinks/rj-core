package ru.jamsys.core.extension.property.item;

import java.util.Map;

public interface PropertiesRepository {

    Map<String, String> getPropValue();

    void setPropValue(String prop, String value);

}
