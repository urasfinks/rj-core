package ru.jamsys.core.resource.http;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.extension.Completable;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

@Component
@Scope("prototype")
public class HttpClientResource extends ExpirationMsMutableImpl implements Completable,  Resource<HttpPoolSettings, HttpClient, HttpResponseEnvelope> {

    @Override
    public void constructor(HttpPoolSettings constructor) {
        //System.out.println(constructor);
    }

    @Override
    public HttpResponseEnvelope execute(HttpClient arguments) {
        arguments.exec();
        return arguments.getHttpResponseEnvelope(null);
    }

    @Override
    public void close() {

    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 0;
    }

}
