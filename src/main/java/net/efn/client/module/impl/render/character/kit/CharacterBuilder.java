package net.efn.client.module.impl.render.character.kit;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.stream.IntStream;

@Environment(EnvType.CLIENT)
public final class CharacterBuilder {

    private static final float DECAL_RESERVE = 0.1F;

    private static final float AO_STEP = 0.38F;

    private static final int MIN_TRIANGLES = 24;

    private static final float MAX_EXTENT = 160.0F;

    private static final float OUTLINE_SHARE = 0.5F;

    private static final float BURIED = 0.02F;

    private static final float SINK = 0.12F;

    private static final int CORNER = 10;

    private CharacterBuilder() {}

    public static CharacterModel build(CharacterDesign characterDesign, Detail detail) {
        if (characterDesign instanceof BakedDesign bakedDesign) {
            return BakedModel.load(bakedDesign, detail);
        } else {
            long l = System.nanoTime();
            Rig rig = characterDesign.rig();
            List list = order(rig);
            int j = list.size();
            if (j == 0) {
                throw new IllegalStateException(
                        characterDesign.id() + ": the design defines no parts");
            } else {
                IdentityHashMap map = new IdentityHashMap();
                for (int k = 0; k < j; k++) {
                    map.put((Part) list.get(k), k);
                }
                ArrayList list2 = new ArrayList();
                ArrayList list3 = new ArrayList();
                Shape[] shapes = new Shape[j];
                float[] floats = new float[j];
                for (int m = 0; m < j; m++) {
                    Part part = (Part) list.get(m);
                    if (part.shape != null) {
                        Shape shape2 = part.shape;
                        if (!shape2.bounded()) {
                            throw new IllegalStateException(
                                    part.name
                                            + ": shape is unbounded; intersect planes with a box");
                        }
                        if (shape2.maxX - shape2.minX > 160.0F
                                || shape2.maxY - shape2.minY > 160.0F
                                || shape2.maxZ - shape2.minZ > 160.0F) {
                            throw new IllegalStateException(
                                    part.name + ": shape is larger than 160.0 units");
                        }
                        floats[m] =
                                SurfaceNets.fitStep(
                                        shape2.maxX - shape2.minX,
                                        shape2.maxY - shape2.minY,
                                        shape2.maxZ - shape2.minZ,
                                        detail.step * part.resolution);
                        float f = floats[m] * 0.75F;
                        shape2 =
                                shape2.map(
                                        shape -> {
                                            Object object;
                                            if (shape instanceof Strand strand) {
                                                object = strand.thickened(f);
                                            } else {
                                                object = shape;
                                            }
                                            return (Shape) object;
                                        });
                        shapes[m] = shape2;
                        list2.add(shape2);
                    }
                    for (Decal decal : part.decals) {
                        list3.add(decal.canvas);
                    }
                }
                Shape shape3 =
                        list2.isEmpty()
                                ? null
                                : Shapes.union(((Shape[]) list2.toArray(new Shape[0])));
                Shape[] shapes2 = covers(list, shapes);
                Atlas atlas = Atlas.pack(list3);
                float[] floats2 = new float[j];
                for (int n = 0; n < j; n++) {
                    Part part2 = (Part) list.get(n);
                    floats2[n] = part2.outline >= 0.0F ? part2.outline : rig.outline;
                }
                Mesh[] meshs = new Mesh[j];
                float[] floats3 = new float[j];
                IntStream.range(0, j)
                        .parallel()
                        .forEach(
                                i -> {
                                    Shape shape4 = shapes[i];
                                    if (shape4 != null) {
                                        meshs[i] =
                                                SurfaceNets.mesh(
                                                        shape4,
                                                        shape4.minX,
                                                        shape4.minY,
                                                        shape4.minZ,
                                                        shape4.maxX,
                                                        shape4.maxY,
                                                        shape4.maxZ,
                                                        floats[i]);
                                        floats3[i] = visible(meshs[i], shapes2[i]);
                                    }
                                });
                int[] ints = new int[j];
                budget(list, meshs, floats3, detail, ints, floats2);
                Mesh[] meshs2 = new Mesh[j];
                IntStream.range(0, j)
                        .parallel()
                        .forEach(
                                i -> {
                                    if (meshs[i] != null && meshs[i].triCount != 0) {
                                        Simplifier simplifier = new Simplifier(meshs[i]);
                                        simplifier.reduce(ints[i]);
                                        meshs2[i] = simplifier.snapshot();
                                    }
                                });
                PartMesh[] partMeshs = new PartMesh[j];
                float[] floats4 = new float[] {3.4028235E38F, -3.4028235E38F};
                int[] ints2 = new int[j];
                int o = 0;
                for (int p = 0; p < j; p++) {
                    Part part3 = (Part) list.get(p);
                    ints2[p] = part3.extra && part3.motion != null ? o++ : -1;
                }
                IntStream.range(0, j)
                        .parallel()
                        .forEach(
                                i -> {
                                    Part part4 = (Part) list.get(i);
                                    Surface surface = null;
                                    Surface surface2 = null;
                                    Surface surface3 = null;
                                    if (meshs2[i] != null && meshs2[i].triCount > 0) {
                                        float g =
                                                part4.occlusion >= 0.0F
                                                        ? part4.occlusion
                                                        : rig.occlusion;
                                        CharacterBuilder.Shaded shaded =
                                                shade(
                                                        shapes[i],
                                                        shape3,
                                                        meshs2[i],
                                                        floats[i],
                                                        g,
                                                        part4.flat >= 0.0F ? part4.flat : rig.flat);
                                        Mesh mesh;
                                        surface =
                                                baseSurface(
                                                        shaded,
                                                        mesh =
                                                                bury(
                                                                        shaded,
                                                                        meshs2[i],
                                                                        shapes2[i],
                                                                        floats[i]),
                                                        part4,
                                                        atlas);
                                        if (surface != null && floats2[i] > 0.0F) {
                                            int q =
                                                    part4.outlineColorSet
                                                            ? part4.outlineColor
                                                            : rig.outlineColor;
                                            surface2 =
                                                    hullSurface(
                                                            shapes[i],
                                                            shaded,
                                                            surface,
                                                            part4,
                                                            floats2[i],
                                                            floats[i],
                                                            q,
                                                            atlas);
                                        }
                                        if (!part4.decals.isEmpty()) {
                                            surface3 = decalSurface(part4, shaded, mesh, atlas);
                                        }
                                        for (int r = 0; r < shaded.count; r++) {
                                            floats4[0] =
                                                    Math.min(floats4[0], shaded.pos[r * 3 + 1]);
                                            floats4[1] =
                                                    Math.max(floats4[1], shaded.pos[r * 3 + 1]);
                                        }
                                    }
                                    int t =
                                            part4.parent == null
                                                    ? -1
                                                    : (Integer) map.get(part4.parent);
                                    Motion motion = part4.extra ? part4.motion : null;
                                    if (motion != null && motion.bendPower() > 0.0F) {
                                        float reach = reach(surface);
                                        weigh(surface, reach, motion.bendPower());
                                        if (surface2 != null && surface != null) {
                                            surface2.weight = surface.weight;
                                        }
                                        weigh(surface3, reach, motion.bendPower());
                                    }
                                    boolean bl =
                                            !part4.extra && (part4.bone.arm() || part4.bone.leg());
                                    Part part5 = part4;
                                    while (part5 != null && !part5.jointSet) {
                                        part5 = part5.parent;
                                    }
                                    boolean bl2 = part5 != null;
                                    if (bl2) {
                                        jointWeights(surface, part4, part5);
                                        jointWeights(surface3, part4, part5);
                                        if (surface2 != null && surface != null) {
                                            surface2.joint = surface.joint;
                                        }
                                    }
                                    partMeshs[i] =
                                            new PartMesh(
                                                    part4.name,
                                                    part4.bone,
                                                    t,
                                                    part4.extra,
                                                    part4.pivotX,
                                                    part4.pivotY,
                                                    part4.pivotZ,
                                                    part4.gripX(),
                                                    part4.gripY(),
                                                    part4.gripZ(),
                                                    motion,
                                                    surface,
                                                    surface2,
                                                    surface3,
                                                    ints2[i],
                                                    bl ? part4.swing : 1.0F,
                                                    bl ? part4.swingLimit : 0.0F,
                                                    bl2,
                                                    bl2 ? part5.jointX - part4.pivotX : 0.0F,
                                                    bl2 ? part5.jointY - part4.pivotY : 0.0F,
                                                    bl2 ? part5.jointZ - part4.pivotZ : 0.0F,
                                                    bl2 && part5.neck,
                                                    bl2 ? part5.lookPitch : 0.0F,
                                                    bl2 ? part5.lookYaw : 0.0F,
                                                    bl2 ? part5.lookLimit : 0.0F);
                                });
                long l2 = System.nanoTime();
                float h = 12.0F;
                Part bone = rig.bones[Bone.RIGHT_LEG.ordinal()];
                Part bone2 = rig.bones[Bone.LEFT_LEG.ordinal()];
                if (bone != null && bone2 != null) {
                    h = (bone.pivotY + bone2.pivotY) * 0.5F;
                } else if (bone != null) {
                    h = bone.pivotY;
                } else if (bone2 != null) {
                    h = bone2.pivotY;
                }
                float f2 = floats4[0] == 3.4028235E38F ? 0.0F : floats4[0];
                float g2 = floats4[1] == -3.4028235E38F ? 32.0F : floats4[1];
                long l3 = (l2 - l) / 1000000L;
                return new CharacterModel(
                        characterDesign.id(),
                        characterDesign.name(),
                        detail,
                        rig.scale,
                        partMeshs,
                        o,
                        atlas.width,
                        atlas.height,
                        atlas.mips,
                        atlas.whiteU(),
                        atlas.whiteV(),
                        f2,
                        g2,
                        h,
                        rig.firstPersonArms,
                        l3);
            }
        }
    }

    private static List<Part> order(Rig rig) {
        ArrayList list = new ArrayList();
        for (Part bone : rig.bones) {
            if (bone != null) {
                list.add(bone);
            }
        }
        list.addAll(rig.extras);
        return list;
    }

    private static Shape[] covers(List<Part> list, Shape[] shapes) {
        int i = list.size();
        Part[] parts = new Part[i];
        for (int j = 0; j < i; j++) {
            Part part = (Part) list.get(j);
            while (part.extra && part.motion == null && part.parent != null) {
                part = part.parent;
            }
            parts[j] = part;
        }
        Shape[] shapes2 = new Shape[i];
        for (int k = 0; k < i; k++) {
            if (shapes[k] != null) {
                ArrayList list2 = new ArrayList();
                for (int m = 0; m < i; m++) {
                    if (m != k
                            && shapes[m] != null
                            && parts[m] == parts[k]
                            && overlaps(shapes[k], shapes[m])) {
                        list2.add(shapes[m]);
                    }
                }
                if (!list2.isEmpty()) {
                    shapes2[k] = Shapes.union(((Shape[]) list2.toArray(new Shape[0])));
                }
            }
        }
        return shapes2;
    }

    private static boolean overlaps(Shape shape, Shape shape2) {
        return shape.minX <= shape2.maxX
                && shape2.minX <= shape.maxX
                && shape.minY <= shape2.maxY
                && shape2.minY <= shape.maxY
                && shape.minZ <= shape2.maxZ
                && shape2.minZ <= shape.maxZ;
    }

    private static float visible(Mesh mesh, Shape shape) {
        if (shape != null && mesh.triCount != 0) {
            boolean[] booleans = new boolean[mesh.vertexCount];
            for (int i = 0; i < mesh.vertexCount; i++) {
                booleans[i] =
                        shape.distance(mesh.pos[i * 3], mesh.pos[i * 3 + 1], mesh.pos[i * 3 + 2])
                                < -0.02F;
            }
            int j = 0;
            for (int k = 0; k < mesh.triCount; k++) {
                if (booleans[mesh.tris[k * 3]]
                        && booleans[mesh.tris[k * 3 + 1]]
                        && booleans[mesh.tris[k * 3 + 2]]) {
                    j++;
                }
            }
            return Math.max(0.05F, 1.0F - (float) j / mesh.triCount);
        } else {
            return 1.0F;
        }
    }

    private static void budget(
            List<Part> list,
            Mesh[] meshs,
            float[] floats,
            Detail detail,
            int[] ints,
            float[] floats2) {
        int i = list.size();
        float f = 0.0F;
        float g = 0.0F;
        float[] floats3 = new float[i];
        for (int j = 0; j < i; j++) {
            Part part = (Part) list.get(j);
            if (meshs[j] != null) {
                floats3[j] = meshs[j].triCount * floats[j] * part.detail;
                f += floats3[j];
                if (floats2[j] > 0.0F) {
                    g += floats3[j];
                }
            }
        }
        if (!(f <= 0.0F)) {
            float h = detail.budget * 0.9F;
            float f2 = h / (1.0F + g / f);
            for (int k = 0; k < i; k++) {
                if (meshs[k] != null) {
                    int m = Math.max(24, Math.round(f2 * floats3[k] / f / floats[k]));
                    ints[k] = Math.min(m, meshs[k].triCount);
                }
            }
        }
    }

    private static CharacterBuilder.Shaded shade(
            Shape shape, Shape shape2, Mesh mesh, float f, float g, float h) {
        CharacterBuilder.Shaded shaded = new CharacterBuilder.Shaded(mesh.vertexCount);
        int j = (mesh.vertexCount + 255) / 256;
        IntStream.range(0, j)
                .parallel()
                .forEach(
                        i -> {
                            float[] floats = new float[3];
                            float[] floats2 = new float[3];
                            Sample sample = new Sample();
                            int k = Math.min(mesh.vertexCount, (i + 1) * 256);
                            for (int m = i * 256; m < k; m++) {
                                floats[0] = mesh.pos[m * 3];
                                floats[1] = mesh.pos[m * 3 + 1];
                                floats[2] = mesh.pos[m * 3 + 2];
                                project(shape, floats, floats2, f);
                                System.arraycopy(floats, 0, shaded.pos, m * 3, 3);
                                System.arraycopy(floats2, 0, shaded.nrm, m * 3, 3);
                                shape.sample(floats[0], floats[1], floats[2], sample);
                                float occlusion =
                                        occlusion(
                                                shape2,
                                                floats[0],
                                                floats[1],
                                                floats[2],
                                                floats2[0],
                                                floats2[1],
                                                floats2[2],
                                                sample.occlusion(g));
                                occlusion += (1.0F - occlusion) * Math.min(1.0F, sample.glow);
                                shaded.ao[m] = occlusion;
                                shaded.flat[m] = sample.flat(h);
                                shaded.glow[m] = (byte) (sample.glow >= 0.5F ? 1 : 0);
                                shaded.color[m] =
                                        0xFF000000
                                                | Sample.channel(sample.r * occlusion) << 16
                                                | Sample.channel(sample.g * occlusion) << 8
                                                | Sample.channel(sample.b * occlusion);
                            }
                        });
        return shaded;
    }

    private static Mesh bury(CharacterBuilder.Shaded shaded, Mesh mesh, Shape shape, float f) {
        if (shape == null) {
            return mesh;
        } else {
            float[] floats = new float[shaded.count];
            int j = (shaded.count + 255) / 256;
            IntStream.range(0, j)
                    .parallel()
                    .forEach(
                            i -> {
                                float[] floats2 = new float[3];
                                float[] floats3 = new float[3];
                                int k = Math.min(shaded.count, (i + 1) * 256);
                                for (int m = i * 256; m < k; m++) {
                                    floats2[0] = shaded.pos[m * 3];
                                    floats2[1] = shaded.pos[m * 3 + 1];
                                    floats2[2] = shaded.pos[m * 3 + 2];
                                    float distance;
                                    floats[m] =
                                            distance =
                                                    shape.distance(
                                                            floats2[0], floats2[1], floats2[2]);
                                    if (distance < 0.0F) {
                                        gradient(
                                                shape, floats2, Math.max(0.01F, f * 0.3F), floats3);
                                        float g = Math.min(0.12F, -2.0F * distance);
                                        shaded.pos[m * 3] = floats2[0] - floats3[0] * g;
                                        shaded.pos[m * 3 + 1] = floats2[1] - floats3[1] * g;
                                        shaded.pos[m * 3 + 2] = floats2[2] - floats3[2] * g;
                                    }
                                }
                            });
            shaded.cover = floats;
            int[] ints = new int[mesh.triCount * 3];
            int n = 0;
            for (int o = 0; o < mesh.triCount; o++) {
                int tri = mesh.tris[o * 3];
                int tri2 = mesh.tris[o * 3 + 1];
                int tri3 = mesh.tris[o * 3 + 2];
                if (!(floats[tri] < -0.02F)
                        || !(floats[tri2] < -0.02F)
                        || !(floats[tri3] < -0.02F)
                        || !(shape.distance(
                                        (mesh.pos[tri * 3]
                                                        + mesh.pos[tri2 * 3]
                                                        + mesh.pos[tri3 * 3])
                                                / 3.0F,
                                        (mesh.pos[tri * 3 + 1]
                                                        + mesh.pos[tri2 * 3 + 1]
                                                        + mesh.pos[tri3 * 3 + 1])
                                                / 3.0F,
                                        (mesh.pos[tri * 3 + 2]
                                                        + mesh.pos[tri2 * 3 + 2]
                                                        + mesh.pos[tri3 * 3 + 2])
                                                / 3.0F)
                                < -0.02F)) {
                    ints[n * 3] = tri;
                    ints[n * 3 + 1] = tri2;
                    ints[n * 3 + 2] = tri3;
                    n++;
                }
            }
            return n == mesh.triCount
                    ? mesh
                    : new Mesh(mesh.pos, mesh.vertexCount, Arrays.copyOf(ints, n * 3), n);
        }
    }

    static void project(Shape shape, float[] floats, float[] floats2, float f) {
        project(shape, floats, floats2, f, 0.0F);
    }

    static void project(Shape shape, float[] floats, float[] floats2, float f, float g) {
        float h = Math.max(0.01F, f * 0.3F);
        float f2 = f * 0.01F;
        float g2 = shape.distance(floats[0], floats[1], floats[2]) - g;
        gradient(shape, floats, h, floats2);
        for (int i = 0; i < 2 && Math.abs(g2) > f2; i++) {
            float h2 = Math.max(-f, Math.min(f, g2));
            floats[0] -= floats2[0] * h2;
            floats[1] -= floats2[1] * h2;
            floats[2] -= floats2[2] * h2;
            g2 = shape.distance(floats[0], floats[1], floats[2]) - g;
            gradient(shape, floats, h, floats2);
        }
    }

    static void gradient(Shape shape, float[] floats, float f, float[] floats2) {
        float g = floats[0];
        float h = floats[1];
        float f2 = floats[2];
        float g2 = shape.distance(g + f, h, f2) - shape.distance(g - f, h, f2);
        float h2;
        float f3;
        float g3 =
                (float)
                        Math.sqrt(
                                g2 * g2
                                        + (f3 =
                                                        shape.distance(g, h + f, f2)
                                                                - shape.distance(g, h - f, f2))
                                                * f3
                                        + (h2 =
                                                        shape.distance(g, h, f2 + f)
                                                                - shape.distance(g, h, f2 - f))
                                                * h2);
        if (g3 < 1.0E-12F) {
            floats2[0] = 0.0F;
            floats2[1] = 1.0F;
            floats2[2] = 0.0F;
        } else {
            floats2[0] = g2 / g3;
            floats2[1] = f3 / g3;
            floats2[2] = h2 / g3;
        }
    }

    private static float occlusion(
            Shape shape, float f, float g, float h, float f2, float g2, float h2, float f3) {
        if (shape != null && !(f3 <= 0.0F)) {
            float g3 = 0.0F;
            float h3 = 0.0F;
            float f4 = 1.0F;
            for (int m = 1; m <= 5; m++) {
                float g4 = 0.38F * m;
                float distance = shape.distance(f + f2 * g4, g + g2 * g4, h + h2 * g4);
                g3 += Math.max(0.0F, g4 - distance) * f4;
                h3 += g4 * f4;
                f4 *= 0.72F;
            }
            float h4 = Math.min(1.0F, g3 / h3 * 1.6F);
            return 1.0F - f3 * h4 * (1.4F - 0.4F * h4);
        } else {
            return 1.0F;
        }
    }

    private static Surface baseSurface(
            CharacterBuilder.Shaded shaded, Mesh mesh, Part part, Atlas atlas) {
        if (mesh.triCount == 0) {
            return null;
        } else {
            Surface.Builder builder = new Surface.Builder();
            float whiteU = atlas.whiteU();
            float whiteV = atlas.whiteV();
            for (int i = 0; i < shaded.count; i++) {
                builder.vertex(
                        shaded.pos[i * 3] - part.pivotX,
                        shaded.pos[i * 3 + 1] - part.pivotY,
                        shaded.pos[i * 3 + 2] - part.pivotZ,
                        shaded.nrm[i * 3],
                        shaded.nrm[i * 3 + 1],
                        shaded.nrm[i * 3 + 2],
                        whiteU,
                        whiteV,
                        shaded.color[i],
                        shaded.glow[i] != 0,
                        shaded.flat[i]);
            }
            pair(mesh.tris, mesh.triCount, mesh.vertexCount, builder);
            return builder.build();
        }
    }

    private static Surface hullSurface(
            Shape shape,
            CharacterBuilder.Shaded shaded,
            Surface surface,
            Part part,
            float f,
            float g,
            int i,
            Atlas atlas) {
        float[] floats = new float[shaded.count * 3];
        float[] floats2 = new float[shaded.count];
        float h = f / 0.5F * 1.5F;
        int j = (shaded.count + 255) / 256;
        IntStream.range(0, j)
                .parallel()
                .forEach(
                        ix -> {
                            float[] floats3 = new float[3];
                            float[] floats4 = new float[3];
                            int k = Math.min(shaded.count, (ix + 1) * 256);
                            for (int m = ix * 256; m < k; m++) {
                                float f2 = shaded.pos[m * 3];
                                float g2 = shaded.pos[m * 3 + 1];
                                float h2 = shaded.pos[m * 3 + 2];
                                float f3 = shaded.nrm[m * 3];
                                float g3 = shaded.nrm[m * 3 + 1];
                                float h3 = shaded.nrm[m * 3 + 2];
                                float f4 =
                                        shaded.cover != null && shaded.cover[m] < 0.0F ? 0.0F : f;
                                if (f4 > 0.0F) {
                                    float g4 = 0.0F;
                                    for (int n = 1; n <= 5; n++) {
                                        float h4 = h * n / 5.0F;
                                        g4 =
                                                Math.max(
                                                        g4,
                                                        -shape.distance(
                                                                f2 - f3 * h4,
                                                                g2 - g3 * h4,
                                                                h2 - h3 * h4));
                                    }
                                    f4 = Math.min(f4, 0.5F * g4);
                                }
                                floats3[0] = f2 + f3 * f4;
                                floats3[1] = g2 + g3 * f4;
                                floats3[2] = h2 + h3 * f4;
                                if (f4 > 0.0F) {
                                    project(shape, floats3, floats4, g, f4);
                                }
                                floats[m * 3] = floats3[0];
                                floats[m * 3 + 1] = floats3[1];
                                floats[m * 3 + 2] = floats3[2];
                                floats2[m] = f4;
                            }
                        });
        Surface.Builder builder = new Surface.Builder();
        float whiteU = atlas.whiteU();
        float whiteV = atlas.whiteV();
        for (int o = 0; o < shaded.count; o++) {
            builder.vertex(
                    floats[o * 3] - part.pivotX,
                    floats[o * 3 + 1] - part.pivotY,
                    floats[o * 3 + 2] - part.pivotZ,
                    0.0F,
                    1.0F,
                    0.0F,
                    whiteU,
                    whiteV,
                    i | 0xFF000000,
                    false);
        }
        int[] quads = surface.quads;
        for (int p = 0; p < quads.length; p += 4) {
            int quad = quads[p];
            int quad2 = quads[p + 1];
            int quad3 = quads[p + 2];
            int quad4 = quads[p + 3];
            if (!(floats2[quad] <= 0.0F)
                    || !(floats2[quad2] <= 0.0F)
                    || !(floats2[quad3] <= 0.0F)
                    || !(floats2[quad4] <= 0.0F)) {
                builder.quad(quad, quad4, quad3, quad2, quad3 == quad4 ? 1 : 2);
            }
        }
        return builder.build();
    }

    static void pair(int[] ints, int i, int j, Surface.Builder builder) {
        long[] longs = new long[i * 3];
        for (int k = 0; k < i; k++) {
            for (int m = 0; m < 3; m++) {
                int n = ints[k * 3 + m];
                int o = ints[k * 3 + (m + 1) % 3];
                long l = (long) Math.min(n, o) * j + Math.max(n, o);
                longs[k * 3 + m] = l << 21 | k * 3L + m;
            }
        }
        Arrays.sort(longs);
        int[] ints2 = new int[i * 3];
        Arrays.fill(ints2, -1);
        long l2 = 2097151L;
        for (int p = 0; p + 1 < longs.length; p++) {
            if (longs[p] >>> 21 == longs[p + 1] >>> 21) {
                if (p + 2 < longs.length && longs[p + 2] >>> 21 == longs[p] >>> 21) {
                    while (p + 1 < longs.length && longs[p + 1] >>> 21 == longs[p] >>> 21) {
                        p++;
                    }
                } else {
                    int q = (int) (longs[p] & l2);
                    int r;
                    ints2[q] = r = (int) (longs[p + 1] & l2);
                    ints2[r] = q;
                    p++;
                }
            }
        }
        boolean[] booleans = new boolean[i];
        for (int t = 0; t < i; t++) {
            if (!booleans[t]) {
                int u = -1;
                for (int v = 0; v < 3 && u < 0; v++) {
                    int w = ints2[t * 3 + v];
                    if (w >= 0 && !booleans[w / 3] && w / 3 != t) {
                        u = v;
                    }
                }
                int i2 = ints[t * 3];
                int j2 = ints[t * 3 + 1];
                int k2 = ints[t * 3 + 2];
                if (u < 0) {
                    booleans[t] = true;
                    builder.quad(i2, j2, k2, k2, 1);
                } else {
                    int m2 = ints2[t * 3 + u];
                    int n2 = m2 / 3;
                    booleans[t] = true;
                    booleans[n2] = true;
                    int o2 = ints[t * 3 + u];
                    int p2 = ints[t * 3 + (u + 1) % 3];
                    int q2 = ints[t * 3 + (u + 2) % 3];
                    int r2 = ints[n2 * 3 + (m2 % 3 + 2) % 3];
                    builder.quad(o2, r2, p2, q2, 2);
                }
            }
        }
    }

    private static Surface decalSurface(
            Part part, CharacterBuilder.Shaded shaded, Mesh mesh, Atlas atlas) {
        Surface.Builder builder = new Surface.Builder();
        float[] floats = new float[200];
        float[] floats2 = new float[200];
        for (Decal decal : part.decals) {
            for (int i = 0; i < mesh.triCount; i++) {
                int tri = mesh.tris[i * 3];
                int tri2 = mesh.tris[i * 3 + 1];
                int tri3 = mesh.tris[i * 3 + 2];
                if (faces(decal, shaded, tri, tri2, tri3)) {
                    int[] ints = new int[] {tri, tri2, tri3};
                    float f = 0.0F;
                    for (int n = 0; n < 3; n++) {
                        int j = ints[n];
                        int k = n * 10;
                        floats[k] = shaded.pos[j * 3];
                        floats[k + 1] = shaded.pos[j * 3 + 1];
                        floats[k + 2] = shaded.pos[j * 3 + 2];
                        floats[k + 3] = shaded.nrm[j * 3];
                        floats[k + 4] = shaded.nrm[j * 3 + 1];
                        floats[k + 5] = shaded.nrm[j * 3 + 2];
                        floats[k + 8] = shaded.ao[j];
                        floats[k + 9] = shaded.flat[j];
                        float g = floats[k] - decal.cx;
                        float h = floats[k + 1] - decal.cy;
                        float f2 = floats[k + 2] - decal.cz;
                        if (!decal.wrap) {
                            floats[k + 6] =
                                    0.5F
                                            + (g * decal.rx + h * decal.ry + f2 * decal.rz)
                                                    / decal.width;
                            floats[k + 7] =
                                    0.5F
                                            - (g * decal.ux + h * decal.uy + f2 * decal.uz)
                                                    / decal.height;
                        } else {
                            float g2 = g * decal.dx + h * decal.dy + f2 * decal.dz;
                            float h2 = g - g2 * decal.dx;
                            float f3 = h - g2 * decal.dy;
                            float g3 = f2 - g2 * decal.dz;
                            float h3 =
                                    (float)
                                            Math.toDegrees(
                                                    Math.atan2(
                                                            h2 * decal.rx
                                                                    + f3 * decal.ry
                                                                    + g3 * decal.rz,
                                                            h2 * decal.ux
                                                                    + f3 * decal.uy
                                                                    + g3 * decal.uz));
                            if (n == 0) {
                                f = h3;
                            } else {
                                float f4 = h3 - f;
                                while (f4 > 180.0F) {
                                    f4 -= 360.0F;
                                }
                                while (f4 < -180.0F) {
                                    f4 += 360.0F;
                                }
                                h3 = f + f4;
                            }
                            floats[k + 6] = 0.5F + h3 / decal.width;
                            floats[k + 7] = 0.5F - g2 / decal.height;
                        }
                    }
                    emitClipped(decal, floats, floats2, 0.0F, part, atlas, builder);
                    if (decal.wrap && decal.width >= 359.9F) {
                        emitClipped(decal, floats, floats2, 1.0F, part, atlas, builder);
                        emitClipped(decal, floats, floats2, -1.0F, part, atlas, builder);
                    }
                }
            }
        }
        return builder.build();
    }

    private static boolean faces(Decal decal, CharacterBuilder.Shaded shaded, int i, int j, int k) {
        float f = shaded.pos[i * 3 + 1];
        float g = shaded.pos[j * 3 + 1] - f;
        float h = shaded.pos[i * 3 + 2];
        float f2 = shaded.pos[k * 3 + 2] - h;
        float g2 = shaded.pos[j * 3 + 2] - h;
        float h2 = shaded.pos[k * 3 + 1] - f;
        float f3 = g * f2 - g2 * h2;
        float g3 = shaded.pos[i * 3];
        float h3 = shaded.pos[k * 3] - g3;
        float f4 = shaded.pos[j * 3] - g3;
        float g4 = g2 * h3 - f4 * f2;
        float h4 = f4 * h2 - g * h3;
        float f5 = (float) Math.sqrt(f3 * f3 + g4 * g4 + h4 * h4);
        if (f5 < 1.0E-10F) {
            return false;
        } else {
            f3 /= f5;
            g4 /= f5;
            h4 /= f5;
            int[] ints = new int[] {i, j, k};
            if (!decal.wrap) {
                if (f3 * decal.dx + g4 * decal.dy + h4 * decal.dz < decal.cosLimit) {
                    return false;
                } else {
                    for (int m : ints) {
                        float g5 =
                                (shaded.pos[m * 3] - decal.cx) * decal.dx
                                        + (shaded.pos[m * 3 + 1] - decal.cy) * decal.dy
                                        + (shaded.pos[m * 3 + 2] - decal.cz) * decal.dz;
                        if (g5 < decal.near || g5 > decal.far) {
                            return false;
                        }
                    }
                    return true;
                }
            } else {
                float h5 =
                        (shaded.pos[i * 3] + shaded.pos[j * 3] + shaded.pos[k * 3]) / 3.0F
                                - decal.cx;
                float f6 =
                        (shaded.pos[i * 3 + 1] + shaded.pos[j * 3 + 1] + shaded.pos[k * 3 + 1])
                                        / 3.0F
                                - decal.cy;
                float g6 =
                        (shaded.pos[i * 3 + 2] + shaded.pos[j * 3 + 2] + shaded.pos[k * 3 + 2])
                                        / 3.0F
                                - decal.cz;
                float h6 = h5 * decal.dx + f6 * decal.dy + g6 * decal.dz;
                float f7 = h5 - h6 * decal.dx;
                float g7 = f6 - h6 * decal.dy;
                float h7 = g6 - h6 * decal.dz;
                float f8 = (float) Math.sqrt(f7 * f7 + g7 * g7 + h7 * h7);
                if (f8 < 1.0E-5F) {
                    return false;
                } else if ((f3 * f7 + g4 * g7 + h4 * h7) / f8 < decal.cosLimit) {
                    return false;
                } else {
                    float g8 = decal.near == -3.4028235E38F ? 0.0F : decal.near;
                    return f8 >= g8 && f8 <= decal.far;
                }
            }
        }
    }

    private static void emitClipped(
            Decal decal,
            float[] floats,
            float[] floats2,
            float f,
            Part part,
            Atlas atlas,
            Surface.Builder builder) {
        float[] floats3 = new float[200];
        System.arraycopy(floats, 0, floats3, 0, 30);
        if (f != 0.0F) {
            for (int i = 0; i < 3; i++) {
                int j = i * 10 + 6;
                floats3[j] += f;
            }
        }
        int k = 3;
        k = clip(floats3, k, floats2, 6, 0.0F, true);
        k = clip(floats2, k, floats3, 6, 1.0F, false);
        k = clip(floats3, k, floats2, 7, 0.0F, true);
        if ((k = clip(floats2, k, floats3, 7, 1.0F, false)) >= 3) {
            int m = -1;
            for (int n = 0; n < k; n++) {
                int o = n * 10;
                float g = floats3[o + 3];
                float h = floats3[o + 4];
                float f2 = floats3[o + 5];
                float g2 = (float) Math.sqrt(g * g + h * h + f2 * f2);
                if (g2 > 1.0E-8F) {
                    g /= g2;
                    h /= g2;
                    f2 /= g2;
                }
                int channel = Sample.channel(1.0F - decal.occlusion * (1.0F - floats3[o + 8]));
                int p = 0xFF000000 | channel << 16 | channel << 8 | channel;
                float h2 = decal.flat == decal.flat ? decal.flat : floats3[o + 9];
                int vertex =
                        builder.vertex(
                                floats3[o] + g * decal.offset - part.pivotX,
                                floats3[o + 1] + h * decal.offset - part.pivotY,
                                floats3[o + 2] + f2 * decal.offset - part.pivotZ,
                                g,
                                h,
                                f2,
                                atlas.u(decal.canvas, floats3[o + 6]),
                                atlas.v(decal.canvas, floats3[o + 7]),
                                p,
                                decal.glow,
                                h2);
                if (m < 0) {
                    m = vertex;
                }
            }
            int q;
            for (q = 1; q + 2 < k; q += 2) {
                builder.quad(m, m + q, m + q + 1, m + q + 2, 2);
            }
            if (q + 1 < k) {
                builder.quad(m, m + q, m + q + 1, m + q + 1, 1);
            }
        }
    }

    private static int clip(float[] floats, int i, float[] floats2, int j, float f, boolean bl) {
        int k = 0;
        for (int m = 0; m < i; m++) {
            int n = m * 10;
            int o = (m + i - 1) % i * 10;
            float g = bl ? floats[n + j] - f : f - floats[n + j];
            float h = bl ? floats[o + j] - f : f - floats[o + j];
            if (g >= 0.0F) {
                if (h < 0.0F) {
                    k = cut(floats, o, n, h, g, floats2, k);
                }
                System.arraycopy(floats, n, floats2, k * 10, 10);
                k++;
            } else if (h >= 0.0F) {
                k = cut(floats, o, n, h, g, floats2, k);
            }
            if (k >= 19) {
                break;
            }
        }
        return k;
    }

    private static int cut(float[] floats, int i, int j, float f, float g, float[] floats2, int k) {
        float h = f / (f - g);
        for (int m = 0; m < 10; m++) {
            floats2[k * 10 + m] = floats[i + m] + (floats[j + m] - floats[i + m]) * h;
        }
        return k + 1;
    }

    private static float reach(Surface surface) {
        if (surface == null) {
            return 1.0F;
        } else {
            float f = 0.0F;
            for (int i = 0; i < surface.vertexCount(); i++) {
                float g = surface.pos[i * 3];
                float h = surface.pos[i * 3 + 1];
                float f2 = surface.pos[i * 3 + 2];
                f = Math.max(f, g * g + h * h + f2 * f2);
            }
            return Math.max(0.5F, (float) Math.sqrt(f));
        }
    }

    private static void jointWeights(Surface surface, Part part, Part part2) {
        if (surface != null) {
            float[] floats = new float[surface.vertexCount()];
            float f = part2.jointX - part.pivotX;
            float g = part2.jointY - part.pivotY;
            float h = part2.jointZ - part.pivotZ;
            float jointBlend = part2.jointBlend;
            boolean bl = false;
            for (int i = 0; i < floats.length; i++) {
                float f2 =
                        (surface.pos[i * 3] - f) * part2.jointDirX
                                + (surface.pos[i * 3 + 1] - g) * part2.jointDirY
                                + (surface.pos[i * 3 + 2] - h) * part2.jointDirZ;
                float g2 = Math.max(0.0F, Math.min(1.0F, (f2 + jointBlend) / (2.0F * jointBlend)));
                floats[i] = g2 * g2 * (3.0F - 2.0F * g2);
                bl |= floats[i] > 0.0F;
            }
            if (bl) {
                surface.joint = floats;
            }
        }
    }

    private static void weigh(Surface surface, float f, float g) {
        if (surface != null) {
            float[] floats = new float[surface.vertexCount()];
            for (int i = 0; i < floats.length; i++) {
                float h = surface.pos[i * 3];
                float f2 = surface.pos[i * 3 + 1];
                float g2 = surface.pos[i * 3 + 2];
                float h2 = Math.min(1.0F, (float) Math.sqrt(h * h + f2 * f2 + g2 * g2) / f);
                floats[i] = (float) Math.pow(h2, g);
            }
            surface.weight = floats;
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class Shaded {

        final int count;

        final float[] pos;

        final float[] nrm;

        final int[] color;

        final byte[] glow;

        final float[] ao;

        final float[] flat;

        float[] cover;

        Shaded(int count2) {
            this.count = count2;
            this.pos = new float[count2 * 3];
            this.nrm = new float[count2 * 3];
            this.color = new int[count2];
            this.glow = new byte[count2];
            this.ao = new float[count2];
            this.flat = new float[count2];
        }
    }
}
