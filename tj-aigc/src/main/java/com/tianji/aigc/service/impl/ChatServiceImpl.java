package com.tianji.aigc.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.contants.Constant;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.service.ChatSessionService;
import com.tianji.aigc.tools.result.CourseInfo;
import com.tianji.aigc.tools.result.PrePlaceOrder;
import com.tianji.aigc.vo.ChatEventVO;
import com.tianji.common.utils.DateUtils;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final ChatClient chatClient;
    private final SystemPromptConfig systemPromptConfig;
    // 存储大模型的生成状态，这里采用ConcurrentHashMap是确保线程安全
    // 目前的版本暂时用Map实现，如果考虑分布式环境的话，可以考虑用redis来实现
    private static final Map<String, Boolean> GENERATE_STATUS = new ConcurrentHashMap<>();
    private final ChatMemory redisChatMemory;
    private final StringRedisTemplate redis;
    private final VectorStore vectorStore;
    private final ChatSessionService chatSessionService;
    //输出结束的标记
    private static final ChatEventVO STOP_EVENT = ChatEventVO.builder()
            .eventType(ChatEventTypeEnum.STOP.getValue())
            .build();
    @Override
    public Flux<ChatEventVO> chat(String question , String sessionId) {
        // 生成会话ID
        String conversationId = ChatService.getConversationId(sessionId);
        Long user = UserContext.getUser();
        StringBuilder assistantText = new StringBuilder();
        //生成请求id
        String requestId = IdUtil.simpleUUID();
        Map<String, Object> map = new HashMap<>();
        map.put(Constant.REQUEST_ID, requestId);
        map.put(Constant.USER_ID, user);
        //更新标题
        chatSessionService.update(sessionId, question, user);
        return this.chatClient.prompt()
                .system(promptSystemSpec -> promptSystemSpec.text(systemPromptConfig.getChatSystemMessage().get())
                        .param("now", DateUtils.now()))
                .advisors(advisorSpec -> advisorSpec.advisors(new QuestionAnswerAdvisor(vectorStore, SearchRequest.builder().query("").topK(999).build())).param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId))
                .toolContext(map)
                .user(question)
                .stream()
                .chatResponse()
                .doFirst(() ->  GENERATE_STATUS.put(sessionId, true))
                .doOnComplete(() -> GENERATE_STATUS.remove(sessionId))
                .doOnError(throwable -> GENERATE_STATUS.remove(sessionId))
                .doOnCancel(()-> saveStopHistoryRecord(conversationId, assistantText.toString()))
                .takeWhile(s -> GENERATE_STATUS.getOrDefault(sessionId, false))
                .map(chatResponse -> {
                    // 获取大模型的输出的内容
                    String text = chatResponse.getResult().getOutput().getText();
                    assistantText.append(text);
                    // 封装响应对象
                    return ChatEventVO.builder()
                            .eventData(text)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                })
                .concatWith(Flux.defer(()->{
                    String courseInfo = (String) redis.opsForHash().get("AIGC:TOOLS:COURSE" , requestId);
                    String orderInfo = (String) redis.opsForHash().get("AIGC:TOOLS:ORDER" , requestId);
                    if (courseInfo != null){
                        CourseInfo bean = JSONUtil.toBean(courseInfo, CourseInfo.class);
                        Map<String,Object> toolsMap = new HashMap<>();
                        //遵循前端的要求，所以这里再套一层map，在tools里套也可以
                        toolsMap.put("courseInfo_"+bean.getId(),bean);
                        return Flux.just(ChatEventVO.builder()
                                .eventData(toolsMap)
                                .eventType(ChatEventTypeEnum.PARAM.getValue())
                                .build(), STOP_EVENT);
                    }
                    if (orderInfo != null){
                        PrePlaceOrder bean = JSONUtil.toBean(orderInfo, PrePlaceOrder.class);
                        Map<String,Object> toolsMap = new HashMap<>();
                        //遵循前端的要求，所以这里再套一层map，在tools里套也可以
                        toolsMap.put("prePlaceOrder",bean);
                        return Flux.just(ChatEventVO.builder()
                                .eventData(toolsMap)
                                .eventType(ChatEventTypeEnum.PARAM.getValue())
                                .build(), STOP_EVENT);
                    }
                    return Flux.just(STOP_EVENT);
                }));
    }

    /**
     * 保存停止输出的记录
     *
     * @param conversationId 会话id
     * @param content        大模型输出的内容
     */
    private void saveStopHistoryRecord(String conversationId , String content) {
        redisChatMemory.add(conversationId, new AssistantMessage(content));
    }

    @Override
    public void stop(String sessionId) {
        GENERATE_STATUS.remove(sessionId);
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
