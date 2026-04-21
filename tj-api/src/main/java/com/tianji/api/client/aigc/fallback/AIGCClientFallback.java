package com.tianji.api.client.aigc.fallback;

import com.tianji.api.client.aigc.AIGCClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class AIGCClientFallback implements FallbackFactory<AIGCClient> {
    @Override
    public AIGCClient create(Throwable cause) {
        return new AIGCClient() {
            @Override
            public String chatText(String question) {
                return "调用aigc服务出错!";
            }
        };
    }
}
