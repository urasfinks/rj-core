package ru.jamsys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.template.Template;

class TemplateTest {

    @Test
    void parse() {
        Assertions.assertEquals("[TemplateItem(isStatic=true, value=H $ryy)]", Template.parse("H $ryy").toString(), "#1");
        Assertions.assertEquals("[TemplateItem(isStatic=true, value=H ), TemplateItem(isStatic=false, value=x)]", Template.parse("H ${x}").toString(), "#2");
        Assertions.assertEquals("[TemplateItem(isStatic=true, value=Hello ), TemplateItem(isStatic=false, value=world)]", Template.parse("Hello ${world}").toString(), "#3");
        Assertions.assertEquals("[TemplateItem(isStatic=true, value=H), TemplateItem(isStatic=false, value=x)]", Template.parse("H${x}").toString(), "#4");
        Assertions.assertEquals("[TemplateItem(isStatic=false, value=x)]", Template.parse("${x}").toString(), "#5");
        Assertions.assertEquals("[TemplateItem(isStatic=false, value=hello), TemplateItem(isStatic=true, value= world)]", Template.parse("${hello} world").toString(), "#6");
        Assertions.assertEquals("[TemplateItem(isStatic=true, value=${{x})]", Template.parse("${{x}").toString(), "#7");
        Assertions.assertEquals("[TemplateItem(isStatic=false, value={x)]", Template.parse("${\\{x}").toString(), "#8");
        Assertions.assertEquals("[TemplateItem(isStatic=true, value=H ), TemplateItem(isStatic=false, value=abc\\x)]", Template.parse("H ${abc\\\\x}").toString(), "#9");
        Assertions.assertEquals("[TemplateItem(isStatic=true, value=H ), TemplateItem(isStatic=false, value=abc{x)]", Template.parse("H ${abc\\{x}").toString(), "#10");
        Assertions.assertEquals("[TemplateItem(isStatic=true, value=H ), TemplateItem(isStatic=false, value=abc}x)]", Template.parse("H ${abc\\}x}").toString(), "#11");
        Assertions.assertEquals("[TemplateItem(isStatic=true, value=H ), TemplateItem(isStatic=false, value=abc${x)]", Template.parse("H ${abc\\$\\{x}").toString(), "#12");
        Assertions.assertEquals("[TemplateItem(isStatic=true, value=$$$)]", Template.parse("$$$").toString(), "#13");
        Assertions.assertEquals("[TemplateItem(isStatic=true, value=$\\$$)]", Template.parse("$\\$$").toString(), "#14");
        Assertions.assertEquals("[TemplateItem(isStatic=false, value=$)]", Template.parse("${\\$}").toString(), "#15");
        Assertions.assertEquals("[TemplateItem(isStatic=true, value=Hello ), TemplateItem(isStatic=false, value=data.username)]", Template.parse("Hello ${data.username}").toString(), "#16");
        Assertions.assertEquals("[TemplateItem(isStatic=true, value=$), TemplateItem(isStatic=false, value=opa)]", Template.parse("$${opa}").toString(), "#17");
        Assertions.assertEquals("[TemplateItem(isStatic=true, value=$$), TemplateItem(isStatic=false, value=opa)]", Template.parse("$$${opa}").toString(), "#18");
        Assertions.assertEquals("[TemplateItem(isStatic=true, value=Hello ), TemplateItem(isStatic=false, value=opa), TemplateItem(isStatic=true, value= world)]", Template.parse("Hello ${opa} world").toString(), "#19");
        Assertions.assertEquals("[TemplateItem(isStatic=false, value=hello), TemplateItem(isStatic=true, value=_), TemplateItem(isStatic=false, value=world)]", Template.parse("${hello}_${world}").toString(), "#20");
        Assertions.assertEquals("[TemplateItem(isStatic=false, value=hello), TemplateItem(isStatic=false, value=world)]", Template.parse("${hello}${world}").toString(), "#21");
        Assertions.assertEquals("[TemplateItem(isStatic=false, value=hello), TemplateItem(isStatic=true, value=$$), TemplateItem(isStatic=false, value=world)]", Template.parse("${hello}$$${world}").toString(), "#22");
        Assertions.assertEquals("[TemplateItem(isStatic=false, value=hello), TemplateItem(isStatic=true, value=$\\$), TemplateItem(isStatic=false, value=world)]", Template.parse("${hello}$\\$${world}").toString(), "#23");
        Assertions.assertEquals("[TemplateItem(isStatic=false, value=h\\ello), TemplateItem(isStatic=true, value=$\\$), TemplateItem(isStatic=false, value=world)]", Template.parse("${h\\\\ello}$\\$${world}").toString(), "#24");
        Assertions.assertEquals("[TemplateItem(isStatic=true, value=${fwe)]", Template.parse("${fwe").toString(), "#24");
        //Assertions.assertEquals("", Template.parse("").toString(), "#");
    }
}