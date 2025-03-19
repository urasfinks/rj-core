package ru.jamsys.core.extension.raw.writer;

import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerExpiration;
import ru.jamsys.core.component.manager.item.Expiration;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

public class TransactionPersistDuplicate<T> {

    private final ConcurrentLinkedDeque<T> input = new ConcurrentLinkedDeque<>();

    private final AtomicInteger inputSize = new AtomicInteger(0);

    private final Expiration<T> expiration;

    private final long rollbackTime;

    public TransactionPersistDuplicate(String key, Class<T> cls, int rollbackTime) {
        this.rollbackTime = rollbackTime;
        expiration = App.get(ManagerExpiration.class).get(key, cls, tDisposableExpirationMsImmutableEnvelope -> {
            T value = tDisposableExpirationMsImmutableEnvelope.getValue();
            if (value != null) {
                inputSize.incrementAndGet();
                input.add(value);
            }
        });
    }

    public boolean isEmpty() {
        return expiration.isEmpty() && input.isEmpty();
    }

    public T readWithAutoCommit() {
        T poll = input.pollLast();
        if (poll != null) {
            this.inputSize.decrementAndGet();
        }
        return poll;
    }

    public Transaction<T> read() {
        T poll = input.pollLast();
        if (poll != null) {
            this.inputSize.decrementAndGet();
            return new Transaction<>(
                    expiration.add(new DisposableExpirationMsImmutableEnvelope<>(poll, rollbackTime)),
                    poll
            );
        }
        return null;
    }

    public void commit(Transaction<T> transaction) {
        expiration.remove(transaction.getId());
    }

}
