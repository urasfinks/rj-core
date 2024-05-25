package ru.jamsys.core.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.sub.ManagerElement;
import ru.jamsys.core.component.manager.sub.PoolResourceArgument;
import ru.jamsys.core.component.manager.PoolResourceManagerForPromiseTask;
import ru.jamsys.core.extension.Completable;
import ru.jamsys.core.extension.trace.Trace;
import ru.jamsys.core.pool.PoolItemEnvelope;
import ru.jamsys.core.resource.PoolResourceForPromiseTask;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

// RA - ResourceArguments
// RR - ResourceResult
// PI - PoolItem

// Переопределён старт задачи. Если раньше старт приводил к передачи задачи в Thread на исполнение,
// То сейчас страт задачи, регистрирует в пуле потребность получения ресурса
// Когда ресурс в пуле высвобождается, пул вызывает start(poolItem) текущей задачи, передавая ссылку на себя ресурс
// Далее по стандартной траектории вызывается super.start(), который запустит run задачи в отдельном потоке
// и в конечном счёте run запустит executeBlock, где мы поработаем с ресурсом в рамках потока исполнения задачи
// и вызовем supplier/procedure c подготовленным уже результатом исполнения

public class PromiseTaskWithResource<RC, RA, RR, PI extends Completable & ExpirationMsMutable & Resource<RC, RA, RR>> extends PromiseTask {

    final ManagerElement<PoolResourceForPromiseTask<RC, RA, RR, PI>, PoolResourceArgument<PI, RC>> poolResourceManagerElement;

    @Getter
    @Setter
    private PoolItemEnvelope<RC, RA, RR, PI> poolItemEnvelope;

    private BiFunction<AtomicBoolean, RR, List<PromiseTask>> supplier;

    private BiConsumer<AtomicBoolean, RR> procedure;

    private final Function<Promise, RA> argumentsFunction;

    public PromiseTaskWithResource(
            String index,
            Promise promise,
            PromiseTaskExecuteType type,
            PoolResourceArgument<PI, RC> poolArgument,
            Function<Promise, RA> argumentsFunction,
            BiConsumer<AtomicBoolean, RR> procedure
    ) {
        super(index, promise, type);
        this.procedure = procedure;
        this.argumentsFunction = argumentsFunction;
        @SuppressWarnings("all")
        PoolResourceManagerForPromiseTask<RC, RA, RR, PI> poolResourceManagerForPromiseTask = App.context.getBean(PoolResourceManagerForPromiseTask.class);
        poolResourceManagerElement = poolResourceManagerForPromiseTask.get(index, poolArgument);
    }

    public PromiseTaskWithResource(
            String index,
            Promise promise,
            PromiseTaskExecuteType type,
            PoolResourceArgument<PI, RC> poolArgument,
            Function<Promise, RA> argumentsFunction,
            BiFunction<AtomicBoolean, RR, List<PromiseTask>> supplier
    ) {
        super(index, promise, type);
        this.supplier = supplier;
        this.argumentsFunction = argumentsFunction;
        @SuppressWarnings("all")
        PoolResourceManagerForPromiseTask<RC, RA, RR, PI> poolResourceManagerForPromiseTask = App.context.getBean(PoolResourceManagerForPromiseTask.class);
        poolResourceManagerElement = poolResourceManagerForPromiseTask.get(index, poolArgument);
    }



    // Этот блок вызывается из Promise.loop() и подразумевает запуск ::run из внешнего потока
    // Мы его переопределили, добавляя задачу в Pool, а вот уже когда освободится ресурс в пуле
    // Пул сам вызовет start с передачей туда ресурса, там то мы и вызовем ::run из внешнего потока
    @Override
    public void start() {
        try {
            poolResourceManagerElement.get().addPromiseTaskPool(this);
        } catch (Exception e) {
            getPromise().complete(this, e);
        }
    }

    @Getter
    private PI resource = null;

    @Override
    protected void executeBlock() throws Throwable {
        try (PoolItemEnvelope<RC, RA, RR, PI> res = getPoolItemEnvelope()) {
            resource  = res.getItem();
            RA argument = argumentsFunction.apply(getPromise());
            if (supplier != null) {
                getPromise().complete(this, supplier.apply(isThreadRun, resource.execute(argument)));
            } else if (procedure != null) {
                procedure.accept(isThreadRun, res.getItem().execute(argument));
                getPromise().complete(this);
            }
        }
    }

    // Пул вызывает этот метод
    public void start(PoolItemEnvelope<RC, RA, RR, PI> poolItem) {
        setPoolItemEnvelope(poolItem);
        getPromise().getTrace().add(new Trace<>(getIndex() + ".PoolItemEnvelope-Received", null, null, null));
        super.start();
    }

}
