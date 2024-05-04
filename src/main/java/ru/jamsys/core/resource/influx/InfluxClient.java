package ru.jamsys.core.resource.influx;

import com.influxdb.client.write.Point;

import java.util.List;

public interface InfluxClient {
    void writePoints(List<Point> list);
}
