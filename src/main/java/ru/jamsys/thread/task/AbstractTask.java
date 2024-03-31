package ru.jamsys.thread.task;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.broker.BrokerCollectible;
import ru.jamsys.statistic.Expired;
import ru.jamsys.statistic.TagIndex;
import ru.jamsys.thread.task.trace.Trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractTask implements TagIndex, Expired, BrokerCollectible {

    //--Expired
    @Getter
    @Setter
    private long keepAliveOnInactivityMs = 30_000; // Время жизни если нет активности

    @Getter
    @Setter
    private volatile long lastActivity = System.currentTimeMillis();

    //--TagIndex
    @Getter
    private final Map<String, String> tag = new HashMap<>();

    @Getter
    @Setter
    private String indexCache = null;

    //--Instance
    List<Trace> listTrace = new ArrayList<>();
    Map<String, Object> property = new HashMap<>();

    final int maxTimeExecute;

    public AbstractTask(int maxTimeExecute) {
        this.maxTimeExecute = maxTimeExecute;
    }

    public int getMaxTimeExecute() {
        return maxTimeExecute;
    }

}
