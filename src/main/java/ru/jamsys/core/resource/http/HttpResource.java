package ru.jamsys.core.resource.http;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.resource.NamespaceResourceConstructor;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.resource.http.client.HttpClient;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.util.function.Function;

@Component
@Scope("prototype")
public class HttpResource extends ExpirationMsMutableImpl implements Resource<HttpClient, HttpResponse> {

    @Override
    public void constructor(NamespaceResourceConstructor constructor) {

    }

    @Override
    public HttpResponse execute(HttpClient arguments) {
        arguments.exec();
        return arguments.getHttpResponseEnvelope();
    }

    @Override
    public void close() {

    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 0;
    }

    @Override
    public Function<Throwable, Boolean> getFatalException() {
        return _ -> false;
    }

}
