package ru.jamsys.core.balancer2;

import lombok.Getter;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.extension.expiration.ExpirationMap;
import ru.jamsys.core.flat.util.Util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class BalancerStickySessions<T> {

    private volatile Snapshot<T> snapshot = new Snapshot<>(List.of());

    private final List<T> list = new CopyOnWriteArrayList<>();

    private final Manager.Configuration<ExpirationMap<String, Resolved<T>>> expirationStickyMapConfiguration;

    // Вложенный результат: значение и статус "новый или нет"
    public record Resolved<T>(T value, boolean isNew) {
    }

    // Immutable snapshot
    private record Snapshot<T>(List<T> list) {
    }

    public BalancerStickySessions(String key, int keepAliveOnInactivityMs) {
        expirationStickyMapConfiguration = ExpirationMap.getInstanceConfigure(key, keepAliveOnInactivityMs);
    }

    public void add(T element) {
        if (element == null) return;
        list.add(element);
        rebuild();
    }

    public void remove(T element) {
        if (element == null) return;
        list.remove(element);
        rebuild();

        // Удаляем все ключи, указывающие на этот элемент
        expirationStickyMapConfiguration.get().entrySet().removeIf(entry -> entry.getValue().value.equals(element));
    }

    private void rebuild() {
        snapshot = new Snapshot<>(List.copyOf(list));
    }

    public Resolved<T> get(String key) {
        if (key == null) return new Resolved<>(null, false);

        Snapshot<T> snap = snapshot;
        if (snap.list.isEmpty()) {
            return new Resolved<>(null, false);
        }

        // Если есть в stickyMap и элемент актуален — вернём его
        ExpirationMap<String, Resolved<T>> stringResolvedExpirationMap = expirationStickyMapConfiguration.get();
        Resolved<T> resolved = stringResolvedExpirationMap.get(key);
        if (resolved != null && snap.list.contains(resolved.value)) {
            return resolved;
        }

        // Выбираем новый элемент и кэшируем
        int index = Util.stringToInt(key, 0, snap.list.size() - 1);
        T selected = snap.list.get(index);
        Resolved<T> newResolved = new Resolved<>(selected, true);
        stringResolvedExpirationMap.put(key, new Resolved<>(selected, false)); // сохраняем как "уже выданный"
        return newResolved;
    }

}
