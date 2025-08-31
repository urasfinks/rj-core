package ru.jamsys.core.extension.expiration;

public interface ExpirationDrop {

    // Должен вызываться, когда элемент выбрасывают по причине протухания
    void onExpirationDrop();
}
