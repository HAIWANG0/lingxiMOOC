package com.tianji.aigc.service.impl;

import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioTranscriptionModel;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioTranscriptionOptions;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeSpeechSynthesisModel;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisPrompt;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResponse;
import com.alibaba.cloud.ai.dashscope.audio.transcription.AudioTranscriptionModel;
import com.github.houbb.opencc4j.util.ZhConverterUtil;
import com.tianji.aigc.service.AudioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import reactor.core.publisher.Flux;

import com.alibaba.cloud.ai.dashscope.api.DashScopeAudioTranscriptionApi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioServiceImpl implements AudioService {
    private final DashScopeSpeechSynthesisModel dashScopeSpeechSynthesisModel;
    private final AudioTranscriptionModel dashScopeAudioTranscriptionModel;


    @Override
    public ResponseBodyEmitter ttsStream(String text) {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter();
        log.info("开始语音合成, 文本内容：{}", text);
        SpeechSynthesisPrompt speechPrompt = new SpeechSynthesisPrompt(text);
        Flux<SpeechSynthesisResponse> responseStream = dashScopeSpeechSynthesisModel.stream(speechPrompt);
        // 订阅响应流并发送数据
/*        responseStream.subscribe(
                speechResponse -> {
                    try {
                        // 获取响应输出的数据，并发送到响应体中
                        byte[] audioBytes = speechResponse.getResult().getOutput().getAudio().array();
                        emitter.send(audioBytes);
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                emitter::completeWithError,
                emitter::complete
        );*/
        responseStream.subscribe(
                speechResponse -> {
                    try {
                        java.nio.ByteBuffer buffer = speechResponse.getResult().getOutput().getAudio();

                        // 正确处理只读 ByteBuffer
                        byte[] audioBytes;
                        if (buffer.hasArray()) {
                            // 有底层数组且可访问
                            audioBytes = buffer.array();
                        } else {
                            // 只读 Buffer 或没有底层数组，需要手动复制
                            audioBytes = new byte[buffer.remaining()];
                            buffer.get(audioBytes);
                        }

                        if (audioBytes.length > 0) {
                            emitter.send(audioBytes);
                            log.debug("发送音频片段: {} bytes", audioBytes.length);
                        }
                    } catch (IOException e) {
                        log.error("发送音频数据失败", e);
                        emitter.completeWithError(e);
                    } catch (Exception e) {
                        log.error("处理音频数据异常", e);
                        emitter.completeWithError(e);
                    }
                },
                error -> {
                    log.error("语音合成流出错", error);
                    emitter.completeWithError(error);
                },
                () -> {
                    log.info("语音合成完成");
                    emitter.complete();
                }
        );
        return emitter;
    }

        @Override
        public String stt(MultipartFile audioFile) {
            File tempFile = null;
            try {
               // 1. 创建临时文件
                tempFile = File.createTempFile("stt-" , ".wav");
                audioFile.transferTo(tempFile);
 /*
                // 2. 使用 FileSystemResource（关键！）
                Resource audioResource = new FileSystemResource(tempFile);
                // 3. 调用识别
                AudioTranscriptionPrompt request = new AudioTranscriptionPrompt(audioResource);
                AudioTranscriptionResponse response = dashScopeAudioTranscriptionModel.call(request);

                String output = response.getResult().getOutput();

                return ZhConverterUtil.toSimple(output);*/

                Path filePath =tempFile.toPath();

                AudioTranscriptionResponse response = dashScopeAudioTranscriptionModel
                        .call(
                                new AudioTranscriptionPrompt(
                                        new FileSystemResource(filePath),
                                        DashScopeAudioTranscriptionOptions.builder()
                                                .withModel("paraformer-v2")
                                                .withFormat(DashScopeAudioTranscriptionOptions.AudioFormat.WAV)
                                                .build()
                                )
                        );

                String output = response.getResult().getOutput();

                return ZhConverterUtil.toSimple(output);
            } catch (Exception e) {
                throw new RuntimeException("语音识别失败" , e);
            } finally {
                if (tempFile != null) {
                    tempFile.delete();
                }
            }
        }

}
