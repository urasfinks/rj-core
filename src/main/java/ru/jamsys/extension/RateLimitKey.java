package ru.jamsys.extension;

import lombok.Getter;
import lombok.NonNull;
import ru.jamsys.statistic.RateLimitGroup;

public class RateLimitKey {

    @Getter
    final RateLimitGroup rateLimitGroup;

    @Getter
    final Class<?> cls;

    @Getter
    final String key;

    public RateLimitKey(@NonNull RateLimitGroup rateLimitGroup, @NonNull Class<?> cls, @NonNull String key) {
        this.rateLimitGroup = rateLimitGroup;
        this.cls = cls;
        this.key = key;
    }

    @Override
    public String toString() {
        return rateLimitGroup.getName() + "." + cls.getSimpleName() + "." + key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RateLimitKey that = (RateLimitKey) o;

        if (rateLimitGroup != that.rateLimitGroup) return false;
        if (!cls.equals(that.cls)) return false;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        int result = rateLimitGroup.hashCode();
        result = 31 * result + cls.hashCode();
        result = 31 * result + key.hashCode();
        return result;
    }
}
