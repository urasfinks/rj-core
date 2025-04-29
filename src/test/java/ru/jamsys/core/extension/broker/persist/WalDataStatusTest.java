package ru.jamsys.core.extension.broker.persist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class WalDataStatusTest {

//    private WalDataStatus walDataStatus;
//    private final short TEST_GROUP = 1;
//    private final long TEST_ID = 100L;
//
//    @BeforeEach
//    void setUp() {
//        walDataStatus = new WalDataStatus();
//    }
//
//    // ----- Тесты для subscribe() -----
//
//    @Test
//    void subscribe_shouldAddNewEntryToGroup() {
//        walDataStatus.subscribe(TEST_ID, List.of(TEST_GROUP));
//
//        assertTrue(walDataStatus.getFirst(TEST_GROUP, 1).contains(TEST_ID));
//    }
//
//    @Test
//    void subscribe_shouldAddEntryToMultipleGroups() {
//        short secondGroup = 2;
//        walDataStatus.subscribe(TEST_ID, List.of(TEST_GROUP, secondGroup));
//
//        assertEquals(1, walDataStatus.getFirst(TEST_GROUP, 1).size());
//        assertEquals(1, walDataStatus.getFirst(secondGroup, 1).size());
//    }
//
//    // ----- Тесты для unsubscribe() -----
//
//    @Test
//    void unsubscribe_shouldRemoveEntryFromGroup() {
//        walDataStatus.subscribe(TEST_ID, List.of(TEST_GROUP));
//        walDataStatus.unsubscribe(TEST_ID, TEST_GROUP);
//
//        assertTrue(walDataStatus.getFirst(TEST_GROUP, 1).isEmpty());
//    }
//
//    @Test
//    void unsubscribe_shouldDoNothingForNonExistentGroup() {
//        assertDoesNotThrow(() -> walDataStatus.unsubscribe(TEST_ID, (short) 999));
//    }
//
//    // ----- Тесты для getFirst() -----
//
//    @Test
//    void getFirst_shouldReturnEmptyListForEmptyGroup() {
//        assertTrue(walDataStatus.getFirst(TEST_GROUP, 1).isEmpty());
//    }
//
//    @Test
//    void getFirst_shouldReturnUnprocessedEntries() {
//        walDataStatus.subscribe(TEST_ID, List.of(TEST_GROUP));
//
//        List<Long> result = walDataStatus.getFirst(TEST_GROUP, 1);
//
//        assertEquals(1, result.size());
//        assertEquals(TEST_ID, result.getFirst());
//    }
//
//    @Test
//    void getFirst_shouldRespectSizeLimit() {
//        walDataStatus.subscribe(1L, List.of(TEST_GROUP));
//        walDataStatus.subscribe(2L, List.of(TEST_GROUP));
//
//        assertEquals(1, walDataStatus.getFirst(TEST_GROUP, 1).size());
//    }
//
//    @Test
//    void getFirst_shouldNotReturnRecentlyProcessedEntries() {
//        walDataStatus.subscribe(TEST_ID, List.of(TEST_GROUP));
//        walDataStatus.getFirst(TEST_GROUP, 1); // Обрабатываем элемент
//
//        assertTrue(walDataStatus.getFirst(TEST_GROUP, 1).isEmpty());
//    }
//
//    @Test
//    void getFirst_shouldReturnEntriesAfterRetryTimeout() throws InterruptedException {
//        walDataStatus.setTimeRetryMs(100); // Устанавливаем маленький таймаут
//        walDataStatus.subscribe(TEST_ID, List.of(TEST_GROUP));
//        walDataStatus.getFirst(TEST_GROUP, 1); // Первая обработка
//
//        Thread.sleep(200); // Ждем больше чем timeRetryMs
//        assertFalse(walDataStatus.getFirst(TEST_GROUP, 1).isEmpty());
//    }
//
//    // ----- Тесты для getLast() -----
//
//    @Test
//    void getLast_shouldReturnEntriesInReverseOrder() {
//        walDataStatus.subscribe(1L, List.of(TEST_GROUP));
//        walDataStatus.subscribe(2L, List.of(TEST_GROUP));
//
//        List<Long> result = walDataStatus.getLast(TEST_GROUP, 2);
//
//        assertEquals(2, result.get(0)); // Последний элемент первый в результате
//        assertEquals(1, result.get(1));
//    }
//
//    // ----- Тесты для блокировок -----
//
//    @Test
//    void concurrentAccess_shouldNotLoseEntries() throws InterruptedException {
//        int threadCount = 10;
//        int entriesPerThread = 100;
//        AtomicInteger counter = new AtomicInteger(0);
//        Runnable writer = () -> {
//            for (long i = 0; i < entriesPerThread; i++) {
//                walDataStatus.subscribe(counter.getAndIncrement(), List.of(TEST_GROUP));
//            }
//        };
//
//        Thread[] threads = new Thread[threadCount];
//        for (int i = 0; i < threadCount; i++) {
//            threads[i] = new Thread(writer);
//            threads[i].start();
//        }
//
//        for (Thread t : threads) {
//            t.join();
//        }
//
//        assertEquals(threadCount * entriesPerThread,
//                walDataStatus.getFirst(TEST_GROUP, threadCount * entriesPerThread).size());
//    }
//
//    // ----- Тесты для граничных случаев -----
//
//    @Test
//    void getFirst_withZeroSize_shouldReturnEmptyList() {
//        walDataStatus.subscribe(TEST_ID, List.of(TEST_GROUP));
//        assertTrue(walDataStatus.getFirst(TEST_GROUP, 0).isEmpty());
//    }
//
//    @Test
//    void getFirst_withNegativeSize_shouldReturnEmptyList() {
//        walDataStatus.subscribe(TEST_ID, List.of(TEST_GROUP));
//        assertTrue(walDataStatus.getFirst(TEST_GROUP, -1).isEmpty());
//    }
//
//    @Test
//    void subscribe_withEmptyGroupList_shouldDoNothing() {
//        assertDoesNotThrow(() -> walDataStatus.subscribe(TEST_ID, List.of()));
//    }
//
//    // ----- Тесты для timeRetryMs -----
//
//    @Test
//    void setTimeRetryMs_shouldAffectProcessingDelay() {
//        walDataStatus.setTimeRetryMs(5000);
//        walDataStatus.subscribe(TEST_ID, List.of(TEST_GROUP));
//        walDataStatus.getFirst(TEST_GROUP, 1); // Обработка
//
//        assertTrue(walDataStatus.getFirst(TEST_GROUP, 1).isEmpty()); // Должно быть пусто (таймаут 5 сек)
//        assertFalse(walDataStatus.getFirst(TEST_GROUP, 1, System.currentTimeMillis() + 5000).isEmpty()); // Должно быть пусто (таймаут 5 сек)
//    }
//
//    @Test
//    void testTiming() {
//        long time = System.currentTimeMillis();
//        for (int i = 0; i < 400_000; i++) {
//            walDataStatus.subscribe(TEST_ID, List.of(TEST_GROUP));
//        }
//    }

}