package ru.jamsys.core.flat.btree;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Узел B-дерева.
 *
 * @param <T> Тип данных, хранящихся в узле. Должен реализовывать интерфейс Comparable.
 */
@JsonPropertyOrder({"leaf", "keys", "pointers", "children"})
@Getter
public class BTreeNode<T extends Comparable<T>> {

    List<T> keys; // Ключи в узле
    List<List<Long>> pointers = new ArrayList<>(); // Списки указателей на данные (например, rowid или адрес на диске)
    List<BTreeNode<T>> children; // Дочерние узлы
    boolean isLeaf; // Является ли узел листом

    /**
     * Конструктор узла.
     *
     * @param isLeaf true, если узел является листом.
     */
    public BTreeNode(boolean isLeaf) {
        this.keys = new ArrayList<>();
        this.children = new ArrayList<>();
        this.isLeaf = isLeaf;
    }
}