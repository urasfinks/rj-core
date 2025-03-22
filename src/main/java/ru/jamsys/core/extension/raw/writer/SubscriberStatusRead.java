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
// В SubscriberStatusModel вы заполняете byteIndex который будет соответствовать индексу бита short.
// Любой бит в один момент времени можно перевести из 0 в 1, тем самым пометив, что операция по какому-то состоянию
// завершена

public class SubscriberStatusRead<T extends SubscriberStatusReadModel> {

    @Getter
    @Setter
    private short subscriberStatusRead;

    private final Class<T> cls;

    public SubscriberStatusRead(short subscriberStatusRead, Class<T> cls) {
        this.subscriberStatusRead = subscriberStatusRead;
        this.cls = cls;
    }

    public void set(T enumSubscriberStatusReadModel, boolean enable) {
        for (SubscriberStatusReadModel subscriberStatusReadModel : cls.getEnumConstants()) {
            if (subscriberStatusReadModel.equals(enumSubscriberStatusReadModel)) {
                if (enable) {
                    this.subscriberStatusRead = UtilByte.setBit(this.subscriberStatusRead, subscriberStatusReadModel.getByteIndex());
                } else {
                    this.subscriberStatusRead = UtilByte.clearBit(this.subscriberStatusRead, subscriberStatusReadModel.getByteIndex());
                }
                break;
            }
        }
    }

    public boolean getStatus(T subscriberStatusReadModel) {
        return UtilByte.getBit(this.subscriberStatusRead, subscriberStatusReadModel.getByteIndex()) != 0;
    }

    public Map<T, Boolean> getStatus() {
        Map<T, Boolean> result = new LinkedHashMap<>();
        for (T subscriberStatusRead : cls.getEnumConstants()) {
            result.put(subscriberStatusRead, UtilByte.getBit(this.subscriberStatusRead, subscriberStatusRead.getByteIndex()) != 0);
        }
        return result;
    }

    @JsonValue
    public Map<String, Object> get() {
        return new HashMapBuilder<String, Object>()
                .append("number", subscriberStatusRead)
                .append("bits", UtilByte.getBits(subscriberStatusRead))
                .append("status", getStatus())
                ;
    }

}
