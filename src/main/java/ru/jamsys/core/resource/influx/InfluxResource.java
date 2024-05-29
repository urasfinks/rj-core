package ru.jamsys.core.resource.influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;
import ru.jamsys.core.App;
import ru.jamsys.core.component.PropertiesComponent;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.util.List;

public class InfluxResource extends ExpirationMsMutableImpl implements Resource<InfluxResourceConstructor, List<Point>, Void> {

    private InfluxDBClient client = null;

    private WriteApiBlocking writer;

    String org;

    String bucket;

    @Override
    public void constructor(InfluxResourceConstructor constructor) {
        PropertiesComponent propertiesComponent = App.context.getBean(PropertiesComponent.class);
        SecurityComponent securityComponent = App.context.getBean(SecurityComponent.class);

        String host = propertiesComponent.getProperties(constructor.namespaceProperties, "influx.host", String.class);
        bucket = propertiesComponent.getProperties(constructor.namespaceProperties, "influx.bucket", String.class);
        org = propertiesComponent.getProperties(constructor.namespaceProperties, "influx.org", String.class);
        String alias = propertiesComponent.getProperties(constructor.namespaceProperties, "influx.security.alias", String.class);

        client = InfluxDBClientFactory.create(host, securityComponent.get(alias));
        writer = client.getWriteApiBlocking();
    }

    @Override
    public Void execute(List<Point> arguments) throws Throwable {
        if (writer != null && !arguments.isEmpty()) {
            writer.writePoints(bucket, org, arguments);
        }
        return null;
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 0;
    }

}
