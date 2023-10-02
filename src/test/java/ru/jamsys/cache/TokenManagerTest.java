package ru.jamsys.cache;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TokenManagerTest {

    @Test
    void add() {
        TokenManager<Integer, String> tokenManager = new TokenManager<>();

        tokenManager.add(1234, "Hello world", 100);
        Assertions.assertEquals(1, tokenManager.get().size(), "#1");

        tokenManager.add(12345, "Hello world", 100);
        Assertions.assertEquals(2, tokenManager.get().size(), "#2");

        Assertions.assertFalse(tokenManager.add(12345, "Hello world", 100), "#3");

        tokenManager.add(123456, "Hello world", 100);
        Assertions.assertEquals(3, tokenManager.get().size(), "#4");
        try {
            Thread.sleep(200);
        } catch (Exception e) {
            e.printStackTrace();
        }
        tokenManager.add(123456, "Hello world", 100);
        Assertions.assertEquals(1, tokenManager.get().size(), "#5");
    }
}