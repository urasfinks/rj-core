package ru.jamsys.core.resource.influx;

import com.influxdb.LogLevel;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;
import com.influxdb.internal.AbstractRestClient;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.expiration.AbstractExpirationResource;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyListener;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public class InfluxResource extends AbstractExpirationResource implements PropertyListener {

    //influx delete --bucket "5gm" -o "ru" --start '1970-01-01T00:00:00Z' --stop '2025-12-31T23:59:00Z' -token ''

    private InfluxDBClient client = null;

    private WriteApiBlocking writer;

    private final PropertyDispatcher<String> propertyDispatcher;

    private final InfluxRepositoryProperty influxRepositoryProperty = new InfluxRepositoryProperty();

    public InfluxResource(String ns) {
        propertyDispatcher = new PropertyDispatcher<>(
                this,
                influxRepositoryProperty,
                getCascadeKey(ns)
        );
    }

    private void down() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                App.error(e);
            }
            client = null;
        }
    }

    private void up() {
        if (client == null) {
            try {
                SecurityComponent securityComponent = App.get(SecurityComponent.class);
                client = InfluxDBClientFactory.create(influxRepositoryProperty.getHost(), securityComponent.get(influxRepositoryProperty.getAlias()));
                client.setLogLevel(LogLevel.NONE);
                // Как вы поняли - верхняя строчка не работает
                Logger.getLogger(AbstractRestClient.class.getName()).setLevel(Level.OFF);
                if (!client.ping()) {
                    throw new RuntimeException("Ping request wasn't successful");
                }
                writer = client.getWriteApiBlocking();
            } catch (Exception e) {
                App.error(e);
                throw new ForwardException(e);
            }
        }
    }

    public Void execute(List<Point> arguments) {
        if (writer != null && !arguments.isEmpty()) {
            writer.writePoints(influxRepositoryProperty.getBucket(), influxRepositoryProperty.getOrg(), arguments);
        }
        return null;
    }

    @Override
    public boolean isValid() {
        if (client == null) {
            return false;
        }
        return client.ping();
    }

    @Override
    public void runOperation() {
        propertyDispatcher.run();
        up();
    }

    @Override
    public void shutdownOperation() {
        propertyDispatcher.shutdown();
        down();
    }

    @Override
    public void onPropertyUpdate(String key, String oldValue, String newValue) {
        down();
        if (influxRepositoryProperty.getHost() == null || influxRepositoryProperty.getAlias() == null) {
            return;
        }
        up();
    }

    @Override
    public boolean checkFatalException(Throwable th) {
        if (th != null) {
            String msg = th.getMessage();
            if (msg == null) {
                App.error(th);
                return false;
            }
            // Не конкурентная проверка
            return msg.contains("Failed to connect");
        }
        return false;
    }

}
