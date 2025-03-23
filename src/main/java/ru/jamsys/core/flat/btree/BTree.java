package ru.jamsys.core.flat.btree;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * Основной класс для управления B-деревом.
 *
 * @param <T> Тип данных, хранящихся в дереве. Должен реализовывать интерфейс Comparable.
 */
public class BTree<T extends Comparable<T>> {

    @Getter
    @Setter
    private BTreeNode<T> root;

    private final BTreeInsert<T> inserter;

    private final BTreeDelete<T> deleter;

    private final BTreeSearch<T> searcher;

    private final BTreeRangeSearch<T> rangeSearcher;

    /**
     * Конструктор.
     *
     * @param t Минимальная степень дерева.
     */

    final int t;

    public BTree(int t) {
        this.t = t;
        this.root = new BTreeNode<>(true);
        this.inserter = new BTreeInsert<>(t, this);
        this.deleter = new BTreeDelete<>(this, t);
        this.searcher = new BTreeSearch<>(t);
        this.rangeSearcher = new BTreeRangeSearch<>(t);
    }

    /**
     * Ищет ключ в дереве.
     *
     * @param key Ключ для поиска.
     * @return true, если ключ найден.
     */
    public Map<T, List<Long>> search(T key) {
        return searcher.search(root, key);
    }

    /**
     * Вставляет ключ в дерево.
     *
     * @param key Ключ для вставки.
     */
    public void insert(T key, long pointer) {
        BTreeNode<T> node = findNodeWithKey(root, key);
        if (node != null) {
            int index = node.keys.indexOf(key);
            if (index != -1) {
                System.out.println("🔄 Обновляем указатель для ключа " + key);
                node.pointers.get(index).add(pointer); // Добавляем новый pointer
                return;
            }
        }

        System.out.println("\n🚀 Вставка ключа: " + key);

        if (root.keys.size() == (2 * t - 1)) {
            System.out.println("⚠️ Корень переполнен, создаем новый корень");
            BTreeNode<T> newRoot = new BTreeNode<>(false);
            newRoot.children.add(root);
            inserter.splitChild(newRoot, 0);
            root = newRoot;
            System.out.println("✅ Новый корень создан: " + root.keys);
        } else {
            System.out.println("✅ Корень не переполнен. Вставляем в существующий.");
        }

        inserter.insertNonFull(root, key, pointer);
    }



    /**
     * Удаляет ключ из дерева.
     *
     * @param key Ключ для удаления.
     */
    public void delete(T key) {
        deleter.delete(key);
    }

    /**
     * Ищет все ключи в диапазоне [start, end].
     *
     * @param start Начало диапазона (включительно).
     * @param end   Конец диапазона (включительно).
     * @return Список ключей, попадающих в диапазон.
     */
    public Map<T, List<Long>> searchRange(T start, T end) {
        return rangeSearcher.searchRange(root, start, end);
    }

    private BTreeNode<T> findNodeWithKey(BTreeNode<T> node, T key) {
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
        return findNodeWithKey(node.children.get(i), key);
    }
}
