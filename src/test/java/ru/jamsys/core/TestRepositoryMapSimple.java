package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.RepositoryMap;

import java.util.HashMap;
import java.util.Map;

// IO time: 11ms
// COMPUTE time: 12ms

public class TestRepositoryMapSimple {

    public static class P implements RepositoryMap<String, Object> {

        Map<String, Object> map = new HashMap<>();

        @Override
        public Map<String, Object> getRepositoryMap() {
            return map;
        }
    }

    @Test
    void test() {
        P p = new P();

        Assertions.assertEquals("y", p.setRepositoryMap("x", "y"));
        // Решил, что репозиторий надо перезаписывать, а то прям наелся говна из-за этого
        // Назначаю suip и в случаи ошибки надо его занулить, что бы он в шаблон не уходил
        // И всё поехали условные конструкции при передачи в шаблон с проверкой наличия ошибки
        // и чем больше логики - тем сложнее, поэтому в жопу - надо переопределить - переопределяем
        // + я сделал для Promise.setDebug(true) где логирую все изменения репозитория
        Assertions.assertEquals("y2", p.setRepositoryMap("x", "y2"));
        Assertions.assertEquals("y2", p.getRepositoryMap(String.class, "x", ""));
        Assertions.assertEquals("def", p.getRepositoryMap(String.class, "x1", "def"));
        Assertions.assertEquals("{x=y2}", p.getRepositoryMap().toString());

    }
}
