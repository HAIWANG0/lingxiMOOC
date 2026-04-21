package com.tianji;

import cn.hutool.core.collection.CollStreamUtil;
import cn.hutool.core.lang.Assert;
import com.tianji.aigc.agent.*;
import com.tianji.aigc.enums.AgentTypeEnum;
import com.tianji.aigc.vo.ChatEventVO;
import com.tianji.common.utils.UserContext;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.List;

@SpringBootTest
@Slf4j
public class YourTestClass  {
    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private RouteAgent routeAgent;
    @Autowired
    private RecommendAgent recommendAgent;
    @Test
    void contextLoads() {
        List<String> messages = List.of(
                "课程id：1880521847886917634\n课程分类：IT·互联网/互联网运营/产品运营\n课程名称：互联网产品运营实战\n适学人群：本课程适合20至35岁之间的学员，要求具备大专及以上学历，并且需要有一定的市场营销或产品管理基础知识。学员应对互联网行业有浓厚的兴趣，愿意深入学习如何管理和优化互联网产品。",

                "课程id：1880522777017528321\n课程分类：IT·互联网/互联网运营/产品运营\n课程名称：高级产品运营策略与实践\n适学人群：年龄20岁以上，具有大专及以上学历，至少1年的产品运营工作经验。适合希望深化自己的运营技能，掌握更高级的产品管理技巧的专业人士。",

                "课程id：1880524734406930434\n课程分类：IT·互联网/互联网运营/产品运营\n课程名称：产品运营入门实战\n适学人群：年龄不限，学历不限，但推荐有基础的互联网使用能力者。适合想要转行进入产品运营领域、或者刚刚开始接触产品运营工作的初学者。",

                "课程id：1880528608253521922\n课程分类：IT·互联网/互联网运营/游戏运营\n课程名称：游戏运营与用户增长策略\n适学人群：本课程适合22至35岁之间的学员，要求具备大专及以上学历，并且需要有一定的市场营销或游戏行业基础知识。学员应对游戏行业有浓厚的兴趣，愿意深入了解如何通过有效的运营策略提升游戏产品的用户粘性和市场表现。",

                "课程id：1880529742615621634\n课程分类：IT·互联网/java开发/java进阶\n课程名称：Java进阶编程实战\n适学人群：年龄不限，学历要求大专及以上，具备基础的Java编程知识和一定的项目开发经验。适合想要深入学习Java高级特性和设计模式，提升自己在实际工作中解决问题能力的开发者。",

                "课程id：1880529463279169537\n课程分类：IT·互联网/java开发/java进阶\n课程名称：Java高级架构与微服务\n适学人群：年龄20岁以上，本科及以上学历，至少1年的Java开发经验。适用于那些希望从单一应用程序开发转向分布式系统架构设计的专业人士。",

                "课程id：1880531521654829057\n课程分类：IT·互联网/java开发/java进阶\n课程名称：高级Java开发与架构设计\n适学人群：本课程适合25至40岁之间的学员，要求具备硕士及以上学历，并且需要至少两年的Java开发工作经验。学员应熟悉Java开发流程和市场需求分析，希望进一步提升自己在Java开发和架构设计方面的能力。",

                "课程id：1880532172006830082\n课程分类：IT·互联网/java开发/java进阶\n课程名称：Java大数据处理与分析\n适学人群：年龄22岁以上，本科及以上学历，具备扎实的Java编程基础，并对大数据技术有兴趣或需求的人士。特别适合那些希望在数据密集型应用场景下利用Java进行高效数据处理的工程师。",

                "课程id：1880532674207625218\n课程分类：IT·互联网/java开发/java进阶\n课程名称：Java进阶与企业级应用开发\n适学人群：本课程适合24至38岁之间的学员，要求具备大专及以上学历，并且需要至少一年的Java开发工作经验。学员应熟悉Java开发流程和市场需求分析，希望进一步提升自己在Java开发和企业级应用开发方面的能力。",

                "课程id：1880533253575225346\n课程分类：IT·互联网/java开发/java零基础\n课程名称：Java开发零基础入门\n适学人群：本课程适合18至28岁之间的学员，要求具备大专及以上学历，无需任何编程基础。学员应具有对软件开发的浓厚兴趣，希望在Java开发领域有所发展。"
        );
        log.info("保存到向量数据库中，消息数据：{}", messages);
        //构建文档
        List<Document> documents = CollStreamUtil.toList(messages, message -> Document.builder()
                .text(message)
                .build());
        //存储到向量数据库中
        this.vectorStore.add(documents);
        log.info("保存到向量数据库成功, 数量：{}", messages.size());
    }
    @Test
    public void testChat(){
        Assert.equals(this.routeAgent.process("最新有哪些课程", "1"), AgentTypeEnum.RECOMMEND.getAgentName());

        Assert.equals(this.routeAgent.process("这个课程是多少钱", "1"), AgentTypeEnum.CONSULT.getAgentName());
//        Assert.equals(this.routeAgent.process("java是什么", "1"), AgentTypeEnum.KNOWLEDGE.getAgentName());
//        Assert.equals(this.routeAgent.process("下单购买这个课程", "1"), AgentTypeEnum.BUY.getAgentName());
    }

    @Test
    public void testRecommend() throws InterruptedException {
        String question = "推荐课程，20岁，本科，无编程基础，对产品运营感兴趣";
        String sessionId = "123";
        UserContext.setUser(123L);
        Flux<ChatEventVO> flux = recommendAgent.processStream(question, sessionId);
        flux.subscribe(System.out::println);

        // 阻塞主线程，防止主线程结束，子线程终止
        Thread.sleep(100000);
    }
    @Resource
    private BuyAgent buyAgent;

    @Test
    public void processStream1() throws InterruptedException {
        String question = "下单购买，课程id为：1589905661084430337";
        String sessionId = "123";
        UserContext.setUser(123L);
        Flux<ChatEventVO> flux = buyAgent.processStream(question, sessionId);
        flux.subscribe(System.out::println);

        // 阻塞主线程，防止主线程结束，子线程终止
        Thread.sleep(100000);
    }
    @Resource
    private ConsultAgent consultAgent;

    @Test
    public void processStream() throws InterruptedException {
        String question = "课程多少钱，课程id为：1589905661084430337";
        String sessionId = "123";
        UserContext.setUser(123L);
        Flux<ChatEventVO> flux = consultAgent.processStream(question, sessionId);
        flux.subscribe(System.out::println);

        // 阻塞主线程，防止主线程结束，子线程终止
        Thread.sleep(100000);
    }

    @Resource
    private KnowledgeAgent knowledgeAgent;

    @Test
    public void processStream3() throws InterruptedException {
        String question = "简要说明，java什么";
        String sessionId = "123";
        UserContext.setUser(123L);
        Flux<ChatEventVO> flux = knowledgeAgent.processStream(question, sessionId);
        flux.subscribe(System.out::println);

        // 阻塞主线程，防止主线程结束，子线程终止
        Thread.sleep(100000);
    }

}
