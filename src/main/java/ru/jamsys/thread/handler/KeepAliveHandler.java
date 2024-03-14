package ru.jamsys.thread.handler;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.KeepAlive;
import ru.jamsys.component.Dictionary;
import ru.jamsys.thread.task.KeepAliveTask;
import ru.jamsys.util.Util;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
@Component
@Lazy
public class KeepAliveHandler implements Handler<KeepAliveTask> {

    final private Dictionary dictionary;

    public KeepAliveHandler(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    @Override
    public void run(KeepAliveTask task, AtomicBoolean isRun) throws Exception {
        Util.riskModifierCollection(
                isRun,
                dictionary.getListKeepAlive(),
                new KeepAlive[0],
                (KeepAlive keepAlive) -> keepAlive.keepAlive(isRun)
        );
    }

    @Override
    public long getTimeoutMs() {
        return 1000;
    }
}
