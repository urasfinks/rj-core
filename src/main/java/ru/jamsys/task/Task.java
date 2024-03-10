package ru.jamsys.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Task extends TagIndex {

    List<Trace> listTrace = new ArrayList<>();
    Map<String, Object> property = new HashMap<>();
    long timeMsExpired = 0;

}
