package ru.jamsys.core.plugin.telegram.sender;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.UniversalPath;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.expiration.ExpirationMap;
import ru.jamsys.core.extension.functional.ConsumerThrowing;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.plugin.telegram.BotRepositoryProperty;
import ru.jamsys.core.plugin.telegram.TelegramRequest;
import ru.jamsys.core.plugin.telegram.message.TelegramOutputMessage;
import ru.jamsys.core.plugin.telegram.structure.Button;
import ru.jamsys.core.plugin.telegram.structure.FileSource;
import ru.jamsys.core.resource.http.HttpResource;
import ru.jamsys.core.resource.http.client.AbstractHttpConnector;
import ru.jamsys.core.resource.http.client.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class TelegramSenderHttp extends AbstractManagerElement implements TelegramSender {

    @JsonIgnore
    private final SecurityComponent securityComponent;

    private final String ns;

    @SuppressWarnings("all")
    private final String key;

    @JsonIgnore
    private final BotRepositoryProperty botRepositoryProperty = new BotRepositoryProperty();

    @JsonIgnore
    private final PropertyDispatcher<Object> propertyDispatcher;

    @JsonIgnore
    private ManagerConfiguration<ExpirationMap<String, String>> fileUploadManagerConfiguration;

    public TelegramSenderHttp(String ns, String key) {
        this.ns = ns;
        this.key = key;
        this.securityComponent = App.get(SecurityComponent.class);
        propertyDispatcher = new PropertyDispatcher<>(
                null,
                botRepositoryProperty,
                ns
        );
        //UtilLog.printInfo(botRepositoryProperty);
    }

    public TelegramRequest.Result send(TelegramOutputMessage telegramOutputMessage) {
        markActive();
        switch (telegramOutputMessage.getMessageType()) {
            case SendPhoto -> {
                return sendImage(telegramOutputMessage);
            }
            case SendVideo -> {
                return sendVideo(telegramOutputMessage);
            }
            case SendMessage -> {
                return nativeSend("sendMessage", telegramOutputMessage, abstractHttpConnector -> {
                    Map<String, Object> requestBody = new HashMap<>();
                    requestBody.put("chat_id", telegramOutputMessage.getIdChat());
                    requestBody.put("parse_mode", "HTML");
                    String message = telegramOutputMessage.getMessage();
                    if (message != null && !message.isEmpty()) {
                        requestBody.put("text", message);
                    }
                    List<Button> buttons = telegramOutputMessage.getButtons();
                    if (buttons != null && !buttons.isEmpty()) {
                        List<List<Map<String, Object>>> list = new ArrayList<>();
                        buttons.forEach(button -> {
                            Map<String, Object> objectObjectHashMap = new HashMap<>();
                            objectObjectHashMap.put("text", button.getData());
                            if (button.getCallback() != null) {
                                objectObjectHashMap.put("callback_data", button.getCallback());
                            }
                            if (button.getUrl() != null) {
                                objectObjectHashMap.put("url", button.getUrl());
                            }
                            if (button.getWebapp() != null) {
                                objectObjectHashMap.put("web_app", new HashMapBuilder<String, String>()
                                        .append("url", button.getWebapp()));
                            }
                            list.add(List.of(objectObjectHashMap));
                        });
                        requestBody.put("reply_markup", new HashMapBuilder<>().append("inline_keyboard", list));
                    }
                    abstractHttpConnector.setBodyRaw(
                            UtilJson.toStringPretty(requestBody, "{}").getBytes(StandardCharsets.UTF_8)
                    );
                });
            }
        }
        return null;
    }

    private TelegramRequest.Result nativeSend(
            String action,
            TelegramOutputMessage telegramOutputMessage,
            ConsumerThrowing<AbstractHttpConnector> setup
    ) {
        ManagerConfiguration<HttpResource> httpResourceManagerConfiguration = ManagerConfiguration.getInstance(
                getClass().getSimpleName(),
                getClass().getSimpleName(),
                HttpResource.class,
                null
        );
        AbstractHttpConnector httpConnector = App.get(Manager.class).get(
                        HttpResource.class,
                        getClass().getSimpleName(),
                        getClass().getSimpleName(),
                        null
                )
                .prepare();
        return TelegramRequest.execute(result -> {
            setup.accept(httpConnector);
            httpConnector.addRequestHeader("Content-Type", "application/json");
            if (httpConnector.getUrl() == null) {
                throw new RuntimeException("url is null");
            }
            httpConnector.setUrl(String.format(
                    httpConnector.getUrl(),
                    new String(securityComponent.get(botRepositoryProperty.getSecurityAlias())),
                    action
            ));
            HttpResponse httpResponse = httpConnector.exec();
            result.setHttpResponse(httpResponse);
            Map<String, Object> parsedResponse = null;
            try {
                parsedResponse = UtilJson.getMapOrThrow(httpResponse.getBodyAsString());
                result.setParsedResponse(parsedResponse);
            } catch (Throwable th) {
                App.error(th);
            }
            if (parsedResponse != null && parsedResponse.containsKey("description")) {
                throw new RuntimeException(parsedResponse.get("description").toString());
            }
            if (!httpResponse.isSuccess()) {
                throw httpResponse.getException();
            }
        });

    }

    private TelegramRequest.Result sendImageMultipart(TelegramOutputMessage telegramOutputMessage) {
        FileSource image = telegramOutputMessage.getImage();
        TelegramRequest.Result sendRaw = nativeSend("sendPhoto", telegramOutputMessage, abstractHttpConnector -> {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("chat_id", String.valueOf(telegramOutputMessage.getIdChat()));
            if (image.getDescription() != null && !image.getDescription().isEmpty()) {
                builder.addTextBody("caption", image.getDescription());
            }
            builder.addBinaryBody(
                    "photo",
                    UtilFile.getWebFile(image.getPath()),
                    ContentType.parse(Files.probeContentType(Path.of(image.getPath()))),
                    new UniversalPath(image.getPath()).getFileName()
            );
            HttpEntity httpEntity = builder.build();
            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                httpEntity.writeTo(byteArrayOutputStream); // keeps us from calling getContent which will throw Content Length unknown
                abstractHttpConnector.setBodyRaw(byteArrayOutputStream.toByteArray());
            }
            abstractHttpConnector.addRequestHeader(httpEntity.getContentType().getName(), httpEntity.getContentType().getValue());
        });
        if (sendRaw.isOk()) {
            String filePhotoId = getFilePhotoId(sendRaw.getParsedResponse());
            if (filePhotoId != null) {
                fileUploadManagerConfiguration.get().put(image.getPath(), filePhotoId);
            }
        }
        return sendRaw;
    }

    private TelegramRequest.Result sendImage(TelegramOutputMessage telegramOutputMessage) {
        FileSource image = telegramOutputMessage.getImage();
        if (image.getId() == null || image.getId().isEmpty()) {
            image.setId(fileUploadManagerConfiguration.get().get(image.getPath()));
        }
        if (image.getId() == null || image.getId().isEmpty()) {
            return sendImageMultipart(telegramOutputMessage);
        }
        return nativeSend("sendPhoto", telegramOutputMessage, abstractHttpConnector -> {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("chat_id", telegramOutputMessage.getMessage());
            String message = telegramOutputMessage.getMessage();
            if (message != null && !message.isEmpty()) {
                requestBody.put("caption", message);
            }
            requestBody.put("photo", image.getId());
            abstractHttpConnector.setBodyRaw(
                    UtilJson.toStringPretty(requestBody, "{}").getBytes(StandardCharsets.UTF_8)
            );
        });
    }


    private TelegramRequest.Result sendVideo(TelegramOutputMessage telegramOutputMessage) {
        // TODO: дописать загрузку видео
        FileSource video = telegramOutputMessage.getImage();
        if (video.getId() == null || video.getId().isEmpty()) {
            return null;
        }
        return nativeSend("sendVideo", telegramOutputMessage, abstractHttpConnector -> {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("chat_id", telegramOutputMessage.getIdChat());
            String message = telegramOutputMessage.getMessage();
            if (message != null && !message.isEmpty()) {
                requestBody.put("caption", message);
            }
            requestBody.put("video", video.getId());
            abstractHttpConnector.setBodyRaw(
                    UtilJson.toStringPretty(requestBody, "{}").getBytes(StandardCharsets.UTF_8)
            );
        });
    }

    private static String getFilePhotoId(Map<String, Object> message) {
        if (message.containsKey("result")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) message.get("result");
            message = result;
        }
        if (!message.containsKey("photo")) {
            return null;
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> photos = (List<Map<String, Object>>) message.get("photo");
        // Проходим по всем фото в массиве
        String largestFileId = null;
        int maxResolution = 0;
        for (Map<String, Object> photo : photos) {
            int width = Integer.parseInt(photo.get("width").toString());
            int height = Integer.parseInt(photo.get("height").toString());
            int resolution = width * height; // Вычисляем разрешение (ширина * высота)
            // Если текущее фото имеет большее разрешение, обновляем данные
            if (resolution > maxResolution) {
                maxResolution = resolution;
                largestFileId = photo.get("file_id").toString();
            }
        }
        return largestFileId;
    }

    @Override
    public void runOperation() {
        propertyDispatcher.run();
        fileUploadManagerConfiguration = ManagerConfiguration.getInstance(
                ns,
                ns,
                ExpirationMap.class,
                integerXTestExpirationMap -> integerXTestExpirationMap
                        .setupTimeoutElementExpirationMs(botRepositoryProperty.getCacheFileUploadTimeoutMs())
        );
    }

    @Override
    public void shutdownOperation() {
        propertyDispatcher.shutdown();
    }

}
