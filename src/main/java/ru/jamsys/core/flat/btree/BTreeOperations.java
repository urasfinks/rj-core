package ru.jamsys.core.flat.btree;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Базовый класс для операций с B-деревом.
 *
 * @param <T> Тип данных, хранящихся в дереве. Должен реализовывать интерфейс Comparable.
 */
class BTreeOperations<T extends Comparable<T>> {
    protected int t; // Минимальная степень дерева
    protected ReentrantLock lock = new ReentrantLock(); // Блокировка для многопоточности

    /**
     * Конструктор.
     *
     * @param t Минимальная степень дерева.
     */
    public BTreeOperations(int t) {
        this.t = t;
    }

    /**
     * Находит индекс ключа в узле.
     *
     * @param node Узел, в котором ищем ключ.
     * @param key  Ключ для поиска.
     * @return Индекс ключа.
     */
    protected int findKeyIndex(BTreeNode<T> node, T key) {
        int i = 0;
        while (i < node.keys.size() && key.compareTo(node.keys.get(i)) > 0) {
            i++;
        }
        return i;
    }

    /**
     * Разделяет дочерний узел, если он переполнен.
     *
     * @param parent Родительский узел.
     */
    protected void splitChild(BTreeNode<T> parent, int index) {
        System.out.println("\n🔻 Вызван splitChild для родителя: " + parent.keys + ", индекс: " + index);

        BTreeNode<T> child = parent.children.get(index);
        BTreeNode<T> newChild = new BTreeNode<>(child.isLeaf);

        System.out.println("  🔻 Исходный узел ДО разделения: " + child.keys);

        T middleKey = child.keys.get(t - 1);
        List<Long> middlePointers = child.pointers.get(t - 1);
        System.out.println("  🔻 Средний ключ для поднятия: " + middleKey);

        parent.keys.add(index, middleKey);
        parent.pointers.add(index, middlePointers);
        System.out.println("  ✅ Родитель ПОСЛЕ вставки среднего ключа: " + parent.keys);

        newChild.keys.addAll(child.keys.subList(t, child.keys.size()));
        newChild.pointers.addAll(child.pointers.subList(t, child.pointers.size()));

        child.keys.subList(t - 1, child.keys.size()).clear();
        child.pointers.subList(t - 1, child.pointers.size()).clear();

        if (!child.isLeaf) {
            newChild.children.addAll(child.children.subList(t, child.children.size()));
            child.children.subList(t, child.children.size()).clear();
        }

        parent.children.add(index + 1, newChild);
        System.out.println("  ✅ Новый дочерний узел создан: " + newChild.keys);
        System.out.println("  ✅ Состояние родительского узла после splitChild: " + parent.keys);
    }


}
