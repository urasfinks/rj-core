package ru.jamsys.task.generator.cron;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TemplateTest {

    @Test
    public void test(){
        Template template = new Template();
        template.parse("30 08 10 06 *");
        Assertions.assertEquals("Template(second=[30], minute=[8], hour=[10], dayOfMonth=[6], monthOfYear=[], dayOfWeek=[])", template.toString());
        //System.out.println(template);
    }

}