package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.rate.limit.RateLimit;
import ru.jamsys.core.rate.limit.item.RateLimitItem;

/*
 * RateLimitItem целевого механизма по удалению элементов - нет
 * Так как если в runTime будут выставлены для кого-то лимиты, а потом этот объект
 * Временно остановится и вместе с собой удалит установленные лимиты, то после восстановления работы
 * ранее установленные лимиты будут утрачены? Это как-то странно?
 * Можно только управлять статусом active = true/false для отрисовки статистики
 * */

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
public class RateLimitManager
        extends AbstractManagerMapItem<RateLimit, RateLimitItem>
        implements StatisticsFlushComponent {

    public RateLimitManager() {
        setCleanableMap(false);
    }

    @Override
    public RateLimit build(String index) {
        return new RateLimit(index);
    }

    public boolean check(String keyRateLimit, String keyRateLimitItem, Integer limit) {
        return get(keyRateLimit).get(keyRateLimitItem).check(limit);
    }

    public long getMax(String keyRateLimit, String keyRateLimitItem) {
        return get(keyRateLimit).get(keyRateLimitItem).getMax();
    }

    public void setMax(String keyRateLimit, String keyRateLimitItem, Integer limit) {
        get(keyRateLimit).get(keyRateLimitItem).setMax(limit);
    }

    //Используйте преимущественно для тестирования
    public void reset(String keyRateLimit, String keyRateLimitItem) {
        get(keyRateLimit).get(keyRateLimitItem).reset();
    }

    public void incrementMax(String keyRateLimit, String keyRateLimitItem) {
        get(keyRateLimit).get(keyRateLimitItem).incrementMax();
    }

    public String getMomentumStatistic(String keyRateLimit, String keyRateLimitItem) {
        return get(keyRateLimit).get(keyRateLimitItem).getMomentumStatistic();
    }

    public void setActive(String keyRateLimit, boolean active){
        get(keyRateLimit).setActive(active);
    }

    public void reset(String keyRateLimit){
        get(keyRateLimit).reset();
    }

    // setCleanableMap(false) - можем себе позволить прихранивать объекты
    @Override
    public RateLimit get(String key) {
        return super.get(key);
    }

}
