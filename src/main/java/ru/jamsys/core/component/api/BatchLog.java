package ru.jamsys.core.component.api;

import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

// Многопоточная вставка логов в виде json
// У лога обязательно должен быть rqUid

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
public class BatchLog {

    // Максимальный размер пачки в кб
    // Оно может гулять, так как размер может быть разным
    @Setter
    long maxBatchSizeKib = 1024;

    ConcurrentLinkedDeque<String> queue = new ConcurrentLinkedDeque<>();

    AtomicInteger curSize = new AtomicInteger(0);

    public void add(String log) {

    }
}
