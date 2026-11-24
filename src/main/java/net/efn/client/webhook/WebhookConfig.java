package net.efn.client.webhook;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.net.URI;
import java.net.URISyntaxException;

@Environment(EnvType.CLIENT)
public record WebhookConfig(String url, String username, String avatarUrl) {

    public static final String DEFAULT_USERNAME = "EFN";

    public static final String DEFAULT_AVATAR_URL = "https://efn.lol/efn-logo.png";

    public WebhookConfig(String url, String username, String avatarUrl) {
        url = normalizeRequired(url, "url");
        username = normalizeOptional(username, "EFN");
        avatarUrl = normalizeOptional(avatarUrl, "https://efn.lol/efn-logo.png");
        this.url = url;
        this.username = username;
        this.avatarUrl = avatarUrl;
    }

    public WebhookConfig(String string) {
        this(string, "EFN", "https://efn.lol/efn-logo.png");
    }

    public static boolean isValidUrl(String string2) {
        try {
            URI uRI = new URI(string2);
            String scheme = uRI.getScheme();
            return uRI.getHost() != null
                    && ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme));
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    private static String normalizeRequired(String string2, String string3) {
        String normalizeOptional = normalizeOptional(string2, null);
        if (normalizeOptional == null) {
            throw new IllegalArgumentException(string3 + " cannot be blank");
        } else {
            return normalizeOptional;
        }
    }

    private static String normalizeOptional(String string2, String string3) {
        if (string2 == null) {
            return string3;
        } else {
            String string4 = string2.trim();
            return string4.isEmpty() ? string3 : string4;
        }
    }
}
