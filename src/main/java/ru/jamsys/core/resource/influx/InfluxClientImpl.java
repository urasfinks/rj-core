package ru.jamsys.core.resource.influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;
import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.Security;
import ru.jamsys.core.component.resource.PropertiesManager;
import ru.jamsys.core.extension.RunnableComponent;

import java.util.List;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
@Lazy
public class InfluxClientImpl implements InfluxClient, RunnableComponent {

    private InfluxDBClient client = null;

    private WriteApiBlocking writeApi;

    @Setter
    private String bucket;

    @Setter
    private String org;

    @Setter
    private String host;

    @Setter
    private String alias;

    private final Security security;

    public InfluxClientImpl(Security security, PropertiesManager propertiesManager) {
        this.security = security;
        this.host = propertiesManager.getProperties("rj.influx.host", String.class);
        this.bucket = propertiesManager.getProperties("rj.influx.bucket", String.class);
        this.org = propertiesManager.getProperties("rj.influx.org", String.class);
        this.alias = propertiesManager.getProperties("rj.influx.security.alias", String.class);
    }

    public static InfluxClient getComponent() {
        return App.context.getBean(InfluxClientImpl.class);
    }

    public static InfluxClient getNewInstance() {
        return new InfluxClientImpl(App.context.getBean(Security.class), App.context.getBean(PropertiesManager.class));
    }

    @Override
    public void writePoints(List<Point> list) {
        if (writeApi != null && !list.isEmpty()) {
            writeApi.writePoints(bucket, org, list);
        }
    }

    @Override
    public void run() {
        try {
            if (client == null) {
                client = InfluxDBClientFactory.create(host, security.get(alias));
                writeApi = client.getWriteApiBlocking();
            }
        } catch (Exception e) {
            App.context.getBean(ExceptionHandler.class).handler(e);
        }
    }

    @Override
    public void shutdown() {
        client.close();
    }

}