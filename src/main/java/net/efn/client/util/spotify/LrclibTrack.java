package net.efn.client.util.spotify;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public record LrclibTrack(
        long id,
        String trackName,
        String artistName,
        String albumName,
        double duration,
        boolean instrumental,
        String plainLyrics,
        String syncedLyrics) {

    public LrclibTrack(
            long id,
            String trackName,
            String artistName,
            String albumName,
            double duration,
            boolean instrumental,
            String plainLyrics,
            String syncedLyrics) {
        trackName = nonNull(trackName);
        artistName = nonNull(artistName);
        albumName = nonNull(albumName);
        plainLyrics = nonNull(plainLyrics);
        syncedLyrics = nonNull(syncedLyrics);
        if (!Double.isFinite(duration) || duration < 0.0) {
            duration = 0.0;
        }
        this.id = id;
        this.trackName = trackName;
        this.artistName = artistName;
        this.albumName = albumName;
        this.duration = duration;
        this.instrumental = instrumental;
        this.plainLyrics = plainLyrics;
        this.syncedLyrics = syncedLyrics;
    }

    public boolean hasSynced() {
        return !this.syncedLyrics.isBlank();
    }

    public boolean hasPlain() {
        return !this.plainLyrics.isBlank();
    }

    public boolean usable() {
        return this.instrumental || this.hasSynced() || this.hasPlain();
    }

    public static LrclibTrack fromJson(JsonElement json) {
        if (json != null && json.isJsonObject()) {
            JsonObject jsonObject = json.getAsJsonObject();
            return new LrclibTrack(
                    number(jsonObject, "id", 0L),
                    text(jsonObject, "trackName"),
                    text(jsonObject, "artistName"),
                    text(jsonObject, "albumName"),
                    number(jsonObject, "duration", 0.0),
                    bool(jsonObject, "instrumental"),
                    text(jsonObject, "plainLyrics"),
                    text(jsonObject, "syncedLyrics"));
        } else {
            return null;
        }
    }

    public static List<LrclibTrack> listFromJson(JsonElement json) {
        if (json != null && json.isJsonArray()) {
            JsonArray jsonArray = json.getAsJsonArray();
            ArrayList list = new ArrayList(jsonArray.size());
            for (JsonElement json2 : jsonArray) {
                LrclibTrack lrclibTrack = fromJson(json2);
                if (lrclibTrack != null) {
                    list.add(lrclibTrack);
                }
            }
            return List.copyOf(list);
        } else {
            return List.of();
        }
    }

    public static JsonElement parse(String string) {
        return JsonParser.parseString(string);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", this.id);
        json.addProperty("trackName", this.trackName);
        json.addProperty("artistName", this.artistName);
        json.addProperty("albumName", this.albumName);
        json.addProperty("duration", this.duration);
        json.addProperty("instrumental", this.instrumental);
        json.addProperty("plainLyrics", this.plainLyrics);
        json.addProperty("syncedLyrics", this.syncedLyrics);
        return json;
    }

    private static String nonNull(String string) {
        return string == null ? "" : string;
    }

    private static String text(JsonObject json, String string) {
        JsonElement json2 = json.get(string);
        JsonPrimitive jsonPrimitive;
        return json2 instanceof JsonPrimitive && (jsonPrimitive = (JsonPrimitive) json2).isString()
                ? jsonPrimitive.getAsString()
                : "";
    }

    private static long number(JsonObject json, String string, long l) {
        JsonElement json2 = json.get(string);
        JsonPrimitive jsonPrimitive;
        if (json2 instanceof JsonPrimitive && (jsonPrimitive = (JsonPrimitive) json2).isNumber()) {
            try {
                return jsonPrimitive.getAsLong();
            } catch (NumberFormatException exception) {
                return l;
            }
        } else {
            return l;
        }
    }

    private static double number(JsonObject json, String string, double d) {
        JsonElement json2 = json.get(string);
        JsonPrimitive jsonPrimitive;
        return json2 instanceof JsonPrimitive && (jsonPrimitive = (JsonPrimitive) json2).isNumber()
                ? jsonPrimitive.getAsDouble()
                : d;
    }

    private static boolean bool(JsonObject json, String string) {
        JsonElement json2 = json.get(string);
        JsonPrimitive jsonPrimitive;
        return json2 instanceof JsonPrimitive
                && (jsonPrimitive = (JsonPrimitive) json2).isBoolean()
                && jsonPrimitive.getAsBoolean();
    }
}
