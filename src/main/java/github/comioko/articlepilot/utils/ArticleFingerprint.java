package github.comioko.articlepilot.utils;

import github.comioko.articlepilot.model.entity.Article;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class ArticleFingerprint {
    private ArticleFingerprint() { }

    public static String of(Article article) {
        // JSON preserves nulls and field boundaries; a delimiter in content cannot cause collisions.
        String payload = GsonUtils.toJson(new String[]{article.getContent(), article.getFullContent()});
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
