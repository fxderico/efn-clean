package net.efn.client.module.impl.render.character.gltf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Environment(EnvType.CLIENT)
final class GltfFile {

    private static final int GLTF = 1179937895;

    private static final int JSON = 1313821514;

    private static final int BIN = 5130562;

    private static final int MAX_ELEMENTS = 33554432;

    final JsonObject json;

    private final ByteBuffer bin;

    private GltfFile(JsonObject json2, ByteBuffer bin2) {
        this.json = json2;
        this.bin = bin2;
    }

    static GltfFile read(byte[] bytes) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if (bytes.length < 20 || byteBuffer.getInt(0) != 1179937895) {
            throw new IOException("not a .glb file");
        } else if (byteBuffer.getInt(4) != 2) {
            throw new IOException("only glTF 2.0 files are supported");
        } else {
            int i = Math.min(byteBuffer.getInt(8), bytes.length);
            int j = 12;
            JsonObject json2 = null;
            ByteBuffer byteBuffer2 = ByteBuffer.allocate(0);
            while (j + 8 <= i) {
                int k = byteBuffer.getInt(j);
                int m = byteBuffer.getInt(j + 4);
                if (k < 0 || k > i - j - 8) {
                    throw new IOException("the file is cut short");
                }
                if (m == 1313821514) {
                    json2 =
                            JsonParser.parseString(
                                            new String(bytes, j + 8, k, StandardCharsets.UTF_8))
                                    .getAsJsonObject();
                } else if (m == 5130562) {
                    byteBuffer2 =
                            ByteBuffer.wrap(bytes, j + 8, k).slice().order(ByteOrder.LITTLE_ENDIAN);
                }
                j += 8 + k;
            }
            if (json2 == null) {
                throw new IOException("the file has no glTF data");
            } else {
                JsonArray array = array(json2, "extensionsRequired");
                if (!array.isEmpty()) {
                    throw new IOException("it needs " + array + ", export it without compression");
                } else {
                    return new GltfFile(json2, byteBuffer2);
                }
            }
        }
    }

    static JsonArray array(JsonObject json2, String string) {
        JsonElement json3 = json2.get(string);
        return json3 != null && json3.isJsonArray() ? json3.getAsJsonArray() : new JsonArray();
    }

    static JsonObject object(JsonObject json2, String string) {
        JsonElement json3 = json2.get(string);
        return json3 != null && json3.isJsonObject() ? json3.getAsJsonObject() : new JsonObject();
    }

    static int integer(JsonObject json2, String string, int i) {
        JsonElement json3 = json2.get(string);
        return json3 != null && json3.isJsonPrimitive() ? json3.getAsInt() : i;
    }

    private static String string(JsonObject json2, String string, String string2) {
        JsonElement json3 = json2.get(string);
        return json3 != null && json3.isJsonPrimitive() ? json3.getAsString() : string2;
    }

    int nodes() {
        return array(this.json, "nodes").size();
    }

    JsonObject node(int i) {
        return array(this.json, "nodes").get(i).getAsJsonObject();
    }

    String nodeName(int i) {
        return string(this.node(i), "name", "node" + i);
    }

    private JsonObject material(int i) {
        return i < 0 ? new JsonObject() : array(this.json, "materials").get(i).getAsJsonObject();
    }

    String materialName(int i) {
        return string(this.material(i), "name", "material" + i);
    }

    boolean translucent(int i) {
        String string = string(this.material(i), "alphaMode", "OPAQUE");
        return string.equals("BLEND") || string.equals("MASK");
    }

    boolean doubleSided(int i) {
        JsonElement json2 = this.material(i).get("doubleSided");
        return json2 != null && json2.getAsBoolean();
    }

    private JsonObject baseColorTexture(int i) {
        return object(object(this.material(i), "pbrMetallicRoughness"), "baseColorTexture");
    }

    int texCoord(int i) {
        return integer(this.baseColorTexture(i), "texCoord", 0);
    }

    float[] baseColorFactor(int i) {
        JsonArray array =
                array(object(this.material(i), "pbrMetallicRoughness"), "baseColorFactor");
        return array.size() != 4
                ? new float[] {1.0F, 1.0F, 1.0F, 1.0F}
                : new float[] {
                    array.get(0).getAsFloat(),
                    array.get(1).getAsFloat(),
                    array.get(2).getAsFloat(),
                    array.get(3).getAsFloat()
                };
    }

    byte[] baseColorImage(int i) throws IOException {
        JsonObject json2 = this.baseColorTexture(i);
        if (!json2.has("index")) {
            return null;
        } else {
            JsonObject jsonObject =
                    array(this.json, "textures")
                            .get(json2.get("index").getAsInt())
                            .getAsJsonObject();
            int integer = integer(jsonObject, "source", -1);
            if (integer < 0) {
                return null;
            } else {
                JsonObject jsonObject2 = array(this.json, "images").get(integer).getAsJsonObject();
                if (jsonObject2.has("bufferView")) {
                    ByteBuffer byteBuffer = this.view(jsonObject2.get("bufferView").getAsInt(), 0);
                    byte[] bytes = new byte[byteBuffer.remaining()];
                    byteBuffer.get(bytes);
                    return bytes;
                } else {
                    String string = string(jsonObject2, "uri", "");
                    if (string.startsWith("data:")) {
                        return Base64.getDecoder().decode(string.substring(string.indexOf(44) + 1));
                    } else {
                        throw new IOException(
                                "its textures are separate files, export it as a single .glb");
                    }
                }
            }
        }
    }

    float[] floats(int i) throws IOException {
        JsonObject json2 = this.accessor(i);
        int j = json2.get("count").getAsInt();
        int components = components(json2.get("type").getAsString());
        int k = json2.get("componentType").getAsInt();
        JsonElement json3 = json2.get("normalized");
        boolean bl = json3 != null && json3.getAsBoolean();
        float[] floats = new float[j * components];
        if (!json2.has("bufferView")) {
            return floats;
        } else {
            ByteBuffer byteBuffer =
                    this.view(json2.get("bufferView").getAsInt(), integer(json2, "byteOffset", 0));
            int stride = this.stride(json2, components, k);
            int m = size(k);
            for (int p = 0; p < j; p++) {
                for (int q = 0; q < components; q++) {
                    int n = p * stride + q * m;
                    floats[p * components + q] =
                            switch (k) {
                                case 5120 ->
                                        bl
                                                ? Math.max(byteBuffer.get(n) / 127.0F, -1.0F)
                                                : byteBuffer.get(n);
                                case 5121 -> (byteBuffer.get(n) & 255) / (bl ? 255.0F : 1.0F);
                                case 5122 ->
                                        bl
                                                ? Math.max(byteBuffer.getShort(n) / 32767.0F, -1.0F)
                                                : byteBuffer.getShort(n);
                                case 5123 ->
                                        (byteBuffer.getShort(n) & '\uffff')
                                                / (bl ? 65535.0F : 1.0F);
                                default -> throw new IOException("unknown component type " + k);
                                case 5125 -> byteBuffer.getInt(n);
                                case 5126 -> byteBuffer.getFloat(n);
                            };
                }
            }
            return floats;
        }
    }

    int[] ints(int i) throws IOException {
        JsonObject json2 = this.accessor(i);
        int j = json2.get("count").getAsInt();
        int components = components(json2.get("type").getAsString());
        int k = json2.get("componentType").getAsInt();
        ByteBuffer byteBuffer =
                this.view(json2.get("bufferView").getAsInt(), integer(json2, "byteOffset", 0));
        int stride = this.stride(json2, components, k);
        int m = size(k);
        int[] ints = new int[j * components];
        for (int p = 0; p < j; p++) {
            for (int q = 0; q < components; q++) {
                int n = p * stride + q * m;
                ints[p * components + q] =
                        switch (k) {
                            case 5121 -> byteBuffer.get(n) & 255;
                            default -> throw new IOException("unknown index type " + k);
                            case 5123 -> byteBuffer.getShort(n) & '\uffff';
                            case 5125 -> byteBuffer.getInt(n);
                        };
            }
        }
        return ints;
    }

    private JsonObject accessor(int i) throws IOException {
        JsonObject jsonObject = array(this.json, "accessors").get(i).getAsJsonObject();
        if (jsonObject.has("sparse")) {
            throw new IOException("sparse accessors aren't supported");
        } else {
            long l =
                    (long) jsonObject.get("count").getAsInt()
                            * components(jsonObject.get("type").getAsString());
            if (l >= 0L && l <= 33554432L) {
                return jsonObject;
            } else {
                throw new IOException("a mesh is far too large");
            }
        }
    }

    private ByteBuffer view(int i, int j) throws IOException {
        JsonObject jsonObject = array(this.json, "bufferViews").get(i).getAsJsonObject();
        if (integer(jsonObject, "buffer", 0) != 0) {
            throw new IOException("its data is split over several files");
        } else {
            int k = integer(jsonObject, "byteOffset", 0) + j;
            int m = jsonObject.get("byteLength").getAsInt() - j;
            if (k >= 0 && m >= 0 && k + m <= this.bin.capacity()) {
                return this.bin.slice(k, m).order(ByteOrder.LITTLE_ENDIAN);
            } else {
                throw new IOException("the file is cut short");
            }
        }
    }

    private int stride(JsonObject json2, int i, int j) {
        JsonObject jsonObject =
                array(this.json, "bufferViews")
                        .get(json2.get("bufferView").getAsInt())
                        .getAsJsonObject();
        return integer(jsonObject, "byteStride", i * size(j));
    }

    private static int size(int i) {
        return switch (i) {
            case 5120, 5121 -> 1;
            case 5122, 5123 -> 2;
            default -> 4;
        };
    }

    private static int components(String string) throws IOException {
        return switch (string) {
            case "SCALAR" -> 1;
            case "VEC2" -> 2;
            case "VEC3" -> 3;
            case "VEC4" -> 4;
            case "MAT4" -> 16;
            default -> throw new IOException("unknown accessor type " + string);
        };
    }
}
