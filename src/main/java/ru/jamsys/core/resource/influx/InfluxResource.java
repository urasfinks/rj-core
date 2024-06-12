package ru.jamsys.core.resource.influx;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.PropertyComponent;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.extension.property.Subscriber;
import ru.jamsys.core.extension.property.PropertySubscriberNotify;
import ru.jamsys.core.resource.NamespaceResourceConstructor;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.util.List;
import java.util.Set;

@Component
@Scope("prototype")
public class InfluxResource
        extends ExpirationMsMutableImpl
        implements Resource<NamespaceResourceConstructor, List<Point>, Void>, PropertySubscriberNotify {

    private InfluxDBClient client = null;

    private WriteApiBlocking writer;

    private Subscriber subscriber;

    private final InfluxProperty property = new InfluxProperty();

    @Override
    public void constructor(NamespaceResourceConstructor constructor) {
        PropertyComponent propertyComponent = App.context.getBean(PropertyComponent.class);
        subscriber = propertyComponent.getSubscriber(this, property, constructor.ns);
    }

    @Override
    public void onPropertyUpdate(Set<String> updatedProp) {
        if (property.getHost() == null || property.getAlias() == null) {
            return;
        }
        if (client != null) {
            close();
        }
        SecurityComponent securityComponent = App.context.getBean(SecurityComponent.class);
        client = InfluxDBClientFactory.create(property.getHost(), securityComponent.get(property.getAlias()));
        writer = client.getWriteApiBlocking();
    }

    @Override
    public Void execute(List<Point> arguments) {
        if (writer != null && !arguments.isEmpty()) {
            writer.writePoints(property.getBucket(), property.getOrg(), arguments);
        }
        return null;
    }

    @Override
    public void close() {
        subscriber.unsubscribe();
        try {
            client.close();
        } catch (Exception e) {
            App.error(e);
        }
    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 0;
    }

}
