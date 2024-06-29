package ru.jamsys.core.resource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;

class PoolTaskWaitTest {
    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{"run.args.remote.log=false"};
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