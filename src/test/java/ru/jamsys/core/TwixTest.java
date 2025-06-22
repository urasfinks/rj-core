package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.template.twix.TemplateTwix;

import java.util.HashMap;
import java.util.Map;

// IO time: 13ms
// COMPUTE time: 13ms

class TwixTest {

    @Test
    void template() {
        Map<String, String> args = new HashMap<>();
        args.put("name", "Ura");
        Assertions.assertEquals("Hello Ura", TemplateTwix.template("Hello ${name}", args));
        Assertions.assertEquals("Hello ${name}", TemplateTwix.template("Hello \\${name}", args));
    }

    @Test
    void parse() {
        Assertions.assertEquals("[TemplateItemTwix(staticFragment=true, value=H $ryy)]", TemplateTwix.getParsedTemplate("H $ryy").toString(), "#1");
        Assertions.assertEquals("[TemplateItemTwix(staticFragment=true, value=H ), TemplateItemTwix(staticFragment=false, value=x)]", TemplateTwix.getParsedTemplate("H ${x}").toString(), "#2");
        Assertions.assertEquals("[TemplateItemTwix(staticFragment=true, value=Hello ), TemplateItemTwix(staticFragment=false, value=world)]", TemplateTwix.getParsedTemplate("Hello ${world}").toString(), "#3");
        Assertions.assertEquals("[TemplateItemTwix(staticFragment=true, value=H), TemplateItemTwix(staticFragment=false, value=x)]", TemplateTwix.getParsedTemplate("H${x}").toString(), "#4");
        Assertions.assertEquals("[TemplateItemTwix(staticFragment=false, value=x)]", TemplateTwix.getParsedTemplate("${x}").toString(), "#5");
        Assertions.assertEquals("[TemplateItemTwix(staticFragment=false, value=hello), TemplateItemTwix(staticFragment=true, value= world)]", TemplateTwix.getParsedTemplate("${hello} world").toString(), "#6");
        Assertions.assertEquals("[TemplateItemTwix(staticFragment=true, value=${{x})]", TemplateTwix.getParsedTemplate("${{x}").toString(), "#7");
        Assertions.assertEquals("[TemplateItemTwix(staticFragment=false, value={x)]", TemplateTwix.getParsedTemplate("${\\{x}").toString(), "#8");
        Assertions.assertEquals("[TemplateItemTwix(staticFragment=true, value=H ), TemplateItemTwix(staticFragment=false, value=abc\\x)]", TemplateTwix.getParsedTemplate("H ${abc\\\\x}").toString(), "#9");
        Assertions.assertEquals("[TemplateItemTwix(staticFragment=true, value=H ), TemplateItemTwix(staticFragment=false, value=abc{x)]", TemplateTwix.getParsedTemplate("H ${abc\\{x}").toString(), "#10");
        Assertions.assertEquals("[TemplateItemTwix(staticFragment=true, value=H ), TemplateItemTwix(staticFragment=false, value=abc}x)]", TemplateTwix.getParsedTemplate("H ${abc\\}x}").toString(), "#11");
        Assertions.assertEquals("[TemplateItemTwix(staticFragment=true, value=H ), TemplateItemTwix(staticFragment=false, value=abc${x)]", TemplateTwix.getParsedTemplate("H ${abc\\$\\{x}").toString(), "#12");
        Assertions.assertEquals("[TemplateItemTwix(staticFragment=true, value=$$$)]", TemplateTwix.getParsedTemplate("$$$").toString(), "#13");
        Assertions.assertEquals("[TemplateItemTwix(staticFragment=true, value=$\\$$)]", TemplateTwix.getParsedTemplate("$\\$$").toString(), "#14");
        Assertions.assertEquals("[TemplateItemTwix(staticFragment=false, value=$)]", TemplateTwix.getParsedTemplate("${\\$}").toString(), "#15");
        Assertions.assertEquals("[TemplateItemTwix(staticFragment=true, value=Hello ), TemplateItemTwix(staticFragment=false, value=data.username)]", TemplateTwix.getParsedTemplate("Hello ${data.username}").toString(), "#16");
        Assertions.assertEquals("[TemplateItemTwix(staticFragment=true, value=$), TemplateItemTwix(staticFragment=false, value=opa)]", TemplateTwix.getParsedTemplate("$${opa}").toString(), "#17");
        Assertions.assertEquals("[TemplateItemTwix(staticFragment=true, value=$$), TemplateItemTwix(staticFragment=false, value=opa)]", TemplateTwix.getParsedTemplate("$$${opa}").toString(), "#18");
        Assertions.assertEquals("[TemplateItemTwix(staticFragment=true, value=Hello ), TemplateItemTwix(staticFragment=false, value=opa), TemplateItemTwix(staticFragment=true, value= world)]", TemplateTwix.getParsedTemplate("Hello ${opa} world").toString(), "#19");
        Assertions.assertEquals("[TemplateItemTwix(staticFragment=false, value=hello), TemplateItemTwix(staticFragment=true, value=_), TemplateItemTwix(staticFragment=false, value=world)]", TemplateTwix.getParsedTemplate("${hello}_${world}").toString(), "#20");
        Assertions.assertEquals("[TemplateItemTwix(staticFragment=false, value=hello), TemplateItemTwix(staticFragment=false, value=world)]", TemplateTwix.getParsedTemplate("${hello}${world}").toString(), "#21");
        Assertions.assertEquals("[TemplateItemTwix(staticFragment=false, value=hello), TemplateItemTwix(staticFragment=true, value=$$), TemplateItemTwix(staticFragment=false, value=world)]", TemplateTwix.getParsedTemplate("${hello}$$${world}").toString(), "#22");
        Assertions.assertEquals("[TemplateItemTwix(staticFragment=false, value=hello), TemplateItemTwix(staticFragment=true, value=$\\$), TemplateItemTwix(staticFragment=false, value=world)]", TemplateTwix.getParsedTemplate("${hello}$\\$${world}").toString(), "#23");
        Assertions.assertEquals("[TemplateItemTwix(staticFragment=false, value=h\\ello), TemplateItemTwix(staticFragment=true, value=$\\$), TemplateItemTwix(staticFragment=false, value=world)]", TemplateTwix.getParsedTemplate("${h\\\\ello}$\\$${world}").toString(), "#24");
        Assertions.assertEquals("[TemplateItemTwix(staticFragment=true, value=${fwe)]", TemplateTwix.getParsedTemplate("${fwe").toString(), "#24");
        //Assertions.assertEquals("", Template.parse("").toString(), "#");
    }

    @Test
    void templateNull() {
        Map<String, String> args = new HashMap<>();
        Assertions.assertEquals("Hello ${name}", TemplateTwix.template("Hello ${name}", args, true));
    }

}