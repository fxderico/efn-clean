/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.util.spotify;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.regex.Pattern;

@Environment(value = EnvType.CLIENT)
final class LyricsDiskCache {

    private static final int FORMAT = 1;

    private static final String SUFFIX = ".json";

    private static final Pattern KEY = Pattern.compile("[0-9a-f]{16,64}");

    private static final long MAX_FILE_BYTES = 524288L;

    private final Path dir;

    private final int maxFiles;

    private final long maxBytes;

    LyricsDiskCache(Path dir2, int maxFiles2, long maxBytes2) {
        this.dir = dir2;
        this.maxFiles = maxFiles2;
        this.maxBytes = maxBytes2;
    }

    Entry read(String string) {
        Path path = this.file(string);
        if (path == null) {
            return null;
        }
        try {
            if (Files.size(path) > 524288L) {
                return null;
            }
            JsonElement string2 =
                    JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            if (!string2.isJsonObject()) {
                return null;
            }
            JsonObject jsonObject = string2.getAsJsonObject();
            if (LyricsDiskCache.intValue(jsonObject, "format") != 1) {
                return null;
            }
            long longValue = LyricsDiskCache.longValue(jsonObject, "fetchedAt");
            if (longValue <= 0L) {
                return null;
            }
            LrclibTrack lrclibTrack =
                    jsonObject.has("track") ? LrclibTrack.fromJson(jsonObject.get("track")) : null;
            LyricsDiskCache.touch(path);
            return new Entry(lrclibTrack, longValue);
        } catch (JsonParseException
                | IOException
                | IllegalStateException
                | UnsupportedOperationException exception) {
            return null;
        }
    }

    void write(String string, LrclibTrack lrclibTrack, long l) {
        Path path = this.file(string);
        if (path == null) {
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("format", 1);
        json.addProperty("fetchedAt", l);
        if (lrclibTrack != null) {
            json.add("track", lrclibTrack.toJson());
        }
        Path sibling = path.resolveSibling(String.valueOf(path.getFileName()) + ".tmp");
        try {
            Files.createDirectories(this.dir, new FileAttribute[0]);
            Files.writeString(
                    sibling,
                    (CharSequence) json.toString(),
                    StandardCharsets.UTF_8,
                    new OpenOption[0]);
            try {
                Files.move(
                        sibling,
                        path,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException exception) {
                Files.move(sibling, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception2) {
            LyricsDiskCache.delete(sibling);
        }
    }

    void prune() {
        if (!Files.isDirectory(this.dir, new LinkOption[0])) {
            return;
        }
        ArrayList<Stored> list = new ArrayList<Stored>();
        long l = 0L;
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(this.dir)) {
            for (Path path : directoryStream) {
                BasicFileAttributes attributes;
                String string = path.getFileName().toString();
                if (string.endsWith(".json.tmp")) {
                    LyricsDiskCache.delete(path);
                    continue;
                }
                if (!string.endsWith(SUFFIX)
                        || !KEY.matcher(string.substring(0, string.length() - SUFFIX.length()))
                                .matches()
                        || !(attributes =
                                        Files.readAttributes(
                                                path, BasicFileAttributes.class, new LinkOption[0]))
                                .isRegularFile()) continue;
                list.add(
                        new Stored(
                                path, attributes.size(), attributes.lastModifiedTime().toMillis()));
                l += attributes.size();
            }
        } catch (IOException exception) {
            return;
        }
        if (list.size() <= this.maxFiles && l <= this.maxBytes) {
            return;
        }
        list.sort(Comparator.comparingLong(Stored::usedAt));
        int i = list.size();
        for (Stored stored : list) {
            if (i <= this.maxFiles && l <= this.maxBytes) break;
            if (!LyricsDiskCache.delete(stored.file())) continue;
            --i;
            l -= stored.size();
        }
    }

    private Path file(String string) {
        return string != null && KEY.matcher(string).matches()
                ? this.dir.resolve(string + SUFFIX)
                : null;
    }

    private static boolean delete(Path path) {
        try {
            Files.deleteIfExists(path);
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private static void touch(Path path) {
        try {
            Files.setLastModifiedTime(path, FileTime.fromMillis(System.currentTimeMillis()));
        } catch (IOException exception) {
            // empty catch block
        }
    }

    private static int intValue(JsonObject json, String string) {
        JsonPrimitive jsonPrimitive;
        JsonElement json2 = json.get(string);
        return json2 instanceof JsonPrimitive && (jsonPrimitive = (JsonPrimitive) json2).isNumber()
                ? jsonPrimitive.getAsInt()
                : -1;
    }

    private static long longValue(JsonObject json, String string) {
        JsonPrimitive jsonPrimitive;
        JsonElement json2 = json.get(string);
        return json2 instanceof JsonPrimitive && (jsonPrimitive = (JsonPrimitive) json2).isNumber()
                ? jsonPrimitive.getAsLong()
                : -1L;
    }

    @Environment(value = EnvType.CLIENT)
    record Entry(LrclibTrack track, long fetchedAt) {

        boolean found() {
            return this.track != null;
        }
    }

    @Environment(value = EnvType.CLIENT)
    private record Stored(Path file, long size, long usedAt) {}
}
