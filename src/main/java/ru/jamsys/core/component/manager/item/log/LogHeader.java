package ru.jamsys.core.component.manager.item.log;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilLogConverter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

@ToString
@Getter
@Setter
@Accessors(chain = true)
@JsonPropertyOrder({"writerFlag", "logType", "timeAdd", "header", "data"})
public class LogHeader implements Log {

    private short writerFlag;

    public long timeAdd = System.currentTimeMillis();

    public Map<String, String> header = new LinkedHashMap<>();

    public String body;

    public LogType logType;

    public LogHeader() {
    }

    public LogHeader(LogType logType, Class<?> cls, Object body) {
        this.logType = logType;
        if (body != null) {
            this.body = body instanceof String ? body.toString() : UtilJson.toStringPretty(body, "--");
        }
        addHeader("time", timeAdd);
        addHeader("thread", Thread.currentThread().getName());
        addHeader("source", cls.getName());
    }

    public LogHeader addHeader(String key, Object value) {
        addHeader(key, String.valueOf(value));
        return this;
    }

    public LogHeader addHeader(String key, String value) {
        this.header.put(key, value);
        return this;
    }

    @Override
    public short getWriterFlag() {
        return writerFlag;
    }

    @Override
    public void setWriterFlag(short writerFlag) {
        this.writerFlag = writerFlag;
    }

    public byte[] toByte() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(UtilByte.shortToBytes((short) logType.ordinal())); //Записали тип лога в первые 2 байта
        os.write(UtilByte.shortToBytes((short) header.size())); // В следующие 2 байта записали кол-во заголовков

        // Запись заголовков
        for (String key : header.keySet()) {
            UtilLogConverter.writeShortString(os, key);
            UtilLogConverter.writeShortString(os, header.get(key));
        }
        // Запись тела
        UtilLogConverter.writeString(os, body);
        return os.toByteArray();
    }

    public static LogHeader instanceFromBytes(byte[] bytes, short writerFlag) throws Exception {
        LogHeader logHeader = new LogHeader();
        logHeader.toObject(bytes);
        logHeader.setWriterFlag(writerFlag);
        return logHeader;
    }

    @Override
    public void toObject(byte[] bytes) throws Exception {
        InputStream fis = new ByteArrayInputStream(bytes);
        setLogType(LogType.valueOfOrdinal(UtilByte.bytesToShort(fis.readNBytes(2))));
        short countHeader = UtilByte.bytesToShort(fis.readNBytes(2));
        for (int i = 0; i < countHeader; i++) {
            addHeader(UtilLogConverter.readShortString(fis), UtilLogConverter.readShortString(fis));
        }
        setBody(UtilLogConverter.readString(fis));
    }

    @JsonIgnore
    @Override
    public String getView() {
        header.put("time", UtilDate.msFormat(timeAdd));
        if (body != null) {
            return UtilJson.toString(header, "--") + "\r\n" + body;
        } else {
            return UtilJson.toString(header, "--");
        }
    }

}