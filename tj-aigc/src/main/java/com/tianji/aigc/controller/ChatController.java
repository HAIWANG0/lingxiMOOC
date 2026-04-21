package com.tianji.aigc.controller;

import com.tianji.aigc.dto.ChatDTO;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.vo.ChatEventVO;
import com.tianji.aigc.vo.TemplateVO;
import com.tianji.common.annotations.NoWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @NoWrapper
    public Flux<ChatEventVO> chat(@RequestBody ChatDTO chatDTO) {
        return chatService.chat(chatDTO.getQuestion(), chatDTO.getSessionId());
    }
    @PostMapping("/stop")
    public void stop (@RequestParam String sessionId) {
        chatService.stop(sessionId);
    }
    @PostMapping("/text")
    public String chatText(@RequestBody String question) {
        return this.chatService.chatText(question);
    }
    private static final TemplateVO TEMPLATE_VO = new TemplateVO();

    @GetMapping("/templates")
    public TemplateVO getTemplates() {
        return TEMPLATE_VO;
    }
}
