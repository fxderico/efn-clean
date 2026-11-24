package net.efn.client.module.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ColorSetting extends Setting {

    private final float[] value;

    private int packed;

    private int packedR;

    private int packedG;

    private int packedB;

    private int packedA;

    public ColorSetting(String string, int i) {
        super(string);
        this.value =
                new float[] {
                    (i >> 16 & 0xFF) / 255.0F,
                    (i >> 8 & 0xFF) / 255.0F,
                    (i & 0xFF) / 255.0F,
                    (i >>> 24 & 0xFF) / 255.0F
                };
    }

    public float[] value() {
        return this.value;
    }

    public void setArgb(int fromArgb) {
        if (this.argb() != fromArgb) {
            this.setFromArgb(fromArgb);
            this.notifyChanged();
        }
    }

    public int argb() {
        int floatToRawIntBits = Float.floatToRawIntBits(this.value[0]);
        int floatToRawIntBits2 = Float.floatToRawIntBits(this.value[1]);
        int floatToRawIntBits3 = Float.floatToRawIntBits(this.value[2]);
        int floatToRawIntBits4 = Float.floatToRawIntBits(this.value[3]);
        if (floatToRawIntBits != this.packedR
                || floatToRawIntBits2 != this.packedG
                || floatToRawIntBits3 != this.packedB
                || floatToRawIntBits4 != this.packedA) {
            this.packed = this.pack();
            this.packedR = floatToRawIntBits;
            this.packedG = floatToRawIntBits2;
            this.packedB = floatToRawIntBits3;
            this.packedA = floatToRawIntBits4;
        }
        return this.packed;
    }

    public int rgb() {
        return this.argb() & 16777215;
    }

    public int argbWithAlpha(int i) {
        return clamp255(i) << 24 | this.argb() & 16777215;
    }

    private int pack() {
        return pack(this.value[0], this.value[1], this.value[2], this.value[3]);
    }

    private static int pack(float f, float g, float h, float f2) {
        int i = Math.round(clamp01(f2) * 255.0F);
        int j = Math.round(clamp01(f) * 255.0F);
        int k = Math.round(clamp01(g) * 255.0F);
        int m = Math.round(clamp01(h) * 255.0F);
        return i << 24 | j << 16 | k << 8 | m;
    }

    @Override
    public JsonElement compact(JsonElement json) {
        try {
            if (json != null && json.isJsonObject()) {
                JsonObject jsonObject = json.getAsJsonObject();
                return new JsonPrimitive(
                        hex(
                                pack(
                                        readFloat(jsonObject, "r", 0.0F),
                                        readFloat(jsonObject, "g", 0.0F),
                                        readFloat(jsonObject, "b", 0.0F),
                                        readFloat(jsonObject, "a", 1.0F))));
            }
            if (json != null && json.isJsonArray() && json.getAsJsonArray().size() >= 3) {
                JsonArray jsonArray = json.getAsJsonArray();
                float f = jsonArray.size() > 3 ? jsonArray.get(3).getAsFloat() : 1.0F;
                return new JsonPrimitive(
                        hex(
                                pack(
                                        jsonArray.get(0).getAsFloat(),
                                        jsonArray.get(1).getAsFloat(),
                                        jsonArray.get(2).getAsFloat(),
                                        f)));
            }
        } catch (RuntimeException exception) {
        }
        return json;
    }

    public static String hex(int i) {
        return String.format("%08X", i);
    }

    @Override
    public JsonElement serialize() {
        JsonObject json = new JsonObject();
        json.addProperty("r", clamp01(this.value[0]));
        json.addProperty("g", clamp01(this.value[1]));
        json.addProperty("b", clamp01(this.value[2]));
        json.addProperty("a", clamp01(this.value[3]));
        return json;
    }

    @Override
    protected void deserializeValue(JsonElement json) {
        if (json != null) {
            try {
                if (json.isJsonObject()) {
                    JsonObject jsonObject = json.getAsJsonObject();
                    this.value[0] = readFloat(jsonObject, "r", this.value[0]);
                    this.value[1] = readFloat(jsonObject, "g", this.value[1]);
                    this.value[2] = readFloat(jsonObject, "b", this.value[2]);
                    this.value[3] = readFloat(jsonObject, "a", this.value[3]);
                    this.clampValues();
                    return;
                }
                if (json.isJsonArray()) {
                    JsonArray jsonArray = json.getAsJsonArray();
                    for (int i = 0; i < Math.min(4, jsonArray.size()); i++) {
                        this.value[i] = clamp01(jsonArray.get(i).getAsFloat());
                    }
                    return;
                }
                if (json.isJsonPrimitive()) {
                    JsonPrimitive jsonPrimitive = json.getAsJsonPrimitive();
                    if (jsonPrimitive.isString()) {
                        this.setFromHex(jsonPrimitive.getAsString());
                    } else if (jsonPrimitive.isNumber()) {
                        this.setFromArgb(jsonPrimitive.getAsInt());
                    }
                }
            } catch (RuntimeException exception) {
            }
        }
    }

    private void setFromHex(String string) {
        String string2 = string.trim();
        if (string2.startsWith("#")) {
            string2 = string2.substring(1);
        }
        if (string2.length() == 6) {
            string2 = "FF" + string2;
        }
        if (string2.length() == 8) {
            this.setFromArgb((int) Long.parseLong(string2, 16));
        }
    }

    private void setFromArgb(int i) {
        this.value[0] = (i >> 16 & 0xFF) / 255.0F;
        this.value[1] = (i >> 8 & 0xFF) / 255.0F;
        this.value[2] = (i & 0xFF) / 255.0F;
        this.value[3] = (i >>> 24 & 0xFF) / 255.0F;
    }

    private void clampValues() {
        for (int i = 0; i < this.value.length; i++) {
            this.value[i] = clamp01(this.value[i]);
        }
    }

    private static float readFloat(JsonObject json, String string, float f) {
        return !json.has(string) ? f : clamp01(json.get(string).getAsFloat());
    }

    private static float clamp01(float f) {
        return !Float.isNaN(f) && !Float.isInfinite(f) ? Math.max(0.0F, Math.min(1.0F, f)) : 1.0F;
    }

    private static int clamp255(int i) {
        return Math.max(0, Math.min(255, i));
    }
}
