package github.comioko.articlepilot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import static github.comioko.articlepilot.constant.ArticleConstant.SVG_DEFAULT_HEIGHT;
import static github.comioko.articlepilot.constant.ArticleConstant.SVG_DEFAULT_WIDTH;

/**
 * SVG 概念示意图生成配置
 *
 * @author comioko
 */
@Configuration
@ConfigurationProperties(prefix = "svg-diagram")
@Data
public class SvgDiagramConfig {

    /**
     * 默认宽度
     */
    private Integer defaultWidth = SVG_DEFAULT_WIDTH;

    /**
     * 默认高度
     */
    private Integer defaultHeight = SVG_DEFAULT_HEIGHT;

    /**
     * COS 存储文件夹
     */
    private String folder = "svg-diagrams";
}