package ru.jamsys.core.resource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;

class PoolTaskTest {
    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.run(args);
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    void test() {

    }
}