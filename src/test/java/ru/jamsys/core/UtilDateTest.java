package ru.jamsys.core;

import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilDate;

class UtilDateTest {

    @Test
    void t1() {
        long timestamp = UtilDate.getTimestamp();
        System.out.println(timestamp);
    }

}