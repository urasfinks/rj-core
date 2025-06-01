package ru.jamsys.core.handler.web.http.plugin;

import org.springframework.stereotype.Component;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.exception.AuthException;
import ru.jamsys.core.extension.http.ServletHandler;
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
public class GlobalAuthInterceptor implements HttpInterceptor {

    private final SecurityComponent securityComponent;

    private final RepositoryProperty<String> uriRegexp = new RepositoryProperty<>(String.class);

    public GlobalAuthInterceptor(ServiceProperty serviceProperty, SecurityComponent securityComponent) {
        this.securityComponent = securityComponent;

        new PropertyDispatcher<>(
                null,
                uriRegexp,
                "run.args.web.auth.uri.regexp"
        )
                .addSubscriptionRegexp("run\\.args\\.web\\.auth\\.uri\\.regexp\\.*")
                .run();
    }

    @Override
    public boolean handle(ServletHandler servletHandler) throws AuthException {
        String requestURI = servletHandler.getRequest().getRequestURI();
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
            servletHandler.getRequestReader().basicAuthHandler((user, password) -> {
                try {
                    if (password == null || !password.equals(new String(securityComponent.get("web.auth.password.admin." + user)))) {
                        // 0xFF - просто что бы различать
                        throw new AuthException("Authorization failed 0xFF for user: " + user);
                    }
                } catch (Exception e) {
                    // 0x1F - просто что бы различать
                    throw new AuthException("Authorization failed 0x1F for user: " + user, e);
                }
            });
            return true;
        } catch (AuthException th) {
            servletHandler.responseUnauthorized();
        } catch (Exception e) {
            servletHandler.setResponseStatus(500);
        }
        return false;
    }

}
