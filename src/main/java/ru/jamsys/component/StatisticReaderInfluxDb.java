package ru.jamsys.component;


import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.AbstractCoreComponent;
import ru.jamsys.scheduler.SchedulerType;
import ru.jamsys.statistic.AggregatorDataStatistic;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.StatisticEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Lazy
public class StatisticReaderInfluxDb extends AbstractCoreComponent {

    private final Scheduler scheduler;
    private final Broker broker;

    public StatisticReaderInfluxDb(Scheduler scheduler, Broker broker) {
        this.scheduler = scheduler;
        this.broker = broker;
    }

    private InfluxDBClient client;

    public void run() {
        scheduler.get(SchedulerType.SCHEDULER_STATISTIC_READ).add(this::flushStatistic);
        String token = "LmbVFdM8Abe6T6atTD6Ai3LJOKrEVrKB61mrFqJzqx5HzANJ13HItZrbWuhDdJXsdLL9mJLn7UB6MtAbLG4AxQ==";
        client = InfluxDBClientFactory.create("http://localhost:8086", token.toCharArray());
    }


    @Override
    public void flushStatistic() {
        while (true) {
            @SuppressWarnings("unchecked")
            AggregatorDataStatistic<Class<?>, Statistic> first = broker.pollFirst(AggregatorDataStatistic.class);
            if (first != null) {
                Map<Class<?>, Statistic> map = first.getMap();
                List<Point> listPoints = new ArrayList<>();
                for (Class<?> cls : map.keySet()) {
                    map.get(cls).getStatisticEntity().forEach((StatisticEntity statisticEntity) -> listPoints.add(
                            Point.measurement(cls.getSimpleName())
                                    .addTag("host", "host2")
                                    .addTags(statisticEntity.getTags())
                                    .addFields(statisticEntity.getFields())
                                    .time(first.getTimestamp(), WritePrecision.MS)
                    ));
                }
                if (client != null && !listPoints.isEmpty()) {
                    WriteApiBlocking writeApi = client.getWriteApiBlocking();
                    String bucket = "5gm";
                    String org = "ru";
                    writeApi.writePoints(bucket, org, listPoints);
                }
                //Util.logConsole(Thread.currentThread(), UtilJson.toStringPretty(res, null));
            } else {
                break;
            }
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        scheduler.get(SchedulerType.SCHEDULER_STATISTIC_READ).remove(this::flushStatistic);
    }
}
