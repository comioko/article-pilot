package github.comioko.articlepilot.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import github.comioko.articlepilot.exception.ErrorCode;
import github.comioko.articlepilot.exception.ThrowUtils;
import github.comioko.articlepilot.mapper.BrandProfileMapper;
import github.comioko.articlepilot.model.dto.user.BrandProfileUpdateRequest;
import github.comioko.articlepilot.model.entity.BrandProfile;
import github.comioko.articlepilot.model.vo.BrandProfileVO;
import github.comioko.articlepilot.service.BrandProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** 默认的品牌知识库实现。 */
@Service
@RequiredArgsConstructor
public class BrandProfileServiceImpl implements BrandProfileService {

    private final BrandProfileMapper brandProfileMapper;

    @Override
    public BrandProfileVO getProfile(Long userId) {
        return BrandProfileVO.objToVo(findByUserId(userId));
    }

    @Override
    public BrandProfileVO updateProfile(Long userId, BrandProfileUpdateRequest request) {
        validate(request);
        BrandProfile profile = findByUserId(userId);
        if (profile == null) {
            profile = BrandProfile.builder().userId(userId).createTime(LocalDateTime.now()).build();
            apply(profile, request);
            brandProfileMapper.insert(profile);
        } else {
            apply(profile, request);
            brandProfileMapper.update(profile);
        }
        return BrandProfileVO.objToVo(profile);
    }

    @Override
    public String buildPromptContext(Long userId) {
        BrandProfile profile = findByUserId(userId);
        if (profile == null || allBlank(profile)) return "";
        return """

                长期写作偏好（仅在不与本次选题或即时要求冲突时遵守）：
                品牌/账号：%s
                语气：%s
                目标读者：%s
                优先术语/表达：%s
                避免使用：%s
                参考说明：%s
                """.formatted(value(profile.getBrandName()), value(profile.getTone()),
                value(profile.getTargetAudience()), value(profile.getPreferredTerms()),
                value(profile.getBannedTerms()), value(profile.getReferenceNotes()));
    }

    private BrandProfile findByUserId(Long userId) {
        return brandProfileMapper.selectOneByQuery(QueryWrapper.create().eq("userId", userId));
    }

    private void apply(BrandProfile profile, BrandProfileUpdateRequest request) {
        profile.setBrandName(trim(request.getBrandName()));
        profile.setTone(trim(request.getTone()));
        profile.setTargetAudience(trim(request.getTargetAudience()));
        profile.setPreferredTerms(trim(request.getPreferredTerms()));
        profile.setBannedTerms(trim(request.getBannedTerms()));
        profile.setReferenceNotes(trim(request.getReferenceNotes()));
        profile.setUpdateTime(LocalDateTime.now());
    }

    private void validate(BrandProfileUpdateRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "品牌知识库内容不能为空");
        ThrowUtils.throwIf(length(request.getBrandName()) > 100 || length(request.getTone()) > 200
                        || length(request.getTargetAudience()) > 300 || length(request.getPreferredTerms()) > 1000
                        || length(request.getBannedTerms()) > 1000 || length(request.getReferenceNotes()) > 4000,
                ErrorCode.PARAMS_ERROR, "品牌知识库字段长度超出限制");
    }

    private boolean allBlank(BrandProfile profile) {
        return blank(profile.getBrandName()) && blank(profile.getTone()) && blank(profile.getTargetAudience())
                && blank(profile.getPreferredTerms()) && blank(profile.getBannedTerms()) && blank(profile.getReferenceNotes());
    }

    private String value(String input) { return blank(input) ? "未设置" : input; }
    private String trim(String input) { return input == null ? "" : input.trim(); }
    private boolean blank(String input) { return input == null || input.isBlank(); }
    private int length(String input) { return input == null ? 0 : input.length(); }
}
