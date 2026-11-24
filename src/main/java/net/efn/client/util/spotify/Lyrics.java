package net.efn.client.util.spotify;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public record Lyrics(
        Lyrics.Status status, SyncedLyrics synced, List<String> plainLines, long sourceId) {

    public static final Lyrics LOADING =
            new Lyrics(Lyrics.Status.LOADING, SyncedLyrics.EMPTY, List.of(), 0L);

    public static final Lyrics NOT_FOUND =
            new Lyrics(Lyrics.Status.NOT_FOUND, SyncedLyrics.EMPTY, List.of(), 0L);

    public static final Lyrics UNAVAILABLE =
            new Lyrics(Lyrics.Status.UNAVAILABLE, SyncedLyrics.EMPTY, List.of(), 0L);

    public Lyrics(
            Lyrics.Status status, SyncedLyrics synced, List<String> plainLines, long sourceId) {
        if (status == null) {
            status = Lyrics.Status.NOT_FOUND;
        }
        if (synced == null) {
            synced = SyncedLyrics.EMPTY;
        }
        plainLines = plainLines == null ? List.of() : List.copyOf(plainLines);
        this.status = status;
        this.synced = synced;
        this.plainLines = plainLines;
        this.sourceId = sourceId;
    }

    public static Lyrics of(LrclibTrack lrclibTrack) {
        if (lrclibTrack == null) {
            return NOT_FOUND;
        } else {
            SyncedLyrics syncedLyrics = SyncedLyrics.parse(lrclibTrack.syncedLyrics());
            List list = plainLines(lrclibTrack.plainLyrics());
            if (list.isEmpty() && syncedLyrics.isEmpty()) {
                list = plainLines(lrclibTrack.syncedLyrics());
            }
            if (!syncedLyrics.isEmpty() && !syncedLyrics.texts().isEmpty()) {
                return new Lyrics(
                        Lyrics.Status.SYNCED,
                        syncedLyrics,
                        list.isEmpty() ? syncedLyrics.texts() : list,
                        lrclibTrack.id());
            } else if (!list.isEmpty()) {
                return new Lyrics(Lyrics.Status.PLAIN, SyncedLyrics.EMPTY, list, lrclibTrack.id());
            } else {
                return !lrclibTrack.instrumental() && syncedLyrics.size() <= 0
                        ? NOT_FOUND
                        : new Lyrics(
                                Lyrics.Status.INSTRUMENTAL,
                                SyncedLyrics.EMPTY,
                                List.of(),
                                lrclibTrack.id());
            }
        }
    }

    static List<String> plainLines(String string) {
        if (string != null && !string.isBlank()) {
            ArrayList list = new ArrayList();
            boolean bl = false;
            for (String string2 : string.split("\r\n|\r|\n")) {
                String string3 = string2.strip();
                if (string3.isEmpty()) {
                    bl = !list.isEmpty();
                } else {
                    if (bl) {
                        list.add("");
                    }
                    bl = false;
                    list.add(string3);
                }
            }
            return List.copyOf(list);
        } else {
            return List.of();
        }
    }

    @Environment(EnvType.CLIENT)
    public static enum Status {
        LOADING,
        SYNCED,
        PLAIN,
        INSTRUMENTAL,
        NOT_FOUND,
        UNAVAILABLE;

        private Status() {}
    }
}
