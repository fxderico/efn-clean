package net.efn.client.module.impl.render.character.gltf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Environment(EnvType.CLIENT)
final class PosedMesh {

    private static final int MAX_TRIANGLES = 4000000;

    final GltfFile file;

    final float[][] globals;

    final int[] parent;

    final Set<Integer> joints = new HashSet<>();

    float[] pos = new float[0];

    float[] nrm = new float[0];

    float[] uv = new float[0];

    int[] material = new int[0];

    int[] color = new int[0];

    boolean colored;

    int[] influence = new int[0];

    float[] weight = new float[0];

    int[] tris = new int[0];

    int vertices;

    int triangles;

    private float scale = 1.0F;

    private float shiftX;

    private float shiftY;

    private float shiftZ;

    private boolean flipped;

    private PosedMesh(GltfFile file2) {
        this.file = file2;
        this.globals = new float[file2.nodes()][];
        this.parent = new int[file2.nodes()];
        Arrays.fill(this.parent, -1);
    }

    static PosedMesh pose(GltfFile gltfFile, Map<Integer, Float> map) throws IOException {
        PosedMesh posedMesh = new PosedMesh(gltfFile);
        for (int i = 0; i < gltfFile.nodes(); i++) {
            for (JsonElement json : GltfFile.array(gltfFile.node(i), "children")) {
                posedMesh.parent[json.getAsInt()] = i;
            }
        }
        JsonArray array = GltfFile.array(gltfFile.json, "scenes");
        JsonArray array2 = new JsonArray();
        if (array.isEmpty()) {
            for (int j = 0; j < gltfFile.nodes(); j++) {
                if (posedMesh.parent[j] < 0) {
                    array2.add(j);
                }
            }
        } else {
            array2 =
                    GltfFile.array(
                            array.get(GltfFile.integer(gltfFile.json, "scene", 0))
                                    .getAsJsonObject(),
                            "nodes");
        }
        for (JsonElement json2 : array2) {
            posedMesh.walk(json2.getAsInt(), Mat4.identity(), map);
        }
        for (JsonElement json3 : GltfFile.array(gltfFile.json, "skins")) {
            for (JsonElement json4 : GltfFile.array(json3.getAsJsonObject(), "joints")) {
                posedMesh.joints.add(json4.getAsInt());
            }
        }
        for (JsonElement json5 : array2) {
            posedMesh.collect(json5.getAsInt());
        }
        if (posedMesh.triangles == 0) {
            throw new IOException("it has no triangles");
        } else {
            return posedMesh;
        }
    }

    private void walk(int i, float[] floats, Map<Integer, Float> map) {
        float[] floats2 = Mat4.mul(floats, Mat4.local(this.file.node(i)));
        Float value = (Float) map.get(i);
        if (value != null) {
            floats2 =
                    Mat4.mul(Mat4.rollAbout(value, Mat4.point(floats2, 0.0F, 0.0F, 0.0F)), floats2);
        }
        this.globals[i] = floats2;
        for (JsonElement json : GltfFile.array(this.file.node(i), "children")) {
            this.walk(json.getAsInt(), floats2, map);
        }
    }

    private void collect(int i) throws IOException {
        JsonObject json = this.file.node(i);
        if (json.has("mesh")) {
            JsonObject jsonObject =
                    GltfFile.array(this.file.json, "meshes")
                            .get(json.get("mesh").getAsInt())
                            .getAsJsonObject();
            int integer = GltfFile.integer(json, "skin", -1);
            for (JsonElement json2 : GltfFile.array(jsonObject, "primitives")) {
                this.primitive(json2.getAsJsonObject(), i, integer);
            }
        }
        for (JsonElement json3 : GltfFile.array(json, "children")) {
            this.collect(json3.getAsInt());
        }
    }

    private void primitive(JsonObject json, int i, int j) throws IOException {
        if (GltfFile.integer(json, "mode", 4) == 4) {
            JsonObject json2 = GltfFile.object(json, "attributes");
            int integer = GltfFile.integer(json, "material", -1);
            float[] floats = this.file.floats(json2.get("POSITION").getAsInt());
            int k = floats.length / 3;
            float[] floats2 =
                    json2.has("NORMAL") ? this.file.floats(json2.get("NORMAL").getAsInt()) : null;
            String string = "TEXCOORD_" + this.file.texCoord(integer);
            float[] floats3 =
                    json2.has(string)
                            ? this.file.floats(json2.get(string).getAsInt())
                            : new float[k * 2];
            float[] floats4 =
                    json2.has("COLOR_0") ? this.file.floats(json2.get("COLOR_0").getAsInt()) : null;
            int m = floats4 == null ? 0 : floats4.length / k;
            this.colored |= floats4 != null;
            int[] ints =
                    json.has("indices") ? this.file.ints(json.get("indices").getAsInt()) : null;
            if (ints == null) {
                ints = new int[k];
                int n = 0;
                while (n < k) {
                    ints[n] = n++;
                }
            }
            for (int o : ints) {
                if (o < 0 || o >= k) {
                    throw new IOException("a mesh has broken indices");
                }
            }
            if (this.triangles + ints.length / 3 > 4000000) {
                throw new IOException("it has more than four million triangles");
            } else {
                float[][] floatGrid = null;
                int[] ints2 = null;
                int[] ints3 = null;
                float[] floats5 = null;
                if (j >= 0 && json2.has("JOINTS_0") && json2.has("WEIGHTS_0")) {
                    JsonObject jsonObject =
                            GltfFile.array(this.file.json, "skins").get(j).getAsJsonObject();
                    JsonArray array = GltfFile.array(jsonObject, "joints");
                    ints3 = new int[array.size()];
                    float[] floats6 =
                            jsonObject.has("inverseBindMatrices")
                                    ? this.file.floats(
                                            jsonObject.get("inverseBindMatrices").getAsInt())
                                    : null;
                    floatGrid = new float[array.size()][];
                    for (int q = 0; q < array.size(); q++) {
                        ints3[q] = array.get(q).getAsInt();
                        float[] floats7 =
                                floats6 == null
                                        ? Mat4.identity()
                                        : Arrays.copyOfRange(floats6, q * 16, q * 16 + 16);
                        float[] global = this.globals[ints3[q]];
                        floatGrid[q] = Mat4.mul(global == null ? Mat4.identity() : global, floats7);
                    }
                    ints2 = this.file.ints(json2.get("JOINTS_0").getAsInt());
                    floats5 = this.file.floats(json2.get("WEIGHTS_0").getAsInt());
                }
                float[] global2 = this.globals[i];
                int vertices2 = this.vertices;
                this.grow(k, ints.length / 3);
                for (int t = 0; t < k; t++) {
                    float f = floats[t * 3];
                    float g = floats[t * 3 + 1];
                    float h = floats[t * 3 + 2];
                    float f2 = floats2 == null ? 0.0F : floats2[t * 3];
                    float g2 = floats2 == null ? 1.0F : floats2[t * 3 + 1];
                    float h2 = floats2 == null ? 0.0F : floats2[t * 3 + 2];
                    int p = vertices2 + t;
                    Arrays.fill(this.influence, p * 4, p * 4 + 4, -1);
                    float[] floats8;
                    float[] floats9;
                    if (floatGrid != null) {
                        floats9 = new float[3];
                        floats8 = new float[3];
                        for (int v = 0; v < 4; v++) {
                            float f3 = floats5[t * 4 + v];
                            int r = ints2[t * 4 + v];
                            if (!(f3 <= 0.0F) && r >= 0 && r < floatGrid.length) {
                                float[] floats10 = Mat4.point(floatGrid[r], f, g, h);
                                float[] floats11 = Mat4.dir(floatGrid[r], f2, g2, h2);
                                for (int i2 = 0; i2 < 3; i2++) {
                                    floats9[i2] += floats10[i2] * f3;
                                    floats8[i2] += floats11[i2] * f3;
                                }
                                this.influence[p * 4 + v] = ints3[r];
                                this.weight[p * 4 + v] = f3;
                            }
                        }
                    } else {
                        floats9 = Mat4.point(global2, f, g, h);
                        floats8 = Mat4.dir(global2, f2, g2, h2);
                        this.influence[p * 4] = i;
                        this.weight[p * 4] = 1.0F;
                    }
                    System.arraycopy(floats9, 0, this.pos, p * 3, 3);
                    Mat4.normalise(floats8);
                    System.arraycopy(floats8, 0, this.nrm, p * 3, 3);
                    this.uv[p * 2] = floats3[t * 2];
                    this.uv[p * 2 + 1] = floats3[t * 2 + 1];
                    this.material[p] = integer;
                    this.color[p] =
                            floats4 == null
                                    ? -1
                                    : 0xFF000000
                                            | srgb(floats4[t * m]) << 16
                                            | srgb(floats4[t * m + 1]) << 8
                                            | srgb(floats4[t * m + 2]);
                }
                for (int j2 = 0; j2 < ints.length / 3; j2++) {
                    for (int k2 = 0; k2 < 3; k2++) {
                        this.tris[(this.triangles + j2) * 3 + k2] = vertices2 + ints[j2 * 3 + k2];
                    }
                }
                int triangles2 = this.triangles;
                this.vertices += k;
                this.triangles += ints.length / 3;
                if (floats2 == null) {
                    this.smoothNormals(vertices2, k, triangles2);
                }
            }
        }
    }

    private void smoothNormals(int i, int j, int k) {
        Arrays.fill(this.nrm, i * 3, (i + j) * 3, 0.0F);
        for (int m = k; m < this.triangles; m++) {
            float[] floats =
                    Mat4.faceNormal(
                            this.pos, this.tris[m * 3], this.tris[m * 3 + 1], this.tris[m * 3 + 2]);
            for (int n = 0; n < 3; n++) {
                int tri = this.tris[m * 3 + n];
                for (int p = 0; p < 3; p++) {
                    int o = tri * 3 + p;
                    this.nrm[o] = this.nrm[o] + floats[p];
                }
            }
        }
        for (int r = i; r < i + j; r++) {
            float[] floats2 =
                    new float[] {this.nrm[r * 3], this.nrm[r * 3 + 1], this.nrm[r * 3 + 2]};
            Mat4.normalise(floats2);
            System.arraycopy(floats2, 0, this.nrm, r * 3, 3);
        }
    }

    private void grow(int i, int j) {
        int k = this.vertices + i;
        int m = this.triangles + j;
        this.pos = Arrays.copyOf(this.pos, k * 3);
        this.nrm = Arrays.copyOf(this.nrm, k * 3);
        this.uv = Arrays.copyOf(this.uv, k * 2);
        this.influence = Arrays.copyOf(this.influence, k * 4);
        this.weight = Arrays.copyOf(this.weight, k * 4);
        this.material = Arrays.copyOf(this.material, k);
        this.color = Arrays.copyOf(this.color, k);
        this.tris = Arrays.copyOf(this.tris, m * 3);
    }

    private static int srgb(float f) {
        return Math.round(
                (float) Math.pow(Math.max(0.0F, Math.min(1.0F, f)), 0.45454545454545453) * 255.0F);
    }

    void cluster(int i) {
        if (this.triangles > i) {
            float[] floats = this.bounds();
            float f =
                    Math.max(
                                    floats[3] - floats[0],
                                    Math.max(floats[4] - floats[1], floats[5] - floats[2]))
                            / 600.0F;
            int[] ints = new int[this.vertices];
            while (this.weld(f, ints) > i) {
                f *= 1.25F;
            }
            int j = 0;
            for (int k : ints) {
                j = Math.max(j, k + 1);
            }
            float[] floats2 = new float[j * 3];
            float[] floats3 = new float[j * 3];
            float[] floats4 = new float[j * 2];
            int[] ints2 = new int[j];
            int[] ints3 = new int[j];
            int[] ints4 = new int[j * 4];
            float[] floats5 = new float[j * 4];
            long[] longs = new long[j * 3];
            for (int m = 0; m < this.vertices; m++) {
                int n;
                int o = n = ints[m];
                int p = ints2[o]++;
                if (p == 0) {
                    ints3[n] = this.material[m];
                    System.arraycopy(this.influence, m * 4, ints4, n * 4, 4);
                    System.arraycopy(this.weight, m * 4, floats5, n * 4, 4);
                }
                for (int q = 0; q < 3; q++) {
                    int r = n * 3 + q;
                    floats2[r] += this.pos[m * 3 + q];
                    int t = n * 3 + q;
                    floats3[t] += this.nrm[m * 3 + q];
                    int u = n * 3 + q;
                    longs[u] += this.color[m] >> 16 - q * 8 & 0xFF;
                }
                int v = n * 2;
                floats4[v] += this.uv[m * 2];
                int w = n * 2 + 1;
                floats4[w] += this.uv[m * 2 + 1];
            }
            int[] ints5 = new int[j];
            for (int i2 = 0; i2 < j; i2++) {
                for (int j2 = 0; j2 < 3; j2++) {
                    int k2 = i2 * 3 + j2;
                    floats2[k2] /= ints2[i2];
                }
                int m2 = i2 * 2;
                floats4[m2] /= ints2[i2];
                int n2 = i2 * 2 + 1;
                floats4[n2] /= ints2[i2];
                float[] floats6 =
                        new float[] {floats3[i2 * 3], floats3[i2 * 3 + 1], floats3[i2 * 3 + 2]};
                Mat4.normalise(floats6);
                System.arraycopy(floats6, 0, floats3, i2 * 3, 3);
                ints5[i2] =
                        0xFF000000
                                | (int) (longs[i2 * 3] / ints2[i2]) << 16
                                | (int) (longs[i2 * 3 + 1] / ints2[i2]) << 8
                                | (int) (longs[i2 * 3 + 2] / ints2[i2]);
            }
            HashSet set = new HashSet();
            int[] ints6 = new int[this.triangles * 3];
            int o2 = 0;
            for (int p2 = 0; p2 < this.triangles; p2++) {
                int q2 = ints[this.tris[p2 * 3]];
                int r2 = ints[this.tris[p2 * 3 + 1]];
                int t2 = ints[this.tris[p2 * 3 + 2]];
                if (q2 != r2 && r2 != t2 && q2 != t2 && set.add(key(q2, r2, t2))) {
                    ints6[o2++] = q2;
                    ints6[o2++] = r2;
                    ints6[o2++] = t2;
                }
            }
            this.pos = floats2;
            this.nrm = floats3;
            this.uv = floats4;
            this.material = ints3;
            this.influence = ints4;
            this.weight = floats5;
            this.color = ints5;
            this.tris = Arrays.copyOf(ints6, o2);
            this.vertices = j;
            this.triangles = o2 / 3;
        }
    }

    private int weld(float f, int[] ints) {
        HashMap map = new HashMap();
        for (int i = 0; i < this.vertices; i++) {
            String string =
                    (int) Math.floor(this.pos[i * 3] / f)
                            + ","
                            + (int) Math.floor(this.pos[i * 3 + 1] / f)
                            + ","
                            + (int) Math.floor(this.pos[i * 3 + 2] / f)
                            + ","
                            + this.material[i]
                            + ","
                            + Math.round(this.uv[i * 2] * 64.0F)
                            + ","
                            + Math.round(this.uv[i * 2 + 1] * 64.0F);
            Integer integer = (Integer) map.putIfAbsent(string, map.size());
            ints[i] = integer == null ? map.size() - 1 : integer;
        }
        HashSet set = new HashSet();
        for (int j = 0; j < this.triangles; j++) {
            int k = ints[this.tris[j * 3]];
            int m = ints[this.tris[j * 3 + 1]];
            int n = ints[this.tris[j * 3 + 2]];
            if (k != m && m != n && k != n) {
                set.add(key(k, m, n));
            }
        }
        return set.size();
    }

    private static long key(int i, int j, int k) {
        int m = Math.min(i, Math.min(j, k));
        int n = Math.max(i, Math.max(j, k));
        int o = i + j + k - m - n;
        return (m * 2654435761L + o) * 2246822519L + n;
    }

    int[] separate(int[] ints) {
        HashMap map = new HashMap();
        ArrayList list = new ArrayList();
        ArrayList list2 = new ArrayList();
        for (int i = 0; i < this.triangles; i++) {
            int owner =
                    CharacterBake.owner(
                            ints, this.tris[i * 3], this.tris[i * 3 + 1], this.tris[i * 3 + 2]);
            for (int k = 0; k < 3; k++) {
                int tri = this.tris[i * 3 + k];
                if (ints[tri] != owner) {
                    Integer integer = (Integer) map.get(tri * 8L + owner);
                    if (integer == null) {
                        integer = this.vertices + list.size();
                        map.put(tri * 8L + owner, integer);
                        list.add(tri);
                        list2.add(owner);
                    }
                    this.tris[i * 3 + k] = integer;
                }
            }
        }
        int vertices2 = this.vertices;
        this.grow(list.size(), 0);
        int[] ints2 = Arrays.copyOf(ints, vertices2 + list.size());
        for (int o = 0; o < list.size(); o++) {
            int j = (Integer) list.get(o);
            int m = vertices2 + o;
            System.arraycopy(this.pos, j * 3, this.pos, m * 3, 3);
            System.arraycopy(this.nrm, j * 3, this.nrm, m * 3, 3);
            System.arraycopy(this.uv, j * 2, this.uv, m * 2, 2);
            System.arraycopy(this.influence, j * 4, this.influence, m * 4, 4);
            System.arraycopy(this.weight, j * 4, this.weight, m * 4, 4);
            this.material[m] = this.material[j];
            this.color[m] = this.color[j];
            ints2[m] = (Integer) list2.get(o);
        }
        this.vertices = this.vertices + list.size();
        return ints2;
    }

    void normalise(float f, boolean flipped2) {
        this.flipped = flipped2;
        if (flipped2) {
            for (int i = 0; i < this.vertices; i++) {
                this.pos[i * 3] = -this.pos[i * 3];
                this.pos[i * 3 + 2] = -this.pos[i * 3 + 2];
                this.nrm[i * 3] = -this.nrm[i * 3];
                this.nrm[i * 3 + 2] = -this.nrm[i * 3 + 2];
            }
        }
        float[] floats = this.bounds();
        this.scale = f / Math.max(1.0E-6F, floats[4] - floats[1]);
        this.shiftX = -(floats[0] + floats[3]) * 0.5F;
        this.shiftY = -floats[1];
        this.shiftZ = -(floats[2] + floats[5]) * 0.5F;
        for (int j = 0; j < this.vertices; j++) {
            this.pos[j * 3] = (this.pos[j * 3] + this.shiftX) * this.scale;
            this.pos[j * 3 + 1] = (this.pos[j * 3 + 1] + this.shiftY) * this.scale;
            this.pos[j * 3 + 2] = (this.pos[j * 3 + 2] + this.shiftZ) * this.scale;
        }
    }

    float[] bounds() {
        float[] floats =
                new float[] {
                    3.4028235E38F,
                    3.4028235E38F,
                    3.4028235E38F,
                    -3.4028235E38F,
                    -3.4028235E38F,
                    -3.4028235E38F
                };
        for (int i = 0; i < this.vertices; i++) {
            for (int j = 0; j < 3; j++) {
                floats[j] = Math.min(floats[j], this.pos[i * 3 + j]);
                floats[j + 3] = Math.max(floats[j + 3], this.pos[i * 3 + j]);
            }
        }
        return floats;
    }

    float[] world(int i) {
        float[] global = this.globals[i];
        return global == null ? new float[3] : Mat4.point(global, 0.0F, 0.0F, 0.0F);
    }

    float[] joint(int i) {
        float[] floats = this.world(i);
        float f = this.flipped ? -floats[0] : floats[0];
        float g = this.flipped ? -floats[2] : floats[2];
        return new float[] {
            (f + this.shiftX) * this.scale,
            (floats[1] + this.shiftY) * this.scale,
            (g + this.shiftZ) * this.scale
        };
    }

    boolean under(int i, int j) {
        int k = i;
        for (int m = 0; k >= 0 && m <= this.parent.length; m++) {
            if (k == j) {
                return true;
            }
            k = this.parent[k];
        }
        return false;
    }

    float[] below(int i) {
        float[] floats = new float[this.vertices];
        for (int j = 0; j < this.vertices; j++) {
            for (int k = 0; k < 4; k++) {
                int m = this.influence[j * 4 + k];
                if (m >= 0 && this.under(m, i)) {
                    floats[j] += this.weight[j * 4 + k];
                }
            }
        }
        return floats;
    }
}
