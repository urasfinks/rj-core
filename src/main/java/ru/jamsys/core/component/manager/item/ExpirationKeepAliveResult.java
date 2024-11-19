package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import lombok.ToString;
import ru.jamsys.core.flat.util.UtilDate;

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
        readBucket.forEach(x -> result.add(UtilDate.msFormat(x)));
        return result;
    }
}
