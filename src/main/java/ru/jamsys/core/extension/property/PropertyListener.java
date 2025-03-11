package ru.jamsys.core.extension.property;

public interface PropertyListener {

    void onPropertyUpdate(String key, String oldValue, Property property); // Alias - это то что указано в аннотации @PropertyName

}
