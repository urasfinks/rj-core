package ru.jamsys.core.pool;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.expiration.AbstractExpirationResource;

// Этот объект передаётся во внешнее управление.
// Что бы вернуть элемент в pool надо знать к какому pool этот элемент принадлежит. Поэтому собираем такую обёртку,
// что бы она содержала и пул и элемент и авто закрытие с возвращением элемента в пул

@Getter
@Setter
public class PoolItemCompletable<T extends AbstractExpirationResource> implements AutoCloseable {

    private final Pool<T> pool;

    private final T item;

    private Throwable throwable = null;

    public PoolItemCompletable(Pool<T> pool, T item) {
        this.pool = pool;
        this.item = item;
    }

    @Override
    public void close() {
        this.pool.release(this.item, throwable);
    }

}
