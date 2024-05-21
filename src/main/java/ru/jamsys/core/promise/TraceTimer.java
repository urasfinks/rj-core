package ru.jamsys.core.promise;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import ru.jamsys.core.flat.util.Util;

@JsonPropertyOrder({"start", "stop", "nano"})
@Getter
public class TraceTimer {
    String start;
    String stop;
    long nano;

    public TraceTimer(long start, long stop, long nano) {
        this.start = Util.msToDataFormat(start);
        this.stop = Util.msToDataFormat(stop);
        this.nano = nano;
    }

    public static TraceTimer getInstanceZero() {
        long time = System.currentTimeMillis();
        return  new TraceTimer(time, time, 0);
    }

}
