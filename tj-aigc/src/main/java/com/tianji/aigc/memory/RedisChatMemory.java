package com.tianji.aigc.memory;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.tianji.common.utils.CollUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class RedisChatMemory implements ChatMemory {

    // 默认redis中key的前缀
    public static final String DEFAULT_PREFIX = "CHAT:";

    private final String prefix;

    public RedisChatMemory() {
        this.prefix = DEFAULT_PREFIX;
    }

    public RedisChatMemory(String prefix) {
        this.prefix = prefix;
    }

    // 注入spring redis模板，进行redis的操作
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 将消息添加到指定会话的Redis列表中。
     *
     * @param conversationId 会话ID
     * @param messages       需要添加的消息列表
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        if (CollUtil.isEmpty(messages)) {
            // 如果消息列表为空则直接返回
            return;
        }
        var redisKey = this.getKey(conversationId);
        var listOps = this.stringRedisTemplate.boundListOps(redisKey);
        // 将消息序列化并添加到Redis列表的右侧
        messages.forEach(message -> listOps.rightPush(MessageUtil.toJson( message)));
    }

    /**
     * 根据会话ID获取指定数量的最新消息
     *
     * @param conversationId 会话唯一标识符
     * @param lastN          需要获取的最后N条消息数量（N>0）
     * @return 包含消息对象的列表，若lastN<=0则返回空列表
     */
    @Override
    public List<Message> get(String conversationId, int lastN) {
        // 验证参数有效性，当lastN非正数时直接返回空结果
        if (lastN <= 0) {
            return List.of();
        }

        //redis查询，因为数据是右边进的，所以左边是最老的，符合分页
        List<String> list = stringRedisTemplate.opsForList().range(getKey(conversationId), 0, lastN);
        if(CollUtils.isEmpty(list)){
            log.error("redis没有查到记忆：conversionId：{}，lastN：{}",conversationId,lastN);
            return new ArrayList<>();
        }
        //逐条转换
        return list.stream().map(MessageUtil::toMessage).toList();
    }

    @Override
    public void clear(String conversationId) {
        var redisKey = this.getKey(conversationId);
        this.stringRedisTemplate.delete(redisKey);
    }

    private String getKey(String conversationId) {
        return prefix + conversationId;
    }
}