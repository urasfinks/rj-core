package ru.jamsys.core.notification;

import lombok.Setter;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.HtmlEmail;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.resource.PropertiesComponent;
import ru.jamsys.core.component.Security;
import ru.jamsys.core.resource.http.HttpResponseEnvelope;
import ru.jamsys.core.template.twix.Template;
import ru.jamsys.core.template.twix.TemplateItem;
import ru.jamsys.core.util.UtilFileResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@Component
@Lazy
public class NotificationEmail implements Notification {

    private final Security security;

    @Setter
    private String host;

    @Setter
    private String template;

    @Setter
    private String supportAddress;

    @Setter
    private String user;

    @Setter
    private String from;

    @Setter
    private String charset;

    @Setter
    private String securityAlias;

    @Setter
    private int port;

    @Setter
    private int connectTimeoutMs;

    @Setter
    private boolean ssl;

    List<TemplateItem> parsedTemplate = null;

    public NotificationEmail(Security security, PropertiesComponent propertiesComponent) {

        this.security = security;


        this.template = propertiesComponent.getProperties("rj.notification.email.template", String.class);
        this.supportAddress = propertiesComponent.getProperties("rj.notification.email.support.address", String.class);

        this.host = propertiesComponent.getProperties("rj.notification.email.host", String.class);
        this.user = propertiesComponent.getProperties("rj.notification.email.user", String.class);
        this.from = propertiesComponent.getProperties("rj.notification.email.from", String.class);
        this.charset = propertiesComponent.getProperties("rj.notification.email.charset", String.class);
        this.securityAlias = propertiesComponent.getProperties("rj.notification.email.security.alias", String.class);

        this.port = propertiesComponent.getProperties("rj.notification.email.port", Integer.class);
        this.connectTimeoutMs = propertiesComponent.getProperties("rj.notification.email.connectTimeoutMs", Integer.class);

        this.ssl = propertiesComponent.getProperties("rj.notification.email.ssl", Boolean.class);

    }

    public String compileTemplate(Map<String, String> args) throws IOException {
        if (parsedTemplate == null) {
            parsedTemplate = Template.getParsedTemplate(UtilFileResource.getAsString(template));
        }
        args.put("rj.notification.email.support.address", supportAddress);
        return Template.template(parsedTemplate, args);
    }

    @Override
    public HttpResponseEnvelope notify(String title, Object data, String to) {
        return notify(title, null, data.toString(), to);
    }

    public HttpResponseEnvelope notify(String title, String data, String dataHtml, String to) {
        HttpResponseEnvelope httpResponseEnvelope = new HttpResponseEnvelope();
        HtmlEmail email = new HtmlEmail();
        try {
            setting(email);
        } catch (Exception e) {
            httpResponseEnvelope.addException(e);
        }
        if (httpResponseEnvelope.isStatus()) {
            try {
                email.addTo(to);
                email.setSubject(title);
                if (data != null) {
                    email.setTextMsg(data);
                }
                email.setHtmlMsg(dataHtml);
                email.send();
            } catch (Exception e) {
                httpResponseEnvelope.addException(e);
            }
        }
        return httpResponseEnvelope;
    }

    @Override
    public Notification getInstance() {
        Security security = App.context.getBean(Security.class);
        PropertiesComponent propertiesComponent = App.context.getBean(PropertiesComponent.class);
        return new NotificationEmail(security, propertiesComponent);
    }

    private void setting(HtmlEmail email) throws Exception {
        email.setHostName(host);
        email.setSmtpPort(port);
        email.setAuthenticator(new DefaultAuthenticator(user, new String(security.get(securityAlias))));
        email.setSSLOnConnect(ssl);
        email.setFrom(from);
        email.setCharset(charset);
        email.setSocketConnectionTimeout(connectTimeoutMs);
        email.setSocketTimeout(connectTimeoutMs);
    }

}
