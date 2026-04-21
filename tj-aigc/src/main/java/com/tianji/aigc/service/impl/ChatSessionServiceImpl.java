package com.tianji.aigc.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tianji.aigc.config.SessionProperties;
import com.tianji.aigc.entity.ChatSession;
import com.tianji.aigc.enums.MessageTypeEnum;
import com.tianji.aigc.mapper.ChatSessionMapper;
import com.tianji.aigc.memory.RedisChatMemory;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.service.ChatSessionService;
import com.tianji.aigc.vo.ChatHistoryVO;
import com.tianji.aigc.vo.MessageVO;
import com.tianji.aigc.vo.SessionVO;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.RandomUtils;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatSessionServiceImpl implements ChatSessionService {
    private final SessionProperties sessionProperties;
    private final ChatSessionMapper sessionMapper;
    private final ChatMemory redisChatMemory;
    public static final int HISTORY_SIZE = 1000;

    @Override
    public SessionVO createSession(Integer n) {
        SessionVO sessionVO = BeanUtil.toBean(sessionProperties , SessionVO.class);
        sessionVO.setExamples(RandomUtils.randomEleList(sessionVO.getExamples(), n));
        sessionVO.setSessionId(IdUtil.fastSimpleUUID());
        ChatSession build = ChatSession.builder()
                .sessionId(sessionVO.getSessionId())
                .userId(UserContext.getUser())
                .build();
        sessionMapper.insert(build);
        return sessionVO;
    }

    @Override
    public List<MessageVO> getSessionMessages(String sessionId) {
        String conversationId = ChatService.getConversationId(sessionId);
        List<Message> messages = redisChatMemory.get(conversationId , HISTORY_SIZE);
        return messages.stream()
                .filter(message -> message.getMessageType() == MessageType.ASSISTANT || message.getMessageType() == MessageType.USER)
                .map(message -> MessageVO.builder()
                        .type(message.getMessageType() == MessageType.ASSISTANT ? MessageTypeEnum.ASSISTANT : MessageTypeEnum.USER)
                        .content(message.getText())
                        .build())
                .toList();
    }

    @Override
    @Async
    public void update(String sessionId , String title , Long userId) {
        List<ChatSession> session = sessionMapper.selectList(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, userId));
        if (session.isEmpty()) {
            return;
        }
        //获取会话实例
        ChatSession chatSession = session.get(0);
        // 如果聊天会话的标题为空，并且新标题不为空，则更新标题
        if (StrUtil.isEmpty(chatSession.getTitle()) && !StrUtil.isEmpty(title)) {
            chatSession.setTitle(StrUtil.sub(title, 0, 100));
        }
        // 设置更新字段为updateTime为当前时间
        chatSession.setUpdateTime(LocalDateTimeUtil.now());
        // 更新数据库中的聊天会话信息
        sessionMapper.updateById(chatSession);

    }

    /**
     * 获取会话历史
     * @return
     */
    @Override
    public Map<String, List<ChatHistoryVO>> getSessionHistory() {
        Long user = UserContext.getUser();
        List<ChatSession> sessions = sessionMapper.selectList(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, user)
                .isNotNull(ChatSession::getTitle)
                .orderByDesc(ChatSession::getUpdateTime)
                .last("LIMIT 30")
        );
        if (sessions.isEmpty()) {
            log.info("No chat sessions found for user: {}", user);
            return Map.of();
        }
        //数据转换
        List<ChatHistoryVO> chatHistoryVOList = sessions.stream().map(session -> ChatHistoryVO.builder()
                .sessionId(session.getSessionId())
                .title(session.getTitle())
                .updateTime(session.getUpdateTime()).build()).toList();
        final String TODAY = "当天";
        final String LAST_30_DAYS = "最近30天";
        final String LAST_YEAR = "最近1年";
        final String MORE_THAN_YEAR = "1年以上";

        // 分组
        return CollStreamUtil.groupByKey(chatHistoryVOList, chatHistoryVO -> {
            long between = LocalDateTimeUtil.between(chatHistoryVO.getUpdateTime(), LocalDateTimeUtil.now()).toDays();
            if (between <= 1) {
                return TODAY;
            } else if (between <= 30) {
                return LAST_30_DAYS;
            } else if (between <= 365) {
                return LAST_YEAR;
            } else {
                return MORE_THAN_YEAR;
            }
        });
    }

    @Override
    public void deleteHistorySession(String sessionId) {
        sessionMapper.delete(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, UserContext.getUser()));
        //删除redis中的数据
        String conversationId = ChatService.getConversationId(sessionId);
        redisChatMemory.clear(conversationId);
    }

    @Override
    public void updateTitle(String sessionId , String title) {
        sessionMapper.update(null, new LambdaUpdateWrapper<ChatSession>()
                .eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, UserContext.getUser())
                .set(ChatSession::getTitle, title.substring(0, Math.min(title.length(), 100))));
    }
}
