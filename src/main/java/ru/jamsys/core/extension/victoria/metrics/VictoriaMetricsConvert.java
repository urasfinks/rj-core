package ru.jamsys.core.extension.victoria.metrics;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.statistic.StatisticDataHeader;
import ru.jamsys.core.flat.util.UtilJson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class VictoriaMetricsConvert {

    public static Point getInfluxFormat(StatisticDataHeader statisticDataHeader) {
        Map<String, Object> header = statisticDataHeader.getHeader();
        Point point = Point
                .measurement(statisticDataHeader.getCls())
                .addFields(header)
                .time(statisticDataHeader.getTimeAdd(), WritePrecision.MS);
        if (statisticDataHeader.getNs() != null) {
            point.addTag("ns", statisticDataHeader.getNs());
        }
        return point;
    }

    @SuppressWarnings("unused")
    public static List<String> getVmFormat(StatisticDataHeader statisticDataHeader) {
        List<String> result = new ArrayList<>();
        Map<String, Object> header = statisticDataHeader.getHeader();
        for (String key : header.keySet()) {
            VictoriaMetricsLineProtocolBuilder victoriaMetricsLineProtocolBuilder =
                    new VictoriaMetricsLineProtocolBuilder()
                            .setMeasurement(CascadeKey.complex(statisticDataHeader.getCls(), key))
                            .setTimestampMillis(statisticDataHeader.getTimeAdd())
                    ;
            if (statisticDataHeader.getNs() != null) {
                victoriaMetricsLineProtocolBuilder.addLabel("ns", statisticDataHeader.getNs());
            }
            result.add(victoriaMetricsLineProtocolBuilder.build());

        }
        return result;
    }

    @SuppressWarnings("unused")
    public static List<String> getJsonFormat(StatisticDataHeader statisticDataHeader) {
        List<String> result = new ArrayList<>();
        Map<String, Object> header = statisticDataHeader.getHeader();
        for (String key : header.keySet()) {
            Map<String, Object> map = new LinkedHashMap<>();
            Map<String, Object> metric = new LinkedHashMap<>();
            metric.put("__name__", CascadeKey.complex(statisticDataHeader.getCls(), key));
            if(statisticDataHeader.getNs() != null){
                metric.put("ns", statisticDataHeader.getNs());
            }
            map.put("metric", metric);
            map.put("values", List.of(header.get(key)));
            map.put("timestamps", statisticDataHeader.getTimeAdd());
            result.add(UtilJson.toString(map, "{}"));
        }
        return result;
    }

}
