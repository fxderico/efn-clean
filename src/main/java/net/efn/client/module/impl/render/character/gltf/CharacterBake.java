/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.module.impl.render.character.gltf;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Environment(value = EnvType.CLIENT)
public final class CharacterBake {

    static final int HEAD = 0;

    static final int BODY = 1;

    static final int RIGHT_ARM = 2;

    static final int LEFT_ARM = 3;

    static final int RIGHT_LEG = 4;

    static final int LEFT_LEG = 5;

    private static final int[] BUDGETS = new int[] {7500, 15000, 30000};

    private static final int DENSE = 150000;

    private static final float[] STICK = new float[] {0.7f, 0.0f, 0.6f, 0.6f, 0.3f, 0.3f};

    private CharacterBake() {}

    public static Result bake(byte[] bytes, Options options, ImageDecoder imageDecoder)
            throws IOException {
        GltfFile gltfFile = GltfFile.read(bytes);
        PosedMesh posedMesh = PosedMesh.pose(gltfFile, Map.of());
        Skeleton skeleton = Skeleton.find(posedMesh, options.bones);
        Map<Integer, Float> map = skeleton.armTurns();
        if (!map.isEmpty()) {
            posedMesh = PosedMesh.pose(gltfFile, map);
            skeleton = Skeleton.find(posedMesh, options.bones);
        }
        posedMesh.normalise(options.height, skeleton.facesBack);
        posedMesh.cluster(150000);
        Cut cut =
                skeleton.found()
                        ? CharacterBake.skeletonCut(posedMesh, skeleton, options)
                        : ShapeCut.cut(posedMesh);
        Cut cut2 = cut;
        if (cut == null) {
            cut = CharacterBake.still(posedMesh);
        }
        List<Piece> list = CharacterBake.split(posedMesh, cut.part());
        CharacterBake.reduce(posedMesh, list);
        TextureAtlas textureAtlas =
                TextureAtlas.pack(gltfFile, CharacterBake.uvBounds(posedMesh, list), imageDecoder);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        CharacterBake.write(
                new DataOutputStream(byteArrayOutputStream),
                posedMesh,
                cut,
                list,
                textureAtlas,
                options);
        ArrayList<String> list2 = new ArrayList<String>();
        if (CharacterBake.has(list, 0)) {
            list2.add("head");
        }
        if (CharacterBake.has(list, 2) || CharacterBake.has(list, 3)) {
            list2.add("arms");
        }
        if (CharacterBake.has(list, 4) || CharacterBake.has(list, 5)) {
            list2.add("legs");
        }
        return new Result(byteArrayOutputStream.toByteArray(), list2);
    }

    public static String describe(byte[] bytes, Options options) throws IOException {
        GltfFile gltfFile = GltfFile.read(bytes);
        PosedMesh posedMesh = PosedMesh.pose(gltfFile, Map.of());
        Skeleton skeleton = Skeleton.find(posedMesh, options.bones);
        StringBuilder sb =
                new StringBuilder(
                        String.format(
                                Locale.ROOT,
                                "%d vertices, %d triangles, %d bones%n",
                                posedMesh.vertices,
                                posedMesh.triangles,
                                posedMesh.joints.size()));
        sb.append(
                skeleton.found()
                        ? skeleton.describe()
                        : "no usable skeleton, limbs are cut by shape\n");
        HashSet<Integer> set = new HashSet<Integer>();
        for (int i = 0; i < posedMesh.vertices; ++i) {
            set.add(posedMesh.material[i]);
        }
        Iterator iterator = set.iterator();
        while (iterator.hasNext()) {
            int j = (Integer) iterator.next();
            sb.append("material ").append(gltfFile.materialName(j)).append('\n');
        }
        return sb.toString();
    }

    private static Cut still(PosedMesh posedMesh) {
        float[][] floatGrid = new float[6][];
        floatGrid[1] = new float[] {0.0f, 24.0f, 0.0f};
        return new Cut(
                new int[posedMesh.vertices],
                floatGrid,
                new float[6][],
                new float[6][],
                new float[6][]);
    }

    private static CharacterBake.Cut skeletonCut(
            PosedMesh posedMesh, Skeleton skeleton, CharacterBake.Options options) {
        int[] ints = skeleton.groups();
        boolean bl = skeleton.thigh[0] >= 0 && skeleton.thigh[1] >= 0;
        float floor2 = options.floor;
        if (floor2 < 0.0F && skeleton.shin[0] >= 0 && skeleton.shin[1] >= 0) {
            floor2 =
                    Math.min(
                            posedMesh.joint(skeleton.shin[0])[1],
                            posedMesh.joint(skeleton.shin[1])[1]);
        }
        boolean bl2 = !options.headMaterials.isEmpty() && skeleton.head >= 0;
        int[] ints2 = new int[posedMesh.vertices];
        float[] floats = new float[6];
        for (int i = 0; i < posedMesh.vertices; i++) {
            float f = posedMesh.pos[i * 3];
            float g = posedMesh.pos[i * 3 + 1];
            if (bl2
                    && options.headMaterials.contains(
                            posedMesh.file.materialName(posedMesh.material[i]))) {
                ints2[i] = g < options.neck ? 1 : 0;
            } else {
                Arrays.fill(floats, 0.0F);
                for (int j = 0; j < 4; j++) {
                    int k = posedMesh.influence[i * 4 + j];
                    if (k >= 0) {
                        int m = ints[k];
                        floats[m] += posedMesh.weight[i * 4 + j];
                    }
                }
                int n = 1;
                for (int o = 0; o < 6; o++) {
                    if (o != 1
                            && floats[o] >= STICK[o]
                            && floats[o] > floats[n]
                            && (o != 0 || !bl2)) {
                        n = o;
                    }
                }
                boolean bl3 = n == 1 || n == 4 || n == 5;
                if (bl && bl3 && options.hip > 0.0F) {
                    n = g >= options.hip ? 1 : (f < 0.0F ? 4 : 5);
                } else if (bl && n == 1 && g < floor2) {
                    n = f < 0.0F ? 4 : 5;
                }
                ints2[i] = n;
            }
        }
        float[][] floatGrid = new float[6][];
        float[][] floatGrid2 = new float[6][];
        float[][] floatGrid3 = new float[6][];
        float[][] floatGrid4 = new float[6][];
        floatGrid[1] = new float[] {0.0F, 24.0F, 0.0F};
        if (skeleton.head >= 0) {
            floatGrid[0] = posedMesh.joint(skeleton.head);
        }
        for (int p = 0; p < 2; p++) {
            int q = p == 0 ? 2 : 3;
            int r = p == 0 ? 4 : 5;
            if (skeleton.upperArm[p] >= 0) {
                floatGrid[q] = posedMesh.joint(skeleton.upperArm[p]);
                int t = skeleton.hand[p] >= 0 ? skeleton.hand[p] : skeleton.forearm[p];
                float[] floats2 = floatGrid2[q] = t >= 0 ? posedMesh.joint(t) : floatGrid[q];
                if (skeleton.forearm[p] >= 0) {
                    floatGrid3[q] = posedMesh.joint(skeleton.forearm[p]);
                    floatGrid4[q] = bend(posedMesh, skeleton.forearm[p], skeleton.upperArm[p]);
                }
            }
            if (skeleton.thigh[p] >= 0) {
                floatGrid[r] = posedMesh.joint(skeleton.thigh[p]);
                if (skeleton.shin[p] >= 0) {
                    floatGrid3[r] = posedMesh.joint(skeleton.shin[p]);
                    floatGrid4[r] = bend(posedMesh, skeleton.shin[p], skeleton.thigh[p]);
                }
            }
        }
        return new CharacterBake.Cut(ints2, floatGrid, floatGrid2, floatGrid3, floatGrid4);
    }

    static int owner(int[] ints, int i, int j, int k) {
        return ints[j] == ints[k] ? ints[j] : ints[i];
    }

    private static List<Piece> split(PosedMesh posedMesh, int[] ints) {
        int tri;
        int tri2;
        Piece[][] pieceGrid = new Piece[6][2];
        for (int i = 0; i < posedMesh.triangles; ++i) {
            int j;
            tri2 = posedMesh.tris[i * 3];
            tri = posedMesh.tris[i * 3 + 1];
            int tri3 = posedMesh.tris[i * 3 + 2];
            if (Mat4.area(posedMesh.pos, tri2, tri, tri3) < 1.0E-9f) continue;
            int owner = CharacterBake.owner(ints, tri2, tri, tri3);
            Piece piece =
                    pieceGrid[owner][
                            j = posedMesh.file.translucent(posedMesh.material[tri2]) ? 1 : 0];
            if (piece == null) {
                Piece piece2;
                pieceGrid[owner][j] = piece2 = new Piece(owner, j);
                piece = piece2;
            }
            piece.source.add(i);
            if (j != 1 || !posedMesh.file.doubleSided(posedMesh.material[tri2])) continue;
            piece.source.add(~i);
        }
        ArrayList<Piece> list = new ArrayList<Piece>();
        Piece[][] pieceGrid2 = pieceGrid;
        tri2 = pieceGrid2.length;
        for (tri = 0; tri < tri2; ++tri) {
            Piece[] pieces;
            for (Piece piece3 : pieces = pieceGrid2[tri]) {
                if (piece3 == null) continue;
                list.add(piece3);
            }
        }
        return list;
    }

    private static void reduce(PosedMesh posedMesh, List<Piece> list) {
        double d = 0.0;
        for (Piece piece : list) {
            d += (double) piece.source.size() * (piece.bone == 0 ? 2.0 : 1.0);
        }
        for (Piece piece2 : list) {
            int i;
            int[] ints = new int[BUDGETS.length];
            for (i = 0; i < BUDGETS.length; ++i) {
                double e = (double) piece2.source.size() * (piece2.bone == 0 ? 2.0 : 1.0) / d;
                ints[i] =
                        (int)
                                Math.min(
                                        (long) piece2.source.size(),
                                        Math.max(16L, Math.round((double) BUDGETS[i] * e)));
            }
            i = ints[BUDGETS.length - 1] < piece2.source.size() ? 1 : 0;
            piece2.levels = new MeshReducer(posedMesh, piece2.source, i != 0).reduce(ints);
        }
    }

    private static Map<Integer, float[]> uvBounds(
            PosedMesh posedMesh, List<CharacterBake.Piece> list) {
        LinkedHashMap map = new LinkedHashMap();
        for (CharacterBake.Piece piece : list) {
            for (int i : piece.source) {
                int j = i < 0 ? ~i : i;
                for (int k = 0; k < 3; k++) {
                    int tri = posedMesh.tris[j * 3 + k];
                    float[] floats =
                            (float[])
                                    map.computeIfAbsent(
                                            posedMesh.material[tri],
                                            integer ->
                                                    new float[] {1.0E9F, 1.0E9F, -1.0E9F, -1.0E9F});
                    floats[0] = Math.min(floats[0], posedMesh.uv[tri * 2]);
                    floats[1] = Math.min(floats[1], posedMesh.uv[tri * 2 + 1]);
                    floats[2] = Math.max(floats[2], posedMesh.uv[tri * 2]);
                    floats[3] = Math.max(floats[3], posedMesh.uv[tri * 2 + 1]);
                }
            }
        }
        return map;
    }

    private static boolean has(List<Piece> list, int i) {
        for (Piece piece : list) {
            if (piece.bone != i) continue;
            return true;
        }
        return false;
    }

    private static Piece find(List<Piece> list, int i, int j) {
        for (Piece piece : list) {
            if (piece.bone != i || piece.layer != j) continue;
            return piece;
        }
        return null;
    }

    private static void write(
            DataOutputStream dataOutputStream,
            PosedMesh posedMesh,
            Cut cut,
            List<Piece> list,
            TextureAtlas textureAtlas,
            Options options)
            throws IOException {
        int k;
        float[][] floatGrid = cut.pivot();
        float[][] floatGrid2 = cut.grip();
        float[][] floatGrid3 = cut.bendAt();
        float[][] floatGrid4 = cut.bendWeights();
        float[] floats = posedMesh.bounds();
        float f =
                floatGrid[4] != null && floatGrid[5] != null
                        ? (floatGrid[4][1] + floatGrid[5][1]) * 0.5f
                        : 12.0f;
        dataOutputStream.writeInt(1095648072);
        dataOutputStream.writeInt(3);
        dataOutputStream.writeFloat(floats[1]);
        dataOutputStream.writeFloat(floats[4]);
        dataOutputStream.writeFloat(f);
        dataOutputStream.writeFloat(options.flat);
        dataOutputStream.writeBoolean(true);
        dataOutputStream.writeBoolean(posedMesh.colored);
        dataOutputStream.writeInt(BUDGETS.length);
        int m = 0;
        for (k = 0; k < 6; ++k) {
            if (!CharacterBake.has(list, k)) continue;
            ++m;
        }
        dataOutputStream.writeInt(m);
        for (k = 0; k < 6; ++k) {
            if (!CharacterBake.has(list, k)) continue;
            dataOutputStream.writeByte(k);
            for (float g : floatGrid[k]) {
                dataOutputStream.writeFloat(g);
            }
            for (float h : floatGrid2[k] != null ? floatGrid2[k] : floatGrid[k]) {
                dataOutputStream.writeFloat(h);
            }
            float[] floats2 = CharacterBake.bounds(posedMesh, list, k);
            for (float f2 : floats2) {
                dataOutputStream.writeFloat(f2);
            }
            boolean bl = k == 2 || k == 3;
            int n = k == 4 || k == 5 ? 1 : 0;
            int o = n;
            dataOutputStream.writeFloat(bl ? 0.6f : (n != 0 ? 0.55f : 1.0f));
            dataOutputStream.writeFloat(bl ? 40.0f : (n != 0 ? 30.0f : 0.0f));
            dataOutputStream.writeBoolean(floatGrid3[k] != null);
            if (floatGrid3[k] != null) {
                for (float g2 : floatGrid3[k]) {
                    dataOutputStream.writeFloat(g2);
                }
            }
            for (int i = 0; i < BUDGETS.length; ++i) {
                for (int j = 0; j < 2; ++j) {
                    CharacterBake.writeLevel(
                            dataOutputStream,
                            posedMesh,
                            textureAtlas,
                            CharacterBake.find(list, k, j),
                            i,
                            floats2,
                            floatGrid4[k],
                            floatGrid3[k] != null);
                }
            }
        }
        byte[] bytes = textureAtlas.packed();
        dataOutputStream.writeInt(textureAtlas.width);
        dataOutputStream.writeInt(textureAtlas.height);
        dataOutputStream.writeInt(bytes.length);
        dataOutputStream.write(bytes);
        dataOutputStream.flush();
    }

    private static float[] bend(PosedMesh posedMesh, int i, int j) {
        float[] floats = posedMesh.joint(i);
        float[] floats2 = posedMesh.below(i);
        float[] floats3 = posedMesh.below(j);
        for (int k = 0; k < posedMesh.vertices; ++k) {
            if (floats3[k] >= 0.5f) continue;
            float f = Math.max(0.0f, Math.min(1.0f, floats[1] - posedMesh.pos[k * 3 + 1] + 0.5f));
            floats2[k] = f * f * (3.0f - 2.0f * f);
        }
        return floats2;
    }

    private static float[] bounds(PosedMesh posedMesh, List<Piece> list, int i) {
        float[] floats = new float[] {1.0E9f, 1.0E9f, 1.0E9f, -1.0E9f, -1.0E9f, -1.0E9f};
        for (Piece piece : list) {
            if (piece.bone != i) continue;
            for (int m : piece.source) {
                int n = m < 0 ? ~m : m;
                for (int j = 0; j < 3; ++j) {
                    int tri = posedMesh.tris[n * 3 + j];
                    for (int k = 0; k < 3; ++k) {
                        floats[k] = Math.min(floats[k], posedMesh.pos[tri * 3 + k]);
                        floats[k + 3] = Math.max(floats[k + 3], posedMesh.pos[tri * 3 + k]);
                    }
                }
            }
        }
        for (int j = 0; j < 3; ++j) {
            if (!(floats[j + 3] - floats[j] < 0.001f)) continue;
            floats[j + 3] = floats[j] + 0.001f;
        }
        return floats;
    }

    private static void writeLevel(
            DataOutputStream dataOutputStream,
            PosedMesh posedMesh,
            TextureAtlas textureAtlas,
            Piece piece,
            int i,
            float[] floats,
            float[] floats2,
            boolean bl)
            throws IOException {
        int k;
        if (piece == null) {
            dataOutputStream.writeInt(0);
            dataOutputStream.writeInt(0);
            return;
        }
        int[] level = piece.levels[i];
        HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
        ArrayList<Integer> list = new ArrayList<Integer>();
        int[] ints = new int[level.length];
        for (int j = 0; j < level.length; ++j) {
            Integer integer = (Integer) map.get(level[j]);
            if (integer == null) {
                integer = list.size();
                map.put(level[j], integer);
                list.add(level[j]);
            }
            ints[j] = integer;
        }
        dataOutputStream.writeInt(list.size());
        Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
            int m = (Integer) iterator.next();
            k = m < 0 ? ~m : m;
            for (int j = 0; j < 3; ++j) {
                float f = (posedMesh.pos[k * 3 + j] - floats[j]) / (floats[j + 3] - floats[j]);
                dataOutputStream.writeChar(
                        Math.round(Math.max(0.0f, Math.min(1.0f, f)) * 65535.0f));
            }
        }
        iterator = list.iterator();
        while (iterator.hasNext()) {
            int n = (Integer) iterator.next();
            k = n < 0 ? ~n : n;
            float g = n < 0 ? -1.0f : 1.0f;
            for (int j = 0; j < 3; ++j) {
                dataOutputStream.writeByte(Math.round(posedMesh.nrm[k * 3 + j] * g * 127.0f));
            }
        }
        iterator = list.iterator();
        while (iterator.hasNext()) {
            int o = (Integer) iterator.next();
            k = o < 0 ? ~o : o;
            float[] floats3 =
                    textureAtlas.map(
                            posedMesh.material[k], posedMesh.uv[k * 2], posedMesh.uv[k * 2 + 1]);
            dataOutputStream.writeChar(
                    Math.round(Math.max(0.0f, Math.min(1.0f, floats3[0])) * 65535.0f));
            dataOutputStream.writeChar(
                    Math.round(Math.max(0.0f, Math.min(1.0f, floats3[1])) * 65535.0f));
        }
        if (posedMesh.colored) {
            iterator = list.iterator();
            while (iterator.hasNext()) {
                int p = (Integer) iterator.next();
                int q = posedMesh.color[p < 0 ? ~p : p];
                dataOutputStream.writeByte(q >> 16);
                dataOutputStream.writeByte(q >> 8);
                dataOutputStream.writeByte(q);
            }
        }
        if (bl) {
            iterator = list.iterator();
            while (iterator.hasNext()) {
                int r = (Integer) iterator.next();
                dataOutputStream.writeByte(
                        Math.round(
                                Math.max(0.0f, Math.min(1.0f, floats2[r < 0 ? ~r : r])) * 255.0f));
            }
        }
        dataOutputStream.writeInt(ints.length / 3);
        boolean bl2 = list.size() > 65535;
        for (int t : ints) {
            if (bl2) {
                dataOutputStream.writeInt(t);
                continue;
            }
            dataOutputStream.writeChar(t);
        }
    }

    @Environment(value = EnvType.CLIENT)
    public static final class Options {

        public float height = 30.0f;

        public float flat;

        public final Map<String, String> bones = new HashMap<String, String>();

        public final Set<String> headMaterials = new HashSet<String>();

        public float neck = -1.0f;

        public float hip = -1.0f;

        public float floor = -1.0f;
    }

    @Environment(value = EnvType.CLIENT)
    record Cut(
            int[] part, float[][] pivot, float[][] grip, float[][] bendAt, float[][] bendWeights) {}

    @Environment(value = EnvType.CLIENT)
    public record Result(byte[] data, List<String> limbs) {}

    @Environment(value = EnvType.CLIENT)
    private static final class Piece {

        final int bone;

        final int layer;

        final List<Integer> source = new ArrayList<Integer>();

        int[][] levels;

        Piece(int bone2, int layer2) {
            this.bone = bone2;
            this.layer = layer2;
        }
    }
}
