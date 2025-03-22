package ru.jamsys.core.extension.raw.writer;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.UtilByte;

import java.util.LinkedHashMap;
import java.util.Map;

// 16 бит short это значит можно оперировать 16 статусами
// Допустим можно зафиксировать 16 переходов каких-либо состояний
// Например лог считан, лог записан, лог передан
// Это не последовательность, это одновременно может происходить, то есть каждый статус это бит short
// с двумя состояниями 0 и 1, изначально все 16 состояний = 0 [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0].
// В StatusCodeModel вы заполняете byteIndex который будет соответствовать индексу бита short.
// Любой бит в один момент времени можно перевести из 0 в 1, тем самым пометив, что операция по какому-то состоянию
// завершена

public class StatusCode<T extends StatusCodeModel> {

    @Getter
    @Setter
    private short statusCode;

    private final Class<T> cls;

    public StatusCode(short statusCode, Class<T> cls) {
        this.statusCode = statusCode;
        this.cls = cls;
    }

    public void set(T enumStatusCode, boolean enable) {
        for (StatusCodeModel statusCodeModel : cls.getEnumConstants()) {
            if (statusCodeModel.equals(enumStatusCode)) {
                if (enable) {
                    this.statusCode = UtilByte.setBit(this.statusCode, statusCodeModel.getByteIndex());
                } else {
                    this.statusCode = UtilByte.clearBit(this.statusCode, statusCodeModel.getByteIndex());
                }
                break;
            }
        }
    }

    public boolean getStatus(T statusCode) {
        return UtilByte.getBit(this.statusCode, statusCode.getByteIndex()) != 0;
    }

    public Map<T, Boolean> getStatus() {
        Map<T, Boolean> result = new LinkedHashMap<>();
        for (T statusCode : cls.getEnumConstants()) {
            result.put(statusCode, UtilByte.getBit(this.statusCode, statusCode.getByteIndex()) != 0);
        }
        return result;
    }

    @JsonValue
    public Map<String, Object> get() {
        return new HashMapBuilder<String, Object>()
                .append("number", statusCode)
                .append("bits", UtilByte.getBits(statusCode))
                .append("status", getStatus())
                ;
    }

}
