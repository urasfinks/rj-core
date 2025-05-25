package ru.jamsys.core.balancer;

import lombok.Getter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public abstract class AbstractBalancer<T> {

    // Потокобезопасный список
    private final List<T> list = new CopyOnWriteArrayList<>();

    public void add(T element) {
        if (element == null) {
            return;
        }
        list.add(element);
    }

    public void remove(T element) {
        if (element == null) {
            return;
        }
        list.remove(element);
    }

    abstract public T get();

}
