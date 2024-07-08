package ru.jamsys.core.resource.notification.email;

import ru.jamsys.core.extension.ForwardException;
import ru.jamsys.core.flat.template.twix.TemplateTwix;
import ru.jamsys.core.flat.util.UtilFileResource;

import java.util.Map;

public class EmailTemplateNotificationRequest extends EmailNotificationRequest {

    public EmailTemplateNotificationRequest(String title, String data, String template, Map<String, String> args, String to) {
        super(title, data, "", to);
        try {
            setDataHtml(TemplateTwix.template(UtilFileResource.getAsString(template), args));
        } catch (Exception e) {
            throw new ForwardException(e);
        }
    }

}
