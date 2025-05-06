package ru.jamsys.core.extension.broker.persist;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.extension.ByteSerialization;

class BrokerPersistTest {

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runCore();
        //App.get(ServiceProperty.class).set("App.AsyncFileWriter.test.file.path", "tmp.dat");
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Getter
    @Setter
    public static class X implements ByteSerialization {

        private String value;

        @Override
        public byte[] toByte() {
            return value.getBytes();
        }

        @Override
        public void toObject(byte[] bytes) {
            setValue(new String(bytes));
        }

    }

    @Test
    public void test() throws Throwable {
        BrokerPersist<X> test = App.get(Manager.class).configure(
                BrokerPersist.class,
                "test",
                ns -> new BrokerPersist<>(ns, App.context)
        ).getGeneric();
        test.add(new X());

    }
}