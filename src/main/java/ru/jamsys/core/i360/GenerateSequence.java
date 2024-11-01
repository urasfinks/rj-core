package ru.jamsys.core.i360;

import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.i360.entity.Entity;
import ru.jamsys.core.i360.entity.adapter.Sequence;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GenerateSequence {
    public static void generate(List<Context> listContext) {
        Map<List<Entity>, AtomicInteger> x = new HashMap<>();
        listContext.forEach(context -> {
            List<List<Entity>> evclid = evclid(context);
            evclid.forEach(list -> {
                AtomicInteger atomicInteger = x.computeIfAbsent(list, _ -> new AtomicInteger());
                atomicInteger.incrementAndGet();
            });
        });

        System.out.println(UtilJson.toStringPretty(x, "[]"));
        ArrayList<List<Entity>> lists = new ArrayList<>(x.keySet());
        Map<List<Entity>, List<List<Entity>>> seqSource = new HashMap<>();
        for (int i = 0; i < lists.size(); i++) {
            for (List<Entity> list : lists) {
                List<Entity> first = lists.get(i);
                Integer is = removeContains(first, list);
                if (is != null) {
                    List<List<Entity>> lists1 = seqSource.computeIfAbsent(list, _ -> new ArrayList<>());
                    lists1.add(first);
                }
            }
        }
        // Если ключ встречается в подмножестве существующего ключа - удалим ключ
        List<List<Entity>> lists1 = new ArrayList<>(seqSource.keySet());
        lists1.forEach(key -> {
            ArrayList<List<Entity>> lists2 = new ArrayList<>(seqSource.keySet());
            for (List<Entity> fv : lists2) {
                if (seqSource.get(fv) != null && (
                        (seqSource.get(fv).contains(key) && !fv.equals(key))
                                || (seqSource.get(fv).size() == 1 && fv.equals(key))
                )) {
                    seqSource.remove(key);
                }
            }
        });
        Map<List<Entity>, Sequence> seq = new HashMap<>();
        seqSource.forEach((key, value) -> {
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for (List<Entity> entities : value) {
                if (max < entities.size()) {
                    max = entities.size();
                }
                if (min > entities.size()) {
                    min = entities.size();
                }
            }
            Sequence sequence = new Sequence(new HashMapBuilder<String, Object>()
                    .append("entity", key)
                    .append("min", min + "")
                    .append("max", max + ""),
                    null
            );
            seq.put(key, sequence);
//            System.out.println(key);
//            System.out.println("min: " + min + "; max: " + max);

        });
        System.out.println(UtilJson.toStringPretty(seq, "[]"));
    }

    public static Integer removeContains(List<Entity> l1, List<Entity> l2) {
        List<Entity> lc1 = new ArrayList<>(l1);
        int c = 0;
        while (true) {
            if (lc1.isEmpty()) {
                return c;
            }
            c++;
            for (Entity e2 : l2) {
                if (lc1.isEmpty()) {
                    return null;
                }
                if (lc1.getFirst().equals(e2)) {
                    lc1.remove(e2);
                } else {
                    return null;
                }
            }
        }
    }

    public static List<List<Entity>> evclid(Context context) {
        List<Entity> listEntity = context.getListEntity();
        //System.out.println("> " + listEntity);
        /*
         * 0 0 9
         * --
         * 0
         * 0 0
         * 0 0 9
         * 0
         * 0 9
         * 9
         * */
        List<List<Entity>> listResult = new ArrayList<>();
        for (int i = 0; i < listEntity.size(); i++) {
            for (int j = i; j < listEntity.size(); j++) {
                listResult.add(copy(listEntity, i, j));
            }
        }
        return listResult;
    }

    public static List<Entity> copy(List<Entity> listEntity, int start, int end) {
        List<Entity> list = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            list.add(listEntity.get(i));
        }
        //System.out.println("copy " + start + " - " + end + " = " + list);
        return list;
    }

    @SuppressWarnings("unchecked")
    public static <R extends Collection<?>> R cartesian(Supplier nCol, Collection<?>... cols) {
        // проверка supplier не есть null
        if (nCol == null) return null;
        return (R) Arrays.stream(cols)
                // ненулевые и непустые коллекции
                .filter(col -> col != null && !col.isEmpty())
                // представить каждый элемент коллекции как одноэлементную коллекцию
                .map(col -> (Collection<Collection<?>>) col.stream()
                        .map(e -> Stream.of(e).collect(Collectors.toCollection(nCol)))
                        .collect(Collectors.toCollection(nCol)))
                // суммирование пар вложенных коллекций
                .reduce((col1, col2) -> (Collection<Collection<?>>) col1.stream()
                        // комбинации вложенных коллекций
                        .flatMap(inner1 -> col2.stream()
                                // объединить в одну коллекцию
                                .map(inner2 -> Stream.of(inner1, inner2)
                                        .flatMap(Collection::stream)
                                        .collect(Collectors.toCollection(nCol))))
                        // коллекция комбинаций
                        .collect(Collectors.toCollection(nCol)))
                // иначе пустая коллекция
                .orElse((Collection<Collection<?>>) nCol.get());
    }

}
