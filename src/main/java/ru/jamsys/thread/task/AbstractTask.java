package ru.jamsys.thread.task;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.broker.BrokerCollectible;
import ru.jamsys.statistic.Expired;
import ru.jamsys.statistic.TagIndexImpl;
import ru.jamsys.thread.task.trace.Trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractTask extends TagIndexImpl implements Task, Expired, BrokerCollectible {

    @Getter
    @Setter
    private long keepAliveOnInactivityMs = 30_000; // Время жизни если нет активности

    @Getter
    @Setter
    private volatile long lastActivity = System.currentTimeMillis();

    List<Trace> listTrace = new ArrayList<>();
    Map<String, Object> property = new HashMap<>();

    final int maxTimeExecute;

    public AbstractTask(int maxTimeExecute) {
        this.maxTimeExecute = maxTimeExecute;
    }

    @Override
    public int getMaxTimeExecute() {
        return maxTimeExecute;
    }

}
