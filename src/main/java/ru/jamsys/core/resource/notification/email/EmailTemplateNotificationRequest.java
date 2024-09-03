package ru.jamsys.core.resource.notification.email;

import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.template.twix.TemplateTwix;
import ru.jamsys.core.flat.util.UtilFileResource;

import java.util.Map;

public class EmailTemplateNotificationRequest extends EmailNotificationRequest {

    public EmailTemplateNotificationRequest(
            String title,
            String data,
            String templateClassLoader,
            String templatePath,
            Map<String, String> args,
            String to
    ) {
        super(title, data, "", to);
        try {
            setDataHtml(TemplateTwix.template(
                    UtilFileResource.getAsString(templatePath, UtilFileResource.Direction.valueOf(templateClassLoader)),
                    args
            ));
        } catch (Exception e) {
            throw new ForwardException(e);
        }
    }

}
