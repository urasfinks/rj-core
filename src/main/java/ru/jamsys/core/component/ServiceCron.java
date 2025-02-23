package ru.jamsys.core.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.cron.CronTask;
import ru.jamsys.core.extension.CascadeName;
import ru.jamsys.core.extension.LifeCycleComponent;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.template.cron.Cron;
import ru.jamsys.core.flat.template.cron.release.CronConfigurator;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

// Запуск обещаний помеченных
@SuppressWarnings("unused")
@Component
@Lazy
public class ServiceCron implements LifeCycleComponent, CascadeName {

    final private Thread thread;

    final private List<CronTask> listItem = new ArrayList<>();

    final private AtomicBoolean spin = new AtomicBoolean(true);

    final private AtomicBoolean run = new AtomicBoolean(true);

    @SuppressWarnings("all")
    public ServiceCron(
            ExceptionHandler exceptionHandler,
            ServiceClassFinder serviceClassFinder,
            ApplicationContext applicationContext
    ) {
        initList(serviceClassFinder, exceptionHandler);
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                long nextStartMs = System.currentTimeMillis();
                try {
                    while (spin.get() && !thread.isInterrupted()) {
                        nextStartMs = Util.zeroLastNDigits(nextStartMs + 1000, 3);
                        long curTimeMs = System.currentTimeMillis();

                        runCronTask(curTimeMs);

                        if (spin.get()) {
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
                    Util.logConsole(getClass(), "interrupt()");
                } catch (Throwable th) {
                    App.error(th);
                }
                run.set(false);
            }
        });
        thread.setName(getCascadeName());
    }

    private void runCronTask(long curTimeMs) {
        listItem.forEach((CronTask cronTask) -> {
            Cron.CompileResult compile = cronTask.getCron().compile(curTimeMs);
            if (compile.isTimeHasCome() && cronTask.getCronConfigurator().isTimeHasCome(compile)) {
                String indexPromise = null;
                try {
                    Promise promise = cronTask.getPromiseGenerator().generate();
                    if (promise != null) {
                        indexPromise = promise.getIndex();
                        promise.run();
                    }
                } catch (Exception e) {
                    App.error(new ForwardException("Cron task (" + indexPromise + ")::" + cronTask.getClass(), e));
                }
            }
        });
    }

    private void initList(
            ServiceClassFinder serviceClassFinder,
            ExceptionHandler exceptionHandler
    ) {
        serviceClassFinder.findByInstance(CronConfigurator.class).forEach((Class<CronConfigurator> cronTemplateClass) -> {
            CronConfigurator cronConfigurator = serviceClassFinder.instanceOf(cronTemplateClass);
            if (cronConfigurator instanceof PromiseGenerator promiseGenerator) {
                listItem.add(new CronTask(
                        new Cron(cronConfigurator.getCronTemplate()),
                        promiseGenerator,
                        cronConfigurator
                ));
            } else {
                exceptionHandler.handler(new RuntimeException(
                        "CronTemplate class: "
                                + cronConfigurator.getClass().getName()
                                + " not realise "
                                + PromiseGenerator.class.getName()
                                + " interface"
                ));
            }
        });
    }

    @Override
    public boolean isRun() {
        return run.get();
    }

    @Override
    public void run() {
        run.set(true);
        thread.start();
    }

    @Override
    public void shutdown() {
        spin.set(false);
        thread.interrupt();
        Util.await(run, 1500, getClass().getSimpleName() + " not stop interrupt");
    }

    @Override
    public int getInitializationIndex() {
        return 6;
    }

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public CascadeName getParentCascadeName() {
        return App.cascadeName;
    }

}
