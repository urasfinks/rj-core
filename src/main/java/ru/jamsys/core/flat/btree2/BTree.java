package ru.jamsys.core.flat.btree2;

import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class BTree<T extends Comparable<T>> {
    private long rootId;
    private final int t;
    private final BTreeStorage<T> storage = new BTreeStorage<>();
    private final Map<Long, SoftReference<BTreeNode<T>>> cache = new ConcurrentHashMap<>();
    private long nodeIdCounter = 0;

    public BTree(int t) {
        this.t = t;
        BTreeNode<T> root = new BTreeNode<>(true, generateNodeId());
        storage.saveNode(root);
        this.rootId = root.nodeId;
    }

    private long generateNodeId() {
        return nodeIdCounter++;
    }

    private BTreeNode<T> getNode(long nodeId) {
        SoftReference<BTreeNode<T>> ref = cache.get(nodeId);
        BTreeNode<T> node = (ref != null) ? ref.get() : null;
        if (node == null) {
            node = storage.loadNode(nodeId);
            if (node != null) cache.put(nodeId, new SoftReference<>(node));
        }
        return node;
    }

    public void insert(T key, long pointer) {
        BTreeNode<T> root = getNode(rootId);
        if (root.keys.size() == (2 * t - 1)) {
            BTreeNode<T> newRoot = new BTreeNode<>(false, generateNodeId());
            newRoot.children.add(root.nodeId);
            splitChild(newRoot, 0);
            rootId = newRoot.nodeId;
            storage.saveNode(newRoot);
        }
        insertNonFull(getNode(rootId), key, pointer);
    }

    private void insertNonFull(BTreeNode<T> node, T key, long pointer) {
        int i = node.keys.size() - 1;
        if (node.isLeaf) {
            while (i >= 0 && key.compareTo(node.keys.get(i)) < 0) i--;
            node.keys.add(i + 1, key);
            node.pointers.add(i + 1, pointer);
            storage.saveNode(node);
        } else {
            while (i >= 0 && key.compareTo(node.keys.get(i)) < 0) i--;
            i++;
            BTreeNode<T> child = getNode(node.children.get(i));
            if (child.keys.size() == (2 * t - 1)) {
                splitChild(node, i);
                if (key.compareTo(node.keys.get(i)) > 0) i++;
            }
            insertNonFull(getNode(node.children.get(i)), key, pointer);
        }
    }

    private void splitChild(BTreeNode<T> parent, int index) {
        BTreeNode<T> child = getNode(parent.children.get(index));
        BTreeNode<T> newChild = new BTreeNode<>(child.isLeaf, generateNodeId());

        parent.keys.add(index, child.keys.get(t - 1));
        parent.children.add(index + 1, newChild.nodeId);

        newChild.keys.addAll(child.keys.subList(t, 2 * t - 1));
        child.keys.subList(t - 1, 2 * t - 1).clear();

        if (!child.isLeaf) {
            newChild.children.addAll(child.children.subList(t, 2 * t));
            child.children.subList(t, 2 * t).clear();
        }
        storage.saveNode(child);
        storage.saveNode(newChild);
        storage.saveNode(parent);
    }

    public Long search(T key) {
        return searchRecursive(getNode(rootId), key);
    }

    private Long searchRecursive(BTreeNode<T> node, T key) {
        int i = 0;
        while (i < node.keys.size() && key.compareTo(node.keys.get(i)) > 0) {
            i++;
        }
        if (i < node.keys.size() && key.compareTo(node.keys.get(i)) == 0) {
            return node.pointers.get(i);
        }
        if (node.isLeaf) {
            return null;
        }
        return searchRecursive(getNode(node.children.get(i)), key);
    }

}
