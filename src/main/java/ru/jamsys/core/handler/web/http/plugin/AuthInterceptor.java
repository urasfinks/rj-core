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
import ru.jamsys.core.extension.property.PropertySubscriber;
import ru.jamsys.core.extension.property.repository.PropertyRepositoryMap;
import ru.jamsys.core.flat.util.UtilText;
import ru.jamsys.core.handler.web.http.HttpInterceptor;

/*
 * Http перехватчик, для проверки авторизации к защищённым uri
 * */

@Component
@SuppressWarnings("unused")
public class AuthInterceptor implements HttpInterceptor {

    private final SecurityComponent securityComponent;

    private final PropertyRepositoryMap<String> users = new PropertyRepositoryMap<>(String.class);

    private final PropertyRepositoryMap<String> uriRegexp = new PropertyRepositoryMap<>(String.class);

    public AuthInterceptor(ServiceProperty serviceProperty, SecurityComponent securityComponent) {
        this.securityComponent = securityComponent;
        new PropertySubscriber(
                serviceProperty,
                null,
                users,
                "run.args.web.auth.user.password.alias"
        )
                .addSubscriptionRegexp("run\\.args\\.web\\.auth\\.user\\.password\\.alias\\.*")
                .run();

        new PropertySubscriber(
                serviceProperty,
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
        String[] patternKey = uriRegexp.getRepository().keySet().toArray(new String[0]);
        boolean needAuth = false;
        for (String key : patternKey) {
            String repositoryItem = uriRegexp.getRepositoryItem(key);
            if (repositoryItem != null && UtilText.regexpFind(requestURI, repositoryItem) != null) {
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
                String userPasswordSecurityAlias = users.getRepositoryItem(user);
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
