package net.efn.client.module.impl.render.character.gltf;

import com.google.gson.JsonElement;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;

@Environment(EnvType.CLIENT)
final class Skeleton {

    static final int RIGHT = 0;

    static final int LEFT = 1;

    private static final Set<String> NOISE =
            Set.of(
                    "j",
                    "bip",
                    "bip01",
                    "def",
                    "deform",
                    "mch",
                    "org",
                    "jnt",
                    "jt",
                    "bone",
                    "bn",
                    "b",
                    "c",
                    "x",
                    "stretch",
                    "valve",
                    "biped",
                    "mixamorig");

    private static final Set<String> LEFT_WORDS = Set.of("left", "l", "lf", "lft");

    private static final Set<String> RIGHT_WORDS = Set.of("right", "r", "rt", "rgt");

    private static final List<String> UPPER_ARM = List.of("upperarm", "uparm", "armupper");

    private static final List<String> FOREARM =
            List.of("forearm", "lowerarm", "elbow", "armlower", "loarm");

    private static final List<String> HAND = List.of("hand", "wrist");

    private static final List<String> THIGH = List.of("thigh", "upleg", "upperleg", "legupper");

    private static final List<String> SHIN =
            List.of("calf", "shin", "knee", "lowerleg", "leglower", "loleg");

    int head = -1;

    final int[] upperArm = new int[] {-1, -1};

    final int[] forearm = new int[] {-1, -1};

    final int[] hand = new int[] {-1, -1};

    final int[] thigh = new int[] {-1, -1};

    final int[] shin = new int[] {-1, -1};

    boolean facesBack;

    private final PosedMesh mesh;

    private final String[] core;

    private final int[] named;

    private Skeleton(PosedMesh mesh2) {
        this.mesh = mesh2;
        int nodes = mesh2.file.nodes();
        this.core = new String[nodes];
        this.named = new int[nodes];
        for (int j = 0; j < nodes; j++) {
            List list = words(mesh2.file.nodeName(j));
            this.named[j] = -1;
            StringBuilder sb = new StringBuilder();
            for (String string : (Iterable<String>) list) {
                if (LEFT_WORDS.contains(string)) {
                    this.named[j] = 1;
                } else if (RIGHT_WORDS.contains(string)) {
                    this.named[j] = 0;
                } else if (!NOISE.contains(string)) {
                    sb.append(string);
                }
            }
            this.core[j] = sb.toString().replaceAll("\\d+$", "");
        }
    }

    static Skeleton find(PosedMesh posedMesh, Map<String, String> map) throws IOException {
        Skeleton skeleton = new Skeleton(posedMesh);
        if (map.isEmpty()) {
            skeleton.facesBack = skeleton.facingBack();
            skeleton.head = skeleton.shallowest(i -> skeleton.core[i].startsWith("head"));
            for (int m = 0; m < 2; m++) {
                int n = m;
                int shallowest;
                int o =
                        shallowest =
                                skeleton.shallowest(
                                        j -> skeleton.side(j) == n && skeleton.is(j, FOREARM));
                int shallowest2 =
                        skeleton.shallowest(
                                k ->
                                        skeleton.side(k) == n
                                                && (skeleton.is(k, UPPER_ARM)
                                                        || skeleton.core[k].equals("arm"))
                                                && (shallowest < 0
                                                        || posedMesh.under(shallowest, k)
                                                                && k != shallowest));
                if (shallowest2 < 0 && o >= 0) {
                    shallowest2 = posedMesh.parent[o];
                }
                if (o < 0 && shallowest2 >= 0) {
                    o = skeleton.mainChild(shallowest2);
                }
                skeleton.upperArm[n] = shallowest2;
                skeleton.forearm[n] = o;
                if (o >= 0) {
                    final int p = o;
                    int shallowest3 =
                            skeleton.shallowest(
                                    j -> j != p && posedMesh.under(j, p) && skeleton.is(j, HAND));
                    skeleton.hand[n] = shallowest3 >= 0 ? shallowest3 : skeleton.mainChild(o);
                }
                int shallowest4;
                int q =
                        shallowest4 =
                                skeleton.shallowest(
                                        j -> skeleton.side(j) == n && skeleton.is(j, SHIN));
                int shallowest5 =
                        skeleton.shallowest(
                                k ->
                                        skeleton.side(k) == n
                                                && (skeleton.is(k, THIGH)
                                                        || skeleton.core[k].equals("leg"))
                                                && (shallowest4 < 0
                                                        || posedMesh.under(shallowest4, k)
                                                                && k != shallowest4));
                final int r = shallowest5;
                if (q < 0
                        && shallowest5 >= 0
                        && (q =
                                        skeleton.shallowest(
                                                j ->
                                                        j != r
                                                                && posedMesh.under(j, r)
                                                                && skeleton.core[j].equals("leg")))
                                < 0) {
                    q = skeleton.mainChild(shallowest5);
                }
                if (shallowest5 < 0 && q >= 0) {
                    shallowest5 = posedMesh.parent[q];
                }
                skeleton.thigh[n] = shallowest5;
                skeleton.shin[n] = q;
            }
            return skeleton;
        } else {
            skeleton.head = skeleton.named(map, "head");
            for (int w = 0; w < 2; w++) {
                String string = w == 0 ? "right " : "left ";
                skeleton.upperArm[w] = skeleton.named(map, string + "arm");
                skeleton.forearm[w] = skeleton.named(map, string + "elbow");
                skeleton.hand[w] = skeleton.named(map, string + "hand");
                skeleton.thigh[w] = skeleton.named(map, string + "leg");
                skeleton.shin[w] = skeleton.named(map, string + "knee");
            }
            skeleton.facesBack =
                    skeleton.upperArm[1] >= 0
                            && skeleton.upperArm[0] >= 0
                            && posedMesh.world(skeleton.upperArm[1])[0]
                                    < posedMesh.world(skeleton.upperArm[0])[0];
            return skeleton;
        }
    }

    private boolean is(int i, List<String> list) {
        for (String string : list) {
            if (this.core[i].contains(string)) {
                return true;
            }
        }
        return false;
    }

    private int named(Map<String, String> map, String string) throws IOException {
        String string2 = (String) map.get(string);
        if (string2 == null) {
            return -1;
        } else {
            for (int i = 0; i < this.core.length; i++) {
                if (this.mesh.file.nodeName(i).equalsIgnoreCase(string2)) {
                    return i;
                }
            }
            String string3 = string2.toLowerCase(Locale.ROOT);
            for (int j = 0; j < this.core.length; j++) {
                if (this.mesh.file.nodeName(j).toLowerCase(Locale.ROOT).startsWith(string3)) {
                    return j;
                }
            }
            throw new IOException("no bone named " + string2);
        }
    }

    private static List<String> words(String string) {
        String string2 =
                string.replaceAll("_\\d+$", "")
                        .replaceAll("([a-z])([A-Z])", "$1 $2")
                        .toLowerCase(Locale.ROOT)
                        .replace("\u624b\u9996", " wrist ")
                        .replace("\u8db3\u9996", " ankle ")
                        .replace("\u5de6", " left ")
                        .replace("\u53f3", " right ")
                        .replace("\u3072\u3058", " elbow ")
                        .replace("\u3072\u3056", " knee ")
                        .replace("\u8155", " arm ")
                        .replace("\u8db3", " leg ")
                        .replace("\u982d", " head ")
                        .replace("\u9996", " neck ");
        ArrayList list = new ArrayList();
        for (String string3 : string2.split("[^a-z0-9]+")) {
            if (!string3.isEmpty()) {
                list.add(string3);
            }
        }
        return list;
    }

    private boolean facingBack() {
        int shallowest = this.shallowest(i -> this.named[i] == 1 && this.is(i, HAND));
        int shallowest2 = this.shallowest(i -> this.named[i] == 0 && this.is(i, HAND));
        if (shallowest < 0 || shallowest2 < 0) {
            shallowest = this.shallowest(i -> this.named[i] == 1 && this.is(i, SHIN));
            shallowest2 = this.shallowest(i -> this.named[i] == 0 && this.is(i, SHIN));
        }
        if (shallowest >= 0 && shallowest2 >= 0) {
            return this.mesh.world(shallowest)[0] < this.mesh.world(shallowest2)[0];
        } else {
            float[] floats = this.mesh.bounds();
            float f = floats[4] - floats[1];
            float g = 0.0F;
            float h = 0.0F;
            int j = 0;
            int k = 0;
            for (int o = 0; o < this.mesh.vertices; o++) {
                float f2 = this.mesh.pos[o * 3 + 1] - floats[1];
                float g2 = this.mesh.pos[o * 3 + 2];
                if (f2 < f * 0.04F) {
                    g += g2;
                    j++;
                } else if (f2 > f * 0.1F && f2 < f * 0.2F) {
                    h += g2;
                    k++;
                }
            }
            return j > 0 && k > 0 && g / j < h / k - f * 0.01F;
        }
    }

    private int side(int i) {
        if (this.named[i] >= 0) {
            return this.named[i];
        } else {
            return this.mesh.world(i)[0] < 0.0F != this.facesBack ? 0 : 1;
        }
    }

    private int shallowest(IntPredicate intPredicate) {
        int i = -1;
        int j = 2147483647;
        for (int joint : this.mesh.joints) {
            if (intPredicate.test(joint)) {
                int k = this.mesh.parent[joint];
                int m;
                for (m = 0; k >= 0 && m <= this.core.length; m++) {
                    k = this.mesh.parent[k];
                }
                if (m < j || m == j && joint < i) {
                    i = joint;
                    j = m;
                }
            }
        }
        return i;
    }

    private int mainChild(int i) {
        int j = -1;
        int k = -1;
        for (JsonElement json : GltfFile.array(this.mesh.file.node(i), "children")) {
            int m = json.getAsInt();
            int n = 0;
            for (int o = 0; o < this.core.length; o++) {
                if (this.mesh.under(o, m)) {
                    n++;
                }
            }
            if (n > k) {
                j = m;
                k = n;
            }
        }
        return j;
    }

    Map<Integer, Float> armTurns() {
        HashMap map = new HashMap();
        for (int i = 0; i < 2; i++) {
            if (this.upperArm[i] >= 0 && this.forearm[i] >= 0) {
                float[] floats = this.mesh.world(this.upperArm[i]);
                float[] floats2 = this.mesh.world(this.forearm[i]);
                float f = floats2[0] - floats[0];
                float g = floats2[1] - floats[1];
                float h = (float) Math.toDegrees(Math.atan2(Math.abs(f), -g));
                if (h > 40.0F) {
                    map.put(this.upperArm[i], (f < 0.0F ? 1.0F : -1.0F) * (h - 10.0F));
                }
            }
        }
        return map;
    }

    int[] groups() {
        int[] ints = new int[this.core.length];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = 1;
            if (this.head >= 0 && this.mesh.under(i, this.head)) {
                ints[i] = 0;
            } else if (this.upperArm[0] >= 0 && this.mesh.under(i, this.upperArm[0])) {
                ints[i] = 2;
            } else if (this.upperArm[1] >= 0 && this.mesh.under(i, this.upperArm[1])) {
                ints[i] = 3;
            } else if (this.thigh[0] >= 0 && this.mesh.under(i, this.thigh[0])) {
                ints[i] = 4;
            } else if (this.thigh[1] >= 0 && this.mesh.under(i, this.thigh[1])) {
                ints[i] = 5;
            }
        }
        return ints;
    }

    boolean found() {
        return this.head >= 0
                || this.upperArm[0] >= 0
                || this.upperArm[1] >= 0
                || this.thigh[0] >= 0
                || this.thigh[1] >= 0;
    }

    String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append("facing ").append(this.facesBack ? "-z (turned around)" : "+z").append('\n');
        this.line(sb, "head", this.head);
        for (int i = 0; i < 2; i++) {
            String string = i == 0 ? "right " : "left ";
            this.line(sb, string + "arm", this.upperArm[i]);
            this.line(sb, string + "elbow", this.forearm[i]);
            this.line(sb, string + "hand", this.hand[i]);
            this.line(sb, string + "leg", this.thigh[i]);
            this.line(sb, string + "knee", this.shin[i]);
        }
        return sb.toString();
    }

    private void line(StringBuilder sb, String string, int i) {
        sb.append(
                String.format(
                        Locale.ROOT,
                        "%-11s %s%n",
                        string,
                        i < 0 ? "-" : this.mesh.file.nodeName(i)));
    }
}
