package ru.jamsys.core.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.cron.CronPromise;
import ru.jamsys.core.extension.ClassName;
import ru.jamsys.core.extension.LifeCycleComponent;
import ru.jamsys.core.flat.template.cron.Cron;
import ru.jamsys.core.flat.template.cron.release.CronTemplate;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.promise.PromiseGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Lazy
public class CronComponent implements LifeCycleComponent, ClassName {

    final private Thread thread;
    final private List<CronPromise> listItem = new ArrayList<>();
    final private AtomicBoolean isWhile = new AtomicBoolean(true);

    @SuppressWarnings("all")
    public CronComponent(
            ExceptionHandler exceptionHandler,
            ClassFinderComponent classFinderComponent,
            ApplicationContext applicationContext
    ) {
        initList(classFinderComponent, applicationContext, exceptionHandler);
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                long nextStartMs = System.currentTimeMillis();
                try {
                    while (isWhile.get() && !thread.isInterrupted()) {
                        nextStartMs = Util.zeroLastNDigits(nextStartMs + 1000, 3);
                        long curTimeMs = System.currentTimeMillis();

                        runCronTask(curTimeMs);

                        if (isWhile.get()) {
                            long calcSleepMs = nextStartMs - System.currentTimeMillis();
                            if (calcSleepMs > 0) {
                                Thread.sleep(calcSleepMs);
                            } else {
                                Thread.sleep(1);//Что бы поймать Interrupt
                                nextStartMs = System.currentTimeMillis();
                            }
                        } else {
                            break;
                        }
                    }
                } catch (InterruptedException ie) {
                    Util.logConsole("STOP");
                } catch (Exception e) {
                    App.context.getBean(ExceptionHandler.class).handler(e);
                }
            }
        });
        thread.setName("CronComponent");
    }

    private void runCronTask(long curTimeMs) {
        listItem.forEach((CronPromise cronPromise) -> {
            if (cronPromise.getCron().isTimeHasCome(curTimeMs)) {
                try {
                    cronPromise.getPromiseGenerator().generate().run();
                } catch (Exception e) {
                    App.context.getBean(ExceptionHandler.class).handler(e);
                }
            }
        });
    }

    private void initList(
            ClassFinderComponent classFinderComponent,
            ApplicationContext applicationContext,
            ExceptionHandler exceptionHandler
    ) {
        classFinderComponent.findByInstance(CronTemplate.class).forEach((Class<CronTemplate> statisticsCollectorClass) -> {
            CronTemplate cronTemplate = applicationContext.getBean(statisticsCollectorClass);
            if (cronTemplate instanceof PromiseGenerator) {
                listItem.add(
                        new CronPromise(new Cron(cronTemplate.getCronTemplate()), (PromiseGenerator) cronTemplate)
                );
            } else {
                exceptionHandler.handler(new RuntimeException(
                        "CronTemplate class: "
                                + cronTemplate.getClass().getName()
                                + " not realise "
                                + PromiseGenerator.class.getName()
                                + " interface"
                ));
            }
        });
    }

    @Override
    public void run() {
        thread.start();
    }

    @Override
    public void shutdown() {
        isWhile.set(false);
        thread.interrupt();
    }

}
