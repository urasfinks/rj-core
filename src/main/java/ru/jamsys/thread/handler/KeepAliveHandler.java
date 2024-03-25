package ru.jamsys.thread.handler;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.component.Dictionary;
import ru.jamsys.extension.KeepAliveComponent;
import ru.jamsys.thread.task.KeepAlive;
import ru.jamsys.util.Util;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
@Component
@Lazy
public class KeepAliveHandler implements Handler<KeepAlive> {

    final private Dictionary dictionary;

    public KeepAliveHandler(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    @Override
    public void run(KeepAlive task, AtomicBoolean isRun) throws Exception {
        Util.riskModifierCollection(
                isRun,
                dictionary.getListKeepAliveComponent(),
                new KeepAliveComponent[0],
                (KeepAliveComponent keepAliveComponent) -> keepAliveComponent.keepAlive(isRun)
        );
    }

    @Override
    public long getTimeoutMs() {
        return 1000;
    }
}
