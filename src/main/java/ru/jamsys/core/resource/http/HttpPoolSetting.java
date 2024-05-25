package ru.jamsys.core.resource.http;

import ru.jamsys.core.component.manager.sub.PoolResourceArgument;

public class HttpPoolSetting {

    public static PoolResourceArgument<
            HttpClientResource,
            HttpPoolSetting
            > setting = new PoolResourceArgument<>(HttpClientResource.class, null, _ -> false);

}
