package ru.jamsys.core.extension.expiration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import ru.jamsys.core.flat.util.UtilDate;

// Интерфейс просто наделяет статическим функционал объект протухания, даёт базовый набор метрик

@JsonIgnoreType
@JsonIgnoreProperties
public interface ExpirationMs {

    // Получить время когда объект будет просрочен
    default long getExpirationTimeMs() {
        return getLastActivityMs() + getInactivityTimeoutMs();
    }

    // Объект просрочен
    default boolean isExpired() {
        return isExpired(System.currentTimeMillis());
    }

    // Объект просрочен
    default boolean isExpired(long curTimeMs) {
        if (isStopped()) {
            return false;
        }
        return curTimeMs > getExpirationTimeMs();
    }

    // Объект просрочен без проверки остановки
    default boolean isExpiredIgnoringStop() {
        return isExpiredIgnoringStop(System.currentTimeMillis());
    }

    // Объект просрочен без проверки остановки
    default boolean isExpiredIgnoringStop(long curTimeMs) {
        return curTimeMs > getExpirationTimeMs();
    }

    default TimeOutException genExpiredException() {
        return new TimeOutException(getLastActivityMs(), getInactivityTimeoutMs());
    }

    // Кол-во миллисекунд до момента, когда наступит протухание
    default long getRemainingMs(long curTime) {
        if (isStopped()) {
            return 0L;
        }
        return getExpirationTimeMs() - curTime;
    }

    // Кол-во миллисекунд до момента, когда наступит протухание
    default long getRemainingMs() {
        return getRemainingMs(System.currentTimeMillis());
    }

    // Кол-во миллисекунд с момента последней активности до остановки (если конечно она произошла)
    default long getDurationSinceLastActivityMs(long curTime) {
        if (isStopped()) {
            return getStopTimeMs() - getLastActivityMs();
        } else {
            return curTime - getLastActivityMs();
        }
    }

    // Кол-во миллисекунд с момента последней активности до остановки (если конечно она произошла)
    default long getDurationSinceLastActivityMs() {
        return getDurationSinceLastActivityMs(System.currentTimeMillis());
    }

    // Время последней активности
    default String getLastActivityFormatted() {
        return UtilDate.msFormat(getLastActivityMs());
    }

    // Возвратит время, когда объект будет просрочен
    default String getExpirationFormatted() {
        return UtilDate.msFormat(getExpirationTimeMs());
    }

    // Возвратит время, когда объект будет просрочен
    default String getStopTimeFormatted() {
        Long stop = getStopTimeMs();
        return stop != null ? UtilDate.msFormat(stop) : "-";
    }

    // Зафиксировать конец активности
    default void markStop(long curTime) {
        setStopTimeMs(curTime);
    }

    // Зафиксировать конец активности
    default void markStop() {
        markStop(System.currentTimeMillis());
    }

    // Остановлено
    default boolean isStopped() {
        return getStopTimeMs() != null;
    }

    // Время последней активности
    long getLastActivityMs();

    // Время жизни если нет активности
    long getInactivityTimeoutMs();


    void setStopTimeMs(Long timeMs);

    Long getStopTimeMs();

    // Поменить объект, что они живой, что бы его не слил менеджер
    void markActive();

}
