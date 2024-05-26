package ru.jamsys.core.resource.http;

import ru.jamsys.core.component.manager.sub.PoolResourceArgument;

public class HttpPoolSettings {

    public static PoolResourceArgument<
            HttpClientResource,
            HttpPoolSettings
            > setting = new PoolResourceArgument<>(HttpClientResource.class, null, _ -> false);

}
