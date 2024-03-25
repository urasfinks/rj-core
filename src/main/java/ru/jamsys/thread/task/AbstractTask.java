package ru.jamsys.thread.task;

import ru.jamsys.broker.BrokerCollectible;
import ru.jamsys.statistic.TagIndexImpl;
import ru.jamsys.thread.task.trace.Trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractTask extends TagIndexImpl implements Task, BrokerCollectible {

    List<Trace> listTrace = new ArrayList<>();
    Map<String, Object> property = new HashMap<>();
    long timeMsExpired = 0;

}
