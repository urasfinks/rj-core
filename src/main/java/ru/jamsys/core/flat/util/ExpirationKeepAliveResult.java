package ru.jamsys.core.flat.util;

import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@ToString
public class ExpirationKeepAliveResult {

    List<Long> readBucket = new ArrayList<>();
    AtomicInteger countRemove = new AtomicInteger(0);

    public List<String> getReadBucketFormat() {
        List<String> result = new ArrayList<>();
        readBucket.forEach(x -> result.add(UtilDate.msToDataFormat(x)));
        return result;
    }
}
