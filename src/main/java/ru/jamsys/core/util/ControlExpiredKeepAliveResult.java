package ru.jamsys.core.util;

import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@ToString
public class ControlExpiredKeepAliveResult {

    List<Long> readBucket = new ArrayList<>();
    AtomicInteger countRemove = new AtomicInteger(0);

    public List<String> getReadBucketFormat() {
        List<String> result = new ArrayList<>();
        readBucket.forEach(x -> result.add(Util.msToDataFormat(x)));
        return result;
    }
}
