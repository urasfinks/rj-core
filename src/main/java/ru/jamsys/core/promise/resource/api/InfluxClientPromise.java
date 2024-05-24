package ru.jamsys.core.promise.resource.api;

import com.influxdb.client.write.Point;
import lombok.Getter;
import ru.jamsys.core.resource.influx.InfluxClient;
import ru.jamsys.core.resource.influx.InfluxClientImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class InfluxClientPromise extends AbstractPromiseApi<InfluxClientPromise> {

    private final InfluxClient influxClient = InfluxClientImpl.getComponent();

    @Getter
    private final List<Point> list = new ArrayList<>();

    @Override
    public Consumer<AtomicBoolean> getExecutor() {
        return this::execute;
    }

    private void execute(AtomicBoolean isThreadRun) {
        influxClient.writePoints(list);
    }

}
