package ru.jamsys.core.component.cron;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.expiration.ExpirationList;
import ru.jamsys.core.flat.template.cron.release.Cron1s;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

// Нам надо вызывать helper у ExpiredList 1 раз в секунду, а не 1раз в 3сек

@SuppressWarnings("unused")
@Component
@Lazy
public class Helper1s implements Cron1s, PromiseGenerator {

    private final ServicePromise servicePromise;

    public Helper1s(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(App.getUniqueClassName(getClass()), 6_000L)
                .append(
                        ExpirationList.class.getSimpleName(),
                        (run, _, _) -> UtilRisc.forEach(run, ExpirationList.expirationListSet, expirationList -> {
                            expirationList.helper(run, System.currentTimeMillis());
                        }))
                ;
    }

}
