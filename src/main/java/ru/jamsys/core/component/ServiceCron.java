package ru.jamsys.core.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.cron.CronTask;
import ru.jamsys.core.extension.AbstractLifeCycle;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.LifeCycleComponent;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.template.cron.Cron;
import ru.jamsys.core.flat.template.cron.release.CronConfigurator;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

// Запуск обещаний помеченных
@SuppressWarnings("unused")
@Component
@Lazy
public class ServiceCron extends AbstractLifeCycle implements LifeCycleComponent, CascadeKey {

    private Thread thread;

    final private List<CronTask> listItem = new ArrayList<>();

    final private AtomicBoolean spin = new AtomicBoolean(true);

    final private AtomicBoolean threadWork = new AtomicBoolean(false);

    private void runCronTask(long curTimeMs) {
        listItem.forEach((CronTask cronTask) -> {
            Cron.CompileResult compile = cronTask.getCron().compile(curTimeMs);
            if (compile.isTimeHasCome() && cronTask.getCronConfigurator().isTimeHasCome(compile)) {
                Promise promise = null;
                try {
                    promise = cronTask.getPromiseGenerator().generate();
                    if (promise != null) {
                        promise.run();
                    }
                } catch (Exception e) {
                    App.error(new ForwardException(e), new HashMapBuilder<String, Object>()
                            .append("cronTask", cronTask)
                            .append("promise", promise));
                }
            }
        });
    }

    private void initList() {
        listItem.clear();
        ServiceClassFinder serviceClassFinder = App.get(ServiceClassFinder.class);
        serviceClassFinder.findByInstance(CronConfigurator.class).forEach((Class<CronConfigurator> cronTemplateClass) -> {
            CronConfigurator cronConfigurator = serviceClassFinder.instanceOf(cronTemplateClass);
            if (cronConfigurator instanceof PromiseGenerator promiseGenerator) {
                listItem.add(new CronTask(
                        new Cron(cronConfigurator.getCronTemplate()),
                        promiseGenerator,
                        cronConfigurator
                ));
            } else {
                App.error(new RuntimeException(
                        "CronTemplate class: "
                                + cronConfigurator.getClass().getName()
                                + " not realise "
                                + PromiseGenerator.class.getName()
                                + " interface"));

            }
        });
    }

    @Override
    public void runOperation() {
        initList();
        spin.set(true);
        thread = new Thread(() -> {
            threadWork.set(true);
            long nextStartMs = System.currentTimeMillis();
            try {
                while (spin.get() && !thread.isInterrupted()) {
                    nextStartMs = Util.resetLastNDigits(nextStartMs + 1000, 3);
                    long curTimeMs = System.currentTimeMillis();

                    runCronTask(curTimeMs);

                    if (spin.get()) {
                        long calcSleepMs = nextStartMs - System.currentTimeMillis();
                        if (calcSleepMs > 0) {
                            Thread.sleep(calcSleepMs);
                        } else {
                            nextStartMs = System.currentTimeMillis();
                        }
                    } else {
                        break;
                    }
                }
            } catch (InterruptedException ie) {
                UtilLog.printError("interrupt()");
            } catch (Throwable th) {
                App.error(th);
            } finally {
                threadWork.set(false);
            }
        });
        thread.setName(getCascadeKey());
        thread.start();
    }

    @Override
    public void shutdownOperation() {
        spin.set(false);
        Util.await(threadWork, 1500, 100, () -> {
            thread.interrupt();
            UtilLog.printError("interrupt");
        });
    }

    @Override
    public int getInitializationIndex() {
        return 6;
    }

}
