package ru.jamsys.core.extension.trace;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import ru.jamsys.core.flat.util.Util;

@JsonPropertyOrder({"start", "stop", "nano"})
public class TraceTimer {

    long start;

    long stop;

    long nano;

    public TraceTimer(long start, long stop, long nano) {
        this.start = start;
        this.stop = stop;
        this.nano = nano;
    }

    public String getStart() {
        return Util.msToDataFormat(start);
    }

    public String getStop() {
        return Util.msToDataFormat(stop);
    }

}
