package net.efn.client.util.spotify;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public record LyricsQuery(String title, String artist, String album, int durationSeconds) {

    static final int MAX_DURATION_SECONDS = 3600;

    public LyricsQuery(String title, String artist, String album, int durationSeconds) {
        title = title == null ? "" : title.strip();
        artist = artist == null ? "" : artist.strip();
        album = album == null ? "" : album.strip();
        if (durationSeconds < 0 || durationSeconds > 3600) {
            durationSeconds = 0;
        }
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.durationSeconds = durationSeconds;
    }

    public LyricsQuery withDurationMillis(long l) {
        long l2 = Math.max(0L, (l + 500L) / 1000L);
        return new LyricsQuery(this.title, this.artist, this.album, l2 > 3600L ? 0 : (int) l2);
    }

    public boolean hasDuration() {
        return this.durationSeconds > 0;
    }

    public boolean searchable() {
        return !this.title.isEmpty() && !this.artist.isEmpty();
    }

    public String cacheKey() {
        String string =
                squash(this.artist) + "\n" + squash(this.title) + "\n" + this.durationSeconds;
        try {
            byte[] bytes =
                    MessageDigest.getInstance("SHA-256")
                            .digest(string.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes, 0, 16);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is always available", exception);
        }
    }

    private static String squash(String string) {
        return string.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
