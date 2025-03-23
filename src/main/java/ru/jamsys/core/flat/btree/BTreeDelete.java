package ru.jamsys.core.flat.btree;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс для операций удаления из B-дерева.
 *
 * @param <T> Тип данных, хранящихся в дереве. Должен реализовывать интерфейс Comparable.
 */

public class BTreeDelete<T extends Comparable<T>> {
    private final BTree<T> bTree;
    private final int t;

    public BTreeDelete(BTree<T> bTree, int t) {
        this.bTree = bTree;
        this.t = t;
    }

    public void delete(T key) {
        if (bTree.getRoot() == null) return;

        deleteRecursive(bTree.getRoot(), key);

        if (bTree.getRoot().keys.isEmpty()) {
            if (!bTree.getRoot().isLeaf()) {
                bTree.setRoot(bTree.getRoot().children.get(0));
            } else {
                bTree.setRoot(null);
            }
        }
    }

    private void deleteRecursive(BTreeNode<T> node, T key) {
        int idx = findKey(node, key);

        if (idx < node.keys.size() && node.keys.get(idx).compareTo(key) == 0) {
            if (node.isLeaf()) {
                node.keys.remove(idx);
                node.pointers.remove(idx);
            } else {
                deleteFromInternalNode(node, idx);
            }
        } else {
            if (node.isLeaf()) return;

            boolean isLastChild = (idx == node.keys.size());
            BTreeNode<T> child = node.children.get(idx);

            if (child.keys.size() < t) {
                fixChild(node, idx);
            }

            if (isLastChild && idx > node.keys.size()) {
                deleteRecursive(node.children.get(idx - 1), key);
            } else {
                deleteRecursive(node.children.get(idx), key);
            }
        }
    }

    private void deleteFromInternalNode(BTreeNode<T> node, int idx) {
        T key = node.keys.get(idx);
        System.out.println("Deleting key from internal node: " + key);
        BTreeNode<T> left = node.children.get(idx);
        BTreeNode<T> right = node.children.get(idx + 1);

        // 📌 ВСЕГДА сначала выбираем ПРЕЕМНИКА!
        if (right.keys.size() >= t) {
            T successor = getSuccessor(right);
            int successorIndex = right.keys.indexOf(successor);
            List<Long> successorPointers = new ArrayList<>(right.pointers.get(successorIndex));
            System.out.println("Replacing with successor: " + successor);
            node.keys.set(idx, successor);
            node.pointers.set(idx, successorPointers);
            deleteRecursive(right, successor);
        } else if (left.keys.size() >= t) {
            T predecessor = getPredecessor(left);
            int predecessorIndex = left.keys.indexOf(predecessor);
            List<Long> predecessorPointers = new ArrayList<>(left.pointers.get(predecessorIndex));
            System.out.println("Replacing with predecessor: " + predecessor);
            node.keys.set(idx, predecessor);
            node.pointers.set(idx, predecessorPointers);
            deleteRecursive(left, predecessor);
        } else {
            System.out.println("Merging nodes");
            mergeNodes(node, idx);
            deleteRecursive(left, key);
        }
    }

    private void deleteFromInternalNode2(BTreeNode<T> node, int idx) {
        T key = node.keys.get(idx);

        BTreeNode<T> left = node.children.get(idx);
        BTreeNode<T> right = node.children.get(idx + 1);

        if (left.keys.size() >= t) {
            T predecessor = getPredecessor(left);
            int predecessorIndex = left.keys.indexOf(predecessor);
            List<Long> predecessorPointers = new ArrayList<>(left.pointers.get(predecessorIndex));

            node.keys.set(idx, predecessor);
            node.pointers.set(idx, predecessorPointers);

            deleteRecursive(left, predecessor);
        } else if (right.keys.size() >= t) {
            T successor = getSuccessor(right);
            int successorIndex = right.keys.indexOf(successor);
            List<Long> successorPointers = new ArrayList<>(right.pointers.get(successorIndex));

            node.keys.set(idx, successor);
            node.pointers.set(idx, successorPointers);

            deleteRecursive(right, successor);
        } else {
            mergeNodes(node, idx);
            deleteRecursive(left, key);
        }
    }

    private T getPredecessor(BTreeNode<T> node) {
        while (!node.isLeaf()) {
            node = node.children.get(node.children.size() - 1);
        }
        return node.keys.get(node.keys.size() - 1);
    }

    private T getSuccessor(BTreeNode<T> node) {
        while (!node.isLeaf()) {
            node = node.children.get(0);
        }
        return node.keys.get(0);
    }

    private void fixChild(BTreeNode<T> parent, int idx) {
        if (idx > 0 && parent.children.get(idx - 1).keys.size() >= t) {
            borrowFromLeft(parent, idx);
        } else if (idx < parent.keys.size() && parent.children.get(idx + 1).keys.size() >= t) {
            borrowFromRight(parent, idx);
        } else {
            if (idx < parent.keys.size()) {
                mergeNodes(parent, idx);
            } else {
                mergeNodes(parent, idx - 1);
            }
        }
    }

    private void borrowFromLeft(BTreeNode<T> parent, int idx) {
        BTreeNode<T> child = parent.children.get(idx);
        BTreeNode<T> leftSibling = parent.children.get(idx - 1);

        child.keys.add(0, parent.keys.get(idx - 1));
        child.pointers.add(0, parent.pointers.get(idx - 1));

        if (!child.isLeaf()) {
            child.children.add(0, leftSibling.children.remove(leftSibling.children.size() - 1));
        }

        parent.keys.set(idx - 1, leftSibling.keys.remove(leftSibling.keys.size() - 1));
        parent.pointers.set(idx - 1, leftSibling.pointers.remove(leftSibling.pointers.size() - 1));
    }

    private void borrowFromRight(BTreeNode<T> parent, int idx) {
        BTreeNode<T> child = parent.children.get(idx);
        BTreeNode<T> rightSibling = parent.children.get(idx + 1);

        child.keys.add(parent.keys.get(idx));
        child.pointers.add(parent.pointers.get(idx));

        if (!child.isLeaf()) {
            child.children.add(rightSibling.children.remove(0));
        }

        parent.keys.set(idx, rightSibling.keys.remove(0));
        parent.pointers.set(idx, rightSibling.pointers.remove(0));
    }

    private void mergeNodes(BTreeNode<T> parent, int idx) {
        BTreeNode<T> left = parent.children.get(idx);
        BTreeNode<T> right = parent.children.get(idx + 1);

        left.keys.add(parent.keys.remove(idx));
        left.pointers.add(parent.pointers.remove(idx));

        left.keys.addAll(right.keys);
        left.pointers.addAll(right.pointers);

        left.children.addAll(right.children);

        parent.children.remove(idx + 1);
    }

    private int findKey(BTreeNode<T> node, T key) {
        int idx = 0;
        while (idx < node.keys.size() && node.keys.get(idx).compareTo(key) < 0) {
            idx++;
        }
        return idx;
    }
}






