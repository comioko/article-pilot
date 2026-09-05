package github.comioko.articlepilot.service;

import github.comioko.articlepilot.model.dto.user.BrandProfileUpdateRequest;
import github.comioko.articlepilot.model.vo.BrandProfileVO;

/** 管理并渲染用户品牌知识库。 */
public interface BrandProfileService {
    BrandProfileVO getProfile(Long userId);
    BrandProfileVO updateProfile(Long userId, BrandProfileUpdateRequest request);
    String buildPromptContext(Long userId);
}
