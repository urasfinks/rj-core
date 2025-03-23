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

    @Getter
    private final BTreeSearch<T> searcher;

    private final BTreeRangeSearch<T> rangeSearcher;

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
        inserter.insert( key, pointer);
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

}
