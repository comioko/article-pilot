package github.comioko.articlepilot.model.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 首批支持的内容发布渠道。
 */
@Getter
public enum PublishChannelEnum {
    WECHAT("WECHAT", "微信公众号"),
    XIAOHONGSHU("XIAOHONGSHU", "小红书");

    private final String value;
    private final String text;

    PublishChannelEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static PublishChannelEnum getByValue(String value) {
        return Arrays.stream(values())
                .filter(item -> item.value.equals(value))
                .findFirst()
                .orElse(null);
    }
}
