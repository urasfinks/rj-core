package ru.jamsys.core.extension.expiration;

public interface Expiration {

    // Должен вызываться, когда элемент протухает
    void onExpired();
}
