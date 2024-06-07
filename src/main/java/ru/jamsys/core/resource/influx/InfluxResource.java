package ru.jamsys.core.resource.influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.PropComponent;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.resource.NamespaceResourceConstructor;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.util.List;

public class InfluxResource
        extends ExpirationMsMutableImpl
        implements Resource<NamespaceResourceConstructor, List<Point>, Void> {

    private InfluxDBClient client = null;

    private WriteApiBlocking writer;

    String org;

    String bucket;

    String host = null;

    String alias = null;

    @Override
    public void constructor(NamespaceResourceConstructor constructor) {
        PropComponent propComponent = App.context.getBean(PropComponent.class);

        propComponent.getProp(constructor.ns, "influx.host", String.class, s -> {
            this.host = s;
            reInitClient();
        });
        propComponent.getProp(constructor.ns, "influx.bucket", String.class, s -> bucket = s);
        propComponent.getProp(constructor.ns, "influx.org", String.class, s -> org = s);
        propComponent.getProp(constructor.ns, "influx.security.alias", String.class, s -> {
            alias = s;
            reInitClient();
        });
    }

    private void reInitClient() {
        if (host == null || alias == null) {
            return;
        }
        if (client != null) {
            close();
        }
        SecurityComponent securityComponent = App.context.getBean(SecurityComponent.class);
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
        try {
            client.close();
        } catch (Exception e) {
            App.context.getBean(ExceptionHandler.class).handler(e);
        }
    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 0;
    }

}
