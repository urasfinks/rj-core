package ru.jamsys.core.extension.property;

public interface PropertyListener {

    void onPropertyUpdate(String key, String oldValue, String newValue); // Alias - это то что указано в аннотации @PropertyKey

}
