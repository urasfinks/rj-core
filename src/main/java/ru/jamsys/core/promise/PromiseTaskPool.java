package ru.jamsys.core.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.Completable;
import ru.jamsys.core.extension.Resource;
import ru.jamsys.core.pool.PoolItemEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

// RA - ResourceArguments
// RR - ResourceResult
// PI - PoolItem

// Переопределён старт задачи. Если раньше старт приводил к передачи задачи в Thread на исполнение
// То сейчас страт задачи, регистрирует в пуле потребность получения ресурса
// Когда ресурс в пуле высвобождается, пул вызывает start(poolItem) текущей задачи, передавая ссылку на себя ресурс
// Далее по стандартной траектории вызывается super.start(), который запустит run задачи в отдельном потоке
// и в конечном счёте run запустит executeBlock, где мы поработаем с ресурсом в рамках потока исполнения задачи
// и вызовем supplier/procedure c подготовленным уже результатом исполнения

public class PromiseTaskPool<RA, RR, PI extends Completable & ExpirationMsMutable & Resource<RA, RR>> extends PromiseTask {

    final AbstractPoolPromise<RA, RR, PI> pool;

    @Getter
    @Setter
    private PoolItemEnvelope<RA, RR, PI> poolItemEnvelope;

    private BiFunction<AtomicBoolean, RR, List<PromiseTask>> supplier;

    private BiConsumer<AtomicBoolean, RR> procedure;

    private Function<Promise, RA> argumentsFunction;

    RA argument;

    public PromiseTaskPool(
            String index,
            Promise promise,
            PromiseTaskType type,
            AbstractPoolPromise<RA, RR, PI> pool,
            RA argument,
            BiFunction<AtomicBoolean, RR, List<PromiseTask>> supplier
    ) {
        super(index, promise, type);
        this.pool = pool;
        this.argument = argument;
        this.supplier = supplier;
    }

    public PromiseTaskPool(
            String index,
            Promise promise,
            PromiseTaskType type,
            AbstractPoolPromise<RA, RR, PI> pool,
            RA argument,
            BiConsumer<AtomicBoolean, RR> procedure
    ) {
        super(index, promise, type);
        this.pool = pool;
        this.argument = argument;
        this.procedure = procedure;
    }

    public PromiseTaskPool(
            String index,
            Promise promise,
            PromiseTaskType type,
            AbstractPoolPromise<RA, RR, PI> pool,
            Function<Promise, RA> argumentsFunction,
            BiConsumer<AtomicBoolean, RR> procedure
    ) {
        super(index, promise, type);
        this.pool = pool;
        this.argumentsFunction = argumentsFunction;
        this.procedure = procedure;
    }

    // Этот блок вызывается из Promise.loop() и подразумевает запуск ::run из внешнего потока
    // Мы его переопределили, добавляя задачу в Pool, а вот уже когда освободится ресурс в пуле
    // Пул сам вызовет start с передачей туда ресурс, там то мы и вызовем ::run из внешнего потока
    @Override
    public void start() {
        try {
            pool.addPromiseTaskPool(this);
        } catch (Exception e) {
            getPromise().complete(this, e);
        }
    }

    @Override
    protected void executeBlock() throws Throwable {
        try (PoolItemEnvelope<RA, RR, PI> res = getPoolItemEnvelope()) {
            if (argumentsFunction != null) {
                argument = argumentsFunction.apply(getPromise());
            }
            if (supplier != null) {
                getPromise().complete(this, supplier.apply(isThreadRun, res.getItem().execute(argument)));
            } else if (procedure != null) {
                procedure.accept(isThreadRun, res.getItem().execute(argument));
                getPromise().complete(this);
            }
        }
    }

    // Пул вызывает этот метод
    public void start(PoolItemEnvelope<RA, RR, PI> poolItem) {
        setPoolItemEnvelope(poolItem);
        super.start();
    }

}
