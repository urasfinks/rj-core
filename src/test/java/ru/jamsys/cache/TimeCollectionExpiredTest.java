package ru.jamsys.cache;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class TimeCollectionExpiredTest {

    @Test
    void add() {
        long curTimeMs = 1709734264056L; //2024-03-06T17:11:04.056
        TimeCollectionExpired<String> timeCollectionExpired = new TimeCollectionExpired<>((String s) -> System.out.println("Expired: " + s));

        for (int i = 0; i < 10; i++) {
            timeCollectionExpired.add(java.util.UUID.randomUUID().toString(), curTimeMs, 1000);
            timeCollectionExpired.add(java.util.UUID.randomUUID().toString(), curTimeMs, 2000);
            timeCollectionExpired.add(java.util.UUID.randomUUID().toString(), curTimeMs, 3000);
        }

        Map<String, Object> stat = timeCollectionExpired.flushAndGetStatistic(null, null, null).get(0).getFields();
        Assertions.assertEquals(3, (int) stat.get("BucketSize"));
        Assertions.assertEquals(30, (long) stat.get("ItemSize"));

        timeCollectionExpired.keepAlive(null, curTimeMs + 1001);
        stat = timeCollectionExpired.flushAndGetStatistic(null, null, null).get(0).getFields();
        Assertions.assertEquals(2, (int) stat.get("BucketSize"));
        Assertions.assertEquals(20, (long) stat.get("ItemSize"));

        timeCollectionExpired.keepAlive(null, curTimeMs + 2001);
        stat = timeCollectionExpired.flushAndGetStatistic(null, null, null).get(0).getFields();
        Assertions.assertEquals(1, (int) stat.get("BucketSize"));
        Assertions.assertEquals(10, (long) stat.get("ItemSize"));

        timeCollectionExpired.keepAlive(null, curTimeMs + 3001);
        stat = timeCollectionExpired.flushAndGetStatistic(null, null, null).get(0).getFields();
        Assertions.assertEquals(0, (int) stat.get("BucketSize"));
        Assertions.assertEquals(0, (long) stat.get("ItemSize"));
    }

}