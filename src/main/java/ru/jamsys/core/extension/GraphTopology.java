package ru.jamsys.core.extension;

import java.util.*;

public class GraphTopology<T> {

    // Граф зависимостей: ключ — объект, значения — список зависимых от него объектов (дети)
    private final Map<T, List<T>> dependencyGraph = new HashMap<>();

    // Обратный граф: ключ — объект, значения — на кого он ссылается (родители)
    private final Map<T, List<T>> reverseGraph = new HashMap<>();

    // Все объекты
    private final Set<T> allObjects = new HashSet<>();

    public void add(T element) {
        dependencyGraph.computeIfAbsent(element, _ -> new ArrayList<>());
        reverseGraph.computeIfAbsent(element, _ -> new ArrayList<>());
        allObjects.add(element);
    }

    public void addDependency(T parent, T child) {
        dependencyGraph.computeIfAbsent(parent, _ -> new ArrayList<>()).add(child);
        reverseGraph.computeIfAbsent(child, _ -> new ArrayList<>()).add(parent);
        allObjects.add(parent);
        allObjects.add(child);
    }

    public List<T> getSorted() {
        List<T> result = topologicalSort();
        // Останавливаем в обратном порядке (сначала листья)
        Collections.reverse(result);
        return result;
    }

    private List<T> topologicalSort() {
        Map<T, Integer> inDegree = new HashMap<>();
        for (T obj : allObjects) {
            inDegree.put(obj, 0);
        }

        // Посчитаем количество входящих связей (in-degree)
        for (Map.Entry<T, List<T>> entry : dependencyGraph.entrySet()) {
            for (T child : entry.getValue()) {
                inDegree.put(child, inDegree.get(child) + 1);
            }
        }

        // Очередь с объектами без зависимостей
        Queue<T> queue = new LinkedList<>();
        for (Map.Entry<T, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<T> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            T current = queue.poll();
            sorted.add(current);

            for (T dependent : dependencyGraph.getOrDefault(current, Collections.emptyList())) {
                inDegree.put(dependent, inDegree.get(dependent) - 1);
                if (inDegree.get(dependent) == 0) {
                    queue.add(dependent);
                }
            }
        }

        if (sorted.size() != allObjects.size()) {
            Set<T> unresolved = new HashSet<>(allObjects);
            sorted.forEach(unresolved::remove);
            throw new RuntimeException("Cycle detected in dependencies! Unresolved nodes: " + unresolved);
        }

        return sorted;
    }

    public void removeDependency(T parent, T child) {
        List<T> children = dependencyGraph.get(parent);
        if (children != null) {
            children.remove(child);
            if (children.isEmpty()) {
                dependencyGraph.remove(parent);
            }
        }

        List<T> parents = reverseGraph.get(child);
        if (parents != null) {
            parents.remove(parent);
            if (parents.isEmpty()) {
                reverseGraph.remove(child);
            }
        }
    }

    public void remove(T element) {
        // Удалить из родителей
        List<T> parents = reverseGraph.getOrDefault(element, Collections.emptyList());
        for (T parent : parents) {
            List<T> children = dependencyGraph.get(parent);
            if (children != null) {
                children.remove(element);
                if (children.isEmpty()) {
                    dependencyGraph.remove(parent);
                }
            }
        }

        // Удалить из детей
        List<T> children = dependencyGraph.getOrDefault(element, Collections.emptyList());
        for (T child : children) {
            List<T> parentList = reverseGraph.get(child);
            if (parentList != null) {
                parentList.remove(element);
                if (parentList.isEmpty()) {
                    reverseGraph.remove(child);
                }
            }
        }

        dependencyGraph.remove(element);
        reverseGraph.remove(element);
        allObjects.remove(element);
    }

}