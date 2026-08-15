package github.comioko.articlepilot.agent.context;

import github.comioko.articlepilot.model.enums.ArticleStyleEnum;
import github.comioko.articlepilot.model.enums.ImageMethodEnum;

import java.util.List;
import java.util.Map;

/**
 * 文章生成上下文（Context Engineering 入口）
 *
 * <p>所有 Agent 读取 prompt 变量不再直接拼字符串，而是构造一个不可变的 {@code ArticleContext}，
 * 通过 {@link #render(String)} 一次性完成所有占位符替换。
 *
 * <p>集中解决：
 * <ul>
 *   <li>风格片段重复解析（消除 4+ 处 switch 重复）</li>
 *   <li>配图方式说明散落在每个 agent 里</li>
 *   <li>长度预算硬编码在 prompt 字符串里</li>
 *   <li>用户描述 / 标题 / 大纲在 agent 间手动传递</li>
 * </ul>
 *
 * @author AI Passage Creator
 */
public record ArticleContext(
        String taskId,
        String topic,
        String userDescription,
        ArticleStyleEnum style,
        String styleFragment,
        List<ImageMethodEnum> enabledImageMethods,
        String availableMethodsDescription,
        String methodUsageGuide,
        LengthBudget lengthBudget,
        Map<String, String> templateVars
) {

    /**
     * 长度预算（按风格区分的字数目标与上下界）
     */
    public record LengthBudget(int targetWords, int minWords, int maxWords) {
        public String asPromptText() {
            return "建议字数 " + targetWords + "（" + minWords + "-" + maxWords + " 字）";
        }
    }

    /**
     * 一次性替换模板里的所有 {key} 占位符。
     *
     * <p>按 LinkedHashMap 顺序单遍替换；缺失的 key 保留原样（便于调试）。
     *
     * @param template 含 {placeholder} 的 prompt 模板
     * @return 替换后的字符串
     */
    public String render(String template) {
        if (template == null || templateVars == null || templateVars.isEmpty()) {
            return template;
        }
        String out = template;
        for (Map.Entry<String, String> e : templateVars.entrySet()) {
            if (e.getKey() == null) continue;
            if (out.contains(e.getKey())) {
                String v = e.getValue();
                out = out.replace(e.getKey(), v == null ? "" : v);
            }
        }
        return out;
    }

    /**
     * 风格附加片段（用于在 prompt 末尾追加）；未指定风格时返回空串。
     */
    public String styleSuffix() {
        return styleFragment == null ? "" : styleFragment;
    }

    /**
     * 是否启用了指定的配图方式
     */
    public boolean isImageMethodEnabled(ImageMethodEnum method) {
        if (method == null) return false;
        if (enabledImageMethods == null || enabledImageMethods.isEmpty()) return true;
        return enabledImageMethods.contains(method);
    }
}
