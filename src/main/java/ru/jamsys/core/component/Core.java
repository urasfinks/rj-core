package ru.jamsys.core.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.BrokerManager;
import ru.jamsys.core.extension.ClassNameImpl;
import ru.jamsys.core.extension.LifeCycleComponent;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.statistic.StatisticSec;

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
        applicationContext.getBean(BrokerManager.class).initAndGet(
                ClassNameImpl.getClassNameStatic(StatisticSec.class, null, applicationContext),
                StatisticSec.class,
                null
        );
        classFinderComponent.findByInstance(LifeCycleComponent.class).forEach((Class<LifeCycleComponent> runnableComponentClass) -> {
            if (!ClassFinderComponent.instanceOf(this.getClass(), runnableComponentClass)) {
                applicationContext.getBean(runnableComponentClass).run();
            }
        });
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
