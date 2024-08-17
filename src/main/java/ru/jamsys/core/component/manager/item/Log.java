package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import ru.jamsys.core.extension.ByteTransformer;
import ru.jamsys.core.extension.Correlation;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilLog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@ToString
@Getter
@Service
@Component
public class Log implements ByteTransformer, Correlation {

    public Log() { // Это что бы компонент Spring смог инициализировать
        // не используйте этот констроктор
    }

    public long timeAdd = System.currentTimeMillis();

    public LogType logType;

    @Setter
    protected String correlation;

    public String extIndex;

    public String data;

    public Log(LogType logType) {
        this.logType = logType;
        this.correlation = java.util.UUID.randomUUID().toString();
    }

    public Log(LogType logType, String correlation) {
        this.logType = logType;
        this.correlation = correlation;
    }

    public Log setData(String data) {
        this.data = data;
        return this;
    }

    public Log setExtIndex(String extIndex) {
        this.extIndex = extIndex;
        return this;
    }

    public byte[] getByteInstance() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        UtilLog.writeShortString(os, getCorrelation());
        UtilLog.writeShortString(os, logType.getNameCamel());
        UtilLog.writeShortString(os, timeAdd + "");
        UtilLog.writeString(os, data);
        return os.toByteArray();
    }

    @Override
    public void instanceFromByte(byte[] bytes) throws Exception {
        InputStream fis = new ByteArrayInputStream(bytes);
        setCorrelation(UtilLog.readShortString(fis));
        logType = LogType.valueOf(Util.camelToSnake(UtilLog.readShortString(fis)));
        timeAdd = Long.parseLong(UtilLog.readShortString(fis));
        setData(UtilLog.readString(fis));
    }

}
