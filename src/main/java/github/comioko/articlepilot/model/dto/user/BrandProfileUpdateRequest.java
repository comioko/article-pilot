package github.comioko.articlepilot.model.dto.user;

import lombok.Data;

/** 更新用户长期写作偏好。 */
@Data
public class BrandProfileUpdateRequest {
    private String brandName;
    private String tone;
    private String targetAudience;
    private String preferredTerms;
    private String bannedTerms;
    private String referenceNotes;
}
