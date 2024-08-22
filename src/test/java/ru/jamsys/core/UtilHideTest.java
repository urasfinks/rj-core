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

    @Test
    public void split(){
        Assertions.assertEquals("[letter=true, value=Hello}, letter=false, value= }, letter=true, value=world}]", UtilHide.split("Hello world").toString());
        Assertions.assertEquals("[letter=false, value=!!!}]", UtilHide.split("!!!").toString());
        Assertions.assertEquals("[letter=false, value=! }, letter=true, value=world}]", UtilHide.split("! world").toString());
        Assertions.assertEquals("[letter=true, value=H}, letter=false, value=!}, letter=true, value=e}, letter=false, value=!}]", UtilHide.split("H!e!").toString());
        Assertions.assertEquals("[letter=true, value=Hello123}]", UtilHide.split("Hello123").toString());
    }

    @Test
    public void explodeAndReplace(){
        Assertions.assertEquals("P****NAME: Ф***ЛИЯ И*Я О****ТВО; P***OSE: Д*Я Т**ТА; E***UDE_R**S: E***UDE_R**S@I****ANCE_P**S_R*@;", UtilHide.explodeLetterAndMask("PAYERNAME: ФАМИЛИЯ ИМЯ ОТЧЕСТВО; PURPOSE: ДЛЯ ТЕСТА; EXCLUDE_REQS: EXCLUDE_REQS@INSURANCE_PASS_RF@;", 1,4, 40));
        Assertions.assertEquals("P****NAME: Ф***ЛИЯ И*Я О****ТВО; P***OSE: Д*Я Т**ТА; E***UDE_R**S: C**D: 1*****7890; E***UDE_R**S@I****ANCE_P**S_R*@;", UtilHide.explodeLetterAndMask("PAYERNAME: ФАМИЛИЯ ИМЯ ОТЧЕСТВО; PURPOSE: ДЛЯ ТЕСТА; EXCLUDE_REQS: CARD: 1234567890; EXCLUDE_REQS@INSURANCE_PASS_RF@;", 1,4, 40));
        Assertions.assertEquals("О**ов А***сей Т****вич", UtilHide.explodeLetterAndMask("Орлов Алексей Тестович", 1,4, 40));
    }

}