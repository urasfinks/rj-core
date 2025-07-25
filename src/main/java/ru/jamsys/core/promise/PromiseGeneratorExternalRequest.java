package ru.jamsys.core.promise;

import lombok.Getter;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyListener;
import ru.jamsys.core.extension.rate.limit.tps.RateLimitTps;
import ru.jamsys.core.flat.util.UtilText;

import java.util.ArrayList;
import java.util.List;

// Для использования Promise для внешних запросов, например HttpHandler/WebSocketHandler
// имеет RateLimitTps + список пользователей (права доступа) Пароли через Security надо проверять самостоятельно

@Getter
public abstract class PromiseGeneratorExternalRequest extends PromiseGenerator implements PropertyListener {

    private final PropertyDispatcher<Object> propertyDispatcher;

    private final PromiseGeneratorAccessRepositoryProperty promiseGeneratorAccessRepositoryProperty = new PromiseGeneratorAccessRepositoryProperty();

    private final ManagerConfiguration<RateLimitTps> rateLimitConfiguration;

    private final List<String> user = new ArrayList<>(); // Пользователи которым доступен вызов этого Promise

    public PromiseGeneratorExternalRequest() {
        rateLimitConfiguration = ManagerConfiguration.getInstance(
                RateLimitTps.class,
                getCascadeKey(),
                getCascadeKey(),
                null
        );
        propertyDispatcher = new PropertyDispatcher<>(
                this,
                promiseGeneratorAccessRepositoryProperty,
                getCascadeKey()
        );
        propertyDispatcher.run();
        updateUsers(promiseGeneratorAccessRepositoryProperty.getUsers());
    }

    public abstract Promise generate();

    @Override
    public void onPropertyUpdate(String key, String oldValue, String newValue) {
        if (key.equals(PromiseGeneratorAccessRepositoryProperty.Fields.users)) {
            updateUsers(newValue);
        }
    }

    private void updateUsers(String value) {
        user.clear();
        user.addAll(UtilText.stringToList(value, ","));
    }

}
