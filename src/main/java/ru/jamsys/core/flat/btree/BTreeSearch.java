package ru.jamsys.core.flat.btree;

import ru.jamsys.core.extension.builder.HashMapBuilder;

import java.util.List;
import java.util.Map;

/**
 * Класс для операций поиска в B-дереве.
 *
 * @param <T> Тип данных, хранящихся в дереве. Должен реализовывать интерфейс Comparable.
 */
class BTreeSearch<T extends Comparable<T>> extends BTreeOperations<T> {

    /**
     * Конструктор.
     *
     * @param t Минимальная степень дерева.
     */
    public BTreeSearch(int t) {
        super(t);
    }

    /**
     * Ищет ключ в дереве.
     *
     * @param node Узел, в котором ищем ключ.
     * @param key  Ключ для поиска.
     * @return BTreeNode<T>, если ключ найден.
     */
    public Map<T, List<Long>> search(BTreeNode<T> node, T key) {
        int i = 0;
        while (i < node.keys.size() && key.compareTo(node.keys.get(i)) > 0) {
            i++;
        }
        if (i < node.keys.size() && key.compareTo(node.keys.get(i)) == 0) {
            return new HashMapBuilder<T, List<Long>>().append(
                    node.keys.get(i),
                    node.getPointers().get(i)
            );
        }
        if (node.isLeaf) {
            return null;
        }
        return search(node.children.get(i), key);
    }

    public BTreeNode<T> nativeSearch(BTreeNode<T> node, T key) {
        if (node == null) return null;

        // Ищем ключ в текущем узле
        int i = 0;
        while (i < node.keys.size() && key.compareTo(node.keys.get(i)) > 0) {
            i++;
        }

        // Если ключ найден в этом узле - возвращаем его
        if (i < node.keys.size() && key.equals(node.keys.get(i))) {
            return node;
        }

        // Если дошли до листа - ключа точно нет
        if (node.isLeaf) {
            return null;
        }

        // Рекурсивно ищем в подходящем дочернем узле
        return nativeSearch(node.children.get(i), key);
    }

}
