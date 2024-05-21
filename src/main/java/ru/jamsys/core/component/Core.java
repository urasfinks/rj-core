package ru.jamsys.core.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.RateLimitManager;
import ru.jamsys.core.extension.LifeCycleComponent;
import ru.jamsys.core.extension.LifeCycleInterface;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
@Lazy
public class Core implements LifeCycleInterface {

    private final ClassFinder classFinder;
    private final ApplicationContext applicationContext;

    public Core(ApplicationContext applicationContext, ClassFinder classFinder) {
        this.applicationContext = applicationContext;
        this.classFinder = classFinder;
    }

    @Override
    public void run() {
        classFinder.findByInstance(LifeCycleComponent.class).forEach((Class<LifeCycleComponent> runnableComponentClass) -> {
            if (!ClassFinder.instanceOf(this.getClass(), runnableComponentClass)) {
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
        classFinder.findByInstance(LifeCycleComponent.class).forEach((Class<LifeCycleComponent> runnableComponentClass) -> {
            if (!ClassFinder.instanceOf(this.getClass(), runnableComponentClass)) {
                applicationContext.getBean(runnableComponentClass).shutdown();
            }
        });
    }

}
