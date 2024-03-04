package ru.jamsys.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Task extends TagIndex {
    AbstractTaskHandler taskHandler;
    List<Trace> listTrace = new ArrayList<>();
    Map<String, Object> property = new HashMap<>();
}
