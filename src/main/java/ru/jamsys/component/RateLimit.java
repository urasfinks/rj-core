package ru.jamsys.component;

import org.springframework.stereotype.Component;
import ru.jamsys.extension.StatisticsCollectorComponent;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
@Component
public class RateLimit implements StatisticsCollectorComponent {

    Map<String, RateLimitItem> map = new ConcurrentHashMap<>();

    public RateLimitItem add(String key) {
        if (!map.containsKey(key)) {
            map.put(key, new RateLimitItem());
        }
        return map.get(key);
    }

    public void setMaxTps(String key, int maxTps) {
        add(key).setMaxTps(maxTps);
    }

    public boolean check(String key) {
        if (!map.containsKey(key)) {
            map.put(key, new RateLimitItem());
        }
        return map.get(key).check();
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isRun) {
        List<Statistic> result = new ArrayList<>();
        Util.riskModifierMap(isRun, map, new String[0], (String key, RateLimitItem threadPool)
                -> result.add(new Statistic(parentTags, parentFields)
                .addTag("index", key)
                .addField("max", threadPool.maxTps.get())
                .addField("tps", threadPool.tps.getAndSet(0))));
        return result;
    }

    public static class RateLimitItem {
        AtomicInteger tps = new AtomicInteger(0);
        AtomicInteger maxTps = new AtomicInteger(-1);

        public boolean check() {
            int maxTpsInt = maxTps.get();
            boolean result = maxTpsInt < 0 || (maxTpsInt > 0 && tps.get() < maxTpsInt); // -1 = infinity; 0 = reject
            tps.incrementAndGet();
            return result;
        }

        public int getMaxTps() {
            return maxTps.get();
        }

        public void setMaxTps(int maxTps) {
            this.maxTps.set(maxTps);
        }

        public void reset() {
            // Рекомендуется использовать только для тестов
            tps.set(0);
        }
    }
}
