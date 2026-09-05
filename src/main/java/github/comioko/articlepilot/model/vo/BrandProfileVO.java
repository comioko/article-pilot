package github.comioko.articlepilot.model.vo;

import github.comioko.articlepilot.model.entity.BrandProfile;
import lombok.Data;
import org.springframework.beans.BeanUtils;

/** 用户品牌知识库视图。 */
@Data
public class BrandProfileVO {
    private String brandName;
    private String tone;
    private String targetAudience;
    private String preferredTerms;
    private String bannedTerms;
    private String referenceNotes;

    public static BrandProfileVO objToVo(BrandProfile profile) {
        BrandProfileVO vo = new BrandProfileVO();
        if (profile != null) {
            BeanUtils.copyProperties(profile, vo);
        }
        return vo;
    }
}
