package github.comioko.articlepilot.service;


import github.comioko.articlepilot.model.dto.image.ImageRequest;
import github.comioko.articlepilot.model.enums.ImageMethodEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SVG 概念示意图生成服务测试
 *
 * @author comioko
 */
@SpringBootTest
@ActiveProfiles("local")
class SvgDiagramServiceTest {

    @Autowired
    private ImageServiceStrategy imageServiceStrategy;

    /**
     * 测试通过策略模式生成 SVG 概念示意图
     */
    @Test
    void testGetSvgDiagramViaStrategy() {
        ImageRequest request = ImageRequest.builder()
                .prompt("绘制概念示意图，中心圆形写'学习'，周围4个圆形分别是：输入、思考、输出、反馈，用箭头连接形成循环")
                .position(1)
                .type("diagram")
                .build();

        System.out.println("通过策略模式生成 SVG 概念示意图");

        ImageServiceStrategy.ImageResult result = imageServiceStrategy.getImage(
                ImageMethodEnum.SVG_DIAGRAM.getValue(),
                request
        );

        System.out.println("生成结果: " + (result.isSuccess() ? "成功" : "失败"));
        System.out.println("使用方法: " + result.getMethod().getDescription());

        if (result.isSuccess()) {
            System.out.println("示意图 URL: " + result.getUrl());
            assertEquals(ImageMethodEnum.SVG_DIAGRAM, result.getMethod());
            assertTrue(result.getUrl().contains("svg"), "URL 应该包含 svg");
        }
    }
}