package com.tianji.learning.service.impl;

import cn.hutool.core.util.StrUtil;
import com.tianji.api.client.aigc.AIGCClient;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.ReplyDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.service.AIService;
import com.tianji.learning.service.IInteractionReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIServiceImpl implements AIService {
    private final AIGCClient aigcClient;
    private final IInteractionReplyService iInteractionReplyService;

    @Value("${tj.ai.user-id:9999}")
    private Long aiUserId;
    @Async
    @Override
    public void autoReply(InteractionQuestion interactionQuestion) {
        String question = StrUtil.format("""
                这是一个学生提出的问题，请以专业的角度进行回答，不要随意编造。
                标题：{} 。
                描述：{} 。""", interactionQuestion.getTitle(), interactionQuestion.getDescription());
        UserContext.setUser(interactionQuestion.getUserId());
        log.info("开始调用AIGC服务，问题：{}", question);
        // 调用AIGC服务
        String chatText = aigcClient.chatText(question);
        // 保存回复
        ReplyDTO replyDTO = new ReplyDTO();
        replyDTO.setUserId(aiUserId);
        replyDTO.setContent(chatText);
        replyDTO.setAnonymity(false);
        replyDTO.setQuestionId(interactionQuestion.getId());
        replyDTO.setIsStudent(false);
        //
        iInteractionReplyService.saveReply(replyDTO);
    }
}
