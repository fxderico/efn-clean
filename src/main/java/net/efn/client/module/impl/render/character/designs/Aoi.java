package net.efn.client.module.impl.render.character.designs;

import net.efn.client.module.impl.render.character.kit.Bone;
import net.efn.client.module.impl.render.character.kit.Canvas;
import net.efn.client.module.impl.render.character.kit.CharacterDesign;
import net.efn.client.module.impl.render.character.kit.Colors;
import net.efn.client.module.impl.render.character.kit.Decal;
import net.efn.client.module.impl.render.character.kit.Motion;
import net.efn.client.module.impl.render.character.kit.Paint;
import net.efn.client.module.impl.render.character.kit.Paint2D;
import net.efn.client.module.impl.render.character.kit.Part;
import net.efn.client.module.impl.render.character.kit.Rig;
import net.efn.client.module.impl.render.character.kit.Shape;
import net.efn.client.module.impl.render.character.kit.Shape2D;
import net.efn.client.module.impl.render.character.kit.Shapes;
import net.efn.client.module.impl.render.character.kit.Strand;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class Aoi extends CharacterDesign {

    private static final float TAU = 6.2831855F;

    private static final float NECK = 24.4F;

    private static final float HEAD_K = 1.16F;

    private static final float HEAD_DY = -0.25F;

    private static final float FACE_X = 2.25F;

    private static final float FACE_TOP = 28.6F;

    private static final float FACE_LOW = 26.08F;

    private static final float FACE_PX = 80.0F;

    private static final float YOKE = 21.75F;

    private static final float BEND = 13.0F;

    private static final int SKIN = -9274;

    private static final int SKIN_SHADE = -1331558;

    private static final int SKIN_LINE = -6657438;

    private static final int HAIR = -14140794;

    private static final int HAIR_DEEP = -15063975;

    private static final int HAIR_TOP = -12756032;

    private static final int HAIR_BLUE = -12948262;

    private static final int HAIR_TIP = -13246209;

    private static final int HAIR_INNER = -13646610;

    private static final int HAIR_SHINE = -7363350;

    private static final int HAIR_LINE = -15327158;

    private static final int CYAN = -13703425;

    private static final int NAVY = -14932394;

    private static final int JACKET = -12959662;

    private static final int JACKET_DARK = -14210250;

    private static final int JACKET_WHITE = -1446155;

    private static final int JACKET_SHADE = -3945254;

    private static final int LINE = -15065558;

    private static final int TOP = -525828;

    private static final int TOP_SHADE = -2498584;

    private static final int SKIRT = -12892555;

    private static final int SKIRT_DARK = -14472632;

    private static final int BLACK = -15263457;

    private static final int SILVER = -3550757;

    private static final int SOCK = -14012606;

    private static final int SOLE = -920586;

    private static final float SKIRT_ZC = -0.05F;

    private static final float SKIRT_TOP = 17.3F;

    private static final float SKIRT_HEM = 11.25F;

    private static final int PLEATS = 18;

    private static final float[][] MAIN_LOCKS =
            new float[][] {
                {0.95F, -1.55F, 29.85F, -2.0F, 29.5F, -2.3F, 28.45F, -2.24F, 27.65F, -2.0F, 27.42F},
                {0.95F, -1.15F, 29.95F, -1.38F, 29.5F, -1.6F, 28.6F, -1.55F, 28.12F, -1.3F, 27.93F},
                {0.85F, -0.64F, 29.95F, -0.45F, 29.4F, -0.08F, 28.25F, 0.16F, 27.4F, 0.04F, 27.02F},
                {1.0F, -0.08F, 30.0F, 0.35F, 29.6F, 0.9F, 28.7F, 1.2F, 28.22F, 0.98F, 28.0F},
                {1.05F, 0.56F, 30.0F, 1.15F, 29.7F, 1.8F, 28.75F, 2.08F, 27.8F, 1.85F, 27.45F}
            };

    private static final float[][] ACCENT_LOCKS =
            new float[][] {
                {0.55F, -0.95F, 29.85F, -1.0F, 29.3F, -0.9F, 28.5F, -0.76F, 28.3F},
                {0.55F, -0.22F, 29.85F, 0.14F, 29.35F, 0.52F, 28.72F, 0.44F, 28.55F},
                {0.5F, 0.5F, 29.85F, 1.02F, 29.4F, 1.5F, 28.5F, 1.44F, 28.2F}
            };

    public Aoi() {
        super("aoi", "Aoi");
    }

    @Override
    protected void define(Rig rig) {
        rig.scale(1.0F).outline(-15065558, 0.13F).occlusion(0.35F);
        Canvas canvas = stripe();
        Shape shape = headShape();
        List list = fringeLocks(shape, MAIN_LOCKS, 0.2F, 0.17F);
        List list2 = fringeLocks(shape, ACCENT_LOCKS, 0.2F, 0.17F);
        rig.part(Bone.HEAD)
                .pivot(0.0F, 24.4F, 0.0F)
                .shape(grow(shape.color(-9274)))
                .detail(1.1F)
                .resolution(0.85F)
                .outline(-6657438, 0.09F)
                .flatLight(1.0F)
                .occlusion(0.2F)
                .decal(face(list));
        hair(rig, list, list2);
        ponytail(rig);
        Shape shape2 = body(rig, canvas);
        skirt(rig, canvas);
        arms(rig, canvas);
        legs(rig);
        gear(rig, shape2);
    }

    private static Shape grow(Shape shape) {
        return shape.scale(1.16F, 0.0F, 24.4F, 0.0F).move(0.0F, -0.25F, 0.0F);
    }

    private static float hy(float f) {
        return 24.4F + (f - 24.4F) * 1.16F + -0.25F;
    }

    private static Shape headShape() {
        Shape shape = Shapes.ellipsoid(0.0F, 28.52F, -0.15F, 2.28F, 2.24F, 2.32F);
        Shape shape2 = Shapes.ellipsoid(0.0F, 27.4F, 0.7F, 1.95F, 1.1F, 1.45F);
        Shape shape3 = Shapes.cone(0.0F, 27.25F, 0.42F, 1.68F, 0.0F, 26.15F, 1.18F, 0.55F);
        Shape shape4 =
                Shapes.extrude(
                        Shape2D.polygon(
                                -2.6F, 31.5F, -2.6F, 29.0F, -2.24F, 27.6F, -2.11F, 27.2F, -1.9F,
                                26.8F, -1.62F, 26.44F, -1.28F, 26.12F, -0.92F, 25.91F, -0.54F,
                                25.79F, -0.25F, 25.75F, 0.0F, 25.74F, 0.25F, 25.75F, 0.54F, 25.79F,
                                0.92F, 25.91F, 1.28F, 26.12F, 1.62F, 26.44F, 1.9F, 26.8F, 2.11F,
                                27.2F, 2.24F, 27.6F, 2.6F, 29.0F, 2.6F, 31.5F),
                        4.0F,
                        0.0F);
        Shape shape5 = Shapes.blend(0.9F, shape, shape2, shape3);
        Shape shape6 = shape5.intersect(0.3F, shape4);
        return Shapes.field(
                (value, value2, value3) -> {
                    float f =
                            value2 > 28.8F
                                    ? shape5.distance(value, value2, value3)
                                    : shape6.distance(value, value2, value3);
                    return Math.max(f, Math.max(-1.0F - value3, value2 - 30.1F));
                },
                shape5.minX(),
                shape5.minY(),
                -1.1F,
                shape5.maxX(),
                30.2F,
                shape5.maxZ());
    }

    private static List<float[]> fringeLocks(Shape shape, float[][] floatGrid, float f, float g) {
        ArrayList list = new ArrayList();
        for (float[] floats : floatGrid) {
            int i = (floats.length - 1) / 2;
            float[] floats2 = new float[i * 4];
            for (int j = 0; j < i; j++) {
                float h = floats[1 + j * 2];
                float f2 = floats[2 + j * 2];
                float g2 = 1.0F - h * h / 6.76F - (f2 - 28.62F) * (f2 - 28.62F) / 7.08F;
                float h2 =
                        Math.max(
                                frontZ(shape, h, f2, 5.0F),
                                -0.5F + 2.64F * (float) Math.sqrt(Math.max(0.0F, g2)));
                float f3 = f2 - 28.3F;
                float g3 = h2 + 0.4F;
                float h3 = (float) Math.sqrt(h * h + f3 * f3 + g3 * g3);
                float f4 = j / (i - 1.0F);
                float g4 =
                        (j == 0 ? 0.1F : (j == i - 1 ? g : f))
                                + 0.22F * smooth(1.3F, 2.2F, Math.abs(h));
                floats2[j * 4] = h + h / h3 * g4;
                floats2[1 + j * 4] = f2 + f3 / h3 * g4;
                floats2[2 + j * 4] = h2 + g3 / h3 * g4;
                floats2[3 + j * 4] =
                        floats[0] * (float) Math.pow(1.0F - f4, 1.2000000476837158) + 0.05F * f4;
            }
            list.add(floats2);
        }
        return list;
    }

    private static void hair(Rig rig, List<float[]> list, List<float[]> list2) {
        ArrayList list3 = new ArrayList();
        list3.add(
                Shapes.subtract(
                                0.35F,
                                Shapes.ellipsoid(0.0F, 28.62F, -0.2F, 2.6F, 2.66F, 2.64F),
                                Shapes.ellipsoid(0.0F, 26.75F, 2.75F, 2.45F, 2.85F, 2.72F))
                        .intersect(Shapes.above(26.0F)));
        for (float f : new float[] {-155.0F, -118.0F, -80.0F, 80.0F, 118.0F, 155.0F}) {
            list3.add(flowLock(f));
        }
        rig.extra("hair", Bone.HEAD)
                .shape(
                        grow(
                                Shapes.union(((Shape[]) list3.toArray(new Shape[0])))
                                        .paint(capPaint())))
                .detail(0.8F)
                .resolution(0.95F)
                .outline(-15327158, 0.1F)
                .flatLight(0.5F)
                .occlusion(0.4F);
        Part part =
                rig.extra("fringe", Bone.HEAD)
                        .shape(grow(fringe(list, list2)))
                        .detail(0.4F)
                        .resolution(0.55F)
                        .outline(-15327158, 0.09F)
                        .flatLight(0.5F)
                        .occlusion(0.2F);
        shine(part, list);
        Motion motion =
                Motion.hair()
                        .idle(1.5F, 0.0F, 1.5F, 0.35F)
                        .walk(3.0F, 0.0F, 3.0F, 2.0F)
                        .drag(8.0F)
                        .hang(0.7F)
                        .turn(10.0F, 0.0F)
                        .spring(60.0F, 7.0F)
                        .bend(1.3F);
        Paint paint =
                Paint.field(
                        (value, value2, value3) ->
                                Colors.mix(
                                        Colors.mix(
                                                -14140794, -12948262, smooth(28.2F, 25.8F, value2)),
                                        -13246209,
                                        smooth(26.2F, 24.4F, value2)));
        Paint paint2 =
                Paint.field(
                        (value4, value5, value6) ->
                                Colors.mix(-13646610, -13246209, smooth(27.0F, 25.0F, value5)));
        Shape shape =
                Shapes.union(
                        Shapes.tube(
                                        -1.55F, 29.95F, 1.0F, 0.4F, -2.32F, 28.5F, 1.4F, 0.55F,
                                        -2.55F, 27.1F, 1.3F, 0.52F, -2.48F, 25.9F, 1.15F, 0.42F,
                                        -2.3F, 24.9F, 1.2F, 0.28F, -2.1F, 24.3F, 1.34F, 0.04F)
                                .flattenFrom(2.0F, 0.0F, 27.5F, -0.5F)
                                .paint(paint),
                        Shapes.tube(
                                        -1.7F, 29.7F, 0.7F, 0.36F, -2.28F, 28.2F, 1.05F, 0.44F,
                                        -2.36F, 26.8F, 0.95F, 0.42F, -2.22F, 25.6F, 0.9F, 0.3F,
                                        -2.05F, 24.55F, 1.0F, 0.04F)
                                .flattenFrom(2.0F, 0.0F, 27.5F, -0.5F)
                                .paint(paint2));
        Part part2 =
                rig.extra("lock_r", Bone.HEAD)
                        .pivot(-2.32F, hy(29.2F), 1.16F)
                        .shape(grow(shape))
                        .motion(motion)
                        .detail(0.7F)
                        .resolution(0.7F)
                        .outline(-15327158, 0.1F)
                        .flatLight(0.5F)
                        .occlusion(0.35F);
        rig.extra("lock_l", Bone.HEAD).mirrorOf(part2);
    }

    private static Shape flowLock(float f) {
        float g = 0.0F;
        float h = 28.62F;
        float f2 = -0.2F;
        float g2 = 2.6F;
        float h2 = 2.66F;
        float f3 = 2.64F;
        float g3 = (float) Math.toRadians(f);
        float h3 = Math.abs(f);
        float f4 = 29.1F - 1.7F * smooth(20.0F, 90.0F, h3) - 1.2F * smooth(95.0F, 160.0F, h3);
        float g4 = (float) Math.asin(Math.max(-0.95F, Math.min(0.95F, (f4 - h) / h2)));
        float h4 = (float) (Math.sin(g3) * Math.cos(g4));
        float f5 = (float) Math.sin(g4);
        float g5 = (float) (Math.cos(g3) * Math.cos(g4));
        float h5 = 0.0F;
        float f6 = 30.25F - h;
        float g6 = -1.95F - f2;
        float h6 = (float) Math.sqrt(h5 * h5 + f6 * f6 + g6 * g6);
        h5 /= h6;
        f6 /= h6;
        g6 /= h6;
        float[] floats = new float[] {0.0F, 0.35F, 0.7F, 1.0F};
        float[] floats2 = new float[] {0.5F, 0.56F, 0.5F, 0.32F};
        float[] floats3 = new float[16];
        for (int i = 0; i < 4; i++) {
            float f7 = h4 + (h5 - h4) * floats[i];
            float g7 = f5 + (f6 - f5) * floats[i];
            float h7 = g5 + (g6 - g5) * floats[i];
            float f8 = (float) Math.sqrt(f7 * f7 + g7 * g7 + h7 * h7);
            float g8;
            float h8;
            float f9;
            float g9 =
                    1.0F
                            / (float)
                                    Math.sqrt(
                                            (g8 = f7 / f8) * g8 / (g2 * g2)
                                                    + (h8 = g7 / f8) * h8 / (h2 * h2)
                                                    + (f9 = h7 / f8) * f9 / (f3 * f3));
            float h9 = i == 3 ? -0.1F : 0.04F;
            floats3[i * 4] = g + g8 * (g9 + h9);
            floats3[i * 4 + 1] = h + h8 * (g9 + h9);
            floats3[i * 4 + 2] = f2 + f9 * (g9 + h9);
            floats3[i * 4 + 3] = floats2[i];
        }
        return Shapes.tube(floats3).flattenFrom(2.4F, g, h, f2);
    }

    private static Paint capPaint() {
        return Paint.field(
                (value, value2, value3) ->
                        Colors.mix(
                                Colors.mix(-15063975, -14140794, smooth(26.2F, 28.4F, value2)),
                                -12756032,
                                smooth(29.4F, 31.2F, value2) * 0.8F));
    }

    private static Shape fringe(List<float[]> list, List<float[]> list2) {
        Shape[] shapes = new Shape[list.size() + list2.size()];
        for (int j = 0; j < shapes.length; j++) {
            boolean bl = j >= list.size();
            float[] floats = bl ? (float[]) list2.get(j - list.size()) : (float[]) list.get(j);
            int k = floats.length / 4;
            float[] floats2 = new float[k * 2];
            for (int m = 0; m < k; m++) {
                floats2[m * 2] = floats[m * 4];
                floats2[m * 2 + 1] = floats[m * 4 + 1];
            }
            float f = bl ? 1.12F : ((j & 1) == 0 ? 0.93F : 1.04F);
            shapes[j] =
                    Shapes.tube(floats)
                            .flattenFrom(2.6F, 0.0F, 28.4F, -0.3F)
                            .intersect(
                                    Shapes.extrude(
                                            Shape2D.path(floats[3] * 2.6F, 0.02F, floats2),
                                            4.0F,
                                            0.0F))
                            .paint(
                                    Paint.field(
                                            (value, value2, i) ->
                                                    Colors.shade(
                                                            Colors.mix(
                                                                    Colors.mix(
                                                                            -14140794,
                                                                            -12756032,
                                                                            smooth(
                                                                                            29.6F,
                                                                                            30.6F,
                                                                                            value2)
                                                                                    * 0.5F),
                                                                    -12948262,
                                                                    smooth(28.9F, 27.4F, value2)
                                                                            * 0.85F),
                                                            f)));
        }
        return Shapes.union(shapes);
    }

    private static void shine(Part part, List<float[]> list) {
        for (int i = 0; i < list.size(); i++) {
            float[] floats = (float[]) list.get(i);
            int j = floats.length / 4;
            for (int k = 1; k < j; k++) {
                float f = floats[k * 4 - 3];
                float g = floats[k * 4 + 1];
                if (!(f < 29.15F) && !(g > 29.15F)) {
                    float h = (f - 29.15F) / (f - g);
                    float f2 = floats[k * 4 - 4] + (floats[k * 4] - floats[k * 4 - 4]) * h;
                    float g2 = floats[k * 4 - 2] + (floats[k * 4 + 2] - floats[k * 4 - 2]) * h;
                    float h2 = floats[k * 4 - 1] + (floats[k * 4 + 3] - floats[k * 4 - 1]) * h;
                    float f3 = (float) Math.atan2(f2, g2 + 0.2F);
                    if (!(Math.abs(f3) > 0.96F)) {
                        float g3 =
                                h2 * 1.1F / (float) Math.sqrt(f2 * f2 + (g2 + 0.2F) * (g2 + 0.2F));
                        float h3 = 7.0F + (float) Math.toDegrees(g3) * 0.25F;
                        float f4 = 64.0F / (g3 * 1.5F);
                        float[] floats2 = new float[6];
                        float[] floats3 = new float[6];
                        for (int m = 0; m < 3; m++) {
                            float g4 = g3 * 0.6F * (m * 0.5F - 1.0F);
                            float h4 = g3 * 0.6F * m * 0.5F;
                            floats2[m * 2] = 32.0F + g4 * f4;
                            floats2[m * 2 + 1] =
                                    13.0F
                                            + 25.0F
                                                    * ((float) Math.cos(f3)
                                                            - (float) Math.cos(f3 + g4));
                            floats3[m * 2] = 32.0F + h4 * f4;
                            floats3[m * 2 + 1] =
                                    13.0F
                                            + 25.0F
                                                    * ((float) Math.cos(f3)
                                                            - (float) Math.cos(f3 + h4));
                        }
                        Canvas canvas = new Canvas(64, 26);
                        canvas.fill(
                                Shape2D.path(h3 * 0.2F, h3, floats2)
                                        .union(Shape2D.path(h3, h3 * 0.2F, floats3)),
                                Colors.withAlpha(-7363350, 0.5F),
                                5.0F);
                        float f5 =
                                hy(29.1F)
                                        + 0.081F
                                        - 0.29F * (1.0F - (float) Math.cos(f3))
                                        + ((i & 1) * 2 - 1) * 0.03F;
                        part.decal(
                                Decal.wrap(
                                                canvas,
                                                0.0F,
                                                f5,
                                                -0.232F,
                                                0.0F,
                                                1.0F,
                                                0.0F,
                                                (float) Math.sin(f3),
                                                0.0F,
                                                (float) Math.cos(f3),
                                                (float) Math.toDegrees(g3 * 1.5F),
                                                0.30159998F)
                                        .radius(1.8F, 4.2F)
                                        .flatLight(0.8F));
                    }
                }
            }
        }
    }

    private static void ponytail(Rig rig) {
        float[][] floatGrid =
                new float[][] {
                    {30.4F, -2.0F},
                    {31.1F, -2.5F},
                    {31.35F, -3.25F},
                    {30.95F, -3.95F},
                    {29.7F, -4.3F},
                    {27.6F, -4.38F},
                    {25.1F, -4.22F},
                    {22.5F, -4.0F},
                    {19.9F, -3.8F},
                    {17.5F, -3.68F},
                    {15.3F, -3.74F}
                };
        int i = floatGrid.length;
        float[] floats =
                new float[] {
                    0.0F, 0.55F, 1.0F, 1.18F, 1.22F, 1.18F, 1.08F, 0.96F, 0.86F, 0.98F, 1.35F
                };
        float[] floats2 =
                new float[] {
                    0.45F, 0.88F, 1.05F, 1.1F, 1.1F, 1.05F, 0.98F, 0.86F, 0.7F, 0.52F, 0.34F
                };
        float[] floats3 = new float[i];
        float[] floats4 = new float[i];
        for (int j = 0; j < i; j++) {
            int k = Math.max(0, j - 1);
            int m = Math.min(i - 1, j + 1);
            float f = floatGrid[m][0] - floatGrid[k][0];
            float g = floatGrid[m][1] - floatGrid[k][1];
            float h = (float) Math.sqrt(f * f + g * g);
            floats3[j] = g / h;
            floats4[j] = -f / h;
        }
        float[][] floatGrid2 =
                new float[][] {
                    {-1.02F, 0.25F, 10.1F, 0.68F, 0.0F, -1.0F},
                    {-0.5F, -0.3F, 10.25F, 0.72F, 1.1F, -1.0F},
                    {0.02F, 0.3F, 10.8F, 0.74F, 2.3F, 0.0F},
                    {0.54F, -0.28F, 9.7F, 0.72F, 0.6F, 1.0F},
                    {1.03F, 0.22F, 9.55F, 0.66F, 1.7F, 1.0F},
                    {-0.72F, 0.62F, 9.4F, 0.62F, 2.9F, -1.0F},
                    {0.74F, 0.6F, 9.5F, 0.62F, 0.3F, 1.0F},
                    {-0.05F, -0.62F, 10.6F, 0.64F, 2.0F, 0.0F}
                };
        ArrayList list = new ArrayList();
        for (float[] floats5 : floatGrid2) {
            int n = Math.min((int) Math.floor(floats5[2]), i - 1);
            float f2 = floats5[2] - n;
            float[] floats6 = new float[(n + 2) * 4];
            for (int o = 0; o <= n; o++) {
                float g2 = floats[o];
                float h2 = smooth(6.5F, 8.6F, o);
                float f3 =
                        0.14F
                                * (float) Math.sin(o * 0.9F + floats5[4])
                                * smooth(1.0F, 4.0F, o)
                                * (1.0F - h2);
                float g3 = floats5[0] * g2 + f3;
                float h3 = floats5[1] * g2 * 0.75F * (1.0F - 0.6F * h2);
                floats6[o * 4] = g3 + (floats5[5] * 1.15F * g2 + floats5[0] * 0.2F * g2 - g3) * h2;
                floats6[o * 4 + 1] = floatGrid[o][0] + floats3[o] * h3;
                floats6[o * 4 + 2] = floatGrid[o][1] + floats4[o] * h3;
                floats6[o * 4 + 3] = floats5[3] * 1.08F * floats2[o];
            }
            int p = (n + 1) * 4;
            int q = Math.min(n + 1, i - 1);
            float f4 = Math.max(f2, 0.45F);
            floats6[p] = floats6[n * 4] * 1.12F + floats5[5] * 0.2F;
            floats6[p + 1] = floatGrid[n][0] + (floatGrid[q][0] - floatGrid[n][0]) * f4 - 1.1F;
            floats6[p + 2] = floats6[n * 4 + 2] + (floatGrid[q][1] - floatGrid[n][1]) * f4 - 0.1F;
            floats6[p + 3] = 0.04F;
            split(list, floats6, 6, 1.6F, true);
        }
        float[] floats7 = new float[36];
        for (int r = 0; r < 9; r++) {
            floats7[r * 4 + 1] = floatGrid[r][0];
            floats7[r * 4 + 2] = floatGrid[r][1];
            floats7[r * 4 + 3] = 0.64F * floats2[r];
        }
        split(list, floats7, 5, 1.4F, false);
        for (float[] floats8 :
                new float[][] {
                    {
                        0.05F, 30.55F, -2.15F, 0.1F, 31.45F, -2.6F, 0.15F, 32.05F, -3.35F, 0.1F,
                        31.85F, -4.15F
                    },
                    {
                        0.3F, 30.5F, -2.1F, 0.72F, 31.25F, -2.7F, 0.98F, 31.72F, -3.45F, 1.08F,
                        31.42F, -4.05F
                    },
                    {
                        -0.35F, 30.5F, -2.1F, -0.76F, 31.2F, -2.75F, -1.02F, 31.62F, -3.4F, -1.12F,
                        31.32F, -4.0F
                    }
                }) {
            list.add(
                    Shapes.tube(
                                    floats8[0],
                                    floats8[1],
                                    floats8[2],
                                    0.36F,
                                    floats8[3],
                                    floats8[4],
                                    floats8[5],
                                    0.44F,
                                    floats8[6],
                                    floats8[7],
                                    floats8[8],
                                    0.3F,
                                    floats8[9],
                                    floats8[10],
                                    floats8[11],
                                    0.04F)
                            .flattenFrom(1.8F, 0.0F, 26.0F, 0.5F));
        }
        Paint paint =
                Paint.field(
                        (value, value2, value3) -> {
                            int t = Colors.mix(-14140794, -12948262, smooth(27.5F, 21.5F, value2));
                            t = Colors.mix(t, -13246209, smooth(21.5F, 16.0F, value2));
                            float g4 = smooth(30.9F, 31.5F, value2) * smooth(-2.4F, -3.2F, value3);
                            return Colors.mix(t, -7363350, g4 * 0.5F);
                        });
        Shape shape = Shapes.union(((Shape[]) list.toArray(new Shape[0]))).paint(paint);
        Shape shape2 =
                ring(0.0F, 30.38F, -2.05F, 0.0F, 0.55F, -0.83F, 0.52F, 0.38F)
                        .roughen(0.05F, 0.3F)
                        .color(-13703425);
        rig.extra("ponytail", Bone.HEAD)
                .pivot(0.0F, hy(30.35F), -2.3779998F)
                .shape(grow(Shapes.union(shape, shape2)))
                .detail(1.0F)
                .resolution(1.3F)
                .outline(-15327158, 0.12F)
                .flatLight(0.4F)
                .occlusion(0.4F)
                .motion(
                        Motion.hair()
                                .idle(3.0F, 0.0F, 2.5F, 0.3F)
                                .walk(7.0F, 0.0F, 5.0F, 2.0F)
                                .drag(22.0F)
                                .hang(0.3F)
                                .follow(Bone.BODY, 0.6F)
                                .turn(18.0F, 4.0F)
                                .spring(36.0F, 4.2F)
                                .bend(1.5F)
                                .limit(70.0F));
    }

    private static void split(List<Shape> list, float[] floats, int i, float f, boolean bl) {
        int j = floats.length / 4;
        if (j < i + 3) {
            list.add(Shapes.tube(floats).flattenFrom(f, 0.0F, 26.0F, 0.5F));
        } else {
            list.add(
                    Shapes.tube(Arrays.copyOfRange(floats, 0, (i + 2) * 4))
                            .flattenFrom(f, 0.0F, 26.0F, 0.5F));
            Object object =
                    Shapes.tube(Arrays.copyOfRange(floats, (i - 1) * 4, j * 4))
                            .flattenFrom(f, 0.0F, 26.0F, 0.5F);
            if (bl) {
                int k = (j - 3) * 4;
                int m = (j - 2) * 4;
                int n = (j - 1) * 4;
                Shape2D shape2D =
                        Shape2D.path(
                                2.4F,
                                0.02F,
                                floats[k],
                                floats[k + 1],
                                floats[m],
                                floats[m + 1],
                                floats[n],
                                floats[n + 1]);
                object =
                        ((Shape) object)
                                .intersect(
                                        Shapes.union(
                                                Shapes.above(floats[m + 1]),
                                                Shapes.extrude(shape2D, 8.0F, 0.0F)));
            }
            list.add((Shape) object);
        }
    }

    private static Shape body(Rig rig, Canvas canvas) {
        Paint paint =
                Paint.field(
                                (value, value2, value3) ->
                                        Colors.mix(-9274, -1331558, smooth(24.9F, 25.5F, value2)))
                        .flatLight(1.0F)
                        .occlusion(0.1F);
        Shape shape =
                Shapes.cone(0.0F, 23.2F, -0.22F, 0.66F, 0.0F, 26.3F, 0.0F, 0.58F).paint(paint);
        Shape shape2 =
                Shapes.blend(
                                1.0F,
                                Shapes.ellipsoid(0.0F, 21.15F, 0.0F, 2.25F, 2.05F, 1.45F),
                                Shapes.ellipsoid(0.0F, 18.7F, 0.0F, 1.78F, 1.75F, 1.22F),
                                Shapes.ellipsoid(0.0F, 16.6F, -0.05F, 2.0F, 1.5F, 1.4F),
                                Shapes.capsule(-2.2F, 22.3F, -0.18F, 0.0F, 22.62F, -0.18F, 0.88F),
                                Shapes.capsule(0.0F, 22.62F, -0.18F, 2.2F, 22.3F, -0.18F, 0.88F))
                        .blend(
                                0.6F,
                                Shapes.ellipsoid(0.82F, 20.85F, 0.55F, 0.88F, 0.82F, 0.85F)
                                        .mirrorX());
        shape2.paint(Paint.vertical(-2498584, 17.2F, -525828, 19.4F).flatLight(0.4F));
        Shape shape3 =
                Shapes.cone(0.0F, 23.0F, -0.2F, 0.9F, 0.0F, 25.3F, -0.12F, 0.74F)
                        .intersect(0.06F, Shapes.below(25.1F))
                        .color(-15263457);
        Shape shape4 = Shapes.ellipsoid(0.0F, 14.95F, -0.05F, 1.95F, 1.2F, 1.35F).color(-15263457);
        Shape shape5 =
                Shapes.blend(
                                1.1F,
                                Shapes.ellipsoid(0.0F, 21.0F, -0.1F, 2.7F, 2.4F, 1.98F),
                                Shapes.capsule(-2.62F, 22.25F, -0.22F, 0.0F, 22.6F, -0.22F, 1.04F),
                                Shapes.capsule(0.0F, 22.6F, -0.22F, 2.62F, 22.25F, -0.22F, 1.04F),
                                oval(-0.1F, 18.3F, 20.3F, 2.45F, 1.97F, 2.55F, 1.97F))
                        .paint(
                                Paint.field(
                                                (value4, value5, value6) ->
                                                        Colors.mix(
                                                                -12959662,
                                                                -1446155,
                                                                smooth(21.72F, 21.78F, value5)))
                                        .flatLight(0.3F)
                                        .occlusion(0.18F));
        Shape shape6 = oval(-0.1F, 17.42F, 18.45F, 2.45F, 2.03F, 2.47F, 2.02F).color(-14210250);
        Shape shape7 =
                Shapes.torus(0.0F, 23.52F, -0.28F, 1.28F, 0.38F)
                        .rotate(16.0F, 0.0F, 0.0F, 0.0F, 23.52F, -0.28F)
                        .color(-14210250);
        Shape shape8 = Shapes.union(shape5, shape6, shape7);
        Shape shape9 = Shapes.subtract(0.06F, shape8, opening(0.0F));
        float frontZ = frontZ(shape8, -1.85F, 18.95F, 6.0F);
        float f = 16.95F;
        float flare = flare(skirtT(f));
        float g = 1.85F + 1.6F * flare + 0.22F;
        float h = 1.5F + 1.4F * flare + 0.22F;
        Shape shape10 = ovalRing(-0.05F, f, 0.42F, g, h, 0.26F).color(-15263457);
        float f2 = -0.05F + h * (float) Math.sqrt(1.0F - 0.3025F / (g * g)) + 0.26F;
        Shape shape11 =
                Shapes.box(0.55F, f, f2 + 0.05F, 0.46F, 0.36F, 0.14F, 0.1F).color(-13749702);
        Part part =
                rig.part(Bone.BODY)
                        .pivot(0.0F, 24.4F, 0.0F)
                        .shape(
                                Shapes.union(
                                        shape, shape3, shape2, shape4, shape9, shape10, shape11))
                        .detail(1.0F)
                        .outline(-15065558, 0.13F)
                        .occlusion(0.3F);
        Canvas canvas2 = backStripe();
        Canvas canvas3 = band();
        part.decal(topGraphic())
                .decal(Decal.back(canvas2, 2.02F, 19.6F, -2.0F, 0.6F, 4.0F).depth(1.4F, 1.4F))
                .decal(Decal.back(canvas2, -2.02F, 19.6F, -2.0F, 0.6F, 4.0F).depth(1.4F, 1.4F))
                .decal(
                        Decal.wrap(
                                        canvas, 0.0F, 18.12F, -0.1F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F,
                                        1.0F, 360.0F, 0.3F)
                                .radius(1.95F, 3.2F))
                .decal(
                        Decal.front(pocket(), -1.85F, 18.95F, frontZ, 0.95F, 0.36F)
                                .depth(0.5F, 0.5F)
                                .maxAngle(70.0F))
                .decal(
                        Decal.front(pocket().mirrored(), 1.85F, 18.95F, frontZ, 0.95F, 0.36F)
                                .depth(0.5F, 0.5F)
                                .maxAngle(70.0F))
                .decal(
                        Decal.front(canvas3, 0.0F, 17.62F, 1.9F, 2.0F, 0.28F)
                                .depth(1.2F, 0.4F)
                                .maxAngle(70.0F))
                .decal(
                        Decal.wrap(
                                        canvas3, 0.0F, 24.8F, -0.14F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F,
                                        1.0F, 240.0F, 0.26F)
                                .radius(0.4F, 1.05F))
                .decal(
                        Decal.front(buckle(), 0.55F, f, f2 + 0.19F, 0.92F, 0.72F)
                                .depth(0.4F, 0.3F)
                                .maxAngle(60.0F));
        Shape shape12 =
                Shapes.union(
                        Shapes.tube(
                                        0.98F,
                                        16.7F,
                                        f2 + 0.02F,
                                        0.28F,
                                        1.02F,
                                        15.8F,
                                        f2 + 0.26F,
                                        0.28F,
                                        1.05F,
                                        14.9F,
                                        f2 + 0.45F,
                                        0.27F,
                                        1.07F,
                                        14.3F,
                                        f2 + 0.55F,
                                        0.26F)
                                .flatten(2.2F, 0.0F, 0.0F, 1.0F)
                                .color(-15263457),
                        Shapes.box(1.07F, 14.15F, f2 + 0.56F, 0.28F, 0.2F, 0.13F, 0.08F)
                                .color(-3550757));
        rig.extra("strap", Bone.BODY)
                .pivot(0.98F, 16.7F, f2)
                .shape(shape12)
                .resolution(0.7F)
                .detail(0.3F)
                .noOutline()
                .motion(
                        Motion.cloth()
                                .walk(8.0F, 0.0F, 5.0F, 2.0F)
                                .drag(6.0F)
                                .spring(40.0F, 5.0F)
                                .bend(1.2F));
        return shape8;
    }

    private static float openingWidth(float f) {
        return 0.85F + 0.65F * (24.9F - f) / 10.1F;
    }

    private static Shape opening(float f) {
        return Shapes.extrude(
                        Shape2D.polygon(
                                -0.85F - f,
                                24.9F,
                                0.85F + f,
                                24.9F,
                                1.5F + f,
                                14.8F,
                                -1.5F - f,
                                14.8F),
                        2.6F,
                        0.0F)
                .move(0.0F, 0.0F, 3.15F);
    }

    private static Shape piping(Shape shape, float f, float g, float h) {
        float[] floats = new float[24];
        for (int i = 0; i < 8; i++) {
            float f2 = g + (h - g) * i / 7.0F;
            float openingWidth = openingWidth(f2);
            floats[i * 3] = f * (openingWidth + 0.09F);
            floats[i * 3 + 1] = f2;
            floats[i * 3 + 2] = frontZ(shape, f * (openingWidth + 0.1F), f2, 6.0F) - 0.06F;
        }
        return Shapes.strand(0.13F, 0.13F, floats);
    }

    private static void skirt(Rig rig, Canvas canvas) {
        Paint paint =
                Paint.field(
                                (value, value2, value3) -> {
                                    float f = (float) Math.atan2(value, value3 - -0.05F);
                                    float pleat = pleat(f * 18.0F / 6.2831855F);
                                    return Colors.mix(
                                            -14472632,
                                            -12892555,
                                            0.4F + 0.6F * (0.5F + 0.5F * pleat));
                                })
                        .flatLight(0.15F);
        Decal decal =
                Decal.wrap(
                                canvas, 0.0F, 11.77F, -0.05F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F,
                                360.0F, 0.27F)
                        .radius(2.4F, 4.6F);
        float g = (float) Math.toRadians(30.0);
        float h = (float) Math.toRadians(14.0);
        String[] strings = new String[] {"skirt_f", "skirt_s", "skirt_b"};
        float[] floats = new float[] {-30.0F, -90.0F, -150.0F};
        float[] floats2 = new float[] {0.5F, 0.42F, 0.5F};
        float[] floats3 = new float[] {16.0F, 13.0F, 15.0F};
        for (int i = 0; i < 3; i++) {
            float f2 = (float) Math.toRadians(floats[i]);
            float g2 = 1.8F * (float) Math.sin(f2);
            float h2 = -0.05F + 1.45F * (float) Math.cos(f2);
            Motion motion =
                    Motion.skirt(Bone.RIGHT_LEG)
                            .follow(Bone.RIGHT_LEG, floats2[i], floats3[i])
                            .drag(i == 0 ? 0.0F : 3.0F);
            Part part =
                    rig.extra(strings[i] + "_r", Bone.BODY)
                            .pivot(g2, 16.9F, h2)
                            .shape(skirtPanel(f2, g, h).paint(paint))
                            .motion(motion)
                            .detail(0.32F)
                            .resolution(1.0F)
                            .occlusion(0.3F)
                            .noOutline()
                            .decal(decal);
            rig.extra(strings[i] + "_l", Bone.BODY).mirrorOf(part);
        }
    }

    private static float skirtT(float f) {
        return clamp01((17.3F - f) / 6.049999F);
    }

    private static float flare(float f) {
        return f * (1.25F - 0.25F * f);
    }

    private static float skirtRadius(float f, float g, float h, float f2, float g2) {
        float skirtT = skirtT(g2);
        float flare = flare(skirtT);
        float h2 = 1.5F + 1.4F * flare;
        float f3;
        float g3 = (float) Math.sqrt(h2 * h2 * f * f + (f3 = 1.85F + 1.6F * flare) * f3 * g * g);
        float h3 = g3 < 1.0E-5F ? f3 : f3 * h2 * h / g3;
        return h3 + (0.05F + 0.24F * skirtT) * pleat(f2 * 18.0F / 6.2831855F);
    }

    private static Shape skirtPanel(float f, float g, float h) {
        float f2 = 0.0F;
        float g2 = 0.0F;
        float h2 = -0.05F;
        float f3 = -0.05F;
        for (int m = 0; m <= 16; m++) {
            float g3 = f - g - h + (g + h) * 2.0F * m / 16.0F;
            for (float h3 : new float[] {1.4F, 4.05F}) {
                f2 = Math.min(f2, h3 * (float) Math.sin(g3));
                g2 = Math.max(g2, h3 * (float) Math.sin(g3));
                h2 = Math.min(h2, -0.05F + h3 * (float) Math.cos(g3));
                f3 = Math.max(f3, -0.05F + h3 * (float) Math.cos(g3));
            }
        }
        return Shapes.field(
                (i, j, k) -> {
                    float f4 = k - -0.05F;
                    float g4 = (float) Math.atan2(i, f4);
                    float h4 = g4 - f;
                    if (h4 > 3.141592653589793) {
                        h4 -= 6.2831855F;
                    }
                    if (h4 < -3.141592653589793) {
                        h4 += 6.2831855F;
                    }
                    float f5 = Math.abs(h4) - g;
                    float g5 = (float) Math.sqrt(i * i + f4 * f4);
                    float h5 = skirtRadius(i, f4, g5, g4, j) - 0.32F * smooth(0.0F, 0.05F, f5);
                    float f6 = Math.abs(g5 - h5) - 0.42F;
                    float g6 = Math.max(j - 17.3F, 11.25F - j);
                    float h6 = Math.max(-1.2F, Math.min(1.2F, f5 - h));
                    float f7 = g5 * h6 * (1.0F - h6 * h6 / 6.0F);
                    return Math.max(f6, Math.max(g6, f7)) * 0.8F;
                },
                f2 - 0.2F,
                11.05F,
                h2 - 0.2F,
                g2 + 0.2F,
                17.5F,
                f3 + 0.2F);
    }

    private static void arms(Rig rig, Canvas canvas) {
        float f = -3.68F;
        float g = 18.4F;
        float h = -0.1F;
        float f2 = f + 3.3F;
        float g2 = g - 21.8F;
        float h2 = h + 0.12F;
        float f3 = (float) Math.sqrt(f2 * f2 + g2 * g2 + h2 * h2);
        Shape shape =
                Shapes.blend(
                        0.45F,
                        Shapes.sphere(-3.22F, 22.2F, -0.15F, 1.1F),
                        Shapes.cone(-3.3F, 21.8F, -0.12F, 1.07F, f, g, h, 0.96F));
        float g3;
        float h3;
        float f4;
        Shape shape2 =
                shape.intersect(
                                Shapes.plane(f, g, h, g3 = f2 / f3, h3 = g2 / f3, f4 = h2 / f3)
                                        .color(-14210250))
                        .paint(
                                Paint.field(
                                                (value, value2, value3) ->
                                                        Colors.mix(
                                                                -1446155,
                                                                -3945254,
                                                                smooth(-3.2F, -2.6F, value) * 0.7F))
                                        .flatLight(0.3F));
        float g4 = f - g3 * 0.05F;
        float h4 = g - h3 * 0.05F;
        float f5 = h - f4 * 0.05F;
        Shape shape3 = ring(g4, h4, f5, g3, h3, f4, 0.86F, 0.34F).color(-14210250);
        Paint paint = Paint.of(-9274).flatLight(1.0F).occlusion(0.25F);
        Shape shape4 =
                Shapes.cone(f, g - 0.1F, h + 0.05F, 0.55F, -3.92F, 14.66F, 0.15F, 0.39F)
                        .paint(paint)
                        .rotate(-13.0F, 0.0F, 0.0F, f, g, h);
        float g5 = -1.5F;
        float h5 = (float) Math.cos(Math.toRadians(g5));
        float f6 = (float) Math.sin(Math.toRadians(g5));
        float g6 = -3.3F + (g4 + 3.3F) * h5 - (h4 - 22.4F) * f6;
        float h6 = 22.4F + (g4 + 3.3F) * f6 + (h4 - 22.4F) * h5;
        float f7 = g3 * h5 - h3 * f6;
        float g7 = g3 * f6 + h3 * h5;
        float[] floats = bent(-4.15F, 12.75F, 0.45F, f, g, h);
        float h7 = -3.3F + (floats[0] + 3.3F) * h5 - (floats[1] - 22.4F) * f6;
        float f8 = 22.4F + (floats[0] + 3.3F) * f6 + (floats[1] - 22.4F) * h5;
        Part part =
                rig.part(Bone.RIGHT_ARM)
                        .pivot(-3.3F, 22.4F, -0.1F)
                        .grip(h7, f8, floats[2])
                        .shape(
                                Shapes.union(shape2, shape3, shape4)
                                        .rotate(0.0F, 0.0F, g5, -3.3F, 22.4F, -0.1F))
                        .outline(-12959918, 0.12F)
                        .occlusion(0.3F)
                        .swing(0.8F)
                        .joint(-3.78F, 18.41F, -0.1F, 0.55F);
        Part part2 = rig.part(Bone.LEFT_ARM).mirrorOf(part);
        part.decal(
                Decal.wrap(canvas, g6, h6, f5, -f7, -g7, -f4, -1.0F, 0.0F, 0.3F, 250.0F, 0.4F)
                        .radius(0.95F, 1.6F));
        part2.decal(
                Decal.wrap(canvas, -g6, h6, f5, f7, -g7, -f4, 1.0F, 0.0F, 0.3F, 250.0F, 0.4F)
                        .radius(0.95F, 1.6F));
        part2.decal(
                Decal.left(sleevePatch(), 4.45F, 20.3F, -0.1F, 1.05F, 1.05F)
                        .depth(0.8F, 0.8F)
                        .maxAngle(65.0F));
        part.decal(
                Decal.right(sleeveText(), -4.5F, 20.2F, -0.1F, 0.62F, 1.75F)
                        .depth(0.8F, 0.8F)
                        .maxAngle(65.0F));
        Part part3 =
                rig.extra("hand_r", Bone.RIGHT_ARM)
                        .shape(
                                hand().rotate(-13.0F, 0.0F, 0.0F, f, g, h)
                                        .rotate(0.0F, 0.0F, g5, -3.3F, 22.4F, -0.1F))
                        .resolution(0.45F)
                        .detail(0.5F)
                        .outline(-6657438, 0.1F)
                        .flatLight(1.0F)
                        .occlusion(0.25F);
        rig.extra("hand_l", Bone.LEFT_ARM).mirrorOf(part3);
        Part part4 =
                rig.extra("wrist_r", Bone.RIGHT_ARM)
                        .shape(
                                Shapes.ellipsoid(-3.97F, 14.12F, 0.16F, 0.31F, 0.5F, 0.37F)
                                        .color(-9274)
                                        .rotate(-13.0F, 0.0F, 0.0F, f, g, h)
                                        .rotate(0.0F, 0.0F, g5, -3.3F, 22.4F, -0.1F))
                        .resolution(0.6F)
                        .detail(1.4F)
                        .outline(-6657438, 0.1F)
                        .flatLight(1.0F)
                        .occlusion(0.25F);
        rig.extra("wrist_l", Bone.LEFT_ARM).mirrorOf(part4);
    }

    private static float[] bent(float f, float g, float h, float f2, float g2, float h2) {
        double d = Math.toRadians(-13.0);
        float f3 = (float) Math.cos(d);
        float g3 = (float) Math.sin(d);
        float h3 = g - g2;
        float f4 = h - h2;
        return new float[] {f, g2 + h3 * f3 - f4 * g3, h2 + h3 * g3 + f4 * f3};
    }

    private static Shape hand() {
        Shape shape = Shapes.ellipsoid(-4.0F, 13.2F, 0.14F, 0.3F, 0.68F, 0.54F);
        Strand strand =
                Shapes.strand(
                        0.2F, 0.15F, -3.86F, 13.35F, 0.6F, -3.72F, 12.9F, 0.92F, -3.62F, 12.5F,
                        0.98F);
        Shape shape2 =
                Shapes.union(
                        Shapes.strand(
                                0.17F, 0.15F, -3.98F, 12.72F, 0.56F, -3.92F, 12.1F, 0.72F, -3.8F,
                                11.66F, 0.8F),
                        Shapes.strand(
                                0.17F, 0.15F, -4.0F, 12.74F, 0.24F, -3.88F, 12.12F, 0.28F, -3.66F,
                                11.8F, 0.3F),
                        Shapes.strand(
                                0.165F, 0.145F, -4.0F, 12.72F, -0.08F, -3.88F, 12.18F, -0.08F,
                                -3.66F, 11.88F, -0.06F),
                        Shapes.strand(
                                0.15F, 0.13F, -3.99F, 12.66F, -0.36F, -3.88F, 12.3F, -0.36F, -3.7F,
                                12.06F, -0.33F));
        return Shapes.union(
                Shapes.blend(0.12F, shape, strand, shape2).color(-9274),
                Shapes.cylinder(-3.95F, 14.22F, 0.18F, -3.93F, 14.9F, 0.15F, 0.5F, 0.1F)
                        .color(-13703425));
    }

    private static void legs(Rig rig) {
        Shape shape = Shapes.cone(-1.24F, 14.1F, 0.0F, 1.3F, -1.1F, 8.3F, 0.1F, 0.84F);
        Shape shape2 = Shapes.sphere(-1.07F, 7.68F, 0.18F, 0.75F);
        Shape shape3 = Shapes.sphere(-1.06F, 7.8F, 0.5F, 0.52F);
        Shape shape4 = Shapes.cone(-1.07F, 7.3F, 0.06F, 0.78F, -1.12F, 2.6F, -0.1F, 0.5F);
        Shape shape5 = Shapes.ellipsoid(-1.12F, 5.8F, -0.2F, 0.72F, 1.5F, 0.74F);
        Shape shape6 = Shapes.sphere(-1.1F, 14.4F, -0.05F, 1.32F);
        Paint paint =
                Paint.field(
                        (value, value2, value3) ->
                                Colors.mix(
                                        Colors.mix(-14012606, -9274, smooth(6.7F, 6.95F, value2)),
                                        -15263457,
                                        smooth(12.2F, 12.4F, value2)));
        Shape shape7 =
                Shapes.union(
                                Shapes.blend(0.5F, shape, shape2, shape4, shape5)
                                        .blend(0.3F, shape3),
                                shape6)
                        .paint(paint);
        Shape shape8 =
                Shapes.union(
                                Shapes.blend(0.5F, shape4, shape5)
                                        .inflate(0.1F)
                                        .intersect(Shapes.below(6.95F)),
                                Shapes.torus(-1.075F, 6.9F, 0.0F, 0.86F, 0.15F))
                        .color(-14012606);
        Part part =
                rig.part(Bone.RIGHT_LEG)
                        .pivot(-1.2F, 14.3F, 0.0F)
                        .shape(shape7)
                        .outline(-11914188, 0.12F)
                        .flatLight(0.7F)
                        .occlusion(0.3F)
                        .swing(0.55F, 30.0F)
                        .joint(-1.07F, 7.68F, 0.18F, 0.75F);
        rig.part(Bone.LEFT_LEG).mirrorOf(part);
        Part part2 =
                rig.extra("sock_r", Bone.RIGHT_LEG)
                        .shape(shape8)
                        .resolution(0.7F)
                        .detail(0.45F)
                        .outline(-15460836, 0.12F);
        Part part3 = rig.extra("sock_l", Bone.LEFT_LEG).mirrorOf(part2);
        Canvas canvas = socks();
        part2.decal(
                Decal.wrap(
                                canvas, -1.1F, 6.42F, -0.05F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F,
                                360.0F, 0.3F)
                        .radius(0.3F, 1.4F));
        part3.decal(
                Decal.wrap(
                                canvas, 1.1F, 6.42F, -0.05F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F,
                                360.0F, 0.3F)
                        .radius(0.3F, 1.4F));
        float f = -1.14F;
        float g = -7.0F;
        Part part4 =
                rig.extra("shoe_r", Bone.RIGHT_LEG)
                        .shape(shoe(f).rotate(0.0F, g, 0.0F, f, 1.0F, 0.3F))
                        .resolution(0.8F)
                        .detail(0.55F)
                        .outline(-12959664, 0.12F);
        Part part5 = rig.extra("shoe_l", Bone.LEFT_LEG).mirrorOf(part4);
        Canvas canvas2 = laces();
        double d = Math.toRadians(g);
        float h = (float) Math.cos(d);
        float f2 = (float) Math.sin(d);
        float g2 = f + 0.65F * f2;
        float h2 = 0.3F + 0.65F * h;
        part4.decal(
                Decal.project(
                                canvas2,
                                g2,
                                2.1F,
                                h2,
                                0.87F * f2,
                                0.5F,
                                0.87F * h,
                                -0.5F * f2,
                                0.87F,
                                -0.5F * h,
                                0.8F,
                                1.28F)
                        .depth(0.7F, 0.7F)
                        .maxAngle(75.0F));
        part5.decal(
                Decal.project(
                                canvas2,
                                -g2,
                                2.1F,
                                h2,
                                -0.87F * f2,
                                0.5F,
                                0.87F * h,
                                0.5F * f2,
                                0.87F,
                                -0.5F * h,
                                0.8F,
                                1.28F)
                        .depth(0.7F, 0.7F)
                        .maxAngle(75.0F));
    }

    private static Shape shoe(float f) {
        Shape shape =
                Shapes.blend(
                        0.35F,
                        Shapes.box(f, 0.42F, 0.4F, 0.88F, 0.42F, 1.45F, 0.36F),
                        Shapes.ellipsoid(f, 0.44F, 1.62F, 0.88F, 0.44F, 0.95F),
                        Shapes.ellipsoid(f, 0.44F, -0.85F, 0.84F, 0.44F, 0.7F));
        Shape shape2 = Shapes.ellipsoid(f, 1.12F, 1.35F, 0.76F, 0.58F, 1.1F);
        Shape shape3 = Shapes.capsule(f, 1.3F, 0.85F, f, 2.05F, 0.1F, 0.7F);
        Shape shape4 = Shapes.ellipsoid(f, 1.45F, -0.62F, 0.8F, 0.82F, 0.82F);
        Shape shape5 =
                Shapes.union(
                        Shapes.cone(f, 1.8F, -0.3F, 0.84F, f, 3.5F, -0.17F, 0.74F)
                                .intersect(Shapes.below(3.2F)),
                        Shapes.torus(f, 3.18F, -0.19F, 0.66F, 0.15F));
        Shape shape6 = Shapes.ellipsoid(f, 3.05F, 0.42F, 0.46F, 0.72F, 0.3F);
        Shape shape7 = Shapes.blend(0.4F, shape2, shape3, shape4, shape5).blend(0.15F, shape6);
        Paint paint =
                Paint.field(
                        (fx, value, value2) ->
                                value < 0.9F
                                        ? Colors.mix(
                                                -14012872, -920586, smooth(0.05F, 0.24F, value))
                                        : Colors.mix(
                                                -591621, -14210250, smooth(2.8F, 3.05F, value)));
        return Shapes.union(shape, shape7).paint(paint);
    }

    private static void gear(Rig rig, Shape shape) {
        ArrayList list = new ArrayList();
        Canvas canvas = phoneRing();
        Part part = rig.extra("gear", Bone.BODY);
        for (int i = -1; i <= 1; i += 2) {
            float f = i * 0.35F;
            float g = 0.45F;
            float h = 0.82F;
            float f2 = (float) Math.sqrt(f * f + g * g + h * h);
            f /= f2;
            g /= f2;
            h /= f2;
            float g2 = i * 1.55F;
            float h2 = 23.5F;
            float f3 = 1.12F;
            float g3 = g;
            // final copies of the normalized axis for the lambda below
            float h3 = f;
            float f4 = h;
            Shape shape2 =
                    Shapes.cylinder(
                                    g2 - f * 0.1F,
                                    h2 - g * 0.1F,
                                    f3 - h * 0.1F,
                                    g2 + f * 0.16F,
                                    h2 + g * 0.16F,
                                    f3 + h * 0.16F,
                                    0.86F,
                                    0.12F)
                            .paint(
                                    Paint.field(
                                            (value, m, value2) -> {
                                                float g4 =
                                                        (value - g2) * h3
                                                                + (m - h2) * g3
                                                                + (value2 - f3) * f4;
                                                return Colors.mix(
                                                        -14210250,
                                                        -723206,
                                                        smooth(0.03F, 0.1F, g4));
                                            }));
            Shape shape3 =
                    Shapes.cylinder(
                                    g2 - f * 0.36F,
                                    h2 - g * 0.36F,
                                    f3 - h * 0.36F,
                                    g2 - f * 0.08F,
                                    h2 - g * 0.08F,
                                    f3 - h * 0.08F,
                                    0.78F,
                                    0.1F)
                            .color(-14210250);
            Shape shape4 =
                    Shapes.capsule(
                                    g2 - f * 0.1F - i * 0.1F,
                                    h2 + 0.8F,
                                    f3 - h * 0.1F - 0.2F,
                                    i * 1.3F,
                                    24.4F,
                                    0.5F,
                                    0.2F)
                            .color(-14210250);
            list.add(Shapes.union(shape2, shape3, shape4));
            part.decal(
                    Decal.project(
                                    canvas,
                                    g2 + f * 0.17F,
                                    h2 + g * 0.17F,
                                    f3 + h * 0.17F,
                                    f,
                                    g,
                                    h,
                                    0.0F,
                                    1.0F,
                                    0.0F,
                                    1.3F,
                                    1.3F)
                            .depth(0.4F, 0.4F)
                            .maxAngle(70.0F)
                            .glow());
        }
        list.add(
                Shapes.tube(
                                1.3F, 24.4F, 0.5F, 0.24F, 1.22F, 24.85F, -0.35F, 0.26F, 0.72F,
                                25.02F, -1.2F, 0.26F, 0.0F, 25.08F, -1.42F, 0.26F, -0.72F, 25.02F,
                                -1.2F, 0.26F, -1.22F, 24.85F, -0.35F, 0.26F, -1.3F, 24.4F, 0.5F,
                                0.24F)
                        .flattenFrom(1.8F, 0.0F, 24.9F, -0.2F)
                        .color(-14210250));
        list.add(
                Shapes.union(piping(shape, 1.0F, 23.3F, 17.5F), piping(shape, -1.0F, 23.3F, 17.5F))
                        .color(-13703425));
        part.shape(Shapes.union(((Shape[]) list.toArray(new Shape[0]))))
                .resolution(0.7F)
                .detail(0.7F)
                .outline(-15065820, 0.1F)
                .flatLight(0.7F)
                .occlusion(0.15F);
        float[] floats = new float[] {1.75F, 22.65F, 1.85F, 21.8F, 1.92F, 20.9F, 1.95F, 20.15F};
        float[] floats2 = new float[12];
        for (int j = 0; j < 4; j++) {
            floats2[j * 3] = floats[j * 2];
            floats2[j * 3 + 1] = floats[j * 2 + 1];
            floats2[j * 3 + 2] =
                    j == 0 ? 1.3F : frontZ(shape, floats[j * 2], floats[j * 2 + 1], 6.0F) + 0.16F;
        }
        float h4 = floats2[11];
        Shape shape5 =
                Shapes.union(
                        Shapes.strand(0.13F, 0.13F, floats2).color(-7563613),
                        Shapes.capsule(1.95F, 20.2F, h4, 1.96F, 19.65F, h4 + 0.02F, 0.19F)
                                .color(-12959662),
                        Shapes.capsule(1.96F, 19.6F, h4 + 0.02F, 1.96F, 19.3F, h4 + 0.03F, 0.12F)
                                .color(-3550757));
        rig.extra("cable", Bone.BODY)
                .pivot(1.75F, 22.65F, 1.3F)
                .shape(shape5)
                .resolution(0.5F)
                .detail(0.25F)
                .noOutline()
                .motion(
                        Motion.cloth()
                                .idle(1.0F, 0.0F, 1.5F, 0.35F)
                                .walk(4.0F, 0.0F, 5.0F, 2.0F)
                                .drag(-2.0F)
                                .hang(0.4F)
                                .spring(45.0F, 5.0F)
                                .bend(1.1F));
    }

    private static float fu(float f) {
        return (f + 2.25F) * 80.0F;
    }

    private static float fv(float f) {
        return (28.6F - f) * 80.0F;
    }

    private static Decal face(List<float[]> list) {
        Canvas canvas = new Canvas(Math.round(360.0F), Math.round(201.60004F));
        for (float[] floats : list) {
            Shape2D shape2D = null;
            for (int i = 0; i + 7 < floats.length; i += 4) {
                if (!(floats[i + 1] > 29.2F) || !(floats[i + 5] > 29.2F)) {
                    Shape2D shape2D2 =
                            Shape2D.taper(
                                    fu(floats[i]),
                                    fv(floats[i + 1] - 0.22F),
                                    floats[i + 3] * 80.0F * 1.7F,
                                    fu(floats[i + 4]),
                                    fv(floats[i + 5] - 0.22F),
                                    Math.max(2.0F, floats[i + 7] * 80.0F * 1.5F));
                    shape2D = shape2D == null ? shape2D2 : shape2D.union(shape2D2);
                }
            }
            if (shape2D != null) {
                canvas.fill(shape2D, 2028380826, 3.0F);
            }
        }
        for (int j = -1; j <= 1; j += 2) {
            eye(canvas, j);
        }
        canvas.fill(
                Shape2D.taper(fu(0.06F), fv(26.8F), 4.0F, fu(-0.02F), fv(26.64F), 2.0F),
                -1714123650,
                0.0F);
        canvas.fill(
                Shape2D.curve(
                        fu(-0.22F),
                        fv(26.2F),
                        fu(0.0F),
                        fv(26.1F),
                        fu(0.2F),
                        fv(26.23F),
                        3.4F,
                        2.2F),
                -7455931,
                0.0F);
        for (int k = -1; k <= 1; k += 2) {
            canvas.fill(
                    Shape2D.ellipse(fu(k * 1.25F), fv(26.42F), 32.0F, 11.0F), 1442807955, 10.0F);
        }
        float f = 27.34F;
        return Decal.front(canvas, 0.0F, hy(f), 3.132F, 5.22F, 2.9232004F)
                .depth(3.0159998F, 0.8F)
                .maxAngle(72.0F)
                .flatLight(1.0F)
                .occlusion(0.3F);
    }

    private static void eye(Canvas canvas, int i) {
        float fu = fu(i * 0.98F);
        float fv = fv(27.2F);
        float f = fu - i * 2.0F;
        float g = fv + 4.0F;
        float h = fu - i * 46.0F;
        float f2 = fu + i * 47.0F;
        float g2 = fv - 40.0F;
        float h2 = fv - 50.0F;
        float f3 = fu - i * 2.0F;
        float g3 = fv - 49.0F;
        Shape2D shape2D =
                Shape2D.polygon(
                        h - i * 30.0F,
                        g2 + 2.0F,
                        h,
                        g2,
                        f3,
                        g3,
                        f2,
                        h2,
                        f2 + i * 30.0F,
                        h2,
                        f2 + i * 30.0F,
                        fv + 90.0F,
                        h - i * 30.0F,
                        fv + 90.0F);
        Shape2D shape2D2 = Shape2D.ellipse(fu, fv - 2.0F, 47.0F, 53.0F).intersect(shape2D);
        canvas.fill(shape2D2, Paint2D.linear(fu, fv - 50.0F, fu, fv + 50.0F, -2498062, -1, -1));
        Shape2D shape2D3 = Shape2D.ellipse(f, g, 32.0F, 43.0F);
        Shape2D shape2D4 = shape2D3.intersect(shape2D2);
        canvas.fill(
                shape2D4,
                Paint2D.linear(
                        f, g - 43.0F, f, g + 43.0F, -16050107, -15449954, -14711060, -10687489));
        canvas.fill(
                Shape2D.ellipse(f, g + 25.0F, 22.0F, 14.0F).intersect(shape2D4), -1871645953, 6.0F);
        canvas.fill(Shape2D.ellipse(f, g + 3.0F, 12.0F, 19.0F), -16315098, 0.0F);
        canvas.fill(shape2D3.outline(4.5F).intersect(shape2D2), -536210893, 0.0F);
        canvas.fill(
                Shape2D.ellipse(fu, fv - 44.0F, 62.0F, 20.0F).intersect(shape2D2),
                1714438256,
                8.0F);
        canvas.fill(
                Shape2D.box(f - 12.0F, g - 15.0F, 6.5F, 10.5F, 3.5F)
                        .rotate(-14.0F, f - 12.0F, g - 15.0F),
                -1,
                0.0F);
        canvas.fill(
                Shape2D.box(f - 2.0F, g - 20.0F, 2.2F, 5.0F, 1.5F)
                        .rotate(-14.0F, f - 2.0F, g - 20.0F),
                -855638017,
                0.0F);
        canvas.fill(Shape2D.circle(f + 13.0F, g + 21.0F, 4.2F), -570425345, 0.0F);
        int j = -15724002;
        canvas.fill(Shape2D.path(5.0F, 9.0F, h, g2 + 1.0F, f3, g3 - 1.0F, f2, h2), j, 0.0F);
        canvas.fill(
                Shape2D.taper(f2 - i * 6.0F, h2 + 1.0F, 7.5F, f2 + i * 12.0F, h2 - 7.0F, 1.5F),
                j,
                0.0F);
        canvas.fill(
                Shape2D.curve(
                        fu + i * 6.0F,
                        fv + 51.0F,
                        fu + i * 32.0F,
                        fv + 46.0F,
                        f2 - i * 3.0F,
                        fv + 22.0F,
                        1.2F,
                        3.0F),
                -1072030672,
                0.0F);
        canvas.fill(
                Shape2D.curve(
                        fu - i * 40.0F,
                        fv - 72.0F,
                        fu + i * 2.0F,
                        fv - 80.0F,
                        fu + i * 42.0F,
                        fv - 72.0F,
                        3.6F,
                        2.0F),
                -14011280,
                0.0F);
    }

    private static Canvas stripe() {
        Canvas canvas = new Canvas(64, 10);
        canvas.fill(Shape2D.box(32.0F, 3.3F, 40.0F, 2.8F, 0.0F), -13703425);
        canvas.fill(Shape2D.box(32.0F, 8.2F, 40.0F, 0.8F, 0.0F), -1);
        return canvas;
    }

    private static Canvas band() {
        Canvas canvas = new Canvas(64, 8);
        canvas.fill(Shape2D.box(32.0F, 4.0F, 40.0F, 3.0F, 0.0F), -13703425);
        return canvas;
    }

    private static Canvas socks() {
        Canvas canvas = new Canvas(64, 12);
        canvas.fill(Shape2D.box(32.0F, 2.5F, 40.0F, 2.0F, 0.0F), -13703425);
        canvas.fill(Shape2D.box(32.0F, 9.5F, 40.0F, 2.0F, 0.0F), -13703425);
        return canvas;
    }

    private static Canvas phoneRing() {
        Canvas canvas = new Canvas(96, 96);
        canvas.fill(Shape2D.circle(48.0F, 48.0F, 33.0F).outline(5.0F), -13703425);
        return canvas;
    }

    private static Decal topGraphic() {
        Canvas canvas = new Canvas(152, 120);
        canvas.fill(Shape2D.circle(76.0F, 60.0F, 42.0F).outline(10.0F), -13703425);
        float[] floats =
                new float[] {16.0F, 30.0F, 50.0F, 72.0F, 94.0F, 72.0F, 50.0F, 30.0F, 16.0F};
        for (int i = 0; i < floats.length; i++) {
            canvas.fill(
                    Shape2D.box(76.0F + (i - 4) * 15.0F, 60.0F, 4.6F, floats[i] * 0.5F, 3.0F),
                    -14932394);
        }
        return Decal.front(canvas, 0.0F, 20.3F, 1.9F, 1.7F, 1.34F)
                .depth(0.75F, 0.45F)
                .maxAngle(62.0F);
    }

    private static Canvas backStripe() {
        Canvas canvas = new Canvas(12, 80);
        canvas.fill(Shape2D.box(6.0F, 40.0F, 3.1F, 39.0F, 0.9F), -13703425);
        canvas.fill(Shape2D.box(0.9F, 40.0F, 0.7F, 39.0F, 0.35F), -1512206);
        canvas.fill(Shape2D.box(11.1F, 40.0F, 0.7F, 39.0F, 0.35F), -1512206);
        return canvas;
    }

    private static Canvas buckle() {
        Canvas canvas = new Canvas(64, 48);
        canvas.fill(
                Shape2D.box(32.0F, 24.0F, 31.0F, 23.0F, 7.0F),
                Paint2D.linear(0.0F, 2.0F, 0.0F, 46.0F, -1643535, -7563613));
        canvas.fill(Shape2D.box(32.0F, 24.0F, 21.0F, 12.0F, 4.0F), -14276045);
        canvas.fill(Shape2D.box(32.0F, 24.0F, 14.0F, 3.5F, 1.5F), -13703425);
        canvas.fill(Shape2D.box(8.5F, 24.0F, 2.0F, 9.0F, 1.0F), -14276045);
        canvas.fill(Shape2D.box(55.5F, 24.0F, 2.0F, 9.0F, 1.0F), -14276045);
        return canvas;
    }

    private static Canvas pocket() {
        Canvas canvas = new Canvas(64, 24);
        canvas.fill(Shape2D.box(32.0F, 12.0F, 28.0F, 3.4F, 3.0F), -15460837);
        canvas.fill(Shape2D.box(32.0F, 12.0F, 25.0F, 0.8F, 0.4F), -13703425);
        canvas.fill(Shape2D.box(54.0F, 15.5F, 2.2F, 5.0F, 1.2F), -3550757);
        return canvas;
    }

    private static Canvas sleeveText() {
        float f = 0.43F;
        Canvas canvas = new Canvas(41, 117);
        Shape2D shape2D =
                Shape2D.union(
                        Shape2D.line(26.0F, 86.0F, 48.0F, 14.0F, 13.0F),
                        Shape2D.line(48.0F, 14.0F, 70.0F, 86.0F, 13.0F),
                        Shape2D.line(36.0F, 62.0F, 60.0F, 62.0F, 10.0F));
        Shape2D shape2D2 = Shape2D.ellipse(48.0F, 136.0F, 22.0F, 34.0F).outline(13.0F);
        Shape2D shape2D3 =
                Shape2D.union(
                        Shape2D.line(48.0F, 190.0F, 48.0F, 258.0F, 13.0F),
                        Shape2D.line(32.0F, 190.0F, 64.0F, 190.0F, 9.0F),
                        Shape2D.line(32.0F, 258.0F, 64.0F, 258.0F, 9.0F));
        Shape2D shape2D4 = Shape2D.union(shape2D, shape2D2, shape2D3);
        ink(canvas, f, shape2D4.move(3.0F, 3.0F), -13703425, 0.0F);
        ink(canvas, f, shape2D4, -14932394, 0.0F);
        return canvas;
    }

    private static Canvas sleevePatch() {
        float f = 0.625F;
        Canvas canvas = new Canvas(80, 80);
        ink(canvas, f, Shape2D.circle(64.0F, 64.0F, 60.0F), -14210250, 0.0F);
        ink(canvas, f, Shape2D.circle(64.0F, 64.0F, 52.0F).outline(6.0F), -13703425, 0.0F);
        ink(canvas, f, Shape2D.arc(64.0F, 70.0F, 30.0F, 180.0F, 360.0F, 8.0F), -591621, 0.0F);
        ink(canvas, f, Shape2D.box(36.0F, 78.0F, 8.0F, 14.0F, 5.0F), -591621, 0.0F);
        ink(canvas, f, Shape2D.box(92.0F, 78.0F, 8.0F, 14.0F, 5.0F), -591621, 0.0F);
        return canvas;
    }

    private static Canvas laces() {
        float f = 0.5F;
        Canvas canvas = new Canvas(48, 80);
        for (int i = 0; i < 4; i++) {
            float g = 30.0F + i * 32.0F;
            ink(canvas, f, Shape2D.line(20.0F, g, 76.0F, g + 16.0F, 9.0F), -13703425, 0.0F);
            ink(canvas, f, Shape2D.line(76.0F, g, 20.0F, g + 16.0F, 9.0F), -13703425, 0.0F);
            ink(canvas, f, Shape2D.circle(14.0F, g, 5.0F), -3550757, 0.0F);
            ink(canvas, f, Shape2D.circle(82.0F, g, 5.0F), -3550757, 0.0F);
        }
        return canvas;
    }

    private static void ink(Canvas canvas, float f, Shape2D shape2D, int i, float g) {
        canvas.fill(shape2D.scale(f, 0.0F, 0.0F), i, g * f);
    }

    private static float frontZ(Shape shape, float f, float g, float h) {
        float f2 = h;
        for (int i = 0; i < 400 && f2 > -8.0F; i++) {
            float distance = shape.distance(f, g, f2);
            if (distance < 0.003F) {
                return f2;
            }
            f2 -= Math.max(0.005F, distance * 0.7F);
        }
        return f2;
    }

    private static Shape ring(
            float f, float g, float h, float f2, float g2, float h2, float f3, float g3) {
        float h3 = (float) Math.sqrt(f2 * f2 + g2 * g2 + h2 * h2);
        float f4 = f2 / h3;
        float g4 = g2 / h3;
        float h4 = h2 / h3;
        Shape shape = Shapes.torus(f, g, h, f3, g3);
        if (f4 * f4 + h4 * h4 < 1.0E-8F) {
            return shape;
        } else {
            float f5 = (float) Math.toDegrees(Math.acos(Math.max(-1.0F, Math.min(1.0F, g4))));
            return shape.rotateAxis(h4, 0.0F, -f4, f5, f, g, h);
        }
    }

    private static Shape oval(float f, float g, float h, float f2, float g2, float h2, float f3) {
        float g3 = Math.max(f2, h2);
        float h3 = Math.max(g2, f3);
        return Shapes.field(
                (m, value, o) -> {
                    float f4 = clamp01((value - g) / (h - g));
                    float ellipse = ellipse(m, o - f, f2 + (h2 - f2) * f4, g2 + (f3 - g2) * f4);
                    return Math.max(ellipse * 0.95F, Math.max(g - value, value - h));
                },
                -g3,
                g,
                f - h3,
                g3,
                h,
                f + h3);
    }

    private static Shape ovalRing(float f, float g, float h, float f2, float g2, float h2) {
        float f3 = Math.min(h2, h) * 0.5F;
        return Shapes.field(
                (m, value, o) -> {
                    float g3 = Math.abs(ellipse(m, o - f, f2, g2)) - h2 + f3;
                    float h3 = Math.abs(value - g) - h + f3;
                    float f4 = Math.max(g3, 0.0F);
                    float g4 = Math.max(h3, 0.0F);
                    return ((float) Math.sqrt(f4 * f4 + g4 * g4)
                                    + Math.min(Math.max(g3, h3), 0.0F)
                                    - f3)
                            * 0.95F;
                },
                -f2 - h2,
                g - h,
                f - g2 - h2,
                f2 + h2,
                g + h,
                f + g2 + h2);
    }

    private static float ellipse(float f, float g, float h, float f2) {
        float g2 = (float) Math.sqrt(f * f / (h * h) + g * g / (f2 * f2));
        float h2 = (float) Math.sqrt(f * f / (h * h * h * h) + g * g / (f2 * f2 * f2 * f2));
        return h2 < 1.0E-6F ? -Math.min(h, f2) : g2 * (g2 - 1.0F) / h2;
    }

    private static float pleat(float f) {
        float tri = tri(f);
        return tri * (1.5F - 0.5F * tri * tri);
    }

    private static float tri(float f) {
        float g = f - (float) Math.floor(f);
        return 4.0F * Math.min(g, 1.0F - g) - 1.0F;
    }

    private static float clamp01(float f) {
        return f < 0.0F ? 0.0F : Math.min(f, 1.0F);
    }

    private static float smooth(float f, float g, float h) {
        float f2 = clamp01((h - f) / (g - f));
        return f2 * f2 * (3.0F - 2.0F * f2);
    }
}
