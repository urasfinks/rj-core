package ru.jamsys.core.extension.balancer;

import lombok.Getter;

import java.util.List;

@Getter
public class BalancerLeastConnection<T extends LeastConnectionElement> extends AbstractBalancer<T> {

    @Override
    public T get() {
        // Борьба за миллисекунды
        List<T> list = getList();
        T min = list.getFirst();
        for (T element : list) {
            if (element.getCountConnection() < min.getCountConnection()) {
                min = element;
            }
        }
        return min;
    }

}
