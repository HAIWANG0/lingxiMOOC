package com.tianji.aigc.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.tianji.aigc.agent.AbstractAgent;
import com.tianji.aigc.agent.Agent;
import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.enums.AgentTypeEnum;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.vo.ChatEventVO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements ChatService {
    private final ChatClient chatClient;
    private final SystemPromptConfig systemPromptConfig;
    @Override
    public Flux<ChatEventVO> chat(String question , String sessionId) {
        String result = findAgentByType(AgentTypeEnum.ROUTE).process(question , sessionId);
        AgentTypeEnum agentType = AgentTypeEnum.agentNameOf(result);
        Agent agent = findAgentByType(agentType);
        if (agent == null) {
            // 找不到对应的智能体，直接返回结果
            ChatEventVO chatEventVO = ChatEventVO.builder()
                    .eventType(ChatEventTypeEnum.DATA.getValue())
                    .eventData(result)
                    .build();
            return Flux.just(chatEventVO, AbstractAgent.STOP_EVENT);
        }

        return agent.processStream(question, sessionId);
    }

    @Override
    public void stop(String sessionId) {
        findAgentByType(AgentTypeEnum.ROUTE).stop(sessionId);
    }
    public Agent findAgentByType(AgentTypeEnum agentType) {
        if (agentType == null){
            return null;
        }
        // 获取所有Agent
        Map<String, Agent> bean = SpringUtil.getBeansOfType(Agent.class);
        for (Agent agent : bean.values()) {
            if (agent.getAgentType().equals(agentType)){
                return agent;
            }
        }
        return null;
    }

    @Override
    public String chatText(String question) {
        return chatClient.prompt()
                .system(promptSystemSpec -> promptSystemSpec.text(systemPromptConfig.getTextSystemMessage().get()))
                .user( question)
                .call()
                .content();
    }

}

