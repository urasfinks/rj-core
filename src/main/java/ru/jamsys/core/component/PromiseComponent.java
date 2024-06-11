package ru.jamsys.core.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.BrokerManager;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.extension.ClassName;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseImpl;

@Component
@Lazy
public class PromiseComponent implements ClassName {

    Broker<Promise> broker;

    public PromiseComponent(BrokerManager brokerManager, ApplicationContext applicationContext) {
        this.broker = brokerManager.initAndGet(getClassName(applicationContext), Promise.class, promise
                -> promise.timeOut(getClassName("onPromiseTaskExpired")));
    }

    public Promise get(String index, long timeout) {
        PromiseImpl promise = new PromiseImpl(index, timeout);
        broker.add(promise, timeout);
        return promise;
    }

}
