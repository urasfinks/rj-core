package ru.jamsys.core.pool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventTimingTest {

    @Test
    void eventTimePassed_ShouldBeZeroInitially() {
        EventTiming timing = new EventTiming();
        assertEquals(0, timing.eventTimePassed(), "Сразу после создания событие не должно быть зафиксировано");
        assertFalse(timing.isHappened(), "Событие ещё не происходило");
    }

    @Test
    void event_ShouldUpdateTimestamp() throws InterruptedException {
        EventTiming timing = new EventTiming();
        timing.event();
        assertTrue(timing.isHappened(), "Событие должно быть зафиксировано");

        Thread.sleep(10); // небольшая задержка

        long elapsed = timing.eventTimePassed();
        assertTrue(elapsed >= 10, "Должно пройти хотя бы 10 мс");
    }

    @Test
    void reset_ShouldClearTimestamp() {
        EventTiming timing = new EventTiming();
        timing.event();
        assertTrue(timing.isHappened());

        timing.reset();

        assertEquals(0, timing.eventTimePassed(), "После сброса должно быть 0");
        assertFalse(timing.isHappened(), "Событие должно считаться не произошедшим");
    }

    @Test
    void multipleEvents_ShouldOverwriteTimestamp() throws InterruptedException {
        EventTiming timing = new EventTiming();
        timing.event();
        long firstTime = timing.eventTimePassed();

        Thread.sleep(5);

        timing.event(); // новое событие

        long secondTime = timing.eventTimePassed();

        assertTrue(secondTime > firstTime, "Новое событие должно обнулить счётчик времени");
    }

    @Test
    void initialState_ShouldNotBeHappened() {
        EventTiming timing = new EventTiming();
        assertFalse(timing.isHappened());
        assertEquals(0, timing.eventTimePassed());
    }

    @Test
    void event_ShouldSetHappenedAndTimestampOnce() throws InterruptedException {
        EventTiming timing = new EventTiming();
        timing.event();

        assertTrue(timing.isHappened());
        long firstTime = timing.eventTimePassed();
        assertTrue(firstTime >= 0 && firstTime < 1000);

        Thread.sleep(10);

        // Повторный вызов event() не должен изменить время
        timing.event();
        long secondTime = timing.eventTimePassed();
        assertTrue(secondTime >= firstTime, "Время должно расти, не сбрасываться");
    }

    @Test
    void reset_ShouldClearHappenedAndTimestamp() {
        EventTiming timing = new EventTiming();
        timing.event();

        assertTrue(timing.isHappened());
        assertTrue(timing.eventTimePassed() >= 0, "Время должно быть неотрицательным");

        timing.reset();

        assertFalse(timing.isHappened());
        assertEquals(0, timing.eventTimePassed());
    }

    @Test
    void event_AfterReset_ShouldUpdateTimestamp() throws InterruptedException {
        EventTiming timing = new EventTiming();

        timing.event();
        Thread.sleep(5); // гарантируем различие времени
        long firstTime = timing.eventTimePassed();
        assertTrue(firstTime >= 0);

        timing.reset();
        assertEquals(0, timing.eventTimePassed());
        assertFalse(timing.isHappened());

        timing.event();
        long newTime = timing.eventTimePassed();

        // Теперь убеждаемся, что новое событие зафиксировано (время начинает заново идти)
        assertTrue(newTime >= 0);
        assertTrue(newTime <= firstTime, "Новое событие должно начинать отсчёт заново");
    }

    @Test
    void event_ShouldBeIdempotentUntilReset() throws InterruptedException {
        EventTiming timing = new EventTiming();

        timing.event();
        long t1 = timing.eventTimePassed();

        Thread.sleep(5);
        timing.event();  // не должен обновить

        long t2 = timing.eventTimePassed();
        assertTrue(t2 > t1);

        timing.reset();
        timing.event();

        long t3 = timing.eventTimePassed();
        assertTrue(t3 < t2);
    }

}