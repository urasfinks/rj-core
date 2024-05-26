package ru.jamsys.core.resource.http;

import ru.jamsys.core.component.manager.sub.PoolResourceArgument;

public class HttpPoolSettings {

    public static PoolResourceArgument<
            HttpResource,
            HttpPoolSettings
            > setting = new PoolResourceArgument<>(HttpResource.class, null, _ -> false);

}
