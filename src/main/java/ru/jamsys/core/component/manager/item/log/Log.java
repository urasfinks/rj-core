package ru.jamsys.core.component.manager.item.log;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilRisc;

import java.io.PrintStream;

@ToString
@Getter
@Setter
@Accessors(chain = true)
@JsonPropertyOrder({"logType", "timeAdd", "header", "data"})
public class Log extends PersistentDataHeader {

    private final LogType logType;

    public Log(LogType logType, Class<?> cls, Object body) {
        super(cls, body);
        this.logType = logType;
    }

    @Override
    public void print() {
        StringBuilder sb = new StringBuilder();
        UtilRisc.forEach(null, getHeader(), (s, o) -> {
            if (s.equals("time")) {
                sb.append(UtilDate.msFormat(getTimeAdd()));
            } else {
                sb.append(s).append(": ").append(o);
            }
            sb.append("; ");
        });
        Object body1 = getRawBody();
        if (body1 != null) {
            sb.append("\n");
            if (body1 instanceof String) {
                sb.append(body1);
            } else {
                sb.append(UtilJson.toStringPretty(body1, "--"));
            }
        }
        PrintStream ps = getLogType().equals(LogType.ERROR) ? System.err : System.out;
        ps.println(sb);
    }

}
