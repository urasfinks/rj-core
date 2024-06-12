package ru.jamsys.core.statistic.timer;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.statistic.timer.ms.TimerMsEnvelope;
import ru.jamsys.core.statistic.timer.nano.TimerNanoEnvelope;

@Getter
@JsonPropertyOrder({"start", "stop", "ms", "nano"})
public class Timer {

    private final TimerNanoEnvelope<String> nano;

    private final TimerMsEnvelope<Void> ms;

    public Timer(String index) {
        this.nano = new TimerNanoEnvelope<>(index);
        this.ms = new TimerMsEnvelope<>(null);
    }

    public void stop() {
        nano.stop();
        ms.stop();
    }

    public String getStart() {
        return Util.msToDataFormat(ms.getLastActivityMs());
    }

    public String getStop() {
        Long timeStopMs = ms.getTimeStopMs();
        if (timeStopMs != null) {
            return Util.msToDataFormat(timeStopMs);
        }
        return null;
    }

    public long getMs() {
        return ms.getOffsetLastActivityMs();
    }

    public long getNano() {
        return nano.getOffsetLastActivityNano();
    }

    public String getIndex() {
        return nano.getValue();
    }

    public boolean isStop() {
        return nano.isStop();
    }

}
