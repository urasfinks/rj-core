package ru.jamsys.core.extension.expiration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.UtilDate;

@JsonIgnoreType
@JsonIgnoreProperties
public interface ExpirationMs {

    default long getExpiredMs() {
        return getLastActivityMs() + getKeepAliveOnInactivityMs();
    }

    // Объект просрочен
    default boolean isExpired() {
        return isExpired(System.currentTimeMillis());
    }

    // Объект просрочен
    default boolean isExpired(long curTimeMs) {
        if (isStop()) {
            return false;
        }
        return curTimeMs > (getLastActivityMs() + getKeepAliveOnInactivityMs());
    }

    // Объект просрочен
    default boolean isExpiredWithoutStop() {
        return isExpired(System.currentTimeMillis());
    }

    // Объект просрочен без проверки остановки
    default boolean isExpiredWithoutStop(long curTimeMs) {
        return curTimeMs > (getLastActivityMs() + getKeepAliveOnInactivityMs());
    }

    default HashMapBuilder<String, Object> getTimeoutInformation() {
        return new HashMapBuilder<String, Object>()
                .append("now", UtilDate.msFormat(System.currentTimeMillis()))
                .append("lastActivity", getLastActivityFormat())
                .append("timeout", getKeepAliveOnInactivityMs())
                ;
    }

    // Кол-во миллисекунд до момента, когда наступит протухание
    default long getExpiryRemainingMs(long curTime) {
        if (isStop()) {
            return 0L;
        }
        return (getLastActivityMs() + getKeepAliveOnInactivityMs()) - curTime;
    }

    // Кол-во миллисекунд до момента, когда наступит протухание
    default long getExpiryRemainingMs() {
        return getExpiryRemainingMs(System.currentTimeMillis());
    }

    // Кол-во миллисекунд с момента последней активности до остановки (если конечно она произошла)
    default long getInactivityTimeMs(long curTime) {
        if (isStop()) {
            return getStopTimeMs() - getLastActivityMs();
        } else {
            return curTime - getLastActivityMs();
        }
    }

    // Кол-во миллисекунд с момента последней активности до остановки (если конечно она произошла)
    default long getInactivityTimeMs() {
        return getInactivityTimeMs(System.currentTimeMillis());
    }

    // Время последней активности
    default String getLastActivityFormat() {
        return UtilDate.msFormat(getLastActivityMs());
    }

    // Возвратит время, когда объект будет просрочен
    default String getExpiredFormat() {
        return UtilDate.msFormat(getLastActivityMs() + getKeepAliveOnInactivityMs());
    }

    // Возвратит время, когда объект будет просрочен
    default String getStopFormat() {
        return UtilDate.msFormat(getStopTimeMs());
    }

    // Зафиксировать конец активности
    default void stop(long curTime) {
        setStopTimeMs(curTime);
    }

    // Зафиксировать конец активности
    default void stop() {
        stop(System.currentTimeMillis());
    }

    // Зафиксировать конец активности
    default boolean isStop() {
        return getStopTimeMs() != null;
    }

    // Время последней активности
    long getLastActivityMs();

    // Время жизни если нет активности
    long getKeepAliveOnInactivityMs();


    void setStopTimeMs(Long timeMs);

    Long getStopTimeMs();

}
