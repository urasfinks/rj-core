package ru.jamsys.task.handler;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.Setter;
import org.springframework.stereotype.Component;
import ru.jamsys.App;
import ru.jamsys.ApplicationInit;
import ru.jamsys.broker.Queue;
import ru.jamsys.component.Broker;
import ru.jamsys.component.PropertiesManager;
import ru.jamsys.component.Scheduler;
import ru.jamsys.component.Security;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.statistic.StatisticSec;
import ru.jamsys.task.AbstractTaskHandler;
import ru.jamsys.task.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class LoadStatisticToInfluxDb extends AbstractTaskHandler implements ApplicationInit {

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

    public LoadStatisticToInfluxDb(Security security, PropertiesManager propertiesManager) {
        this.security = security;
        this.host = propertiesManager.getProperties("rj.task.handler.LoadStatisticToInfluxDb.host", String.class);
        this.bucket = propertiesManager.getProperties("rj.task.handler.LoadStatisticToInfluxDb.bucket", String.class);
        this.org = propertiesManager.getProperties("rj.task.handler.LoadStatisticToInfluxDb.org", String.class);
        this.alias = propertiesManager.getProperties("rj.task.handler.LoadStatisticToInfluxDb.security.alias", String.class);
    }

    @Override
    public void run(Task task, AtomicBoolean isRun) throws Exception {
        Queue<StatisticSec> queue = App.context.getBean(Broker.class).get(StatisticSec.class);
        List<Point> listPoints = new ArrayList<>();
        while (queue.getSize() > 0 && isRun.get()) {
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
                                    .time(statisticSec.getTimeMs(), WritePrecision.MS)
                    );
                }
            }
        }
        if (isRun.get()) {
            if (!listPoints.isEmpty()) {
                if (client == null) {
                    client = InfluxDBClientFactory.create(host, security.get(alias));
                }
                WriteApiBlocking writeApi = client.getWriteApiBlocking();
                writeApi.writePoints(bucket, org, listPoints);
            }
        }
    }

    @Override
    public long getTimeoutMs() {
        return 5000;
    }

    public void applicationInit() {
        App.context
                .getBean(Scheduler.class)
                .getT5()
                .getListAbstractTaskHandler()
                .add(App.context.getBean(LoadStatisticToInfluxDb.class));
    }
}
