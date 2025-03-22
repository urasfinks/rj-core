package ru.jamsys.core.statistic;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.extension.ByteSerialization;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(callSuper = true)
public class StatisticSec extends ExpirationMsMutableImpl implements ByteSerialization, Serializable {

    private short subscriberStatusRead;

    public List<Statistic> list = new ArrayList<>();

    @Override
    public byte[] toByte() {
        // Я попробовал One Nio, FST и Kryo все кроме Kryo упали, а Kryo не смогла переварить ArrayList
        // Далее были добавлены модифиакторы CollectionSerializer serializer = new CollectionSerializer();
        // И на выходе получилось ровно такое-же что из коробки java
        // Как будто для быстрых вещей надо писать для каждого класса свой сериализатор, иначе - это история не работает
        // По крайней мере у меня
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(this);
            out.flush();
            return bos.toByteArray();
        } catch (Exception ex) {
            throw new ForwardException(ex);
        }
    }

    public static StatisticSec instanceFromBytes(byte[] bytes) {
        StatisticSec statisticSec = new StatisticSec();
        statisticSec.toObject(bytes);
        return statisticSec;
    }

    @Override
    public void toObject(byte[] bytes) {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        try (ObjectInput in = new ObjectInputStream(bis)) {
            StatisticSec statisticSec = (StatisticSec) in.readObject();
            list = statisticSec.getList();
            setLastActivityMs(statisticSec.getLastActivityMs());
            setStopTimeMs(statisticSec.getStopTimeMs());
            setKeepAliveOnInactivityMs(statisticSec.getKeepAliveOnInactivityMs());
        } catch (Exception ex) {
            throw new ForwardException(ex);
        }
    }

}
