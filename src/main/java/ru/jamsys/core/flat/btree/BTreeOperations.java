package ru.jamsys.core.flat.btree;

import java.util.List;

/**
 * Базовый класс для операций с B-деревом.
 *
 * @param <T> Тип данных, хранящихся в дереве. Должен реализовывать интерфейс Comparable.
 */
class BTreeOperations<T extends Comparable<T>> {
    protected int t; // Минимальная степень дерева

    /**
     * Конструктор.
     *
     * @param t Минимальная степень дерева.
     */
    public BTreeOperations(int t) {
        this.t = t;
    }

    /**
     * Разделяет дочерний узел, если он переполнен.
     *
     * @param parent Родительский узел.
     */
    protected void splitChild(BTreeNode<T> parent, int index) {
        BTreeNode<T> child = parent.children.get(index);
        BTreeNode<T> newChild = new BTreeNode<>(child.isLeaf);

        T middleKey = child.keys.get(t - 1);
        List<Long> middlePointers = child.pointers.get(t - 1);
        parent.keys.add(index, middleKey);
        parent.pointers.add(index, middlePointers);
        newChild.keys.addAll(child.keys.subList(t, child.keys.size()));
        newChild.pointers.addAll(child.pointers.subList(t, child.pointers.size()));

        child.keys.subList(t - 1, child.keys.size()).clear();
        child.pointers.subList(t - 1, child.pointers.size()).clear();

        if (!child.isLeaf) {
            newChild.children.addAll(child.children.subList(t, child.children.size()));
            child.children.subList(t, child.children.size()).clear();
        }

        parent.children.add(index + 1, newChild);
    }

}
