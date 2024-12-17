package ru.jamsys.core.resource.notification.yandex.speech;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.UniqueClassNameImpl;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseTask;
import ru.jamsys.core.promise.PromiseTaskExecuteType;
import ru.jamsys.core.promise.PromiseTaskWait;

import java.util.ArrayList;
import java.util.List;

@Getter
public class YandexSpeechNotificationRequest {

    @Setter
    private double speed = 1.0;

    @Setter
    private String voice = "marina";

    @Setter
    private String role = "neutral";

    private final PromiseTask asyncPromiseTask;

    private final String filePath;

    private final String text;

    public YandexSpeechNotificationRequest(Promise promise, String filePath, String text) {
        this.text = text;
        this.filePath = filePath;
        List<PromiseTask> add = new ArrayList<>();
        asyncPromiseTask = new PromiseTask(
                UniqueClassNameImpl.getClassNameStatic(getClass()),
                promise,
                PromiseTaskExecuteType.EXTERNAL_WAIT_COMPUTE,
                null
        );
        add.add(asyncPromiseTask);
        add.add(new PromiseTaskWait(promise));
        promise.addToHead(add);
    }

}
