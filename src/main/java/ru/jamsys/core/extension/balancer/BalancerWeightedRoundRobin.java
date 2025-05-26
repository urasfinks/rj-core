package ru.jamsys.core.extension.balancer;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class BalancerWeightedRoundRobin<T extends WeightElement> extends AbstractBalancer<T> {

    private volatile Snapshot<T> snapshot = new Snapshot<>(new TreeMap<>(), 0);

    // Immutable snapshot
    private record Snapshot<T>(TreeMap<Double, List<T>> map, double sum) {}

    // Добавляем небольшой шум (например, 1e-6)
    private static final double EPSILON = 1e-6;

    public void rebuild() {
        TreeMap<Double, List<T>> pool = new TreeMap<>();
        double totalWeight = 0;

        for (T resource : getList()) {
            int weight = resource.getWeight();
            if (weight > 0) {
                // Шум добавляется, чтобы избежать одинаковых ключей
                totalWeight += weight;
                double key = totalWeight + ThreadLocalRandom.current().nextDouble() * EPSILON;

                pool.computeIfAbsent(key, _ -> new ArrayList<>()).add(resource);
            }
        }

        this.snapshot = new Snapshot<>(pool, totalWeight);
    }

    @Override
    public void add(T element) {
        super.add(element);
        rebuild();
    }

    @Override
    public void remove(T element) {
        super.remove(element);
        rebuild();
    }

    @Override
    public T get() {
        Snapshot<T> snap = snapshot;
        if (snap.sum == 0) {
            return null;
        }
        double random = ThreadLocalRandom.current().nextDouble() * snap.sum;
        Map.Entry<Double, List<T>> entry = snap.map.ceilingEntry(random);
        if (entry == null) {
            return null;
        }

        List<T> candidates = entry.getValue();
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

}
