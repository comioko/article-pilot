package github.comioko.articlepilot.constant;

/**
 * Prompt 模板常量
 *
 * @author comioko
 */
public interface PromptConstant {

    /**
     * 智能体1：生成标题方案
     */
    String AGENT1_TITLE_PROMPT = """
            你是一位爆款文章标题专家,擅长创作吸引人的标题。
            
            根据以下选题,生成 3-5 个爆款文章标题方案:
            选题：{topic}
            
            要求:
            1. 每个方案包含主标题和副标题
            2. 主标题要包含数字、情绪化词汇,吸引眼球
            3. 副标题要补充说明,增强吸引力
            4. 标题要简洁有力,不超过30字
            5. 不同方案要有不同的切入角度
            6. 符合新媒体爆款文章的风格
            
            请直接返回 JSON 格式,不要有其他内容:
            [
              {
                "mainTitle": "主标题1",
                "subTitle": "副标题1"
              },
              {
                "mainTitle": "主标题2",
                "subTitle": "副标题2"
              },
              {
                "mainTitle": "主标题3",
                "subTitle": "副标题3"
              }
            ]
            """;

    /**
     * 智能体2：生成大纲
     */
    String AGENT2_OUTLINE_PROMPT = """
            你是一位专业的文章策划师,擅长设计文章结构。
            
            根据以下标题,生成文章大纲:
            主标题：{mainTitle}
            副标题：{subTitle}
            {descriptionSection}
            
            要求:
            1. 大纲要有清晰的逻辑结构
            2. 包含开头引入、核心观点(3-5个)、结尾升华
            3. 每个章节要有明确的标题和核心要点(2-3个)
            4. 适合2000字左右的文章
            
            请直接返回 JSON 格式,不要有其他内容:
            {
              "sections": [
                {
                  "section": 1,
                  "title": "章节标题",
                  "points": ["要点1", "要点2"]
                }
              ]
            }
            """;

    /**
     * 用户补充描述部分（动态插入到 AGENT2_OUTLINE_PROMPT）
     */
    String AGENT2_DESCRIPTION_SECTION = """
            
            用户补充要求：{userDescription}
            请在大纲中充分体现用户的补充要求。
            """;

    /**
     * SVG 概念示意图生成 Prompt
     */
    String SVG_DIAGRAM_GENERATION_PROMPT = """
            ### 背景 ###
            你是一位资深的信息可视化设计师，擅长将抽象概念转化为直观易懂的 SVG 示意图。
            你的作品曾用于知名媒体和技术文档，风格简洁现代、逻辑清晰。
            
            ### 需求 ###
            {requirement}
            
            ### 任务步骤 ###
            1. 分析需求：理解要表达的核心概念和逻辑关系
            2. 设计布局：确定图形的整体结构（中心辐射、层级、流程等）
            3. 选择元素：使用圆形、矩形、箭头、连线等基础图形
            4. 配色美化：应用现代配色方案，确保视觉协调
            5. 生成代码：输出完整规范的 SVG 代码
            
            ### 技术规范 ###
            - 必须包含 <?xml version="1.0" encoding="UTF-8"?> 声明
            - 必须设置 viewBox="0 0 800 600"，便于自适应缩放
            - 字体使用 font-family="Arial, sans-serif"，确保跨平台兼容
            - 使用语义化的 id 和 class 命名
            
            ### 设计风格 ###
            - 配色：蓝色系为主（#4A90D9、#6BB3F0、#E8F4FC），辅以渐变效果
            - 布局：留白充足，元素间距均匀，层次分明
            - 文字：标签简洁，字号适中（14-18px），颜色对比清晰
            - 连线：使用带箭头的线条表示方向和关系，线条粗细 2-3px
            
            ### 输出要求 ###
            直接返回完整的 SVG XML 代码，不要有任何解释或其他内容。
            """;

    /**
     * 智能体3：生成正文
     */
    String AGENT3_CONTENT_PROMPT = """
            你是一位资深的内容创作者,擅长撰写优质文章。
            
            根据以下大纲,创作文章正文:
            主标题：{mainTitle}
            副标题：{subTitle}
            大纲：
            {outline}
            
            要求:
            1. 内容要充实,每个章节300-400字
            2. 语言流畅,富有感染力
            3. 适当使用金句,增强可读性
            4. 添加过渡句,确保逻辑连贯
            5. 使用 Markdown 格式,章节使用 ## 标题
            
            请直接返回 Markdown 格式的正文内容,不要有其他内容。
            """;

    /**
     * 智能体4：分析配图需求（支持多种图片来源，使用占位符方案）
     */
    String AGENT4_IMAGE_REQUIREMENTS_PROMPT = """
            你是一位专业的新媒体编辑,擅长为文章配图。
            
            根据以下文章内容,分析配图需求,并在正文中插入图片占位符:
            主标题：{mainTitle}
            正文：
            {content}
            
            【重要】可用的配图方式（请严格只从以下方式中选择，禁止使用未列出的方式）：
            {availableMethods}
            
            各配图方式的使用要求：
            {methodUsageGuide}
            
            通用要求:
            1. 识别需要配图的位置(封面、关键章节、段落之间等)
            2. 根据文章内容和结构灵活决定配图数量，避免过多或过少
            3. **在正文中插入占位符**：使用以下两种格式
               - 普通图片占位符：{{IMAGE_PLACEHOLDER_N}}，其中 N 为配图序号（1, 2, 3...），必须独占一行
               - Icon 占位符：{{ICON_PLACEHOLDER_N}}，可以放在文字行内任意位置（用于 ICONIFY 类型）
               - 注意：position=1 的封面图不需要占位符，不要放在正文中
               - 其他配图占位符可以放在任意合适位置（章节标题后、段落之间、列表项中、文字行内等）
            4. **imageSource 字段必须且只能是上述可用配图方式之一，不要使用其他值**
            5. placeholderId 必须与正文中插入的占位符完全一致
            6. position=1 为封面图
            
            请直接返回 JSON 格式,不要有其他内容:
            {
              "contentWithPlaceholders": "",
              "imageRequirements": [
                {
                  "position": 1,
                  "type": "cover",
                  "sectionTitle": "",
                  "imageSource": "NANO_BANANA",
                  "keywords": "",
                  "prompt": "A modern minimalist illustration of AI technology concept, featuring abstract neural network patterns with blue and purple gradient colors, clean design suitable for article cover, 16:9 aspect ratio",
                  "placeholderId": ""
                },
                {
                  "position": 2,
                  "type": "section",
                  "sectionTitle": "章节标题1",
                  "imageSource": "PEXELS",
                  "keywords": "business success teamwork office",
                  "prompt": "",
                  "placeholderId": "{{IMAGE_PLACEHOLDER_1}}"
                },
                {
                  "position": 3,
                  "type": "inline",
                  "sectionTitle": "",
                  "imageSource": "ICONIFY",
                  "keywords": "check circle",
                  "prompt": "",
                  "placeholderId": "{{ICON_PLACEHOLDER_1}}"
                },
                {
                  "position": 4,
                  "type": "section",
                  "sectionTitle": "章节标题2",
                  "imageSource": "MERMAID",
                  "keywords": "",
                  "prompt": "flowchart TB\\n    A[用户请求] --> B[负载均衡]\\n    B --> C[应用服务器]",
                  "placeholderId": "{{IMAGE_PLACEHOLDER_2}}"
                }
              ]
            }
            """;

    // region 文章风格 Prompt

    /**
     * 科技风格 Prompt 附加
     */
    String STYLE_TECH_PROMPT = """
            
            **重要：请使用科技风格进行创作**
            - 语言专业、严谨，多使用专业术语和行业词汇
            - 逻辑清晰，重视数据和事实支撑
            - 叙述客观理性，避免主观情感表达
            - 突出技术创新、发展趋势、解决方案
            - 可适当引用权威资料或专家观点
            """;

    /**
     * 情感风格 Prompt 附加
     */
    String STYLE_EMOTIONAL_PROMPT = """
            
            **重要：请使用情感风格进行创作**
            - 语言温暖细腻，富有感染力和共鸣
            - 善用比喻、排比等修辞手法增强表现力
            - 注重情感表达，讲述真实故事和感悟
            - 引发读者情感共鸣，传递正能量
            - 适当使用抒情语句，增加文章温度
            """;

    /**
     * 教育风格 Prompt 附加
     */
    String STYLE_EDUCATIONAL_PROMPT = """
            
            **重要：请使用教育风格进行创作**
            - 语言通俗易懂，深入浅出地讲解概念
            - 结构清晰，循序渐进，便于学习理解
            - 多用案例、类比帮助读者理解复杂内容
            - 总结重点知识点，提供实用的学习建议
            - 鼓励思考，启发读者自主学习和探索
            """;

    /**
     * 轻松幽默风格 Prompt 附加
     */
    String STYLE_HUMOROUS_PROMPT = """
            
            **重要：请使用轻松幽默风格进行创作**
            - 语言轻松活泼，幽默风趣
            - 善用网络流行语、俏皮话和有趣的比喻
            - 适当自嘲或调侃，增加趣味性
            - 内容轻松易读，让读者在愉快中获取信息
            - 可加入一些有趣的段子或梗，但不失专业性
            """;

    /**
     * AI 修改大纲 Prompt
     */
    String AI_MODIFY_OUTLINE_PROMPT = """
            你是一位专业的文章策划师,擅长根据用户反馈优化文章结构。
            
            当前文章信息：
            主标题：{mainTitle}
            副标题：{subTitle}
            
            当前大纲：
            {currentOutline}
            
            用户修改建议：
            {modifySuggestion}
            
            要求：
            1. 根据用户的修改建议，调整大纲结构
            2. 保持大纲的逻辑性和完整性
            3. 如果用户建议删除某章节，则删除；建议增加则增加；建议修改则修改
            4. 保持 JSON 格式不变
            5. 章节序号自动重新排序
            
            请直接返回修改后的 JSON 格式大纲，不要有其他内容：
            {
              "sections": [
                {
                  "section": 1,
                  "title": "章节标题",
                  "points": ["要点1", "要点2"]
                }
              ]
            }
            """;

    // endregion

    // region Tool Use（PR 3: Dynamic Tool Routing）

    /**
     * 配图需求分析（function-calling 模式，PR 3 升级版：6 个专用 tool）
     *
     * <p>每个 tool 对应一种 imageSource，tool 名称本身就编码了选择。
     * LLM 根据正文意图选择合适的 tool，每个 tool 只需传入对应的关键词/prompt/code/iconName/requirement。
     */
    String AGENT4_TOOL_USE_PROMPT = """
            你是文章配图编辑。阅读正文，为每个需要配图的位置**调用一次合适的工具**做出决策。
            工具名称本身就决定了 imageSource，不要在文本中嵌入图片 URL，使用占位符。

            主标题：{mainTitle}

            【可用工具 — 6 个专用配图 tool，按场景选】
            1. use_pexels_photo(keywords, position, sectionTitle) — 真实照片（图库搜索）
               适用: 真实场景、产品、人物、自然风景、技术应用截图
            2. generate_ai_image(prompt, position, sectionTitle) — AI 生图（Gemini）
               适用: 创意插画、抽象概念、信息图表、需要文字渲染、独特风格
            3. render_mermaid_diagram(code, position, sectionTitle) — Mermaid 图表
               适用: 流程图、架构图、时序图、关系图、甘特图
               code 必须是合法的 Mermaid 源码（以 flowchart / sequenceDiagram / classDiagram 等开头）
            4. fetch_icon(iconName, position, sectionTitle) — 图标
               适用: 小型装饰性图标（箭头、勾、星星、心形等）
               iconName 用英文（check / arrow / star / heart / info 等）
            5. search_emoji_pack(keywords, position, sectionTitle) — 表情包
               适用: 表情包、搞笑图片、轻松幽默配图
            6. generate_svg_diagram(requirement, position, sectionTitle) — SVG 概念示意图
               适用: 思维导图、概念示意图、逻辑关系展示
               requirement 用中文描述要表达的概念

            【正文】
            {content}

            【工作流程】
            1. 阅读正文，识别需要配图的位置（封面 1 张 + 关键章节图 3-5 张）。
            2. 对每个位置，根据内容选择最合适的工具并调用——只传该工具需要的参数。
               - 关键词类工具（Pexels / Emoji / Icon）只需要 keywords / iconName
               - 生图类工具（AI / Mermaid / SVG）只需要 prompt / code / requirement
            3. **不要在正文中嵌入图片 URL**——使用占位符：
               - 封面图：不需要占位符（sectionTitle 留空）
               - 章节大图：独占一行写 {{IMAGE_PLACEHOLDER_N}}（N 从 1 开始递增）
               - 行内小图标：行内写 {{ICON_PLACEHOLDER_N}}
            4. 完成所有工具调用后，**必须在最终消息中返回以下 JSON**（不要包裹在 markdown 代码块里，直接输出）：

            {"contentWithPlaceholders":"正文（含 {{IMAGE_PLACEHOLDER_N}} 占位符）","imageRequirements":[{"position":1,"imageSource":"...","keywords":"...","prompt":"...","sectionTitle":"","placeholderId":""},{"position":2,"imageSource":"...","keywords":"...","prompt":"...","sectionTitle":"...","placeholderId":"{{IMAGE_PLACEHOLDER_1}}"}]}

            注意：
            - imageRequirements 数组里的每个元素必须对应你的一次工具调用
            - placeholderId 仅当 position > 1 时需要；position=1（封面）留空
            - 最终消息以 { 开头、以 } 结尾，不要任何其他文字
            """;

    // endregion

    // region Self-Critique Loop（PR 2: 反思循环）

    /**
     * Critic Agent：对正文打分（0-7）+ 反馈
     */
    String AGENT3_CRITIC_PROMPT = """
            你是一位严格的文章质量评审。请按 7 分制对正文评分，并给出具体可操作的改进建议（中文，不超过 200 字）。

            主标题：{mainTitle}
            副标题：{subTitle}
            长度要求：{lengthBudget}
            风格要求：{styleFragment}

            正文：
            {content}

            评分维度（每维度 0-1 分，总分 = 各维度之和，向上取整）：
            1. 结构覆盖：是否覆盖大纲中的所有核心章节？
            2. 关键词覆盖：是否充分使用选题与大纲的关键概念？
            3. 长度达标：字数是否落在预算区间（{lengthBudget}）？
            4. 占位符完整性：是否在合适位置预留了图片/图标占位符？占位符编号是否连续？
            5. 风格一致性：语气、用词、节奏是否贯穿全文？
            6. 开头钩子：前 200 字是否抓住读者注意力？
            7. 段落过渡：章节之间是否自然过渡、无断裂？

            重要：宽松给分——合格即给 6-7 分。仅在内容有实质性问题时给低分。

            请直接返回 JSON，不要其他内容：
            {
              "score": 6,
              "feedback": "结构完整；开头钩子偏弱，建议用数据/反常识切入；风格在中段偏抒情，需在第三段回到客观语气。"
            }
            """;

    /**
     * Revision Agent：基于 Critic 反馈改写正文
     */
    String AGENT3_REVISION_PROMPT = """
            你是一位资深的内容修订编辑。请根据评审反馈改写正文，必须保留既有结构与占位符。

            主标题：{mainTitle}
            副标题：{subTitle}
            风格要求：{styleFragment}
            长度要求：{lengthBudget}

            评审反馈：
            {critiqueFeedback}

            当前正文（需改进）：
            {content}

            保留约束（不可违反）：
            1. 必须保留所有 {{IMAGE_PLACEHOLDER_N}} 与 {{ICON_PLACEHOLDER_N}} 占位符的位置与编号
            2. 章节顺序与 ## 标题层级与原文保持一致
            3. 字数控制在 {lengthBudget} 范围内
            4. 输出 Markdown，不要添加任何解释
            5. 重点针对反馈建议改进，不要无意义全文重写
            """;

    /**
     * 长度预算 prompt 字面（注入到 critic / revision / write prompt 的 {lengthBudget} 占位符）
     */
    String LENGTH_BUDGET_DEFAULT = "建议字数 2000（1200-2500 字）";
    String LENGTH_BUDGET_TECH = "建议字数 2000（1800-2200 字）";
    String LENGTH_BUDGET_EMOTIONAL = "建议字数 1500（1200-1800 字）";
    String LENGTH_BUDGET_EDUCATIONAL = "建议字数 2400（2000-2800 字）";
    String LENGTH_BUDGET_HUMOROUS = "建议字数 1000（800-1200 字）";

    // endregion

    // region Editor-in-Chief（Supervisor Pattern）

    /**
     * 主编审阅 Prompt：综合评估后决定下一步
     *
     * <p>输入：完整文章状态（outline + content + imageRequirements）
     * <p>输出 JSON：decision + editorialNote/feedback
     */
    String EDITOR_REVIEW_PROMPT = """
            你是主编，审阅一篇刚完成的新媒体文章，做出最终发布/修订决策。

            主标题：{mainTitle}
            副标题：{subTitle}
            风格：{styleFragment}
            长度：{lengthBudget}

            【大纲】
            {outline}

            【正文】
            {content}

            【配图需求】
            {imageRequirements}

            【可用配图方式】
            {availableMethods}

            【决策选项】
            - finish：文章达标，可以发布。必须输出 editorialNote（100-200 字）：说明为什么这样组织、配图策略、风格执行
            - revise_outline：大纲有问题（如章节缺失/逻辑跳跃/顺序不当）。输出 feedback 让大纲 agent 改写
            - revise_content：正文需要补充/重写/精炼。输出 feedback 让正文 agent 改写
            - revise_images：配图不合适（位置错/风格不符/缺图）。输出 feedback 让配图 agent 重选

            【决策原则】
            - 优先 finish：大多数文章首版可接受，不要过度修订
            - 仅在有明确实质性问题时才 revise（如章节缺关键内容、配图位置错误）
            - 修订反馈必须具体可操作（不要"内容需要改进"这种空话）

            返回 JSON（不要 markdown 代码块，直接输出）：
            {"decision":"finish","editorialNote":"结构采用'问题-方案-案例-展望'四段式，配图覆盖封面+3 张章节图，风格保持科技客观，首段用数据钩子吸引读者。"}
            或
            {"decision":"revise_outline","feedback":"第 3 章'性能优化'与第 4 章'实践经验'内容重叠；建议合并到第 3 章，把第 4 章改成'未来展望'。"}
            或
            {"decision":"revise_content","feedback":"第 2 章过于技术化，普通读者读不懂；建议加 1-2 个生活化类比。"}
            或
            {"decision":"revise_images","feedback":"第 3 张配图用 SVG 流程图，但本节是文字分析不是流程；建议换成 Pexels 真实场景图。"}
            """;

    /**
     * 大纲修订 Prompt：基于主编反馈重写大纲
     */
    String OUTLINE_REVISION_PROMPT = """
            你是文章策划师，资深大纲修订编辑。根据主编反馈重写大纲。

            主标题：{mainTitle}
            副标题：{subTitle}
            风格：{styleFragment}

            当前大纲：
            {outline}

            主编反馈：
            {outlineFeedback}

            要求：
            1. 必须根据主编反馈调整（不要无视或弱化）
            2. 保留章节编号连续性
            3. 章节标题要清晰反映新结构
            4. 直接返回 JSON：{"sections":[{"section":1,"title":"...","points":["...","..."]}]}
            """;

    /**
     * 正文主编修订 Prompt：与 Critic 修订不同（更关注主编层面的结构性意见）
     */
    String AGENT3_EDITOR_REVISION_PROMPT = """
            你是资深主编，对正文做结构性修订。请根据主编反馈改写正文。

            主标题：{mainTitle}
            副标题：{subTitle}
            风格：{styleFragment}
            长度：{lengthBudget}

            主编反馈：
            {contentFeedback}

            当前正文：
            {content}

            保留约束（不可违反）：
            1. 必须保留所有 {{IMAGE_PLACEHOLDER_N}} 与 {{ICON_PLACEHOLDER_N}} 占位符的位置与编号
            2. 章节顺序与 ## 标题层级与原文保持一致（除非反馈要求重排）
            3. 字数控制在 {lengthBudget} 范围内
            4. 输出 Markdown，不要添加任何解释

            注意：这是主编反馈（结构性、宏观），不是 Critic 评审（细节、打分）。
            重点关注：章节平衡、叙事节奏、缺漏的关键内容、与大纲的一致性。
            """;

    /**
     * 配图修订 Prompt：根据主编反馈重新选择配图
     */
    String IMAGE_REVISION_PROMPT = """
            你是新媒体编辑，根据主编反馈重新决定配图。

            主标题：{mainTitle}
            风格：{styleFragment}

            可用配图方式：
            {availableMethods}

            各方式使用规则：
            {methodUsageGuide}

            当前正文（占位符已插入）：
            {content}

            当前配图需求：
            {imageRequirements}

            主编反馈：
            {imageFeedback}

            要求：
            1. 必须根据主编反馈调整（移除/替换/新增配图）
            2. 保留占位符编号连续性
            3. 封面图（position=1）不要嵌入正文
            4. 直接返回 JSON：{"contentWithPlaceholders":"...","imageRequirements":[...]}
            """;

    // endregion
}
