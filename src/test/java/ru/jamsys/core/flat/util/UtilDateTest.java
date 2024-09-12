package ru.jamsys.core.flat.util;

import org.junit.jupiter.api.Test;

class UtilDateTest {

    @Test
    void t1() {
        long timestamp = UtilDate.getTimestamp();
        System.out.println(timestamp);
    }

}