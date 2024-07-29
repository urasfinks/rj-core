package ru.jamsys.core.extension.property;

import java.util.Map;

public interface PropertyUpdateDelegate {

    void onPropertyUpdate(Map<String, String> mapAlias); // Alias - это то что указано в аннотации @PropertyName

}
