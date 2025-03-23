package ru.jamsys.core.flat.btree;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BTreeTest {

    @Test
    void test() {
        BTree<Integer> bTree = new BTree<>(3); // Создаем B-дерево с минимальной степенью 3

        bTree.insert(10, 100);
        bTree.insert(20, 101);
        bTree.insert(5, 102);
        bTree.insert(6, 108);
        bTree.insert(12, 104);
        bTree.insert(30, 105);
        bTree.insert(7, 106);
        bTree.insert(17, 107);

        //UtilLog.printInfo(BTreeTest.class, bTree.getRoot());

        Map<Integer, List<Long>> integerListMap = bTree.searchRange(6, 20);
        Assertions.assertEquals(6, integerListMap.size());
        Assertions.assertNotNull(bTree.search(6));
        Assertions.assertNull(bTree.search(4));

        bTree.delete(6);
        Assertions.assertNull(bTree.search(6));
    }

    @Test
    void testInsertAndStructure() {
        BTree<Integer> bTree = new BTree<>(3);

        // Вставляем элементы
        bTree.insert(10, 100);
        bTree.insert(20, 101);
        bTree.insert(5, 102);
        bTree.insert(6, 103);
        bTree.insert(12, 104);
        bTree.insert(30, 105);
        bTree.insert(7, 106);
        bTree.insert(17, 107);

        // Проверяем корень
        assertEquals(10, bTree.getRoot().keys.getFirst());

        // Проверяем, что левый ребенок содержит [5, 6, 7]
        assertTrue(bTree.getRoot().children.get(0).keys.containsAll(List.of(5, 6, 7)));

        // Проверяем, что правый ребенок содержит [12, 17, 20, 30]
        assertTrue(bTree.getRoot().children.get(1).keys.containsAll(List.of(12, 17, 20, 30)));
    }

    @Test
    void testSearch() {
        BTree<Integer> bTree = new BTree<>(3);

        // Вставляем элементы
        bTree.insert(10, 100);
        bTree.insert(20, 101);
        bTree.insert(5, 102);

        // Проверяем поиск существующих ключей
        assertNotNull(bTree.search(10));
        assertNotNull(bTree.search(20));
        assertNotNull(bTree.search(5));

        // Проверяем поиск несуществующего ключа
        assertNull(bTree.search(99));
    }

    @Test
    void testDeleteLeaf() {
        BTree<Integer> bTree = new BTree<>(3);

        bTree.insert(10, 100);
        bTree.insert(20, 101);
        bTree.insert(5, 102);
        bTree.insert(6, 103);
        bTree.insert(12, 104);

        // Удаляем ключ из листа
        bTree.delete(6);
        assertNull(bTree.search(6));

        // Удаляем еще один ключ из листа
        bTree.delete(5);
        assertNull(bTree.search(5));
    }

    @Test
    void testDeleteInternalNode() {
        BTree<Integer> bTree = new BTree<>(3);

        bTree.insert(10, 100);
        bTree.insert(20, 101);
        bTree.insert(5, 102);
        bTree.insert(6, 103);
        bTree.insert(12, 104);
        bTree.insert(30, 105);
        bTree.insert(7, 106);
        bTree.insert(17, 107);

        // Удаляем корневой узел, который заставит дерево перестроиться
        bTree.delete(10);
        assertNull(bTree.search(10));

        // Проверяем, что 12 теперь корень
        assertEquals(12, bTree.getRoot().keys.getFirst());
    }

    @Test
    void testInsertDuplicate() {
        BTree<Integer> bTree = new BTree<>(3);

        bTree.insert(10, 100);
        bTree.insert(10, 101); // Дубликат

        // Убедимся, что ключ 10 есть, но без дубликатов
        assertEquals(1, bTree.getRoot().keys.stream().filter(k -> k == 10).count());
    }

    @Test
    void testDeleteFromEmptyTree() {
        BTree<Integer> bTree = new BTree<>(3);
        assertDoesNotThrow(() -> bTree.delete(10)); // Удаление из пустого дерева не должно вызывать исключений
    }

    @Test
    void testSearchEmptyTree() {
        BTree<Integer> bTree = new BTree<>(3);
        assertNull(bTree.search(10)); // Поиск в пустом дереве должен вернуть null
    }

    @Test
    void testLargeInsertion() {
        BTree<Integer> bTree = new BTree<>(3);

        for (int i = 1; i <= 1000; i++) {
            bTree.insert(i, i);
        }

        for (int i = 1; i <= 1000; i++) {
            assertNotNull(bTree.search(i));
        }
    }

    @Test
    void testLargeDeletion() {
        BTree<Integer> bTree = new BTree<>(3);

        for (int i = 1; i <= 1000; i++) {
            bTree.insert(i, i);
        }

        for (int i = 1; i <= 500; i++) {
            bTree.delete(i);
        }

        for (int i = 1; i <= 500; i++) {
            assertNull(bTree.search(i)); // Первые 500 удалены
        }

        for (int i = 501; i <= 1000; i++) {
            assertNotNull(bTree.search(i)); // Остальные должны быть найдены
        }
    }
}