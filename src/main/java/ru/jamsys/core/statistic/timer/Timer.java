package ru.jamsys.core.statistic.timer;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import ru.jamsys.core.statistic.timer.nano.TimerNanoEnvelope;

@Getter
@JsonPropertyOrder({"start", "stop", "ms", "nano"})
public class Timer {

    private final TimerNanoEnvelope<String> nano;

    public Timer(String index) {
        this.nano = new TimerNanoEnvelope<>(index);
    }

    public void stop() {
        nano.stop();
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
