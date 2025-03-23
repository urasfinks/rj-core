package ru.jamsys.core.flat.btree;

import java.util.*;

/**
 * Класс для поиска ключей в диапазоне в B-дереве.
 *
 * @param <T> Тип данных, хранящихся в дереве. Должен реализовывать интерфейс Comparable.
 */
class BTreeRangeSearch<T extends Comparable<T>> extends BTreeOperations<T> {

    /**
     * Конструктор.
     *
     * @param t Минимальная степень дерева.
     */
    public BTreeRangeSearch(int t) {
        super(t);
    }

    /**
     * Ищет все ключи в диапазоне [start, end].
     *
     * @param node  Узел, в котором начинаем поиск.
     * @param start Начало диапазона (включительно).
     * @param end   Конец диапазона (включительно).
     * @return Список ключей, попадающих в диапазон.
     */
    public Map<T, List<Long>> searchRange(BTreeNode<T> node, T start, T end) {
        Map<T, List<Long>> result = new LinkedHashMap<>();
        searchRangeHelper(node, start, end, result);
        return result;
    }

    /**
     * Вспомогательный метод для поиска ключей в диапазоне.
     *
     * @param node   Узел, в котором ищем ключи.
     * @param start  Начало диапазона (включительно).
     * @param end    Конец диапазона (включительно).
     * @param result Список для хранения найденных ключей.
     */
    private void searchRangeHelper(BTreeNode<T> node, T start, T end, Map<T, List<Long>> result) {
        int i = 0;
        // Проходим по всем ключам в узле
        while (i < node.keys.size()) {
            T currentKey = node.keys.get(i);

            // Если текущий ключ меньше начала диапазона, переходим к следующему ключу
            if (currentKey.compareTo(start) < 0) {
                i++;
            }
            // Если текущий ключ больше конца диапазона, завершаем поиск в этом узле
            else if (currentKey.compareTo(end) > 0) {
                break;
            }
            // Если ключ попадает в диапазон, добавляем его в результат
            else {
                // Если узел не является листом, рекурсивно ищем в дочернем узле
                if (!node.isLeaf) {
                    searchRangeHelper(node.children.get(i), start, end, result);
                }
                // Добавляем текущий ключ в результат
                result.put(node.getKeys().get(i), node.getPointers().get(i));
                i++;
            }
        }

        // Если узел не является листом, проверяем последний дочерний узел
        if (!node.isLeaf) {
            searchRangeHelper(node.children.get(i), start, end, result);
        }
    }
}