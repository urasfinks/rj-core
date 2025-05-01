package ru.jamsys.core.flat.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilByteTest {

    @Test
    void testKilobytesToBytes() {
        assertEquals(1024, UtilByte.kilobytesToBytes(1));
        assertEquals(5120, UtilByte.kilobytesToBytes(5));
        assertEquals(0, UtilByte.kilobytesToBytes(0));
        assertEquals(-1024, UtilByte.kilobytesToBytes(-1));
    }

    @Test
    void testMegabytesToBytes() {
        assertEquals(1048576, UtilByte.megabytesToBytes(1));
        assertEquals(5242880, UtilByte.megabytesToBytes(5));
        assertEquals(0, UtilByte.megabytesToBytes(0));
    }

    @Test
    void testGigabytesToBytes() {
        assertEquals(1073741824, UtilByte.gigabytesToBytes(1));
        assertEquals(5368709120L, UtilByte.gigabytesToBytes(5));
        assertEquals(0, UtilByte.gigabytesToBytes(0));
    }

    @Test
    void testTerabytesToBytes() {
        assertEquals(1099511627776L, UtilByte.terabytesToBytes(1));
        assertEquals(5497558138880L, UtilByte.terabytesToBytes(5));
        assertEquals(0, UtilByte.terabytesToBytes(0));
    }

    @Test
    void testCharsToBytesAndBack() {
        char[] original = {'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd', '!'};
        byte[] bytes = UtilByte.charsToBytes(original);
        char[] restored = UtilByte.bytesToChars(bytes);
        assertArrayEquals(original, restored);
    }

    @Test
    void testIntToBytesAndBack() {
        int original = 123456789;
        byte[] bytes = UtilByte.intToBytes(original);
        int restored = UtilByte.bytesToInt(bytes);
        assertEquals(original, restored);
    }

    @Test
    void testShortToBytesAndBack() {
        short original = 12345;
        byte[] bytes = UtilByte.shortToBytes(original);
        short restored = UtilByte.bytesToShort(bytes);
        assertEquals(original, restored);
    }

    @Test
    void testLongToBytesAndBack() {
        long original = 1234567890123456789L;
        byte[] bytes = UtilByte.longToBytes(original);
        long restored = UtilByte.bytesToLong(bytes);
        assertEquals(original, restored);
    }

    @Test
    void testReverseBytes() {
        byte[] original = {1, 2, 3, 4, 5};
        byte[] expected = {5, 4, 3, 2, 1};
        UtilByte.reverseBytes(original);
        assertArrayEquals(expected, original);
    }

    @Test
    void testReverseBytesEmptyArray() {
        byte[] empty = {};
        UtilByte.reverseBytes(empty);
        assertArrayEquals(new byte[0], empty);
    }

    @Test
    void testGetBit() {
        short number = 0b0000_0000_0000_1010; // 10 in decimal
        assertEquals(0, UtilByte.getBit(number, 15)); // MSB
        assertEquals(1, UtilByte.getBit(number, 1));
        assertEquals(0, UtilByte.getBit(number, 0));
    }

    @Test
    void testSetBit() {
        short number = 0;
        short result = UtilByte.setBit(number, 3);
        assertEquals(8, result); // 2^3 = 8
    }

    @Test
    void testClearBit() {
        short number = 0b0000_0000_0000_1111; // 15 in decimal
        short result = UtilByte.clearBit(number, 0);
        assertEquals(14, result); // 15 - 1 = 14
    }

    @Test
    void testEdgeCases() {
        // Test max/min values
        assertEquals(Short.MAX_VALUE, UtilByte.bytesToShort(UtilByte.shortToBytes(Short.MAX_VALUE)));
        assertEquals(Short.MIN_VALUE, UtilByte.bytesToShort(UtilByte.shortToBytes(Short.MIN_VALUE)));

        assertEquals(Integer.MAX_VALUE, UtilByte.bytesToInt(UtilByte.intToBytes(Integer.MAX_VALUE)));
        assertEquals(Integer.MIN_VALUE, UtilByte.bytesToInt(UtilByte.intToBytes(Integer.MIN_VALUE)));

        assertEquals(Long.MAX_VALUE, UtilByte.bytesToLong(UtilByte.longToBytes(Long.MAX_VALUE)));
        assertEquals(Long.MIN_VALUE, UtilByte.bytesToLong(UtilByte.longToBytes(Long.MIN_VALUE)));
    }

    @Test
    void test(){
        UtilLog.printInfo(UtilByte.getBits((byte) 127));
    }

}