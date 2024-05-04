package ru.jamsys.core.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.api.ClassFinder;
import ru.jamsys.core.component.api.RateLimitManager;
import ru.jamsys.core.component.item.CronPromise;
import ru.jamsys.core.extension.RunnableComponent;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.resource.thread.ThreadEnvelope;
import ru.jamsys.core.resource.thread.ThreadPool;
import ru.jamsys.core.template.cron.Cron;
import ru.jamsys.core.template.cron.release.CronTemplate;
import ru.jamsys.core.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
@Lazy
public class CronManager implements RunnableComponent {

    final private ThreadPool threadPool;
    final private List<CronPromise> listItem = new ArrayList<>();

    public CronManager(
            ExceptionHandler exceptionHandler,
            RateLimitManager rateLimitManager,
            ClassFinder classFinder,
            ApplicationContext applicationContext
    ) {
        initList(classFinder, applicationContext, exceptionHandler);
        this.threadPool = new ThreadPool(
                getClass().getSimpleName(),
                1,
                (ThreadEnvelope threadEnvelope) -> {
                    long nextStartMs = System.currentTimeMillis();
                    AtomicBoolean isWhile = threadEnvelope.getIsWhile();
                    while (isWhile.get() && threadEnvelope.isNotInterrupted()) {
                        nextStartMs = Util.zeroLastNDigits(nextStartMs + 1000, 3);
                        long curTimeMs = System.currentTimeMillis();

                        runCronTask(curTimeMs, isWhile);

                        if (isWhile.get()) {
                            long calcSleepMs = nextStartMs - System.currentTimeMillis();
                            if (calcSleepMs > 0) {
                                Util.sleepMs(calcSleepMs);
                            } else {
                                Util.sleepMs(1);//Что бы поймать Interrupt
                                nextStartMs = System.currentTimeMillis();
                            }
                        } else {
                            break;
                        }
                    }
                    Util.logConsole("STOP");
                    return false;
                }
        );
    }

    private void runCronTask(long curTimeMs, AtomicBoolean isThreadRun) {
        listItem.forEach((CronPromise cronPromise) -> {
            if (cronPromise.getCron().isTimeHasCome(curTimeMs)) {
                try {
                    cronPromise.getPromiseGenerator().generate().run(isThreadRun);
                } catch (Exception e) {
                    App.context.getBean(ExceptionHandler.class).handler(e);
                }
            }
        });
    }

    private void initList(
            ClassFinder classFinder,
            ApplicationContext applicationContext,
            ExceptionHandler exceptionHandler
    ) {
        classFinder.findByInstance(CronTemplate.class).forEach((Class<CronTemplate> statisticsCollectorClass) -> {
            CronTemplate cronTemplate = applicationContext.getBean(statisticsCollectorClass);
            if (cronTemplate instanceof PromiseGenerator) {
                listItem.add(
                        new CronPromise(new Cron(cronTemplate.getCronTemplate()), (PromiseGenerator) cronTemplate)
                );
            } else {
                exceptionHandler.handler(new RuntimeException(
                        "CronTemplate class: "
                                + cronTemplate.getClass().getSimpleName()
                                + " not realise "
                                + PromiseGenerator.class.getSimpleName()
                                + " interface"
                ));
            }
        });
    }

    @Override
    public void run() {
        threadPool.run();
        threadPool.wakeUp();
    }

    @Override
    public void shutdown() {
        threadPool.shutdown();
    }

}
