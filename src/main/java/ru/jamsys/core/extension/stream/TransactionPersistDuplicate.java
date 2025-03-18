package ru.jamsys.core.extension.stream;

import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerExpiration;
import ru.jamsys.core.component.manager.item.Expiration;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

// У нас есть уже записанные файлы на диске, в этих файлах есть разбитые данные с типом ByteTransformer.
// Наша задача проконтролировать, что все элементы файла были считаны и чтение было подтверждено.
// На вход будет поступать класс ByteTransformer и путь до файла.
// Мы загружаем в память все элементы файла и создаём карту ключей по смещению байт, где они лежат: 0-118, 119-234 и тд.
// Когда будут приходить commit чтения мы будем просто записывать в новый файл строки, что уже обработано.
// Это надо для того, что бы если приложение упадёт, можно было понять - что было точно считано, а что нет
// После того, как все элементы на обработку кончаются:
// 1) удаляем файл
// 2) Удаляем файл обработки
// 3) Запрашиваем следующий файл

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
