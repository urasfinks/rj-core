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

    private final ClassFinderComponent classFinderComponent;
    private final ApplicationContext applicationContext;

    public Core(ApplicationContext applicationContext, ClassFinderComponent classFinderComponent) {
        this.applicationContext = applicationContext;
        this.classFinderComponent = classFinderComponent;
    }

    @Override
    public void run() {
        classFinderComponent.findByInstance(LifeCycleComponent.class).forEach((Class<LifeCycleComponent> runnableComponentClass) -> {
            if (!ClassFinderComponent.instanceOf(this.getClass(), runnableComponentClass)) {
                applicationContext.getBean(runnableComponentClass).run();
            }
        });
        rateLimitInit();
    }

    private void rateLimitInit() {
        RateLimitManager rateLimitManager = App.context.getBean(RateLimitManager.class);
    }

    @Override
    public void shutdown() {
        classFinderComponent.findByInstance(LifeCycleComponent.class).forEach((Class<LifeCycleComponent> runnableComponentClass) -> {
            if (!ClassFinderComponent.instanceOf(this.getClass(), runnableComponentClass)) {
                applicationContext.getBean(runnableComponentClass).shutdown();
            }
        });
    }

}
