package ru.jamsys.core.resource.influx;

import com.influxdb.LogLevel;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;
import com.influxdb.internal.AbstractRestClient;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.PropertySubscriberNotify;
import ru.jamsys.core.extension.property.Subscriber;
import ru.jamsys.core.resource.NamespaceResourceConstructor;
import ru.jamsys.core.resource.ResourceCheckException;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@Scope("prototype")
public class InfluxResource
        extends ExpirationMsMutableImpl
        implements
        Resource<NamespaceResourceConstructor, List<Point>, Void>,
        PropertySubscriberNotify,
        ResourceCheckException {

    //influx delete --bucket "5gm" -o "ru" --start '1970-01-01T00:00:00Z' --stop '2025-12-31T23:59:00Z' -token ''

    private InfluxDBClient client = null;

    private WriteApiBlocking writer;

    private Subscriber subscriber;

    private final InfluxProperty property = new InfluxProperty();

    @Override
    public void constructor(NamespaceResourceConstructor constructor) {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        subscriber = serviceProperty.getSubscriber(this, property, constructor.ns);
    }

    @Override
    public void onPropertyUpdate(Set<String> updatedProp) {
        if (property.getHost() == null || property.getAlias() == null) {
            return;
        }
        if (client != null) {
            close();
        }
        SecurityComponent securityComponent = App.get(SecurityComponent.class);
        client = InfluxDBClientFactory.create(property.getHost(), securityComponent.get(property.getAlias()));
        client.setLogLevel(LogLevel.NONE);
        // Как вы поняли) Верхняя строчка не работает
        Logger.getLogger(AbstractRestClient.class.getName()).setLevel(Level.OFF);
        if (!client.ping()) {
            throw new RuntimeException("Ping request wasn't successful");
        }
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
        if (subscriber != null) {
            subscriber.unsubscribe();
        }
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

    @Override
    public Function<Throwable, Boolean> getFatalException() {
        return throwable -> {
            if (throwable != null) {
                String msg = throwable.getMessage();
                // Не конкурентная проверка
                return msg.contains("Failed to connect");
            }
            return false;
        };
    }

}
