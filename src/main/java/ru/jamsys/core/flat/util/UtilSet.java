package ru.jamsys.core.flat.util;

import java.util.LinkedHashSet;
import java.util.Set;

public class UtilSet {

    // Внутренние регионы Venn-диаграммы (только внутри кругов):
    // (лево | пересечение | право)
    // обозначаем: '*' = включено, '-' = не включено

    // (-(*)-) [and A ∧ B] только пересечение
    public static <T> Set<T> and(Set<T> A, Set<T> B) {
        Set<T> r = new LinkedHashSet<>(A);
        r.retainAll(B);
        return r;
    }

    // (*(-)-) [A \ B] только левый круг без пересечения
    public static <T> Set<T> differenceAminusB(Set<T> A, Set<T> B) {
        Set<T> r = new LinkedHashSet<>(A);
        r.removeAll(B);
        return r;
    }

    // (-(-)*) [B \ A] только правый круг без пересечения
    public static <T> Set<T> differenceBminusA(Set<T> A, Set<T> B) {
        Set<T> r = new LinkedHashSet<>(B);
        r.removeAll(A);
        return r;
    }

    // (*(*)-) [A] весь A (лево + пересечение)
    public static <T> Set<T> projectionA(Set<T> A, Set<T> B) {
        return new LinkedHashSet<>(A);
    }

    // (-(*)*) [B] весь B (пересечение + право)
    public static <T> Set<T> projectionB(Set<T> A, Set<T> B) {
        return new LinkedHashSet<>(B);
    }

    // (*(-)*) [xor A ⊕ B] симметрическая разность
    public static <T> Set<T> xor(Set<T> A, Set<T> B) {
        Set<T> r = new LinkedHashSet<>(A);
        r.addAll(B);
        Set<T> tmp = new LinkedHashSet<>(A);
        tmp.retainAll(B);
        r.removeAll(tmp);
        return r;
    }

    // (*(*)*) [or A ∪ B] объединение
    public static <T> Set<T> or(Set<T> A, Set<T> B) {
        return union(A, B);
    }

    private static <T> Set<T> union(Set<T> A, Set<T> B) {
        Set<T> r = new LinkedHashSet<>(A);
        r.addAll(B);
        return r;
    }

    public enum Method {
        AND,
        DIFF_A_MINUS_B,
        DIFF_B_MINUS_A,
        A,
        B,
        XOR,
        OR;

        public <T> Set<T> result(Set<T> A, Set<T> B) {
            return switch (this) {
                case AND -> and(A, B);
                case DIFF_A_MINUS_B -> differenceAminusB(A, B);
                case DIFF_B_MINUS_A -> differenceBminusA(A, B);
                case A -> projectionA(A, B);
                case B -> projectionB(A, B);
                case XOR -> xor(A, B);
                case OR -> or(A, B);
            };
        }

    }

}
