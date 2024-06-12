package ru.jamsys.core.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.BrokerManager;
import ru.jamsys.core.component.manager.FileByteWriterManager;
import ru.jamsys.core.component.manager.item.FileByteWriter;
import ru.jamsys.core.extension.ClassNameImpl;
import ru.jamsys.core.extension.LifeCycleComponent;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.statistic.StatisticSec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@Lazy
public class Core implements LifeCycleInterface {

    private final ClassFinderComponent classFinderComponent;

    private final ApplicationContext applicationContext;

    private final FileByteWriterManager fileByteWriterManager;

    private final BrokerManager brokerManager;

    public Core(
            ApplicationContext applicationContext,
            ClassFinderComponent classFinderComponent,
            FileByteWriterManager fileByteWriterManager,
            BrokerManager brokerManager
    ) {
        this.applicationContext = applicationContext;
        this.classFinderComponent = classFinderComponent;
        this.fileByteWriterManager = fileByteWriterManager;
        this.brokerManager = brokerManager;
    }

    @Override
    public void run() {
        FileByteWriter fileByteWriter = fileByteWriterManager.get("statistic");
        brokerManager.initAndGet(
                ClassNameImpl.getClassNameStatic(StatisticSec.class, null, applicationContext),
                StatisticSec.class,
                fileByteWriter::append
        );
        List<LifeCycleComponent> sortedList = new ArrayList<>();
        classFinderComponent.findByInstance(LifeCycleComponent.class).forEach((Class<LifeCycleComponent> runnableComponentClass) -> {
            if (!ClassFinderComponent.instanceOf(this.getClass(), runnableComponentClass)) {
                sortedList.add(applicationContext.getBean(runnableComponentClass));
            }
        });
        sortedList.sort(Comparator.comparingInt(LifeCycleComponent::getInitializationIndex));
        sortedList.forEach(LifeCycleInterface::run);
    }

    @Override
    public void shutdown() {
        classFinderComponent.findByInstance(LifeCycleComponent.class).forEach((Class<LifeCycleComponent> runnableComponentClass) -> {
            if (!ClassFinderComponent.instanceOf(this.getClass(), runnableComponentClass)) {
                LifeCycleComponent bean = applicationContext.getBean(runnableComponentClass);
                Util.logConsole(bean.getClass().getName());
                bean.shutdown();
            }
        });
    }

}
