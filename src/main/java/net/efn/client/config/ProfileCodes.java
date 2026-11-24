package net.efn.client.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

@Environment(EnvType.CLIENT)
final class ProfileCodes {

    static final String PREFIX = "EfnProfile:";

    static final int MAX_JSON_BYTES = 4194304;

    private static final int MAX_CODE_CHARS = 1048576;

    private static final String DAMAGED = "That code is damaged or incomplete";

    private static final byte[] DICTIONARY = ShareDictionary.bytes();

    private ProfileCodes() {}

    static String encode(JsonObject json) {
        return encode(json, "EfnProfile:");
    }

    static String encode(JsonObject json, String string) {
        byte[] bytes = tidyNumbers(json.deepCopy()).toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream byteArrayOutputStream =
                new ByteArrayOutputStream(bytes.length / 3 + 64);
        Deflater deflater = new Deflater(9, true);
        try {
            deflater.setDictionary(DICTIONARY);
            deflater.setInput(bytes);
            deflater.finish();
            byte[] bytes2 = new byte[4096];
            while (!deflater.finished()) {
                byteArrayOutputStream.write(bytes2, 0, deflater.deflate(bytes2));
            }
        } finally {
            deflater.end();
        }
        return string
                + Base64.getEncoder()
                        .withoutPadding()
                        .encodeToString(byteArrayOutputStream.toByteArray());
    }

    static JsonObject decode(String string) throws ProfileException {
        return decode(string, "EfnProfile:", "No EFN profile code on the clipboard");
    }

    static JsonObject decode(String string, String string2, String string3)
            throws ProfileException {
        String extract = extract(string, string2, string3);
        byte[] nBytes;
        try (InputStream inValue = unpack(Base64.getDecoder().decode(extract))) {
            nBytes = inValue.readNBytes(4194305);
        } catch (IllegalArgumentException | IOException exception) {
            throw new ProfileException("That code is damaged or incomplete");
        }
        if (nBytes.length > 4194304) {
            throw new ProfileException("That code is too large");
        } else {
            return parseObject(
                    new String(nBytes, StandardCharsets.UTF_8),
                    "That code is damaged or incomplete");
        }
    }

    private static InputStream unpack(byte[] bytes) throws IOException {
        if (bytes.length > 1 && (bytes[0] & 255) == 31 && (bytes[1] & 255) == 139) {
            return new GZIPInputStream(new ByteArrayInputStream(bytes));
        } else {
            Inflater inflater = new Inflater(true);
            inflater.setDictionary(DICTIONARY);
            return new InflaterInputStream(
                    new ByteArrayInputStream(Arrays.copyOf(bytes, bytes.length + 1)), inflater);
        }
    }

    private static JsonElement tidyNumbers(JsonElement json) {
        if (json.isJsonObject()) {
            JsonObject jsonObject = json.getAsJsonObject();
            for (String string : List.copyOf(jsonObject.keySet())) {
                jsonObject.add(string, tidyNumbers(jsonObject.get(string)));
            }
        } else if (json.isJsonArray()) {
            JsonArray jsonArray = json.getAsJsonArray();
            for (int i = 0; i < jsonArray.size(); i++) {
                jsonArray.set(i, tidyNumbers(jsonArray.get(i)));
            }
        } else {
            double d;
            if (json.isJsonPrimitive()
                    && json.getAsJsonPrimitive().isNumber()
                    && (d = json.getAsDouble()) == Math.rint(d)
                    && Math.abs(d) < 1.0E15) {
                return new JsonPrimitive((long) d);
            }
        }
        return json;
    }

    static JsonObject parseObject(String string, String string2) throws ProfileException {
        String string3 = string.startsWith("\ufeff") ? string.substring(1) : string;
        try {
            JsonElement string4 = JsonParser.parseString(string3);
            if (string4.isJsonObject()) {
                return string4.getAsJsonObject();
            }
        } catch (RuntimeException exception) {
            throw new ProfileException(string2);
        }
        throw new ProfileException(string2);
    }

    static boolean contains(String string, String string2) {
        if (string == null) {
            return false;
        } else {
            for (int i = 0; i + string2.length() <= string.length(); i++) {
                if (string.regionMatches(true, i, string2, 0, string2.length())) {
                    return true;
                }
            }
            return false;
        }
    }

    private static String extract(String string, String string2, String string3)
            throws ProfileException {
        if (string != null && !string.isBlank()) {
            if (string.length() > 2097152) {
                throw new ProfileException("That code is too large");
            } else {
                int i = -1;
                for (int j = 0; j + string2.length() <= string.length() && i < 0; j++) {
                    if (string.regionMatches(true, j, string2, 0, string2.length())) {
                        i = j;
                    }
                }
                if (i < 0) {
                    throw new ProfileException(string3);
                } else {
                    int k;
                    int m = k = i + string2.length();
                    while (m < string.length() && isCodeChar(string.charAt(m))) {
                        m++;
                    }
                    if (m == k) {
                        throw new ProfileException("That code is damaged or incomplete");
                    } else if (m - k > 1048576) {
                        throw new ProfileException("That code is too large");
                    } else {
                        return string.substring(k, m).replace('-', '+').replace('_', '/');
                    }
                }
            }
        } else {
            throw new ProfileException("The clipboard is empty");
        }
    }

    private static boolean isCodeChar(char c) {
        return c >= 'A' && c <= 'Z'
                || c >= 'a' && c <= 'z'
                || c >= '0' && c <= '9'
                || c == '+'
                || c == '/'
                || c == '='
                || c == '-'
                || c == '_';
    }
}
