package ru.jamsys.message;

import java.sql.Date;
import java.sql.Timestamp;

public interface Message {

    @SuppressWarnings("unused")
    void onHandle(MessageHandle handleState, Object service);

    @SuppressWarnings("unused")
    String getBody();

    @SuppressWarnings("unused")
    String getCorrelation();

    default String convertTimestamp(long timestamp) {
        Timestamp stamp = new Timestamp(timestamp);
        Date date = new Date(stamp.getTime());
        return date.toString();
    }

    @SuppressWarnings("unused")
    void setError(Exception e);

    @SuppressWarnings("unused")
    <T> T getHeader(String name);
}
