package ru.jamsys.core.promise;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReentrantLock;

/*
 * Синхронизированный контроль выдачи элементов, до тех пор, пока не встретится wait элемент
 * после этого, когда выдача из очереди завершилась (из-за элемента wait) мы ждём commit по всем выданным задачам.
 * После того как все выданные задачи commit, продолжается выдача задач.
 * */

@Getter
@Setter
@Accessors(chain = true)
public class WaitQueue<T extends WaitQueueElement> {

    private final Deque<T> mainQueue = new ConcurrentLinkedDeque<>(); // Основная очередь

    private final Deque<T> polledQueue = new ConcurrentLinkedDeque<>(); // Очередь изъятых

    private final ReentrantLock lock = new ReentrantLock();

    private List<T> pollWithoutLock() {
        List<T> result = new ArrayList<>();
        if (!polledQueue.isEmpty()) {
            return result;
        }
        while (!mainQueue.isEmpty()) {
            T poll = mainQueue.pollFirst();
            if (poll == null) {
                break;
            }
            if (poll.isWait()) {
                break;
            }
            polledQueue.add(poll);
            result.add(poll);
        }
        return result;
    }

    @NonNull
    public List<T> poll() {
        try {
            lock.lock();
            return pollWithoutLock();
        } finally {
            lock.unlock();
        }
    }

    public void commit(T t) {
        try {
            lock.lock();
            polledQueue.remove(t);
        } finally {
            lock.unlock();
        }
    }

    // Что бы один раз использовать блокировку
    public List<T> commitAndPoll(T t) {
        try {
            lock.lock();
            polledQueue.remove(t);
            return pollWithoutLock();
        } finally {
            lock.unlock();
        }
    }

    // Это история совсем не поточная, предполагается, что должно это всё вызываться из блока самой таски
    // Мы не нарушаем wait, так как удаляем только будущие задачи
    public void skipUntil(String index) {
        try {
            lock.lock();
            for (Iterator<T> it = mainQueue.iterator(); it.hasNext(); ) {
                T elem = it.next();
                if (elem.getIndex().equals(index)) {
                    break;
                }
                it.remove();
            }
        } finally {
            lock.unlock();
        }
    }

    public void addFirst(T t) {
        try {
            lock.lock();
            mainQueue.addFirst(t);
        } finally {
            lock.unlock();
        }
    }

    public void addFirst(List<T> list) {
        try {
            lock.lock();
            ListIterator<T> iterator = list.listIterator(list.size()); // Старт с конца
            while (iterator.hasPrevious()) {
                mainQueue.addFirst(iterator.previous());
            }
        } finally {
            lock.unlock();
        }
    }

    public void addLast(T t) {
        try {
            lock.lock();
            mainQueue.addLast(t);
        } finally {
            lock.unlock();
        }
    }

    public void addLast(List<T> list) {
        try {
            lock.lock();
            for (T t : list) {
                mainQueue.addLast(t);
            }
        } finally {
            lock.unlock();
        }
    }

    // Терминальная прямая
    public void skipAll() {
        try {
            lock.lock();
            mainQueue.clear();
        } finally {
            lock.unlock();
        }
    }

    public boolean isTerminal() {
        // Если очередь пустая и все изъятые закомичены - у нас терминальный статус
        return mainQueue.isEmpty() && polledQueue.isEmpty();
    }

}
