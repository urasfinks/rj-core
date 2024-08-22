package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilHide;

class UtilHideTest {

    @Test
    public void testZero(){

    }

    @Test
    public void test(){
        Assertions.assertEquals("H***o", UtilHide.mask("Hello", 3, 4, 50));
        Assertions.assertEquals("H***o", UtilHide.mask("Hello", 1, 1, 50));
        Assertions.assertEquals("H**lo", UtilHide.mask("Hello", 1, 2, 40));
        Assertions.assertEquals("H**lo", UtilHide.mask("Hello", 1, 3, 40));
        Assertions.assertEquals("H**lo", UtilHide.mask("Hello", 1, 4, 40));
        Assertions.assertEquals("H**lo", UtilHide.mask("Hello", 2, 4, 10));
        Assertions.assertEquals("М**ин", UtilHide.mask("Мухин", 1, 4, 40));
        Assertions.assertEquals("И*****мов", UtilHide.mask("Ибрагимов", 1, 4, 50));
        Assertions.assertEquals("12*****89", UtilHide.mask("123456789", 10, 10, 50));
        Assertions.assertEquals("12*****890", UtilHide.mask("1234567890", 10, 10, 50));
        Assertions.assertEquals("1*****7890", UtilHide.mask("1234567890", 1, 4, 50));
        Assertions.assertEquals("1*3", UtilHide.mask("123", 10, 10, 50));
        Assertions.assertEquals("1*", UtilHide.mask("12", 10, 10, 50));
        Assertions.assertEquals("1", UtilHide.mask("1", 10, 10, 50));
        Assertions.assertEquals("", UtilHide.mask("", 10, 10, 50));
    }
}