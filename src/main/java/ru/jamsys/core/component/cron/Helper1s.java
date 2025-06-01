package ru.jamsys.core.component.cron;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.extension.async.writer.AbstractAsyncFileWriter;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.expiration.ExpirationList;
import ru.jamsys.core.flat.template.cron.release.Cron1s;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

// Нам надо вызывать helper у ExpiredList 1 раз в секунду, а не 1раз в 3сек

@SuppressWarnings("unused")
@Component
@Lazy
public class Helper1s extends PromiseGenerator implements Cron1s  {

    private final ServicePromise servicePromise;
    private final Manager manager;

    public Helper1s(ServicePromise servicePromise, Manager manager) {
        this.servicePromise = servicePromise;
        this.manager = manager;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(App.getUniqueClassName(getClass()), 6_000L)
                .append(
                        ExpirationList.class.getSimpleName(),
                        (run, _, _) -> manager.groupAccept(
                                ExpirationList.class,
                                expirationList -> expirationList.helper(run, System.currentTimeMillis())
                        )
                )
                .append(AbstractAsyncFileWriter.class.getSimpleName(),
                        (run, _, _) -> manager.groupAcceptByInterface(AbstractAsyncFileWriter.class, abstractAsyncFileWriter -> {
                            try {
                                abstractAsyncFileWriter.flush(run);
                            } catch (Throwable e) {
                                throw new ForwardException(abstractAsyncFileWriter, e);
                            }
                        }))
                ;
    }

}
