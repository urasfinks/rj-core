package ru.jamsys.task.handler;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.springframework.stereotype.Component;
import ru.jamsys.App;
import ru.jamsys.broker.Queue;
import ru.jamsys.component.Broker;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.StatisticSec;
import ru.jamsys.task.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class LoadStatisticToInfluxDb extends AbstractHandler {

    //influx delete --bucket "5gm" -o "ru" --start '1970-01-01T00:00:00Z' --stop '2025-12-31T23:59:00Z' -token 'LmbVFdM8Abe6T6atTD6Ai3LJOKrEVrKB61mrFqJzqx5HzANJ13HItZrbWuhDdJXsdLL9mJLn7UB6MtAbLG4AxQ=='
    String token = "LmbVFdM8Abe6T6atTD6Ai3LJOKrEVrKB61mrFqJzqx5HzANJ13HItZrbWuhDdJXsdLL9mJLn7UB6MtAbLG4AxQ==";
    private final InfluxDBClient client = InfluxDBClientFactory.create("http://localhost:8086", token.toCharArray());

    @Override
    public void run(Task task, AtomicBoolean isRun) throws Exception {
        Queue<StatisticSec> queue = App.context.getBean(Broker.class).get(StatisticSec.class);
        List<Point> listPoints = new ArrayList<>();
        while (isRun.get()) {
            StatisticSec statisticSec = queue.pollFirst();
            if (statisticSec != null) {
                List<Statistic> list = statisticSec.getList();
                for (Statistic statistic : list) {
                    HashMap<String, String> newTags = new HashMap<>(statistic.getTags());
                    String measurement = newTags.remove("measurement");
                    listPoints.add(
                            Point.measurement(measurement)
                                    .addTags(newTags)
                                    .addFields(statistic.getFields())
                                    .time(statisticSec.getTimestamp(), WritePrecision.MS)
                    );
                }
            } else {
                break;
            }
        }
        if (isRun.get()) {
            if (!listPoints.isEmpty()) {
                WriteApiBlocking writeApi = client.getWriteApiBlocking();
                String bucket = "5gm";
                String org = "ru";
                writeApi.writePoints(bucket, org, listPoints);
            }
        }
    }

    @Override
    public long getTimeout() {
        return 5000;
    }
}
