package com.tianji.aigc.tools;

import cn.hutool.json.JSONUtil;
import com.tianji.aigc.contants.Constant;
import com.tianji.aigc.tools.result.CourseInfo;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseBaseInfoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseTools {
    private final CourseClient courseClient;
    private final StringRedisTemplate redisTemplate;
    @Tool(description = Constant.Tools.QUERY_COURSE_BY_ID)
    public CourseInfo queryCourseInfo(@ToolParam(description = Constant.ToolParams.COURSE_ID) Long courseId, ToolContext toolContext) {
        CourseBaseInfoDTO courseBaseInfoDTO = courseClient.baseInfo(courseId, true);
        CourseInfo courseInfo = CourseInfo.of(courseBaseInfoDTO);
        String requestId = (String) toolContext.getContext().get(Constant.REQUEST_ID);
        redisTemplate.opsForHash().put("AIGC:TOOLS:COURSE",requestId, JSONUtil.toJsonStr(courseInfo));
        return courseInfo;
    }
}
