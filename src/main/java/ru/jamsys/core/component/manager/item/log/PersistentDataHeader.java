package ru.jamsys.core.component.manager.item.log;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilFileByteReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

@ToString
@Getter
@Setter
@Accessors(chain = true)
@JsonPropertyOrder({"statusCode", "logType", "timeAdd", "header", "data"})
public class PersistentDataHeader implements PersistentData {

    private short writerFlag;

    public long timeAdd = System.currentTimeMillis();

    public Map<String, String> header = new LinkedHashMap<>();

    public Object body;

    public LogType logType;

    public PersistentDataHeader() {
    }

    public PersistentDataHeader(LogType logType, Class<?> cls, Object body) {
        this.logType = logType;
        this.body = body;
        addHeader("time", timeAdd);
        addHeader("thread", Thread.currentThread().getName());
        addHeader("source", cls.getName());
    }

    public String getBody() {
        return String.valueOf(body);
    }

    public PersistentDataHeader addHeader(String key, Object value) {
        addHeader(key, String.valueOf(value));
        return this;
    }

    public PersistentDataHeader addHeader(String key, String value) {
        this.header.put(key, value);
        return this;
    }

    @Override
    public short getStatusCode() {
        return writerFlag;
    }

    @Override
    public void setStatusCode(short writerFlag) {
        this.writerFlag = writerFlag;
    }

    public byte[] toByte() throws Exception {
        // [2] LogType
        // [2] SizeHeader
        //*[2] KeyHeaderLength
        // ...
        //*[4] ValueHeaderLength
        // ..s
        // [4] BodyLength
        // ...

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(UtilByte.shortToBytes((short) logType.ordinal())); //Записали тип лога в первые 2 байта
        os.write(UtilByte.shortToBytes((short) header.size())); // В следующие 2 байта записали кол-во заголовков

        // Запись заголовков
        for (String key : header.keySet()) {
            UtilFileByteReader.writeShortString(os, key);
            UtilFileByteReader.writeShortString(os, header.get(key));
        }
        // Запись тела
        UtilFileByteReader.writeString(os, getBody());
        return os.toByteArray();
    }

    public static PersistentDataHeader instanceFromBytes(byte[] bytes, short writerFlag) throws Exception {
        PersistentDataHeader persistentDataHeader = new PersistentDataHeader();
        persistentDataHeader.toObject(bytes);
        persistentDataHeader.setStatusCode(writerFlag);
        return persistentDataHeader;
    }

    @Override
    public void toObject(byte[] bytes) throws Exception {
        InputStream fis = new ByteArrayInputStream(bytes);
        setLogType(LogType.valueOfOrdinal(UtilByte.bytesToShort(fis.readNBytes(2))));
        short countHeader = UtilByte.bytesToShort(fis.readNBytes(2));
        for (int i = 0; i < countHeader; i++) {
            addHeader(UtilFileByteReader.readShortString(fis), UtilFileByteReader.readShortString(fis));
        }
        setBody(UtilFileByteReader.readString(fis));
    }

    @JsonIgnore
    @Override
    public String getView() {
        return UtilJson.toStringPretty(
                new HashMapBuilder<String, Object>()
                        .append(
                                "header",
                                new HashMapBuilder<>(header)
                                        .append("time", UtilDate.msFormat(timeAdd))
                        )
                        .append("body", body),
                "--"
        );
    }

}