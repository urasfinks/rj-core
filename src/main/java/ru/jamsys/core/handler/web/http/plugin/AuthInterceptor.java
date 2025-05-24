package ru.jamsys.core.handler.web.http.plugin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.exception.AuthException;
import ru.jamsys.core.extension.http.ServletRequestReader;
import ru.jamsys.core.extension.http.ServletResponseWriter;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyEnvelope;
import ru.jamsys.core.extension.property.repository.RepositoryProperty;
import ru.jamsys.core.flat.util.UtilText;
import ru.jamsys.core.handler.web.http.HttpInterceptor;

/*
 * Http перехватчик, для проверки авторизации к защищённым uri
 * */

@Component
@SuppressWarnings("unused")
public class AuthInterceptor implements HttpInterceptor {

    private final SecurityComponent securityComponent;

    private final RepositoryProperty<String> users = new RepositoryProperty<>(String.class);

    private final RepositoryProperty<String> uriRegexp = new RepositoryProperty<>(String.class);

    public AuthInterceptor(ServiceProperty serviceProperty, SecurityComponent securityComponent) {
        this.securityComponent = securityComponent;
        new PropertyDispatcher<>(
                null,
                users,
                "run.args.web.auth.user.password.alias"
        )
                .addSubscriptionRegexp("run\\.args\\.web\\.auth\\.user\\.password\\.alias\\.*")
                .run();

        new PropertyDispatcher<>(
                null,
                uriRegexp,
                "run.args.web.auth.uri.regexp"
        )
                .addSubscriptionRegexp("run\\.args\\.web\\.auth\\.uri\\.regexp\\.*")
                .run();
    }

    @Override
    public boolean handle(HttpServletRequest request, HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        boolean needAuth = false;
        for (PropertyEnvelope<String> propertyEnvelope : uriRegexp.getListPropertyEnvelopeRepository()) {
            String regexpPattern = propertyEnvelope.getValue();
            if (regexpPattern != null && UtilText.regexpFind(requestURI, regexpPattern) != null) {
                needAuth = true;
                break;
            }
        }
        if (!needAuth) {
            return true;
        }
        try {
            ServletRequestReader servletRequestReader = new ServletRequestReader(request);
            servletRequestReader.basicAuthHandler((user, password) -> {
                String cred = null;
                PropertyEnvelope<String> propertyEnvelope = users.getByRepositoryPropertyKey(user);
                String userPasswordSecurityAlias = propertyEnvelope.getValue();
                if (userPasswordSecurityAlias == null) {
                    throw new AuthException("Authorization failed for user: " + user);
                }
                char[] chars = securityComponent.get(userPasswordSecurityAlias);
                if (chars != null) {
                    cred = new String(chars);
                }
                if (cred == null || !cred.equals(password)) {
                    throw new AuthException("Authorization failed for user: " + user);
                }
            });
            return true;
        } catch (AuthException th) {
            ServletResponseWriter.setResponseUnauthorized(response);
        } catch (Throwable th) {
            App.error(th);
        }
        return false;
    }

}
