package github.comioko.articlepilot.model.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 用户的长期写作偏好与品牌规范。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "brand_profile", camelToUnderline = false)
public class BrandProfile {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long userId;
    private String brandName;
    private String tone;
    private String targetAudience;
    private String preferredTerms;
    private String bannedTerms;
    private String referenceNotes;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
