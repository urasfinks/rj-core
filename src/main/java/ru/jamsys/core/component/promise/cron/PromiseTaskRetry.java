package ru.jamsys.core.component.promise.cron;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.core.promise.*;
import ru.jamsys.core.statistic.time.mutable.ExpiredMsMutableEnvelope;
import ru.jamsys.core.template.cron.release.Cron1s;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
//TODO: тут бы доделать
public class PromiseTaskRetry implements Cron1s, PromiseGenerator {

    public PromiseTaskRetry(ApplicationContext applicationContext) {
//        @SuppressWarnings("unchecked")
//        SessionManager<PromiseTask> sessionManager = applicationContext.getBean(SessionManager.class);
        //this.promiseCache = cacheManager.get(ClassNameImpl.getClassNameStatic(PromiseTask.class, null, applicationContext));
        //this.promiseCache.setOnExpired(this::retryPromiseTask);
    }

    private void retryPromiseTask(ExpiredMsMutableEnvelope<PromiseTask> tePromiseTask) {
        tePromiseTask.getValue().start();
    }

    public void add(PromiseTask promiseTask) {
        //promiseCache.add(promiseTask.getPromise().getRqUid(), promiseTask, promiseTask.getRetryDelayMs());
    }

    @Override
    public Promise generate() {
//        return new PromiseImpl(getClass().getName())
//                .append(this.getClass().getName(), PromiseTaskType.IO, (AtomicBoolean isThreadRun)
//                        -> promiseCache.keepAlive(isThreadRun));
        return null;
    }

}
