package ru.jamsys.core.flat.btree;

import ru.jamsys.core.flat.util.UtilLog;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Класс для операций вставки в B-дерево.
 *
 * @param <T> Тип данных, хранящихся в дереве. Должен реализовывать интерфейс Comparable.
 */
class BTreeInsert<T extends Comparable<T>> extends BTreeOperations<T> {


    /**
     * Конструктор.
     *
     * @param t       Минимальная степень дерева.
     * @param setRoot Функция для обновления корня.
     */
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
        System.out.println("\n🚀 Вставка ключа: " + key);

        if (btree.getRoot().keys.size() == (2 * t - 1)) {  // Если корень заполнен - создаем новый
            System.out.println("⚠️ Корень переполнен, создаем новый корень");

            BTreeNode<T> newRoot = new BTreeNode<>(false);
            newRoot.children.add(btree.getRoot());
            splitChild(newRoot, 0);

            btree.setRoot(newRoot);
              // Новый корень
            System.out.println("✅ Новый корень создан: " + btree.getRoot().keys);
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
        System.out.println("🔹 Вызван insertNonFull для узла: " + node.keys + " с ключом: " + key);

        int i = node.keys.size() - 1;

        if (node.isLeaf) {  // Если лист, просто вставляем
            System.out.println("  🔹 Узел " + node.keys + " - это лист. Ищем место для вставки...");

            while (i >= 0 && key.compareTo(node.keys.get(i)) < 0) {
                i--;
            }

            node.keys.add(i + 1, key);
            node.pointers.add(i + 1, new ArrayList<>(List.of(pointer)));
            System.out.println("  ✅ Вставили ключ " + key + " в лист. Итоговый узел: " + node.keys);
        } else {  // Если не лист, ищем нужного ребенка
            while (i >= 0 && key.compareTo(node.keys.get(i)) < 0) {
                i--;
            }
            i++;

            System.out.println("  🔹 Будем вставлять ключ " + key + " в дочерний узел " + node.children.get(i).keys);

            if (node.children.get(i).keys.size() == (2 * t - 1)) {  // Если ребенок переполнен, разделяем его
                System.out.println("  ⚠️ Дочерний узел " + node.children.get(i).keys + " ПЕРЕПОЛНИЛСЯ! Разделяем...");
                splitChild(node, i);

                if (key.compareTo(node.keys.get(i)) > 0) {
                    i++;
                }
            }

            insertNonFull(node.children.get(i), key, pointer);
        }
    }













}