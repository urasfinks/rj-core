package ru.jamsys.core.resource.yandex.speech;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.promise.*;

import java.util.ArrayList;
import java.util.List;

@Getter
public class YandexSpeechRequest {

    @Setter
    private double speed = 1.0;

    @Setter
    private String voice = "marina";

    @Setter
    private String role = "neutral";

    private final AbstractPromiseTask asyncPromiseTask;

    private final String filePath;

    private final String text;

    public YandexSpeechRequest(Promise promise, String filePath, String text) {
        this.text = text;
        this.filePath = filePath;
        List<AbstractPromiseTask> add = new ArrayList<>();
        asyncPromiseTask = new PromiseTask(
                App.getUniqueClassName(getClass()),
                promise,
                PromiseTaskExecuteType.EXTERNAL_WAIT_COMPUTE,
                null
        );
        add.add(asyncPromiseTask);
        add.add(new PromiseTaskWait(promise));
        promise.getQueueTask().addFirst(add);
    }

}
