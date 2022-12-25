package ru.jamsys.pool;

import lombok.Data;
import org.springframework.lang.Nullable;

@Data
public class PoolStatisticData implements Cloneable {

    String name;

    int tpsGet;
    int tpsAdd;
    int tpsRemove;
    int list;
    int park;
    int tpsParkIn;

    @Nullable
    public PoolStatisticData clone() {
        try {
            return (PoolStatisticData) super.clone();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
