package ru.jamsys.thread.handler;


import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.broker.BrokerQueue;
import ru.jamsys.component.Broker;
import ru.jamsys.component.PropertiesManager;
import ru.jamsys.component.Security;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.StatisticSec;
import ru.jamsys.thread.ThreadEnvelope;
import ru.jamsys.thread.task.StatisticSecFlush;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SuppressWarnings("unused")
@Component
@Lazy
public class StatisticSecFlushToInfluxHandler implements Handler<StatisticSecFlush> {

    //influx delete --bucket "5gm" -o "ru" --start '1970-01-01T00:00:00Z' --stop '2025-12-31T23:59:00Z' -token 'LmbVFdM8Abe6T6atTD6Ai3LJOKrEVrKB61mrFqJzqx5HzANJ13HItZrbWuhDdJXsdLL9mJLn7UB6MtAbLG4AxQ=='

    private InfluxDBClient client;

    @Setter
    private String bucket;

    @Setter
    private String org;

    @Setter
    private String host;

    @Setter
    private String alias;

    private final Security security;

    private final Broker<StatisticSec> broker;

    public StatisticSecFlushToInfluxHandler(Security security, PropertiesManager propertiesManager, Broker<StatisticSec> broker) {
        this.security = security;
        this.host = propertiesManager.getProperties("rj.task.handler.ReadStatisticSecToInfluxTaskHandler.host", String.class);
        this.bucket = propertiesManager.getProperties("rj.task.handler.ReadStatisticSecToInfluxTaskHandler.bucket", String.class);
        this.org = propertiesManager.getProperties("rj.task.handler.ReadStatisticSecToInfluxTaskHandler.org", String.class);
        this.alias = propertiesManager.getProperties("rj.task.handler.ReadStatisticSecToInfluxTaskHandler.security.alias", String.class);
        this.broker = broker;
    }

    @Override
    public void run(StatisticSecFlush task, ThreadEnvelope threadEnvelope) throws Exception {
        BrokerQueue<StatisticSec> queue = broker.get(StatisticSec.class.getSimpleName());
        List<Point> listPoints = new ArrayList<>();
        while (!queue.isEmpty() && threadEnvelope.getIsWhile().get()) {
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
                                    .time(statisticSec.getLastActivity(), WritePrecision.MS)
                    );
                }
            }
        }
        if (threadEnvelope.getIsWhile().get()) {
            if (!listPoints.isEmpty()) {
                if (client == null) {
                    client = InfluxDBClientFactory.create(host, security.get(alias));
                }
                WriteApiBlocking writeApi = client.getWriteApiBlocking();
                writeApi.writePoints(bucket, org, listPoints);
            }
        }
    }

}
