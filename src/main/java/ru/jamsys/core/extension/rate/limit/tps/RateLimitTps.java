package ru.jamsys.core.extension.rate.limit.tps;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.log.DataHeader;
import ru.jamsys.core.extension.property.PropertyDispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitTps extends AbstractManagerElement {

    private final AtomicInteger tps = new AtomicInteger(0);

    @Getter
    private final String ns;

    private final RateLimitTpsRepositoryProperty property = new RateLimitTpsRepositoryProperty();

    private final PropertyDispatcher<Integer> propertyDispatcher;

    public RateLimitTps(String ns) {
        this.ns = ns;
        propertyDispatcher = new PropertyDispatcher<>(
                null,
                property,
                getCascadeKey(ns)
        );
    }

    @SuppressWarnings("unused")
    public int getCurrentValue() {
        return tps.get();
    }

    @JsonValue
    public Object getValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("ns", ns)
                ;
    }

    public boolean check() {
        return tps.incrementAndGet() <= property.getMax();
    }

    public int getCount() {
        return tps.get();
    }

    public int getMax() {
        return property.getMax();
    }

    public String getPropertyKey() {
        return getCascadeKey(ns);
    }

    @Override
    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        List<DataHeader> result = new ArrayList<>();
        result.add(new DataHeader()
                .setBody(getCascadeKey(ns))
                .addHeader("tps", tps.getAndSet(0))
                .addHeader("max", property.getMax())
        );
        return result;
    }

    @Override
    public void runOperation() {
        propertyDispatcher.run();
    }

    @Override
    public void shutdownOperation() {
        propertyDispatcher.shutdown();
    }

    public void setMax(int value) {
        //+.max потому что поле в репозитории называется max
        App.get(ServiceProperty.class)
                .computeIfAbsent(getPropertyKey() + ".max", null)
                .set(value);
    }

}
