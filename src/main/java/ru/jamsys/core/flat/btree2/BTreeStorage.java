package ru.jamsys.core.flat.btree2;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class BTreeStorage<T extends Comparable<T>> {
    private static final String DATA_DIR = "btree_data/";
    private final Map<Long, ReentrantReadWriteLock> locks = new ConcurrentHashMap<>();

    public BTreeStorage() {
        new File(DATA_DIR).mkdirs();
    }

    public void saveNode(BTreeNode<T> node) {
        locks.computeIfAbsent(node.nodeId, k -> new ReentrantReadWriteLock()).writeLock().lock();
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DATA_DIR + node.nodeId))) {
            out.writeObject(node);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            locks.get(node.nodeId).writeLock().unlock();
        }
    }

    public BTreeNode<T> loadNode(long nodeId) {
        locks.computeIfAbsent(nodeId, k -> new ReentrantReadWriteLock()).readLock().lock();
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(DATA_DIR + nodeId))) {
            return (BTreeNode<T>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        } finally {
            locks.get(nodeId).readLock().unlock();
        }
    }

    public void deleteNode(long nodeId) {
        locks.computeIfAbsent(nodeId, k -> new ReentrantReadWriteLock()).writeLock().lock();
        try {
            File file = new File(DATA_DIR + nodeId);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            locks.get(nodeId).writeLock().unlock();
        }
    }
}
