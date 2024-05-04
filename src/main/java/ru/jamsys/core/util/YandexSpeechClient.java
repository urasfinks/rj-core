package ru.jamsys.core.util;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import ru.jamsys.core.extension.Procedure;
import speechkit.common.v3.Common;
import syandex.cloud.api.ai.tts.v3.Tts;
import yandex.cloud.api.ai.tts.v3.SynthesizerGrpc;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class YandexSpeechClient {

    public final SynthesizerGrpc.SynthesizerStub client;

    public final ManagedChannel channel;

    public YandexSpeechClient(String host, int port, String apikey, long timeOutMs) {
        channel = ManagedChannelBuilder
                .forAddress(host, port)
                .keepAliveTimeout(timeOutMs, TimeUnit.MILLISECONDS)
                .build();

        Metadata headers = new Metadata();
        headers.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Api-Key " + apikey);
        headers.put(Metadata.Key.of("x-client-request-id", Metadata.ASCII_STRING_MARSHALLER), UUID.randomUUID().toString());

        client = SynthesizerGrpc.newStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
                .withDeadlineAfter(timeOutMs, TimeUnit.MILLISECONDS);
    }

    public void shutdown() throws Throwable {
        channel.shutdown();
        channel.awaitTermination(5000, TimeUnit.MILLISECONDS);
    }

    public void synthesize(String text, File output, Map<String, Object> defSettings, Procedure onComplete, Consumer<Throwable> onError) {
        Tts.UtteranceSynthesisRequest request = Tts.UtteranceSynthesisRequest
                .newBuilder()
                .setText(text)
                .setOutputAudioSpec(Common.AudioFormatOptions
                        .newBuilder()
                        .setContainerAudio(Common.ContainerAudio
                                .newBuilder()
                                .setContainerAudioType(Common.ContainerAudio.ContainerAudioType.WAV)
                                .build()))
                .addHints(Tts.Hints.newBuilder().setSpeed((double) defSettings.get("speed")))
                .addHints(Tts.Hints.newBuilder().setVoice((String) defSettings.get("voice")))
                .addHints(Tts.Hints.newBuilder().setRole((String) defSettings.get("role")))
                .setLoudnessNormalizationType(Tts.UtteranceSynthesisRequest.LoudnessNormalizationType.LUFS)
                .build();

        client.utteranceSynthesis(request, new StreamObserver<>() {

            final ByteArrayOutputStream result = new ByteArrayOutputStream();

            @Override
            public void onNext(Tts.UtteranceSynthesisResponse utteranceSynthesisResponse) {
                if (utteranceSynthesisResponse.hasAudioChunk()) {
                    try {
                        result.write(utteranceSynthesisResponse.getAudioChunk().getData().toByteArray());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {
                onError.accept(throwable);
            }

            @Override
            public void onCompleted() {
                try {
                    AudioInputStream audioStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(result.toByteArray()));
                    AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, output);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                onComplete.run();
            }
        });
    }

}
