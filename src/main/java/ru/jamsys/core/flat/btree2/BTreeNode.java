package ru.jamsys.core.flat.btree2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

class BTreeNode<T extends Comparable<T>> implements Serializable {
    List<T> keys;
    List<Long> pointers;
    List<Long> children;
    boolean isLeaf;
    long nodeId;

    public BTreeNode(boolean isLeaf, long nodeId) {
        this.keys = new ArrayList<>();
        this.pointers = new ArrayList<>();
        this.children = new ArrayList<>();
        this.isLeaf = isLeaf;
        this.nodeId = nodeId;
    }
}
