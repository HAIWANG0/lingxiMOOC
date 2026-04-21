package com.tianji.aigc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties("tj.ai.prompt")
public class AIProperties {
    private System system;
    @Data
    public static class System{
        private Chat chat;
        private Chat text;
        private Chat routeAgent;
        private Chat recommendAgent;
        private Chat buyAgent;
        private Chat consultAgent;
        private Chat knowledgeAgent;
        @Data
        public static class Chat{
            private String dataId;
            private String group = "DEFAULT_GROUP";
            private Long timeoutMs = 20000L;
        }
    }
}
