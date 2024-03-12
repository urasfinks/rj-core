package ru.jamsys.task.handler.trash;

public class ReadTaskHandlerStatistic {

//    public void run(Task _task, AtomicBoolean isRun) throws Exception {
//        Broker broker = App.context.getBean(Broker.class);
//        Queue<TaskHandlerStatistic> queue = broker.get(TaskHandlerStatistic.class);
//        List<TaskHandlerStatistic> list = queue.getCloneQueue(isRun);
//
//        Map<String, HStat> task = new HashMap<>();
//        Map<String, HStat> taskHandler = new HashMap<>();
//
//
//        list.forEach((TaskHandlerStatistic stat) -> {
//            long timeExecuteMs = stat.getTimeExecuteMs();
//            boolean isFirstPrepare = stat.isFirstPrepare();
//            if (stat.getTask() == null) {
//                //addIdExist(stat.getTaskHandler(), taskHandler, timeExecuteMs, isFirstPrepare);
//            } else {
//                addIdExist(stat.getTask(), task, timeExecuteMs, isFirstPrepare);
//            }
//            if (stat.isTimeout()) {
//                Util.logConsole(
//                        "Alarm!! TaskHandler: "
//                                //+ stat.getTaskHandler().getIndex()
//                                + " timeOut: execute=" + timeExecuteMs
//                                //+ " timeOut=" + stat.getTaskHandler().getTimeoutMs()
//                );
//                //TODO: проверить timeOut и если что прибить поток
//            }
//            if (stat.isFinished()) {
//                queue.remove(stat);
//            }
//        });
//    }
//
//    public static class HStat {
//        public AvgMetric avgMetric = new AvgMetric();
//        public AtomicLong count = new AtomicLong(0);
//    }
//
//    private void addIdExist(TagIndex tagIndex, Map<String, HStat> map, Long executeTimeMs, boolean first) {
//        String index = tagIndex.getIndex();
//        if (!map.containsKey(index)) {
//            map.put(index, new HStat());
//        }
//        HStat hStat = map.get(index);
//        hStat.avgMetric.add(executeTimeMs);
//        if (first) {
//            hStat.count.incrementAndGet();
//        }
//    }
//
//    public long getTimeoutMs() {
//        return 1000;
//    }
}
