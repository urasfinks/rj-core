package ru.jamsys.core.component.manager.item.log;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.flat.util.UtilJson;

import java.io.PrintStream;

@ToString
@Getter
@Setter
@Accessors(chain = true)
@JsonPropertyOrder({"logType", "timeAdd", "header", "data"})
public class Log extends PersistentDataHeader {

    private final LogType logType;

    public Log(LogType logType, String caller, Object body) {
        super(body);
        this.logType = logType;
        // Мы должны это записать в заголовки, иначе это не доедет до Persistent хранилища
        addHeader("logType", logType);
        addHeader("thread", Thread.currentThread().getName());
        addHeader("caller", caller);
    }

    @Override
    public void print() {
        StringBuilder sb = new StringBuilder();
        sb
                .append(UtilDate.msFormat(getTimeAdd()))
                .append("\t")
                .append(UtilJson.toString(getHeader(), "{}"));

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
