package ru.jamsys.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.App;
import ru.jamsys.extension.KeepAliveComponent;
import ru.jamsys.extension.RunnableComponent;
import ru.jamsys.extension.RunnableInterface;
import ru.jamsys.extension.StatisticsCollectorComponent;
import ru.jamsys.pool.ThreadPool;
import ru.jamsys.rate.limit.RateLimitName;
import ru.jamsys.template.cron.Cron;
import ru.jamsys.template.cron.CronTask;
import ru.jamsys.thread.generator.Generator;
import ru.jamsys.thread.handler.Handler;
import ru.jamsys.thread.task.AbstractTask;
import ru.jamsys.thread.task.KeepAlive;
import ru.jamsys.thread.task.StatisticCollectorFlush;
import ru.jamsys.thread.task.StatisticSecFlush;
import ru.jamsys.util.ListSort;

import java.util.List;


@Component
@Lazy
public class Core implements RunnableInterface {

    private final Dictionary dictionary;

    public Core(ApplicationContext applicationContext, Dictionary dictionary, ClassFinder classFinder) {
        this.dictionary = dictionary;
        @SuppressWarnings("rawtypes")
        List<Class<Handler>> list = classFinder.findByInstance(Handler.class);
        for (Class<?> taskHandler : list) {
            List<Class<AbstractTask>> typeInterface = classFinder.getTypeInterface(taskHandler, AbstractTask.class);
            for (Class<AbstractTask> iClass : typeInterface) {
                dictionary.getTaskHandler().put(iClass, (Handler<?>) applicationContext.getBean(taskHandler));
            }
        }
        classFinder.findByInstance(StatisticsCollectorComponent.class).forEach((Class<StatisticsCollectorComponent> statisticsCollectorClass) -> {
            if (!classFinder.instanceOf(this.getClass(), statisticsCollectorClass)) {
                dictionary.getListStatisticsCollectorComponent().add(applicationContext.getBean(statisticsCollectorClass));
            }
        });

        classFinder.findByInstance(KeepAliveComponent.class).forEach((Class<KeepAliveComponent> keepAliveClass) -> {
            if (!classFinder.instanceOf(this.getClass(), keepAliveClass)) {
                dictionary.getListKeepAliveComponent().add(applicationContext.getBean(keepAliveClass));
            }
        });

        classFinder.findByInstance(RunnableComponent.class).forEach((Class<RunnableComponent> runnableComponentClass) -> {
            if (!classFinder.instanceOf(this.getClass(), runnableComponentClass)) {
                dictionary.getListRunnableComponents().add(applicationContext.getBean(runnableComponentClass));
            }
        });

        ListSort<CronTask> generatorListSort = new ListSort<>();
        classFinder.findByInstance(Generator.class).forEach((Class<Generator> generatorClass) -> {
            if (!classFinder.instanceOf(this.getClass(), generatorClass)) {
                Generator generator = applicationContext.getBean(generatorClass);
                generatorListSort.add(generator.getId(), new CronTask(
                        new Cron(generator.getCronTemplate()),
                        generator.getTaskTimeEnvelope()
                ));
            }
        });
        generatorListSort.getSorted().forEach(cronTask -> dictionary.getListCronTask().add(cronTask));
    }

    @Override
    public void run() {
        dictionary.getListRunnableComponents().forEach(RunnableInterface::run);
        rateLimitInit();
    }

    private void rateLimitInit() {
        RateLimitManager rateLimitManager = App.context.getBean(RateLimitManager.class);
        rateLimitManager.initLimit(
                ThreadPool.class,
                StatisticSecFlush.class.getSimpleName(),
                RateLimitName.POOL_SIZE,
                1
        );
        rateLimitManager.initLimit(
                ThreadPool.class,
                KeepAlive.class.getSimpleName(),
                RateLimitName.POOL_SIZE,
                1
        );
        rateLimitManager.initLimit(
                ThreadPool.class,
                StatisticCollectorFlush.class.getSimpleName(),
                RateLimitName.POOL_SIZE,
                1
        );
    }

    @Override
    public void shutdown() {
        dictionary.getListRunnableComponents().forEach(RunnableInterface::shutdown);
    }

    @Override
    synchronized public void reload() {
        shutdown();
        run();
    }
}
