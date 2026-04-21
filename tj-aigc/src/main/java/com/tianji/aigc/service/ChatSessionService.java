package com.tianji.aigc.service;

import com.tianji.aigc.vo.ChatHistoryVO;
import com.tianji.aigc.vo.MessageVO;
import com.tianji.aigc.vo.SessionVO;

import java.util.List;
import java.util.Map;

public interface ChatSessionService {
    SessionVO createSession(Integer n);

    List<MessageVO> getSessionMessages(String sessionId);
    /**
     * 更新会话更新时间
     *
     * @param sessionId 会话ID，用于标识特定的聊天会话
     * @param title     新的会话标题，如果为空则不进行更新
     * @param userId    用户ID
     */
    void update(String sessionId, String title, Long userId);

    /**
     *  获取会话历史
     * @return
     */

    Map<String, List<ChatHistoryVO>> getSessionHistory();

    /**
     * 删除会话
     * @param sessionId
     */
    void deleteHistorySession(String sessionId);

    void updateTitle(String sessionId , String title);
}
