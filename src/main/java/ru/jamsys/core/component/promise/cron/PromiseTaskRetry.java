package ru.jamsys.core.component.promise.cron;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.api.CacheManager;
import ru.jamsys.core.component.item.Cache;
import ru.jamsys.core.extension.CLassNameTitleImpl;
import ru.jamsys.core.promise.*;
import ru.jamsys.core.statistic.time.TimeEnvelopeMs;
import ru.jamsys.core.template.cron.release.Cron1s;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
public class PromiseTaskRetry implements Cron1s, PromiseGenerator {

    final Cache<String, PromiseTask> promiseCache;

    public PromiseTaskRetry(ApplicationContext applicationContext) {
        @SuppressWarnings("unchecked")
        CacheManager<PromiseTask> cacheManager = applicationContext.getBean(CacheManager.class);
        this.promiseCache = cacheManager.get(CLassNameTitleImpl.getClassNameTitleStatic(PromiseTask.class, null, applicationContext));
        this.promiseCache.setOnExpired(this::retryPromiseTask);
    }

    private void retryPromiseTask(TimeEnvelopeMs<PromiseTask> tePromiseTask) {
        tePromiseTask.getValue().start();
    }

    public void add(PromiseTask promiseTask) {
        promiseCache.add(promiseTask.getPromise().getRqUid(), promiseTask, promiseTask.getRetryDelayMs());
    }

    @Override
    public Promise generate() {
        return new PromiseImpl(getClass().getName())
                .append(this.getClass().getName(), PromiseTaskType.IO, (AtomicBoolean isThreadRun)
                        -> promiseCache.keepAlive(isThreadRun));
    }

}
