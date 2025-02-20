package ru.jamsys.core.extension.property;

public interface PropertyUpdater {

    void onPropertyUpdate(String key, Property property); // Alias - это то что указано в аннотации @PropertyName

}
