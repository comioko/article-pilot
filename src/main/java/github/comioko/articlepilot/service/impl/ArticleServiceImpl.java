package github.comioko.articlepilot.service.impl;

import cn.hutool.core.util.IdUtil;
import com.google.gson.reflect.TypeToken;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import github.comioko.articlepilot.exception.BusinessException;
import github.comioko.articlepilot.exception.ErrorCode;
import github.comioko.articlepilot.exception.ThrowUtils;
import github.comioko.articlepilot.mapper.ArticleMapper;
import github.comioko.articlepilot.mapper.ArticleRevisionMapper;
import github.comioko.articlepilot.model.dto.article.ArticleAiRefineRequest;
import github.comioko.articlepilot.model.dto.article.ArticleQueryRequest;
import github.comioko.articlepilot.model.dto.article.ArticlePublishPackageRequest;
import github.comioko.articlepilot.model.dto.article.ArticleRestoreRevisionRequest;
import github.comioko.articlepilot.model.dto.article.ArticleSaveRevisionRequest;
import github.comioko.articlepilot.model.dto.article.ArticleState;
import github.comioko.articlepilot.model.entity.Article;
import github.comioko.articlepilot.model.entity.ArticleRevision;
import github.comioko.articlepilot.model.entity.User;
import github.comioko.articlepilot.model.enums.ArticlePhaseEnum;
import github.comioko.articlepilot.model.enums.ArticleStatusEnum;
import github.comioko.articlepilot.model.enums.ImageMethodEnum;
import github.comioko.articlepilot.model.enums.PublishChannelEnum;
import github.comioko.articlepilot.model.vo.ArticleVO;
import github.comioko.articlepilot.model.vo.ArticleRevisionVO;
import github.comioko.articlepilot.model.vo.ArticlePublishPackageVO;
import github.comioko.articlepilot.service.ArticleAgentService;
import github.comioko.articlepilot.service.ArticleService;
import github.comioko.articlepilot.service.QuotaService;
import github.comioko.articlepilot.utils.GsonUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static github.comioko.articlepilot.constant.UserConstant.ADMIN_ROLE;
import static github.comioko.articlepilot.constant.UserConstant.VIP_ROLE;

/**
 * 文章服务实现类
 *
 * @author comioko
 */
@Service
@Slf4j
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Resource
    private QuotaService quotaService;

    @Resource
    private ArticleAgentService articleAgentService;

    @Resource
    private ArticleRevisionMapper articleRevisionMapper;

    @Override
    public String createArticleTask(String topic, String style, List<String> enabledImageMethods, User loginUser) {
        // 处理配图方式：如果用户未选择，给普通用户设置默认的非 VIP 方式
        List<String> finalImageMethods = processImageMethods(enabledImageMethods, loginUser);
        
        // 校验配图方式权限（普通用户不能使用 NANO_BANANA 和 SVG_DIAGRAM）
        validateImageMethods(finalImageMethods, loginUser);

        // 生成任务ID
        String taskId = IdUtil.simpleUUID();

        // 创建文章记录
        Article article = new Article();
        article.setTaskId(taskId);
        article.setUserId(loginUser.getId());
        article.setTopic(topic);
        article.setStyle(style);
        article.setEnabledImageMethods(finalImageMethods != null && !finalImageMethods.isEmpty() 
                ? GsonUtils.toJson(finalImageMethods) : null);
        article.setStatus(ArticleStatusEnum.PENDING.getValue());
        article.setPhase(ArticlePhaseEnum.PENDING.getValue());
        article.setCreateTime(LocalDateTime.now());

        this.save(article);

        log.info("文章任务已创建, taskId={}, userId={}, style={}", taskId, loginUser.getId(), style);
        return taskId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createArticleTaskWithQuotaCheck(String topic, String style, List<String> enabledImageMethods, User loginUser) {
        // 在同一事务中：先扣配额，再创建任务
        // 如果任务创建失败，配额会自动回滚
        quotaService.checkAndConsumeQuota(loginUser);
        return createArticleTask(topic, style, enabledImageMethods, loginUser);
    }

    @Override
    public Article getByTaskId(String taskId) {
        return this.getOne(
                QueryWrapper.create().eq("taskId", taskId)
        );
    }

    @Override
    public ArticleVO getArticleDetail(String taskId, User loginUser) {
        Article article = getByTaskId(taskId);
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR, "文章不存在");

        // 校验权限：只能查看自己的文章（管理员除外）
        checkArticlePermission(article, loginUser);

        return ArticleVO.objToVo(article);
    }

    @Override
    public Page<ArticleVO> listArticleByPage(ArticleQueryRequest request, User loginUser) {
        long current = request.getPageNum();
        long size = request.getPageSize();

        // 构建查询条件
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("isDelete", 0)
                .orderBy("createTime", false);

        // 非管理员只能查看自己的文章
        if (!ADMIN_ROLE.equals(loginUser.getUserRole())) {
            queryWrapper.eq("userId", loginUser.getId());
        } else if (request.getUserId() != null) {
            queryWrapper.eq("userId", request.getUserId());
        }

        // 按状态筛选
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            queryWrapper.eq("status", request.getStatus());
        }

        // 分页查询
        Page<Article> articlePage = this.page(new Page<>(current, size), queryWrapper);

        // 转换为 VO
        return convertToVOPage(articlePage);
    }

    @Override
    public boolean deleteArticle(Long id, User loginUser) {
        Article article = this.getById(id);
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR);

        // 校验权限：只能删除自己的文章（管理员除外）
        checkArticlePermission(article, loginUser);

        // 逻辑删除
        return this.removeById(id);
    }

    @Override
    public void updateArticleStatus(String taskId, ArticleStatusEnum status, String errorMessage) {
        Article article = getByTaskId(taskId);

        if (article == null) {
            log.error("文章记录不存在, taskId={}", taskId);
            return;
        }

        article.setStatus(status.getValue());
        article.setErrorMessage(errorMessage);
        this.updateById(article);

        log.info("文章状态已更新, taskId={}, status={}", taskId, status.getValue());
    }

    @Override
    public void saveArticleContent(String taskId, ArticleState state) {
        Article article = getByTaskId(taskId);

        if (article == null) {
            log.error("文章记录不存在, taskId={}", taskId);
            return;
        }

        article.setMainTitle(state.getTitle().getMainTitle());
        article.setSubTitle(state.getTitle().getSubTitle());
        article.setOutline(GsonUtils.toJson(state.getOutline().getSections()));
        article.setContent(state.getContent());
        article.setFullContent(state.getFullContent());
        
        // 保存封面图 URL（从 images 列表中提取 position=1 的 URL）
        if (state.getImages() != null && !state.getImages().isEmpty()) {
            ArticleState.ImageResult cover = state.getImages().stream()
                .filter(img -> img.getPosition() != null && img.getPosition() == 1)
                .findFirst()
                .orElse(null);
            if (cover != null && cover.getUrl() != null) {
                article.setCoverImage(cover.getUrl());
            }
        }
        article.setImages(GsonUtils.toJson(state.getImages()));
        article.setCompletedTime(LocalDateTime.now());

        this.updateById(article);
        log.info("文章保存成功, taskId={}", taskId);
    }

    /**
     * 校验文章权限
     *
     * @param article   文章
     * @param loginUser 当前用户
     */
    private void checkArticlePermission(Article article, User loginUser) {
        if (!article.getUserId().equals(loginUser.getId()) &&
                !ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

    /**
     * 将文章分页结果转换为 VO 分页
     *
     * @param articlePage 文章分页
     * @return VO 分页
     */
    private Page<ArticleVO> convertToVOPage(Page<Article> articlePage) {
        Page<ArticleVO> articleVOPage = new Page<>();
        articleVOPage.setPageNumber(articlePage.getPageNumber());
        articleVOPage.setPageSize(articlePage.getPageSize());
        articleVOPage.setTotalRow(articlePage.getTotalRow());

        List<ArticleVO> articleVOList = articlePage.getRecords().stream()
                .map(ArticleVO::objToVo)
                .collect(Collectors.toList());
        articleVOPage.setRecords(articleVOList);

        return articleVOPage;
    }

    @Override
    public void confirmTitle(String taskId, String mainTitle, String subTitle, String userDescription, User loginUser) {
        Article article = getByTaskId(taskId);
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR, "文章不存在");

        // 校验权限
        checkArticlePermission(article, loginUser);

        // 校验当前阶段（必须是 TITLE_SELECTING）
        ArticlePhaseEnum currentPhase = ArticlePhaseEnum.getByValue(article.getPhase());
        ThrowUtils.throwIf(currentPhase != ArticlePhaseEnum.TITLE_SELECTING,
                ErrorCode.OPERATION_ERROR, "当前阶段不允许此操作");

        // 保存用户选择的标题和补充描述
        article.setMainTitle(mainTitle);
        article.setSubTitle(subTitle);
        article.setUserDescription(userDescription);
        article.setPhase(ArticlePhaseEnum.OUTLINE_GENERATING.getValue());

        this.updateById(article);
        log.info("用户确认标题, taskId={}, mainTitle={}", taskId, mainTitle);
    }

    @Override
    public void confirmOutline(String taskId, List<ArticleState.OutlineSection> outline, User loginUser) {
        Article article = getByTaskId(taskId);
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR, "文章不存在");

        // 校验权限
        checkArticlePermission(article, loginUser);

        // 校验当前阶段（必须是 OUTLINE_EDITING）
        ArticlePhaseEnum currentPhase = ArticlePhaseEnum.getByValue(article.getPhase());
        ThrowUtils.throwIf(currentPhase != ArticlePhaseEnum.OUTLINE_EDITING,
                ErrorCode.OPERATION_ERROR, "当前阶段不允许此操作");

        // 保存用户编辑后的大纲
        article.setOutline(GsonUtils.toJson(outline));
        article.setPhase(ArticlePhaseEnum.CONTENT_GENERATING.getValue());

        this.updateById(article);
        log.info("用户确认大纲, taskId={}, sectionsCount={}", taskId, outline.size());
    }

    @Override
    public void updatePhase(String taskId, ArticlePhaseEnum phase) {
        Article article = getByTaskId(taskId);
        if (article == null) {
            log.error("文章记录不存在, taskId={}", taskId);
            return;
        }

        article.setPhase(phase.getValue());
        this.updateById(article);
        log.info("文章阶段已更新, taskId={}, phase={}", taskId, phase.getValue());
    }

    @Override
    public void saveTitleOptions(String taskId, List<ArticleState.TitleOption> titleOptions) {
        Article article = getByTaskId(taskId);
        if (article == null) {
            log.error("文章记录不存在, taskId={}", taskId);
            return;
        }

        article.setTitleOptions(GsonUtils.toJson(titleOptions));
        this.updateById(article);
        log.info("标题方案已保存, taskId={}, optionsCount={}", taskId, titleOptions.size());
    }

    @Override
    public List<ArticleState.OutlineSection> aiModifyOutline(String taskId, String modifySuggestion, User loginUser) {
        Article article = getByTaskId(taskId);
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR, "文章不存在");

        // 校验权限
        checkArticlePermission(article, loginUser);

        // 校验 VIP 权限（普通用户不能使用 AI 修改大纲）
        ThrowUtils.throwIf(!isVipOrAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, 
                "AI 修改大纲功能仅限 VIP 会员使用");

        // 校验当前阶段（必须是 OUTLINE_EDITING）
        ArticlePhaseEnum currentPhase = ArticlePhaseEnum.getByValue(article.getPhase());
        ThrowUtils.throwIf(currentPhase != ArticlePhaseEnum.OUTLINE_EDITING,
                ErrorCode.OPERATION_ERROR, "当前阶段不允许此操作");

        // 获取当前大纲
        List<ArticleState.OutlineSection> currentOutline = GsonUtils.fromJson(
                article.getOutline(),
                new TypeToken<List<ArticleState.OutlineSection>>(){}
        );

        // 调用 AI 修改大纲
        List<ArticleState.OutlineSection> modifiedOutline = articleAgentService.aiModifyOutline(
                article.getMainTitle(),
                article.getSubTitle(),
                currentOutline,
                modifySuggestion
        );

        // 保存修改后的大纲
        article.setOutline(GsonUtils.toJson(modifiedOutline));
        this.updateById(article);

        log.info("AI修改大纲完成, taskId={}, sectionsCount={}", taskId, modifiedOutline.size());
        return modifiedOutline;
    }

    @Override
    public String aiRefineContent(ArticleAiRefineRequest request, User loginUser) {
        Article article = requireEditableArticle(request.getTaskId(), loginUser);
        ThrowUtils.throwIf(isBlank(request.getSelectedText()), ErrorCode.PARAMS_ERROR, "请选择要精修的内容");
        String selectedText = request.getSelectedText();
        ThrowUtils.throwIf(selectedText.length() > 8000, ErrorCode.PARAMS_ERROR, "单次精修内容不能超过 8000 字符");

        String instruction = request.getInstruction() == null ? "" : request.getInstruction().trim();
        ThrowUtils.throwIf(instruction.isEmpty() || instruction.length() > 1000, ErrorCode.PARAMS_ERROR, "精修要求需为 1-1000 字符");

        return articleAgentService.aiRefineContent(
                article.getMainTitle(), article.getStyle(), selectedText, instruction);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleVO saveRevision(ArticleSaveRevisionRequest request, User loginUser) {
        Article article = getMapper().selectForEditing(request.getTaskId());
        validateEditableArticle(article, loginUser);
        if (request.getBaseFingerprint() != null) {
            ThrowUtils.throwIf(!request.getBaseFingerprint().equals(
                    github.comioko.articlepilot.utils.ArticleFingerprint.of(article)),
                    ErrorCode.OPERATION_ERROR, "文章已在其他页面修改，请保留草稿并重新加载后再保存");
        }
        ThrowUtils.throwIf(isBlank(request.getContent()) && isBlank(request.getFullContent()),
                ErrorCode.PARAMS_ERROR, "文章内容不能为空");

        ThrowUtils.throwIf(request.getRevisionNote() != null && request.getRevisionNote().length() > 500,
                ErrorCode.PARAMS_ERROR, "版本说明不能超过 500 字符");
        validateContentSize(request.getContent());
        validateContentSize(request.getFullContent());
        ensureInitialRevision(article);
        article.setContent(request.getContent() == null ? article.getContent() : request.getContent());
        article.setFullContent(request.getFullContent() == null ? article.getFullContent() : request.getFullContent());
        ThrowUtils.throwIf(!this.updateById(article), ErrorCode.OPERATION_ERROR, "保存失败，请重试");

        createRevision(article, isBlank(request.getRevisionNote()) ? "精修后保存" : request.getRevisionNote().trim());
        log.info("文章精修版本已保存, taskId={}", article.getTaskId());
        return ArticleVO.objToVo(article);
    }

    @Override
    public List<ArticleRevisionVO> listRevisions(String taskId, User loginUser) {
        Article article = requireEditableArticle(taskId, loginUser);
        return findRevisions(article.getTaskId()).stream()
                .sorted(Comparator.comparing(ArticleRevision::getRevisionNumber).reversed())
                .map(ArticleRevisionVO::objToVo)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleVO restoreRevision(ArticleRestoreRevisionRequest request, User loginUser) {
        Article article = getMapper().selectForEditing(request.getTaskId());
        validateEditableArticle(article, loginUser);
        if (request.getBaseFingerprint() != null) {
            ThrowUtils.throwIf(!request.getBaseFingerprint().equals(
                    github.comioko.articlepilot.utils.ArticleFingerprint.of(article)),
                    ErrorCode.OPERATION_ERROR, "文章已在其他页面修改，请保留草稿并重新加载后再保存");
        }
        ArticleRevision revision = findRevisions(article.getTaskId()).stream()
                .filter(item -> item.getId().equals(request.getRevisionId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "历史版本不存在"));

        createRevision(article, "恢复前自动备份");
        article.setContent(revision.getContent() == null ? "" : revision.getContent());
        article.setFullContent(revision.getFullContent() == null ? "" : revision.getFullContent());
        ThrowUtils.throwIf(!this.updateById(article), ErrorCode.OPERATION_ERROR, "保存失败，请重试");

        createRevision(article, "恢复自版本 " + revision.getRevisionNumber());
        log.info("文章历史版本已恢复, taskId={}, revision={}", article.getTaskId(), revision.getRevisionNumber());
        return ArticleVO.objToVo(article);
    }

    @Override
    public ArticlePublishPackageVO generatePublishPackage(ArticlePublishPackageRequest request, User loginUser) {
        Article article = requireEditableArticle(request.getTaskId(), loginUser);
        PublishChannelEnum channel = PublishChannelEnum.getByValue(request.getChannel());
        ThrowUtils.throwIf(channel == null, ErrorCode.PARAMS_ERROR, "暂不支持该发布渠道");

        String sourceContent = isBlank(article.getFullContent()) ? article.getContent() : article.getFullContent();
        String content = articleAgentService.generatePublishPackage(
                article.getMainTitle(), sourceContent, channel.getText(), getChannelGuide(channel));
        return new ArticlePublishPackageVO(channel.getValue(), article.getMainTitle(), content);
    }

    private Article requireEditableArticle(String taskId, User loginUser) {
        ThrowUtils.throwIf(isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务ID不能为空");
        Article article = getByTaskId(taskId);
        validateEditableArticle(article, loginUser);
        return article;
    }

    private void validateEditableArticle(Article article, User loginUser) {
        ThrowUtils.throwIf(article == null, ErrorCode.NOT_FOUND_ERROR, "文章不存在");
        checkArticlePermission(article, loginUser);
        ThrowUtils.throwIf(!ArticleStatusEnum.COMPLETED.getValue().equals(article.getStatus()),
                ErrorCode.OPERATION_ERROR, "文章尚未生成完成，暂不能编辑");
    }

    private void validateContentSize(String content) {
        ThrowUtils.throwIf(content != null && content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 65000,
                ErrorCode.PARAMS_ERROR, "正文过长，请缩短后保存（最多 65000 字节）");
    }

    private void ensureInitialRevision(Article article) {
        if (findRevisions(article.getTaskId()).isEmpty()) {
            createRevision(article, "初始成稿");
        }
    }

    private void createRevision(Article article, String note) {
        int nextRevision = findRevisions(article.getTaskId()).stream()
                .map(ArticleRevision::getRevisionNumber)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        ArticleRevision revision = ArticleRevision.builder()
                .taskId(article.getTaskId())
                .userId(article.getUserId())
                .revisionNumber(nextRevision)
                .content(article.getContent())
                .fullContent(article.getFullContent())
                .revisionNote(note)
                .createTime(LocalDateTime.now())
                .build();
        articleRevisionMapper.insert(revision);
    }

    private List<ArticleRevision> findRevisions(String taskId) {
        return articleRevisionMapper.selectListByQuery(QueryWrapper.create().eq("taskId", taskId));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String getChannelGuide(PublishChannelEnum channel) {
        return switch (channel) {
            case WECHAT -> "保留 Markdown 标题层级和段落结构；适当拆分过长段落；开头给出一句引言，结尾给出一句自然的互动引导。不要使用过多 emoji 或话题标签。";
            case XIAOHONGSHU -> "改为高信息密度、短段落、易扫读的笔记体；首行给出有吸引力的标题（20 字以内），多用编号和适量 emoji；结尾附 3-6 个相关话题标签。不要使用 Markdown 标题符号。";
        };
    }

    /**
     * 处理配图方式
     * 如果用户未选择，给普通用户设置默认的非 VIP 方式，VIP 用户不限制
     */
    private List<String> processImageMethods(List<String> enabledImageMethods, User loginUser) {
        // 如果用户已选择，直接返回
        if (enabledImageMethods != null && !enabledImageMethods.isEmpty()) {
            return enabledImageMethods;
        }

        // VIP 和管理员：不限制，返回 null 表示支持所有方式
        if (isVipOrAdmin(loginUser)) {
            return null;
        }

        // 普通用户：返回默认的非 VIP 方式
        return List.of(
                ImageMethodEnum.PEXELS.getValue(),
                ImageMethodEnum.MERMAID.getValue(),
                ImageMethodEnum.ICONIFY.getValue(),
                ImageMethodEnum.EMOJI_PACK.getValue()
        );
    }

    /**
     * 校验配图方式权限
     * 普通用户不能使用 NANO_BANANA 和 SVG_DIAGRAM
     */
    private void validateImageMethods(List<String> enabledImageMethods, User loginUser) {
        if (enabledImageMethods == null || enabledImageMethods.isEmpty()) {
            return;
        }

        // VIP 和管理员无限制
        if (isVipOrAdmin(loginUser)) {
            return;
        }

        // 普通用户限制
        for (String method : enabledImageMethods) {
            if (ImageMethodEnum.NANO_BANANA.getValue().equals(method) || 
                ImageMethodEnum.SVG_DIAGRAM.getValue().equals(method)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, 
                        "高级配图功能（AI 生图、SVG 图表）仅限 VIP 会员使用");
            }
        }
    }

    /**
     * 判断是否为 VIP 或管理员
     */
    private boolean isVipOrAdmin(User user) {
        return ADMIN_ROLE.equals(user.getUserRole()) || 
               VIP_ROLE.equals(user.getUserRole());
    }
}
