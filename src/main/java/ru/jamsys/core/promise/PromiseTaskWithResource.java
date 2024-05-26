package ru.jamsys.core.promise;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.PoolResourceManagerForPromiseTask;
import ru.jamsys.core.component.manager.sub.ManagerElement;
import ru.jamsys.core.component.manager.sub.PoolResourceArgument;
import ru.jamsys.core.extension.trace.Trace;
import ru.jamsys.core.pool.PoolItemEnvelope;
import ru.jamsys.core.resource.PoolResourceForPromiseTask;
import ru.jamsys.core.resource.Resource;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class PromiseTaskWithResource<T extends Resource<?, ?, ?>> extends PromiseTask {

    @Getter
    @Setter
    private PoolItemEnvelope<?, ?, ?, T> poolItemEnvelope;

    private BiFunction<AtomicBoolean, T, List<PromiseTask>> supplier;

    private BiConsumer<AtomicBoolean, T> procedure;

    private final ManagerElement<PoolResourceForPromiseTask<?, ?, ?, ?>, PoolResourceArgument<?, ?>> managerElement;

    @SuppressWarnings("all")
    private final Class<T> classResource;

    @SuppressWarnings("all")
    public PromiseTaskWithResource(
            String index,
            Promise promise,
            PromiseTaskExecuteType type,
            PoolResourceArgument<T, ?> poolArgument,
            BiConsumer<AtomicBoolean, T> procedure,
            Class<T> classResource
    ) {
        super(index, promise, type);
        this.procedure = procedure;
        this.classResource = classResource;
        PoolResourceManagerForPromiseTask poolResourceManagerForPromiseTask = App.context.getBean(PoolResourceManagerForPromiseTask.class);
        managerElement = poolResourceManagerForPromiseTask.get(index, poolArgument);
    }

    @SuppressWarnings("all")
    public PromiseTaskWithResource(
            String index,
            Promise promise,
            PromiseTaskExecuteType type,
            PoolResourceArgument<T, ?> poolArgument,
            BiFunction<AtomicBoolean, T, List<PromiseTask>> supplier,
            Class<T> classResource
    ) {
        super(index, promise, type);
        this.supplier = supplier;
        this.classResource = classResource;
        PoolResourceManagerForPromiseTask poolResourceManagerForPromiseTask = App.context.getBean(PoolResourceManagerForPromiseTask.class);
        managerElement = poolResourceManagerForPromiseTask.get(index, poolArgument);
    }

    // Этот блок вызывается из Promise.loop() и подразумевает запуск ::run из внешнего потока
    // Мы его переопределили, добавляя задачу в Pool, а вот уже когда освободится ресурс в пуле
    // Пул сам вызовет start с передачей туда ресурса, там то мы и вызовем ::run из внешнего потока
    @Override
    public void start() {
        try {
            managerElement.get().addPromiseTaskPool(this);
        } catch (Exception e) {
            getPromise().complete(this, e);
        }
    }

    @Override
    protected void executeBlock() throws Throwable {
        try (PoolItemEnvelope<?, ?, ?, T> res = getPoolItemEnvelope()) {
            if (supplier != null) {
                getPromise().complete(this, supplier.apply(isThreadRun, res.getItem()));
            } else if (procedure != null) {
                procedure.accept(isThreadRun, res.getItem());
                getPromise().complete(this);
            }
        }
    }

    // Пул вызывает этот метод
    public void start(PoolItemEnvelope<?, ?, ?, T> poolItem) {
        setPoolItemEnvelope(poolItem);
        getPromise().getTrace().add(new Trace<>(getIndex() + ".PoolItemEnvelope-Received", null, null, null));
        super.start();
    }

}
