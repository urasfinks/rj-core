package ru.jamsys.core.flat.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UtilSetTest {
    private Set<Integer> set(Integer... elems) {
        return new HashSet<>(Arrays.asList(elems));
    }

    @Test
    public void testAnd() {
        Set<Integer> A = set(1, 2, 3);
        Set<Integer> B = set(3, 4);
        assertEquals(set(3), UtilSet.and(A, B));
    }

    @Test
    public void testDifferenceAminusB() {
        Set<Integer> A = set(1, 2, 3);
        Set<Integer> B = set(2, 4);
        assertEquals(set(1, 3), UtilSet.differenceAminusB(A, B));
    }

    @Test
    public void testDifferenceBminusA() {
        Set<Integer> A = set(1, 3);
        Set<Integer> B = set(1, 2, 3, 4);
        assertEquals(set(2, 4), UtilSet.differenceBminusA(A, B));
    }

    @Test
    public void testProjectionA() {
        Set<Integer> A = set(5, 6);
        Set<Integer> B = set(7, 8);
        assertEquals(set(5, 6), UtilSet.projectionA(A, B));
    }

    @Test
    public void testProjectionB() {
        Set<Integer> A = set(9);
        Set<Integer> B = set(10, 11);
        assertEquals(set(10, 11), UtilSet.projectionB(A, B));
    }

    @Test
    public void testXor() {
        Set<Integer> A = set(1, 2, 3);
        Set<Integer> B = set(3, 4);
        assertEquals(set(1, 2, 4), UtilSet.xor(A, B));
    }

    @Test
    public void testOr() {
        Set<Integer> A = set(1, 2);
        Set<Integer> B = set(2, 3);
        assertEquals(set(1, 2, 3), UtilSet.or(A, B));
    }

    // Tests for Method enum
    @Test
    public void testMethodEnumAnd() {
        Set<Integer> A = set(1, 2, 3);
        Set<Integer> B = set(3, 4);
        assertEquals(UtilSet.and(A, B), UtilSet.Method.AND.result(A, B));
    }

    @Test
    public void testMethodEnumDiffAminusB() {
        Set<Integer> A = set(1, 2, 3);
        Set<Integer> B = set(2, 4);
        assertEquals(UtilSet.differenceAminusB(A, B), UtilSet.Method.DIFF_A_MINUS_B.result(A, B));
    }

    @Test
    public void testMethodEnumDiffBminusA() {
        Set<Integer> A = set(1, 3);
        Set<Integer> B = set(1, 2, 3, 4);
        assertEquals(UtilSet.differenceBminusA(A, B), UtilSet.Method.DIFF_B_MINUS_A.result(A, B));
    }

    @Test
    public void testMethodEnumA() {
        Set<Integer> A = set(5, 6);
        Set<Integer> B = set(7, 8);
        assertEquals(UtilSet.projectionA(A, B), UtilSet.Method.A.result(A, B));
    }

    @Test
    public void testMethodEnumB() {
        Set<Integer> A = set(9);
        Set<Integer> B = set(10, 11);
        assertEquals(UtilSet.projectionB(A, B), UtilSet.Method.B.result(A, B));
    }

    @Test
    public void testMethodEnumXor() {
        Set<Integer> A = set(1, 2, 3);
        Set<Integer> B = set(3, 4);
        assertEquals(UtilSet.xor(A, B), UtilSet.Method.XOR.result(A, B));
    }

    @Test
    public void testMethodEnumOr() {
        Set<Integer> A = set(1, 2);
        Set<Integer> B = set(2, 3);
        assertEquals(UtilSet.or(A, B), UtilSet.Method.OR.result(A, B));
    }
}