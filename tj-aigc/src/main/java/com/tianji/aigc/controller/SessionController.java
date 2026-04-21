package com.tianji.aigc.controller;

import com.tianji.aigc.service.ChatSessionService;
import com.tianji.aigc.vo.ChatHistoryVO;
import com.tianji.aigc.vo.MessageVO;
import com.tianji.aigc.vo.SessionVO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/session")
@RequiredArgsConstructor
public class SessionController {
    private final ChatSessionService sessionService;

    /**
     *  创建会话
     * @param n
     * @return
     */
    @PostMapping
    public SessionVO createSession(@RequestParam(value = "n", defaultValue = "3") Integer n){
        return sessionService.createSession(n);
    }
    @GetMapping("/{sessionId}")
    public List<MessageVO> getSessionMessages(@PathVariable String sessionId){
        return sessionService.getSessionMessages(sessionId);
    }

    /**
     *  获取会话历史
     * @return
     */
    @GetMapping("/history")
    public Map<String, List<ChatHistoryVO>> getSessionHistory(){
        return sessionService.getSessionHistory();
    }
    /**
     * 删除历史会话列表
     */
    @DeleteMapping("/history")
    public void deleteHistorySession(@RequestParam("sessionId") String sessionId) {
        this.sessionService.deleteHistorySession(sessionId);
    }
    /**
     * 更新历史会话标题
     */
    @PutMapping("/history")
    public void updateTitle(@RequestParam("sessionId") String sessionId,
                            @RequestParam("title") String title) {
        this.sessionService.updateTitle(sessionId, title);
    }
}
