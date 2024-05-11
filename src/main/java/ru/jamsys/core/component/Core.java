package ru.jamsys.core.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.api.ClassFinder;
import ru.jamsys.core.component.manager.RateLimitManager;
import ru.jamsys.core.extension.RunnableComponent;
import ru.jamsys.core.extension.RunnableInterface;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
@Lazy
public class Core implements RunnableInterface {

    private final ClassFinder classFinder;
    private final ApplicationContext applicationContext;

    public Core(ApplicationContext applicationContext, ClassFinder classFinder) {
        this.applicationContext = applicationContext;
        this.classFinder = classFinder;
    }

    @Override
    public void run() {
        classFinder.findByInstance(RunnableComponent.class).forEach((Class<RunnableComponent> runnableComponentClass) -> {
            if (!classFinder.instanceOf(this.getClass(), runnableComponentClass)) {
                applicationContext.getBean(runnableComponentClass).run();
            }
        });
        rateLimitInit();
    }

    private void rateLimitInit() {
        RateLimitManager rateLimitManager = App.context.getBean(RateLimitManager.class);
//        rateLimitManager.initLimit(
//                ThreadPool.class,
//                "StatisticSecFlush",
//                RateLimitType.POOL_SIZE,
//                1
//        );
    }

    @Override
    public void shutdown() {
        classFinder.findByInstance(RunnableComponent.class).forEach((Class<RunnableComponent> runnableComponentClass) -> {
            if (!classFinder.instanceOf(this.getClass(), runnableComponentClass)) {
                applicationContext.getBean(runnableComponentClass).shutdown();
            }
        });
    }

}
