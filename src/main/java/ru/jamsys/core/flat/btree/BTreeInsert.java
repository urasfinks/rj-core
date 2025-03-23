package ru.jamsys.core.flat.btree;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс для операций вставки в B-дерево.
 *
 * @param <T> Тип данных, хранящихся в дереве. Должен реализовывать интерфейс Comparable.
 */
class BTreeInsert<T extends Comparable<T>> extends BTreeOperations<T> {

    final BTree<T> btree;
    public BTreeInsert(int t, BTree<T> btree) {
        super(t);
        this.btree = btree;
    }

    /**
     * Вставляет ключ в дерево.
     * Если узел переполнен, он разделяется, и ключ вставляется в соответствующий дочерний узел.
     *
     *
     * @param key     Ключ для вставки.
     * @param pointer Указатель на данные.
     */

    public void insert(T key, Long pointer) {
        BTreeNode<T> node = btree.getSearcher().nativeSearch(btree.getRoot(), key);
        if (node != null) {
            int index = node.keys.indexOf(key);
            if (index != -1) {
                node.pointers.get(index).add(pointer); // Добавляем новый pointer
                return;
            }
        }
        if (btree.getRoot().keys.size() == (2 * t - 1)) {
            BTreeNode<T> newRoot = new BTreeNode<>(false);
            newRoot.children.add(btree.getRoot());
            splitChild(newRoot, 0);
            btree.setRoot(newRoot);
        }
        insertNonFull(btree.getRoot(), key, pointer);
    }

    /**
     * Вставляет ключ и указатель в неполный узел.
     *
     * @param node    Узел, в который вставляем ключ.
     * @param key     Ключ для вставки.
     * @param pointer Указатель на данные.
     */
    public  void insertNonFull(BTreeNode<T> node, T key, Long pointer) {
        int i = node.keys.size() - 1;
        if (node.isLeaf) {  // Если лист, просто вставляем
            while (i >= 0 && key.compareTo(node.keys.get(i)) < 0) {
                i--;
            }
            node.keys.add(i + 1, key);
            node.pointers.add(i + 1, new ArrayList<>(List.of(pointer)));
        } else {  // Если не лист, ищем нужного ребенка
            while (i >= 0 && key.compareTo(node.keys.get(i)) < 0) {
                i--;
            }
            i++;
            if (node.children.get(i).keys.size() == (2 * t - 1)) {  // Если ребенок переполнен, разделяем его
                splitChild(node, i);
                if (key.compareTo(node.keys.get(i)) > 0) {
                    i++;
                }
            }
            insertNonFull(node.children.get(i), key, pointer);
        }
    }

}