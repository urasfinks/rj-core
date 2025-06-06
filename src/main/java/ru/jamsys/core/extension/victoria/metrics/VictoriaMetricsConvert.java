package ru.jamsys.core.extension.victoria.metrics;

import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.log.StatDataHeader;
import ru.jamsys.core.flat.util.UtilJson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class VictoriaMetricsConvert {

    public static Point getInfluxFormat(StatDataHeader statDataHeader) {
        Map<String, Object> header = statDataHeader.getHeader();
        Point point = Point
                .measurement(statDataHeader.getCls())
                .addFields(header)
                .time(statDataHeader.getTimeAdd(), WritePrecision.MS);
        if (statDataHeader.getNs() != null) {
            point.addTag("ns", statDataHeader.getNs());
        }
        return point;
    }

    @SuppressWarnings("unused")
    public static List<String> getVmFormat(StatDataHeader statDataHeader) {
        List<String> result = new ArrayList<>();
        Map<String, Object> header = statDataHeader.getHeader();
        for (String key : header.keySet()) {
            VictoriaMetricsLineProtocolBuilder victoriaMetricsLineProtocolBuilder =
                    new VictoriaMetricsLineProtocolBuilder()
                            .setMeasurement(CascadeKey.complex(statDataHeader.getCls(), key))
                            .setTimestampMillis(statDataHeader.getTimeAdd())
                    ;
            if (statDataHeader.getNs() != null) {
                victoriaMetricsLineProtocolBuilder.addLabel("ns", statDataHeader.getNs());
            }
            result.add(victoriaMetricsLineProtocolBuilder.build());

        }
        return result;
    }

    @SuppressWarnings("unused")
    public static List<String> getJsonFormat(StatDataHeader statDataHeader) {
        List<String> result = new ArrayList<>();
        Map<String, Object> header = statDataHeader.getHeader();
        for (String key : header.keySet()) {
            Map<String, Object> map = new LinkedHashMap<>();
            Map<String, Object> metric = new LinkedHashMap<>();
            metric.put("__name__", CascadeKey.complex(statDataHeader.getCls(), key));
            if(statDataHeader.getNs() != null){
                metric.put("ns", statDataHeader.getNs());
            }
            map.put("metric", metric);
            map.put("values", List.of(header.get(key)));
            map.put("timestamps", statDataHeader.getTimeAdd());
            result.add(UtilJson.toString(map, "{}"));
        }
        return result;
    }

}
