package github.comioko.articlepilot.agent.context;

import com.alibaba.cloud.ai.graph.OverAllState;
import github.comioko.articlepilot.constant.PromptConstant;
import github.comioko.articlepilot.model.dto.article.ArticleState;
import github.comioko.articlepilot.model.enums.ArticleStyleEnum;
import github.comioko.articlepilot.model.enums.ImageMethodEnum;
import github.comioko.articlepilot.service.ImageServiceStrategy;
import github.comioko.articlepilot.utils.GsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@link ArticleContext} 工厂。
 *
 * <p>所有上下文派生逻辑集中在这里——避免散落在每个 agent 里的 {@code switch}、
 * {@code String.replace} 链。Agent 只负责从 ctx 拿东西。
 *
 * @author AI Passage Creator
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ArticleContextFactory {

    private final ImageServiceStrategy imageServiceStrategy;

    /**
     * 从 ArticleState 构建完整的 ArticleContext。
     *
     * @param state 任意阶段的 ArticleState（标题/大纲/正文阶段都接受）
     * @return 不可变的 ArticleContext
     */
    public ArticleContext from(ArticleState state) {
        if (state == null) {
            throw new IllegalArgumentException("ArticleState 不能为空");
        }

        ArticleStyleEnum styleEnum = ArticleStyleEnum.getEnumByValue(state.getStyle());
        String styleFragment = resolveStyleFragment(styleEnum);
        List<ImageMethodEnum> enabled = parseEnabledMethods(state.getEnabledImageMethods());
        ArticleContext.LengthBudget budget = deriveLengthBudget(styleEnum);
        String methodsDesc = buildMethodsDescription(enabled);
        String methodGuide = buildMethodUsageGuide(enabled);
        Map<String, String> vars = buildTemplateVars(state, styleFragment, methodsDesc, methodGuide, budget);

        return new ArticleContext(
                state.getTaskId(),
                state.getTopic(),
                state.getUserDescription(),
                styleEnum,
                styleFragment,
                enabled,
                methodsDesc,
                methodGuide,
                budget,
                vars
        );
    }

    /**
     * 从 Spring AI Alibaba 的 OverAllState 直接构建 ArticleContext。
     *
     * <p>供 NodeAction agent 调用——它们只能拿到 OverAllState，由本方法桥接到 ArticleState 再构造 ctx。
     *
     * @param state StateGraph 节点的输入状态
     * @return 不可变的 ArticleContext
     */
    public ArticleContext fromOverAllState(OverAllState state) {
        return from(rehydrate(state));
    }

    /**
     * 把 OverAllState 的关键 key 投影到 ArticleState。缺失字段填空。
     */
    private ArticleState rehydrate(OverAllState state) {
        if (state == null) return new ArticleState();

        ArticleState pojo = new ArticleState();
        pojo.setTaskId(stringOrNull(state, "taskId"));
        pojo.setTopic(stringOrNull(state, "topic"));
        pojo.setUserDescription(stringOrNull(state, "userDescription"));
        pojo.setStyle(stringOrNull(state, "style"));

        // 标题：拼装 TitleResult
        String mainTitle = stringOrNull(state, "mainTitle");
        String subTitle = stringOrNull(state, "subTitle");
        if (mainTitle != null || subTitle != null) {
            ArticleState.TitleResult title = new ArticleState.TitleResult();
            title.setMainTitle(mainTitle);
            title.setSubTitle(subTitle);
            pojo.setTitle(title);
        }

        // 大纲：尝试转换回 OutlineResult
        Object outlineObj = state.value("outline").orElse(null);
        if (outlineObj instanceof ArticleState.OutlineResult o) {
            pojo.setOutline(o);
        } else if (outlineObj != null) {
            try {
                ArticleState.OutlineResult o = GsonUtils.fromJson(GsonUtils.toJson(outlineObj),
                        ArticleState.OutlineResult.class);
                pojo.setOutline(o);
            } catch (Exception e) {
                log.debug("outline 字段无法反序列化为 OutlineResult，原样跳过: {}", e.getMessage());
            }
        }

        // 正文 / enabledImageMethods：直接从 key 取
        String content = stringOrNull(state, "content");
        if (content != null) pojo.setContent(content);
        Object enabledObj = state.value("enabledImageMethods").orElse(null);
        if (enabledObj instanceof List<?> list) {
            pojo.setEnabledImageMethods(list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .toList());
        }

        return pojo;
    }

    private static String stringOrNull(OverAllState state, String key) {
        return state.value(key).map(o -> o == null ? null : o.toString()).orElse(null);
    }

    // region style

    private String resolveStyleFragment(ArticleStyleEnum style) {
        if (style == null) return "";
        return switch (style) {
            case TECH -> PromptConstant.STYLE_TECH_PROMPT;
            case EMOTIONAL -> PromptConstant.STYLE_EMOTIONAL_PROMPT;
            case EDUCATIONAL -> PromptConstant.STYLE_EDUCATIONAL_PROMPT;
            case HUMOROUS -> PromptConstant.STYLE_HUMOROUS_PROMPT;
        };
    }

    /**
     * 按风格推导字数预算。
     * 未指定风格时使用 2000 字目标 / 1200-2500 字区间（与 PromptConstant 原硬编码一致）。
     */
    private ArticleContext.LengthBudget deriveLengthBudget(ArticleStyleEnum style) {
        if (style == null) return new ArticleContext.LengthBudget(2000, 1200, 2500);
        return switch (style) {
            case TECH -> new ArticleContext.LengthBudget(2000, 1800, 2200);
            case EMOTIONAL -> new ArticleContext.LengthBudget(1500, 1200, 1800);
            case EDUCATIONAL -> new ArticleContext.LengthBudget(2400, 2000, 2800);
            case HUMOROUS -> new ArticleContext.LengthBudget(1000, 800, 1200);
        };
    }

    // endregion

    // region enabled image methods

    /**
     * 把字符串列表解析为 ImageMethodEnum 列表；过滤掉降级方案（不允许 LLM 选 PICSUM）。
     * null / 空 表示"所有非降级方式"。
     */
    private List<ImageMethodEnum> parseEnabledMethods(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return imageServiceStrategy.getRegisteredMethods().stream()
                    .filter(m -> !m.isFallback())
                    .toList();
        }
        return raw.stream()
                .map(ImageMethodEnum::getByValue)
                .filter(Objects::nonNull)
                .filter(m -> !m.isFallback())
                .toList();
    }

    private String buildMethodsDescription(List<ImageMethodEnum> methods) {
        if (methods == null || methods.isEmpty()) return "（无可用配图方式）";
        StringBuilder sb = new StringBuilder();
        for (ImageMethodEnum m : methods) {
            sb.append("   - ").append(m.getValue())
                    .append(": ").append(m.getDescription())
                    .append("\n");
        }
        return sb.toString();
    }

    private String buildMethodUsageGuide(List<ImageMethodEnum> methods) {
        if (methods == null || methods.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (ImageMethodEnum m : methods) {
            String guide = detailedGuide(m);
            if (guide != null && !guide.isEmpty()) {
                sb.append(guide).append("\n");
            }
        }
        return sb.toString();
    }

    private String detailedGuide(ImageMethodEnum m) {
        if (m == null) return "";
        return switch (m) {
            case PEXELS -> "- PEXELS: 提供英文搜索关键词(keywords)，要准确、具体。prompt 留空。";
            case NANO_BANANA -> "- NANO_BANANA: 提供详细的英文生图提示词(prompt)，描述场景、风格、细节。keywords 留空。";
            case MERMAID -> "- MERMAID: 在 prompt 字段生成完整的 Mermaid 代码（如流程图、架构图）。keywords 留空。";
            case ICONIFY -> "- ICONIFY: 提供英文图标关键词(keywords)，如：check、arrow、star、heart。prompt 留空。";
            case EMOJI_PACK -> "- EMOJI_PACK: 提供中文或英文关键词(keywords)描述表情内容。prompt 留空。系统会自动添加\"表情包\"搜索。";
            case SVG_DIAGRAM -> "- SVG_DIAGRAM: 在 prompt 字段描述示意图需求（中文），说明要表达的概念和关系。keywords 留空。";
            default -> "";
        };
    }

    // endregion

    // region template vars

    /**
     * 把所有 prompt 占位符一次性填进 Map——Agent 不再各自拼字符串。
     *
     * <p>缺失字段（用户没传 userDescription / 没有大纲）一律填空串，render 时不会 NPE。
     */
    private Map<String, String> buildTemplateVars(
            ArticleState state,
            String styleFragment,
            String methodsDesc,
            String methodGuide,
            ArticleContext.LengthBudget budget) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("{topic}", nullToEmpty(state.getTopic()));
        vars.put("{userDescription}", nullToEmpty(state.getUserDescription()));
        vars.put("{mainTitle}", state.getTitle() == null ? "" : nullToEmpty(state.getTitle().getMainTitle()));
        vars.put("{subTitle}", state.getTitle() == null ? "" : nullToEmpty(state.getTitle().getSubTitle()));
        vars.put("{outline}", state.getOutline() == null || state.getOutline().getSections() == null
                ? "" : GsonUtils.toJson(state.getOutline().getSections()));
        vars.put("{content}", nullToEmpty(state.getContent()));
        vars.put("{styleFragment}", nullToEmpty(styleFragment));
        vars.put("{availableMethods}", nullToEmpty(methodsDesc));
        vars.put("{methodUsageGuide}", nullToEmpty(methodGuide));
        vars.put("{lengthBudget}", budget == null ? "" : budget.asPromptText());
        vars.put("{descriptionSection}", buildDescriptionSection(state.getUserDescription()));
        return vars;
    }

    /**
     * 用户补充描述段落（仅当 userDescription 非空时填充；与原 OutlineGeneratorAgent 行为一致）。
     */
    private String buildDescriptionSection(String userDescription) {
        if (userDescription == null || userDescription.isBlank()) return "";
        return PromptConstant.AGENT2_DESCRIPTION_SECTION
                .replace("{userDescription}", userDescription);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    // endregion
}
