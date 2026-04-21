package com.tianji.api.client.aigc;

import com.tianji.api.client.learning.fallback.LearningClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "aigc-service", fallbackFactory = LearningClientFallback.class)
public interface AIGCClient {
    @PostMapping("/chat/text")
    String chatText(@RequestBody String question);
}
