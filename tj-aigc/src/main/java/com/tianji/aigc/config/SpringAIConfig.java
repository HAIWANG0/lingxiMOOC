package com.tianji.aigc.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeSpeechSynthesisApi;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeSpeechSynthesisModel;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeSpeechSynthesisOptions;
import com.tianji.aigc.memory.RedisChatMemory;
import com.tianji.aigc.tools.CourseTools;
import com.tianji.aigc.tools.OrderTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAIConfig {
    /**
     * 配置 ChatClient，自动装配的DashScopeChatModel，也就是阿里百炼大模型
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder,
                                 Advisor loggerAdvisor, ChatMemory redisChatMemory,
                                 CourseTools courseTools,
                                 OrderTools orderTools) {  // 日志记录器
        return chatClientBuilder
                .defaultAdvisors(loggerAdvisor) //添加 Advisor 功能增强
                .defaultAdvisors(new MessageChatMemoryAdvisor(redisChatMemory))
//                .defaultTools(courseTools, orderTools)
                .build();
    }

    /**
     * 日志记录器
     */
    @Bean
    public Advisor loggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }

}
