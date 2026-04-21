package com.tianji.aigc.tools;

import cn.hutool.json.JSONUtil;
import com.tianji.aigc.contants.Constant;
import com.tianji.aigc.tools.result.PrePlaceOrder;
import com.tianji.api.client.trade.TradeClient;
import com.tianji.api.dto.trade.OrderConfirmVO;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderTools {
    private final TradeClient tradeClient;
    private final StringRedisTemplate redisTemplate;
    @Tool(description = Constant.Tools.PRE_PLACE_ORDER)
    public PrePlaceOrder prePlaceOrder(@ToolParam(description = Constant.ToolParams.COURSE_IDS) List<Number> courseIds, ToolContext toolContext){
        List<Long> longCourseIds = courseIds.stream()
                .map(Number::longValue)
                .toList();
        Long userId = (Long) toolContext.getContext().get(Constant.USER_ID);
        UserContext.setUser(userId);
        OrderConfirmVO orderConfirmVO = tradeClient.prePlaceOrder(longCourseIds);
        String requestId = (String) toolContext.getContext().get(Constant.REQUEST_ID);
        PrePlaceOrder prePlaceOrder = PrePlaceOrder.of(orderConfirmVO);
        redisTemplate.opsForHash().put("AIGC:TOOLS:ORDER",requestId, JSONUtil.toJsonStr(prePlaceOrder));
        return prePlaceOrder;

    }
}
