/*
 * Decompiled with CFR 0.152.
 */
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
import net.efn.client.module.impl.render.character.kit.Vec3;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.ArrayList;

@Environment(value = EnvType.CLIENT)
public final class Mika extends CharacterDesign {

    private static final int SKIN = -9532;

    private static final int SKIN_SHADE = -1331558;

    private static final int SKIN_LINE = -5740966;

    private static final int HAIR = -681403;

    private static final int HAIR_DARK = -3647457;

    private static final int HAIR_TIP = -15750;

    private static final int HAIR_LINE = -6142180;

    private static final int EAR_TIP = -4697060;

    private static final int EAR_PINK = -22088;

    private static final int TUFT = -5162;

    private static final int HOODIE = -4200487;

    private static final int HOODIE_SHADE = -7092297;

    private static final int RIB = -5710645;

    private static final int CUFF_IN = -9461870;

    private static final int HOODIE_LINE = -10774650;

    private static final int LINING = -744532;

    private static final int SKIRT = -12892555;

    private static final int SKIRT_DARK = -14275250;

    private static final int SKIRT_LINE = -14999232;

    private static final int TIGHTS = -12965330;

    private static final int TIGHTS_SHEEN = -9808288;

    private static final int TIGHTS_LINE = -15068908;

    private static final int SHOE = -542005;

    private static final int SOLE = -264207;

    private static final int MINT = -5839921;

    private static final int FISH = -8398652;

    private static final int FISH_LINE = -12811148;

    private static final int SHOE_LINE = -6659470;

    private static final int GOLD = -867761;

    private static final int TAIL_TIP = -3618;

    private static final int LASH = -13887470;

    private static final int BROW = -4691398;

    private static final float HX = 0.0f;

    private static final float HY = 27.95f;

    private static final float HZ = -0.15f;

    private static final float RX = 2.3f;

    private static final float RY = 2.3f;

    private static final float RZ = 2.35f;

    private static final float DROP = 0.4f;

    private static final float HS = 1.12f;

    private static final float NY = 24.9f;

    private static final float NZ = -0.3f;

    private static final float PPU = 55.652176f;

    private static final float TOP = 29.0f;

    private static final int PLEATS = 16;

    private static final float[][] FRINGE =
            new float[][] {
                {1.0f, -1.0f, 30.15f, -1.5f, 29.6f, -2.0f, 28.3f, -1.8f, 27.0f, 0.3f},
                {0.95f, -0.42f, 30.25f, -0.75f, 29.75f, -1.05f, 28.6f, -0.85f, 27.9f, -0.35f},
                {1.05f, -0.28f, 30.3f, 0.0f, 29.8f, 0.5f, 28.4f, 0.42f, 27.2f, 0.4f},
                {0.95f, 0.55f, 30.25f, 0.9f, 29.75f, 1.25f, 28.6f, 1.02f, 27.65f, -0.3f},
                {1.0f, 1.1f, 30.15f, 1.6f, 29.6f, 2.05f, 28.3f, 1.85f, 26.75f, 0.35f},
                {0.65f, -0.75f, 30.2f, -1.2f, 29.6f, -1.5f, 28.3f, -1.3f, 27.45f, 0.3f},
                {0.5f, 0.28f, 30.25f, 0.6f, 29.7f, 0.88f, 28.5f, 0.75f, 27.5f, -0.3f},
                {0.5f, -0.45f, 30.25f, -0.4f, 29.7f, -0.3f, 28.3f, -0.18f, 27.6f, 0.3f}
            };

    private static final float[][] CROWN =
            new float[][] {
                {-0.413f, 0.766f, 0.492f},
                {-0.992f, 0.087f, -0.087f},
                {-0.539f, -0.342f, -0.77f},
                {0.464f, -0.375f, -0.803f},
                {0.975f, 0.139f, -0.172f},
                {0.353f, 0.788f, 0.504f}
            };

    public Mika() {
        super("mika", "Mika");
    }

    @Override
    protected void define(Rig rig) {
        rig.scale(1.0f).outline(-12967400, 0.12f).occlusion(0.3f);
        Mika.body(rig);
        Mika.head(rig);
        Mika.hair(rig);
        Mika.ears(rig);
        Mika.arms(rig);
        Mika.legs(rig);
        Mika.tail(rig);
    }

    private static Shape toHead(Shape shape) {
        return shape.move(0.0f, -0.4f, 0.0f).scale(1.12f, 0.0f, 24.9f, -0.3f);
    }

    private static float wx(float f) {
        return f * 1.12f;
    }

    private static float wy(float f) {
        return 24.9f + (f - 0.4f - 24.9f) * 1.12f;
    }

    private static float wz(float f) {
        return -0.3f + (f - -0.3f) * 1.12f;
    }

    private static float u(float f) {
        return 128.0f + f * 55.652176f;
    }

    private static float v(float f) {
        return (29.0f - f) * 55.652176f;
    }

    private static float capZ(float f, float g, float h) {
        float f2 = f / (2.72f + h);
        float g2 = (g - 28.25f) / (2.8f + h);
        return -0.28f + (2.85f + h) * (float) Math.sqrt(Math.max(0.02f, 1.0f - f2 * f2 - g2 * g2));
    }

    private static float[] onCap(float f, float g, float h, float f2) {
        float g2 = (float) Math.sqrt(f * f + g * g + h * h);
        float h2 = (f /= g2) / (2.72f + f2);
        float f3 = (g /= g2) / (2.8f + f2);
        float g3 = (h /= g2) / (2.85f + f2);
        float h3 = 1.0f / (float) Math.sqrt(h2 * h2 + f3 * f3 + g3 * g3);
        return new float[] {f * h3, 28.25f + g * h3, -0.28f + h * h3};
    }

    private static Shape crownClump(float[] floats, float f) {
        float g = 0.15f;
        float h = 0.93f;
        float f2 = -0.35f;
        float[] floats2 = new float[16];
        float[] floats3 = new float[] {0.0f, 0.38f, 0.72f, 1.0f};
        float[] floats4 = new float[] {0.2f, 0.85f * f, 0.8f * f, 0.3f};
        float[] floats5 = new float[] {-0.12f, -0.07f, -0.07f, -0.14f};
        for (int i = 0; i < 4; ++i) {
            float[] floats6 =
                    Mika.onCap(
                            g + (floats[0] - g) * floats3[i],
                            h + (floats[1] - h) * floats3[i],
                            f2 + (floats[2] - f2) * floats3[i],
                            floats5[i]);
            floats2[i * 4] = floats6[0];
            floats2[i * 4 + 1] = floats6[1];
            floats2[i * 4 + 2] = floats6[2];
            floats2[i * 4 + 3] = floats4[i];
        }
        return Shapes.tube(floats2).flattenFrom(3.2f, 0.0f, 28.25f, -0.28f);
    }

    private static float surf(float f, float g, float h) {
        float f2 = f / (2.3f + h);
        float g2 = (g - 27.95f) / (2.3f + h);
        return -0.15f + (2.35f + h) * (float) Math.sqrt(Math.max(0.02f, 1.0f - f2 * f2 - g2 * g2));
    }

    private static void head(Rig rig) {
        Shape shape = Shapes.ellipsoid(0.0f, 27.95f, -0.15f, 2.3f, 2.3f, 2.35f);
        Shape shape2 = Shapes.ellipsoid(0.0f, 26.72f, 0.35f, 1.93f, 1.3f, 1.7f);
        Shape shape3 = Shapes.cone(0.0f, 26.2f, 0.45f, 1.2f, 0.0f, 25.25f, 0.98f, 0.3f);
        Shape shape4 =
                Shapes.union(
                        Shapes.box(0.0f, 29.0f, -1.8f, 3.0f, 2.3f, 1.5f),
                        Shapes.box(0.0f, 30.3f, 0.5f, 3.0f, 1.3f, 3.0f));
        Shape shape5 =
                Shapes.subtract(Shapes.blend(0.7f, shape, shape2, shape3), shape4).color(-9532);
        rig.part(Bone.HEAD)
                .pivot(0.0f, 24.9f, -0.3f)
                .shape(Mika.toHead(shape5))
                .detail(1.5f)
                .flatLight(1.0f)
                .occlusion(0.2f)
                .outline(-5740966, 0.1f)
                .decal(Mika.face());
        Paint paint =
                Paint.vertical(-9532, 23.7f, -1331558, 24.3f).flatLight(1.0f).occlusion(0.25f);
        rig.extra("neck", Bone.BODY)
                .shape(Shapes.capsule(0.0f, 23.1f, -0.28f, 0.0f, 25.2f, -0.2f, 0.62f).paint(paint))
                .outline(-5740966, 0.08f);
    }

    private static void hair(Rig rig) {
        int j;
        Paint paint = Paint.field(Mika::hairColor);
        Shape shape =
                Shapes.ellipsoid(0.0f, 28.25f, -0.28f, 2.72f, 2.8f, 2.85f)
                        .intersect(Shapes.plane(0.0f, 27.4f, 1.2f, 0.0f, -0.6f, 1.0f))
                        .intersect(Shapes.above(26.4f));
        Shape[] shapes = new Shape[CROWN.length];
        for (int i = 0; i < CROWN.length; ++i) {
            shapes[i] = Mika.crownClump(CROWN[i], i % 2 == 0 ? 1.0f : 0.9f);
        }
        Shape shape2 = Shapes.blend(0.3f, shape, Shapes.union(shapes));
        Part part =
                rig.extra("hair", Bone.HEAD)
                        .shape(
                                Mika.toHead(
                                        Shapes.union(
                                                        shape2,
                                                        Mika.backLock(180.0f, 24.95f, 1.05f, 0.25f))
                                                .paint(paint)))
                        .detail(0.9f)
                        .resolution(0.95f)
                        .flatLight(0.5f)
                        .occlusion(0.3f)
                        .outline(-6142180, 0.09f);
        float[][] floatGrid =
                new float[][] {
                    {-54.0f, 25.95f, 0.9f, 0.3f},
                    {-75.0f, 25.45f, 1.0f, -0.3f},
                    {-97.0f, 25.8f, 1.05f, 0.25f},
                    {-119.0f, 25.3f, 1.05f, -0.2f},
                    {-140.0f, 25.5f, 1.05f, 0.3f},
                    {-161.0f, 25.1f, 1.0f, -0.3f}
                };
        ArrayList<Shape> list = new ArrayList<Shape>();
        float[][] floatGrid2 = floatGrid;
        int k = floatGrid2.length;
        for (j = 0; j < k; ++j) {
            float[] floats = floatGrid2[j];
            list.add(Mika.backLock(floats[0], floats[1], floats[2], floats[3]));
        }
        list.add(Mika.topLock(-84.0f, 26.25f));
        list.add(Mika.topLock(-128.0f, 26.5f));
        Part part2 =
                rig.extra("hair_r", Bone.HEAD)
                        .pivot(0.0f, Mika.wy(29.8f), Mika.wz(-0.4f))
                        .shape(Mika.toHead(Shapes.union(list.toArray(new Shape[0])).paint(paint)))
                        .detail(1.0f)
                        .resolution(0.85f)
                        .flatLight(0.5f)
                        .occlusion(0.35f)
                        .outline(-6142180, 0.09f)
                        .motion(
                                Motion.hair()
                                        .idle(1.2f, 0.0f, 1.0f, 0.35f)
                                        .walk(2.5f, 0.0f, 2.0f, 2.0f)
                                        .drag(6.0f)
                                        .hang(0.2f)
                                        .turn(5.0f, 0.0f)
                                        .spring(60.0f, 8.0f)
                                        .bend(1.6f));
        rig.extra("hair_l", Bone.HEAD).mirrorOf(part2);
        Shape[] shapes2 = new Shape[FRINGE.length];
        for (j = 0; j < FRINGE.length; ++j) {
            shapes2[j] = Mika.fringeLock(FRINGE[j]);
        }
        Shape shape3 =
                Shapes.ellipsoid(0.0f, 28.25f, -0.28f, 2.56f, 2.64f, 2.69f)
                        .intersect(Shapes.above(29.3f));
        Shape shape4 = Shapes.subtract(Shapes.union(shapes2), shape3);
        Part part3 =
                rig.extra("fringe", Bone.HEAD)
                        .shape(
                                Mika.toHead(
                                        shape4.paint(
                                                Paint.vertical(-15750, 26.9f, -681403, 29.2f))))
                        .detail(1.3f)
                        .resolution(0.5f)
                        .flatLight(0.5f)
                        .occlusion(0.25f)
                        .outline(-6142180, 0.08f);
        Mika.shine(part, part3);
        Mika.clip(rig);
        Strand strand =
                Shapes.tube(
                                -1.95f, 29.5f, 0.85f, 0.42f, -2.36f, 28.4f, 1.05f, 0.56f, -2.42f,
                                27.1f, 1.08f, 0.6f, -2.3f, 25.9f, 1.02f, 0.5f, -2.26f, 25.0f, 0.98f,
                                0.34f, -2.5f, 24.45f, 0.95f, 0.05f)
                        .flatten(2.3f, -0.55f, 0.0f, 0.83f);
        Part part4 =
                rig.extra("lock_r", Bone.HEAD)
                        .pivot(Mika.wx(-1.95f), Mika.wy(29.4f), Mika.wz(0.85f))
                        .shape(
                                Mika.toHead(
                                        Mika.pointed(strand, -2.5f, 24.45f, 0.95f, 0.4f, 1.0f, 0.3f)
                                                .paint(paint)))
                        .detail(0.8f)
                        .resolution(0.55f)
                        .flatLight(0.5f)
                        .occlusion(0.35f)
                        .outline(-6142180, 0.09f)
                        .motion(Motion.hair().drag(10.0f).hang(0.35f));
        rig.extra("lock_l", Bone.HEAD).mirrorOf(part4);
        Strand strand2 =
                Shapes.tube(
                                0.05f, 30.8f, 0.1f, 0.24f, 0.03f, 31.45f, 0.2f, 0.26f, 0.3f, 32.05f,
                                0.32f, 0.22f, 0.8f, 32.38f, 0.42f, 0.17f, 1.34f, 32.35f, 0.5f,
                                0.11f, 1.72f, 32.02f, 0.55f, 0.05f)
                        .flatten(1.6f, 0.0f, 0.0f, 1.0f);
        rig.extra("ahoge", Bone.HEAD)
                .pivot(Mika.wx(0.05f), Mika.wy(30.85f), Mika.wz(0.1f))
                .shape(Mika.toHead(strand2.paint(Paint.vertical(-681403, 31.2f, -15750, 32.1f))))
                .resolution(0.5f)
                .detail(0.9f)
                .flatLight(0.5f)
                .occlusion(0.3f)
                .outline(-6142180, 0.08f)
                .motion(
                        Motion.hair()
                                .idle(4.0f, 0.0f, 6.0f, 0.5f)
                                .walk(8.0f, 0.0f, 8.0f, 2.0f)
                                .drag(6.0f)
                                .hang(0.3f)
                                .spring(70.0f, 5.0f)
                                .bend(1.2f));
    }

    private static int hairColor(float f, float g, float h) {
        int i = Colors.mix(-15750, -681403, Mika.smooth(25.0f, 27.6f, g));
        float f2 = (float) Math.sqrt(f * f + (h + 0.25f) * (h + 0.25f));
        float g2 = (1.0f - Mika.smooth(1.9f, 2.55f, f2)) * (1.0f - Mika.smooth(26.4f, 27.6f, g));
        return Colors.mix(i, -3647457, g2 * 0.75f);
    }

    private static Shape fringeLock(float[] floats) {
        float f = floats[0] < 0.7f ? 0.52f : (floats[9] > 0.0f ? 0.4f : 0.28f);
        Strand strand =
                Shapes.strand(
                                floats[0],
                                0.05f,
                                floats[1],
                                floats[2],
                                Mika.capZ(floats[1], floats[2], -0.3f),
                                floats[3],
                                floats[4],
                                Mika.capZ(floats[3], floats[4], 0.1f),
                                floats[5],
                                floats[6],
                                Mika.surf(floats[5], floats[6], f),
                                floats[7],
                                floats[8],
                                Mika.surf(floats[7], floats[8], 0.12f))
                        .flattenFrom(2.8f, 0.0f, 27.95f, -0.15f);
        float g = floats[5] - floats[7];
        float h = floats[6] - floats[8];
        float f2 = (float) Math.sqrt(g * g + h * h);
        float g2 = 0.9f * (1.0f + floats[9]);
        float h2 = 0.9f * (1.0f - floats[9]);
        float f3 = 0.34f;
        Shape2D shape2D =
                Shape2D.polygon(
                        floats[7],
                        floats[8],
                        floats[7] + (g /= f2) * g2 - (h /= f2) * f3,
                        floats[8] + h * g2 + g * f3,
                        floats[7] + g * 8.0f - h * 5.0f,
                        floats[8] + h * 8.0f + g * 5.0f,
                        floats[7] + g * 8.0f + h * 5.0f,
                        floats[8] + h * 8.0f - g * 5.0f,
                        floats[7] + g * h2 + h * f3,
                        floats[8] + h * h2 - g * f3);
        return strand.intersect(Shapes.extrude(shape2D, 6.0f, 0.0f));
    }

    private static void clip(Rig rig) {
        Shape2D shape2D =
                Shape2D.blend(
                        0.08f,
                        Shape2D.ellipse(-0.1f, 0.0f, 0.5f, 0.32f),
                        Shape2D.polygon(0.3f, 0.0f, 0.86f, 0.36f, 0.66f, 0.0f, 0.86f, -0.36f));
        Shape shape =
                Shapes.extrude(shape2D, 0.15f, 0.11f)
                        .scale(1.3f)
                        .paint(
                                Paint.gradient(
                                        -3869978, 0.0f, 0.35f, 0.0f, -8398652, 0.0f, -0.35f, 0.0f));
        float f = 1.62f;
        float g = 28.7f;
        float h = 0.75f;
        float surf = Mika.surf(f, g, h);
        float f2 = 2.3f + h;
        float g2 = 2.3f + h;
        float h2 = 2.35f + h;
        float f3 = f / (f2 * f2);
        float g3 = (g - 27.95f) / (g2 * g2);
        float h3 = (surf - -0.15f) / (h2 * h2);
        float f4 = (float) Math.sqrt(f3 * f3 + g3 * g3 + h3 * h3);
        float g4 = (float) Math.toDegrees(Math.atan2(f3, h3));
        float h4 = (float) (-Math.toDegrees(Math.asin(g3 / f4)));
        Shape shape2 = shape.rotate(0.0f, 0.0f, 35.0f).rotate(h4, g4, 0.0f).move(f, g, surf);
        Vec3 vec3 =
                new Vec3(-0.5f, 0.08f, 0.2f)
                        .rotate(0.0f, 0.0f, 35.0f)
                        .rotate(h4, g4, 0.0f)
                        .add(f, g, surf);
        Vec3 vec32 = new Vec3(0.0f, 0.0f, 1.0f).rotate(0.0f, 0.0f, 35.0f).rotate(h4, g4, 0.0f);
        Canvas canvas = new Canvas(32, 32);
        canvas.fill(Shape2D.circle(16.0f, 16.0f, 7.0f), -14796236);
        canvas.fill(Shape2D.circle(13.5f, 13.5f, 2.2f), -855638017);
        Decal decal =
                Decal.project(
                                canvas,
                                Mika.wx(vec3.x()),
                                Mika.wy(vec3.y()),
                                Mika.wz(vec3.z()),
                                vec32.x(),
                                vec32.y(),
                                vec32.z(),
                                0.0f,
                                1.0f,
                                0.0f,
                                0.33600003f,
                                0.33600003f)
                        .depth(0.25f, 0.25f);
        rig.extra("clip", Bone.HEAD)
                .shape(Mika.toHead(shape2))
                .resolution(0.4f)
                .detail(0.5f)
                .flatLight(0.4f)
                .outline(-12811148, 0.1f)
                .decal(decal);
    }

    private static void shine(Part part, Part part2) {
        Canvas canvas = new Canvas(96, 24);
        canvas.fill(
                Shape2D.union(
                        Shape2D.taper(3.0f, 12.0f, 1.0f, 48.0f, 12.0f, 17.0f),
                        Shape2D.taper(48.0f, 12.0f, 17.0f, 93.0f, 12.0f, 1.0f)),
                -419442022,
                1.5f);
        float[][] floatGrid =
                new float[][] {
                    {-52.0f, 0.72f, 0.12f, 6.0f},
                    {-24.0f, 0.9f, -0.06f, -4.0f},
                    {4.0f, 0.66f, 0.08f, 5.0f},
                    {27.0f, 0.82f, -0.08f, -6.0f},
                    {57.0f, 0.7f, 0.1f, 3.0f}
                };
        for (float[] floats : floatGrid) {
            double d = Math.toRadians(floats[0]);
            float f = (float) Math.sin(d);
            float g = (float) Math.cos(d);
            float[] floats2 = Mika.onCap(f, 0.36f + 0.3f * (1.0f - g) + floats[2], g, 0.28f);
            float h = floats2[0];
            float f2 = floats2[1] - 28.25f;
            float g2 = floats2[2] + 0.28f;
            Vec3 vec3 = new Vec3(0.0f, 1.0f, 0.0f).rotate(0.0f, 0.0f, floats[3]);
            Decal decal =
                    Decal.project(
                                    canvas,
                                    Mika.wx(floats2[0]),
                                    Mika.wy(floats2[1]),
                                    Mika.wz(floats2[2]),
                                    h,
                                    f2 * 0.6f,
                                    g2,
                                    vec3.x(),
                                    vec3.y(),
                                    vec3.z(),
                                    floats[1] * 1.12f,
                                    0.2464f)
                            .depth(0.896f, 0.896f)
                            .maxAngle(80.0f)
                            .flatLight(0.8f)
                            .occlusion(0.0f);
            part.decal(decal);
            part2.decal(decal);
        }
    }

    private static Shape pointed(
            Strand strand, float f, float g, float h, float f2, float g2, float h2) {
        float f3 = -0.25f;
        float g3 = (float) Math.toDegrees(Math.atan2(f, h - f3));
        float h3 = g2 * (1.0f + h2);
        float f4 = g2 * (1.0f - h2);
        Shape2D shape2D =
                Shape2D.polygon(-5.0f, g + 8.0f, 5.0f, g + 8.0f, f2, g + h3, 0.0f, g, -f2, g + f4);
        return strand.intersect(
                Shapes.extrude(shape2D, 5.0f, 0.0f).rotate(0.0f, g3, 0.0f, 0.0f, 0.0f, f3));
    }

    private static Shape backLock(float f, float g, float h, float f2) {
        double d = Math.toRadians(f);
        float g2 = (float) Math.sin(d);
        float h2 = (float) Math.cos(d);
        float f3 = Math.max(0.0f, Math.min(1.0f, (Math.abs(f) - 100.0f) / 70.0f));
        float[] floats = new float[] {30.1f, 29.1f, 27.9f, 26.9f, (26.9f + g) * 0.5f - 0.1f, g};
        float[] floats2 = new float[] {2.04f, 2.59f, 2.7f, 2.52f, 2.42f, 2.32f};
        float[] floats3 = new float[] {2.14f, 2.72f, 2.83f, 2.62f, 2.48f, 2.35f};
        float[] floats4 = new float[] {-0.34f, -0.06f, 0.04f, 0.04f, 0.02f, -0.06f - 0.24f * f3};
        float[] floats5 = new float[] {0.7f, 1.05f, 1.05f, 0.95f, 0.64f, 0.06f};
        float[] floats6 = new float[24];
        for (int j = 0; j < 6; ++j) {
            floats6[j * 4] = g2 * (floats2[j] + floats4[j]);
            floats6[j * 4 + 1] = floats[j];
            floats6[j * 4 + 2] = -0.28f + h2 * (floats3[j] + floats4[j]);
            floats6[j * 4 + 3] = floats5[j] * (j == 5 ? 1.0f : h);
        }
        return Mika.pointed(
                Shapes.tube(floats6).flattenFrom(2.1f, 0.0f, 27.95f, -0.15f),
                floats6[20],
                g,
                floats6[22],
                0.62f,
                1.25f,
                f2);
    }

    private static Shape topLock(float f, float g) {
        double d = Math.toRadians(f);
        float h = (float) Math.sin(d);
        float f2 = (float) Math.cos(d);
        float[] floats = new float[] {2.2f, 2.8f, 2.98f, 3.0f};
        float[] floats2 = new float[] {29.4f, 28.2f, 27.2f, g};
        float[] floats3 = new float[] {0.55f, 0.75f, 0.55f, 0.08f};
        float[] floats4 = new float[16];
        for (int i = 0; i < 4; ++i) {
            floats4[i * 4] = h * floats[i];
            floats4[i * 4 + 1] = floats2[i];
            floats4[i * 4 + 2] = -0.25f + f2 * floats[i] * 1.05f;
            floats4[i * 4 + 3] = floats3[i];
        }
        return Mika.pointed(
                Shapes.tube(floats4).flattenFrom(2.2f, 0.0f, 27.95f, -0.15f),
                floats4[12],
                g,
                floats4[14],
                0.6f,
                1.15f,
                0.2f);
    }

    private static void ears(Rig rig) {
        Shape shape =
                Shapes.extrude(
                        Shape2D.polygon(-1.1f, 0.0f, 1.1f, 0.0f, 0.08f, 2.55f), 0.64f, 0.32f);
        Shape shape2 =
                Shapes.extrude(
                                Shape2D.polygon(
                                        0.6f, -0.3f, -0.62f, -0.3f, -0.15f, 2.7f, 0.2f, 2.7f),
                                1.3f,
                                0.2f)
                        .rotate(0.0f, 90.0f, 0.0f);
        Shape shape3 =
                Shapes.intersect(shape, shape2)
                        .paint(Paint.vertical(-681403, 1.45f, -4697060, 2.25f));
        Shape shape4 =
                Shapes.extrude(
                                Shape2D.polygon(-0.66f, 0.3f, 0.68f, 0.3f, 0.07f, 1.95f),
                                0.5f,
                                0.14f)
                        .move(0.0f, 0.0f, 0.62f)
                        .paint(Paint.of(-22088).flatLight(0.7f));
        Shape shape5 =
                Shapes.union(
                                Shapes.strand(
                                        0.14f, 0.05f, -0.34f, 0.28f, 0.25f, -0.46f, 0.6f, 0.44f,
                                        -0.38f, 0.9f, 0.5f),
                                Shapes.strand(
                                        0.14f, 0.05f, 0.3f, 0.28f, 0.25f, 0.46f, 0.58f, 0.44f,
                                        0.56f, 0.86f, 0.48f))
                        .paint(Paint.of(-5162).flatLight(0.7f));
        Shape shape6 =
                Shapes.union(Shapes.subtract(0.1f, shape3, shape4), shape5)
                        .scale(1.18f)
                        .rotate(-6.0f, -18.0f, 24.0f)
                        .move(-1.6f, 29.75f, -0.15f);
        Part part =
                rig.extra("ear_r", Bone.HEAD)
                        .pivot(Mika.wx(-1.6f), Mika.wy(29.9f), Mika.wz(-0.15f))
                        .shape(Mika.toHead(shape6))
                        .resolution(0.58f)
                        .detail(0.9f)
                        .flatLight(0.4f)
                        .outline(-6142180, 0.09f)
                        .motion(Mika.earMotion(0.0f, -1.0f));
        rig.extra("ear_l", Bone.HEAD).mirrorOf(part).motion(Mika.earMotion(140.0f, 1.0f));
    }

    private static Motion earMotion(float f, float g) {
        return Motion.none()
                .idle(2.5f, 0.0f, 5.0f * g, 0.55f)
                .idlePhase(f)
                .walk(3.0f, 0.0f, 3.0f * g, 2.0f)
                .spring(90.0f, 7.0f);
    }

    private static void body(Rig rig) {
        Shape shape =
                Shapes.union(
                        Shapes.cone(0.0f, 22.6f, -0.2f, 1.02f, -1.95f, 22.2f, -0.2f, 0.9f),
                        Shapes.cone(0.0f, 22.6f, -0.2f, 1.02f, 1.95f, 22.2f, -0.2f, 0.9f));
        Shape shape2 = Shapes.ellipsoid(0.0f, 20.5f, -0.1f, 2.3f, 2.4f, 1.72f);
        Shape shape3 = Shapes.ellipsoid(0.0f, 17.8f, -0.02f, 2.25f, 2.9f, 1.7f);
        Shape shape4 = Shapes.ellipsoid(0.0f, 15.25f, -0.05f, 2.45f, 1.25f, 1.86f);
        Shape shape5 =
                Shapes.blend(1.0f, shape, shape2, shape3, shape4).paint(Paint.field(Mika::hoodie));
        Shape shape6 =
                Shapes.intersect(
                                Shapes.ellipsoid(0.0f, 16.3f, -0.02f, 2.6f, 2.9f, 2.02f),
                                Shapes.extrude(
                                                Shape2D.polygon(
                                                        -1.6f, 17.3f, 1.6f, 17.3f, 2.15f, 14.75f,
                                                        -2.15f, 14.75f),
                                                2.0f,
                                                0.0f)
                                        .move(0.0f, 0.0f, 2.0f))
                        .color(-4200487);
        Shape shape7 =
                Shapes.extrude(Shape2D.ellipse(0.0f, 0.0f, 2.42f, 1.8f), 0.4f, 0.3f)
                        .rotate(90.0f, 0.0f, 0.0f)
                        .move(0.0f, 14.2f, -0.05f)
                        .color(-5710645);
        rig.part(Bone.BODY)
                .pivot(0.0f, 23.4f, -0.1f)
                .shape(Shapes.union(Shapes.blend(0.25f, shape5, shape6), shape7))
                .resolution(1.15f)
                .detail(1.0f)
                .outline(-10774650, 0.12f)
                .occlusion(0.3f)
                .flatLight(0.2f)
                .decal(Mika.pocketDecal())
                .decal(Mika.pawDecal())
                .decal(Mika.hemRib());
        rig.extra("collar", Bone.BODY)
                .shape(Mika.rim().paint(Paint.field(Mika::rimColor)))
                .resolution(0.7f)
                .detail(0.8f)
                .occlusion(0.3f)
                .flatLight(0.2f)
                .outline(-10774650, 0.1f);
        Shape shape8 =
                Shapes.blend(
                        0.8f,
                        Shapes.ellipsoid(0.0f, 21.7f, -1.85f, 2.2f, 2.2f, 0.85f),
                        Shapes.cone(0.0f, 20.6f, -2.3f, 0.8f, 0.0f, 19.1f, -2.2f, 0.28f));
        Shape shape9 =
                Shapes.ellipsoid(0.0f, 0.0f, 0.0f, 1.55f, 0.35f, 0.85f)
                        .rotate(-26.6f, 0.0f, 0.0f)
                        .move(0.0f, 22.95f, -1.95f);
        Shape shape10 =
                Shapes.subtract(
                                0.15f,
                                shape8.intersect(
                                        Shapes.plane(0.0f, 22.7f, -2.2f, 0.0f, 1.0f, -0.5f)),
                                shape9)
                        .paint(Paint.field(Mika::hoodColor));
        rig.extra("hood", Bone.BODY)
                .shape(shape10)
                .resolution(0.85f)
                .detail(0.6f)
                .occlusion(0.3f)
                .flatLight(0.2f)
                .outline(-10774650, 0.12f);
        rig.extra("skirt", Bone.BODY)
                .pivot(0.0f, 14.6f, 0.0f)
                .shape(Mika.skirt())
                .resolution(0.9f)
                .detail(0.8f)
                .occlusion(0.35f)
                .outline(-14999232, 0.12f)
                .decal(Mika.skirtStripe())
                .motion(
                        Motion.none()
                                .walk(0.0f, 8.0f, 0.0f, 1.0f)
                                .walkPhase(-90.0f)
                                .drag(2.0f)
                                .spring(80.0f, 10.0f));
        Shape shape11 =
                Shapes.tube(
                                -0.32f, 23.5f, 1.45f, 0.13f, -0.38f, 22.6f, 1.75f, 0.13f, -0.44f,
                                21.3f, 1.95f, 0.13f, -0.48f, 20.1f, 2.0f, 0.12f)
                        .color(-264984);
        Shape shape12 =
                Shapes.sphere(-0.48f, 19.6f, 2.05f, 0.45f)
                        .paint(Paint.radial(-3920, -0.62f, 19.8f, 2.35f, 0.6f, -867761));
        Part part =
                rig.extra("cord_r", Bone.BODY)
                        .pivot(-0.32f, 23.5f, 1.45f)
                        .shape(Shapes.union(shape11, shape12))
                        .resolution(0.5f)
                        .detail(0.5f)
                        .outline(-10774650, 0.08f)
                        .decal(Mika.bellDecal(-0.48f))
                        .motion(
                                Motion.none()
                                        .rest(-3.0f, 0.0f, 0.0f)
                                        .idle(1.5f, 0.0f, 2.0f, 0.5f)
                                        .walk(5.0f, 0.0f, 6.0f, 2.0f)
                                        .drag(-4.0f)
                                        .hang(0.9f)
                                        .spring(60.0f, 6.0f)
                                        .bend(0.8f));
        rig.extra("cord_l", Bone.BODY).mirrorOf(part);
    }

    private static Shape rim() {
        return Shapes.tube(
                        0.02f, 23.64f, 1.44f, 0.36f, -0.5f, 23.84f, 1.3f, 0.41f, -1.12f, 24.08f,
                        0.8f, 0.43f, -1.42f, 24.2f, -0.18f, 0.43f, -1.1f, 24.3f, -1.14f, 0.43f,
                        0.1f, 24.34f, -1.68f, 0.43f)
                .flattenFrom(1.25f, 0.0f, 24.1f, -0.25f)
                .mirrorX();
    }

    private static int hoodie(float f, float g, float h) {
        float f2 = (float) Math.sqrt(f * f + (h + 0.2f) * (h + 0.2f));
        float g2 = Mika.smooth(22.4f, 23.3f, g) * (1.0f - Mika.smooth(1.7f, 2.3f, f2)) * 0.6f;
        float h2 = (1.0f - Mika.smooth(14.6f, 15.3f, g)) * 0.55f;
        return Colors.mix(-4200487, -7092297, Math.max(g2, h2));
    }

    private static int rimColor(float f, float g, float h) {
        float f2 = (float) Math.sqrt(f * f + (h + 0.2f) * (h + 0.2f));
        float g2 = Mika.smooth(0.28f, 0.36f, g - 24.0f + 0.22f * h);
        return Colors.mix(-4200487, -744532, Math.max(g2, 1.0f - Mika.smooth(1.12f, 1.38f, f2)));
    }

    private static int hoodColor(float f, float g, float h) {
        float f2 = f / 2.2f;
        float g2 = (g - 21.7f) / 2.2f;
        float h2 = (h + 1.85f) / 0.85f;
        float f3 = (g - 22.7f - 0.5f * (h + 2.2f)) * 0.894f;
        return f3 > -0.5f && f2 * f2 + g2 * g2 + h2 * h2 < 0.8f ? -744532 : -4200487;
    }

    private static Shape skirt() {
        return Shapes.field(Mika::skirtDistance, -3.3f, 11.8f, -2.8f, 3.3f, 14.4f, 2.8f)
                .paint(Paint.field(Mika::skirtColor));
    }

    private static float flare(float f) {
        float g = Math.max(0.0f, Math.min(1.0f, (14.0f - f) / 2.1f));
        return g * (2.0f - g);
    }

    private static float pleat(float f, float g, float h) {
        float f2 = 2.2f + 0.75f * h;
        float g2 = 1.66f + 0.84f * h;
        double d = Math.atan2(f / f2, g / g2) * 16.0 / (Math.PI * 2);
        d -= Math.floor(d);
        return (float) Math.abs(2.0 * d - 1.0);
    }

    private static float skirtDistance(float f, float g, float h) {
        float flare = Mika.flare(g);
        float f2 = 2.2f + 0.75f * flare;
        float g2 = 1.66f + 0.84f * flare;
        float h2 = f / f2;
        float f3 = h / g2;
        float g3 = (float) Math.sqrt(h2 * h2 + f3 * f3);
        float h3 =
                (g3 - 1.0f) * Math.min(f2, g2) * 0.72f
                        - 0.2f * flare * (Mika.pleat(f, h, flare) - 0.5f);
        return Math.max(h3, Math.max(g - 14.3f, 11.9f - g));
    }

    private static int skirtColor(float f, float g, float h) {
        float flare = Mika.flare(g);
        return Colors.mix(
                -14275250,
                -12892555,
                0.45f + 0.55f * (1.0f - flare + flare * Mika.pleat(f, h, flare)));
    }

    private static Decal skirtStripe() {
        Canvas canvas = new Canvas(512, 24);
        canvas.fill(Shape2D.box(256.0f, 12.0f, 256.0f, 7.0f, 0.0f), -5839921);
        return Decal.wrap(
                        canvas, 0.0f, 12.35f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 360.0f,
                        0.16f)
                .radius(2.2f, 3.2f);
    }

    private static void arms(Rig rig) {
        Shape shape = Shapes.sphere(-2.72f, 21.95f, -0.15f, 0.95f);
        Shape shape2 = Shapes.cone(-2.85f, 21.6f, -0.15f, 0.9f, -3.35f, 18.4f, -0.2f, 0.88f);
        Shape shape3 = Shapes.sphere(-3.38f, 18.3f, -0.18f, 0.9f);
        Shape shape4 = Shapes.cone(-3.38f, 18.2f, -0.18f, 0.9f, -3.72f, 15.0f, 0.45f, 1.12f);
        Shape shape5 =
                Shapes.blend(0.4f, shape, shape2, shape3, shape4).paint(Paint.field(Mika::sleeve));
        Shape shape6 =
                Shapes.cylinder(-3.74f, 15.05f, 0.47f, -3.8f, 13.4f, 0.69f, 0.9f, 0.28f)
                        .paint(Paint.field(Mika::cuff));
        Shape shape7 = Shapes.blend(0.15f, shape5, shape6);
        Part part =
                rig.part(Bone.RIGHT_ARM)
                        .pivot(-2.72f, 21.95f, -0.15f)
                        .grip(-3.8f, 12.9f, 0.9f)
                        .shape(shape7)
                        .detail(0.9f)
                        .resolution(1.05f)
                        .outline(-10774650, 0.12f)
                        .occlusion(0.3f)
                        .flatLight(0.2f)
                        .swing(0.65f, 40.0f)
                        .joint(-3.38f, 18.3f, -0.18f, 0.7f)
                        .decal(Mika.cuffRib());
        rig.part(Bone.LEFT_ARM).mirrorOf(part);
        Part part2 =
                rig.extra("hand_r", Bone.RIGHT_ARM)
                        .shape(Mika.hand())
                        .resolution(0.4f)
                        .detail(2.6f)
                        .flatLight(1.0f)
                        .occlusion(0.25f)
                        .outline(-5740966, 0.08f);
        rig.extra("hand_l", Bone.LEFT_ARM).mirrorOf(part2);
    }

    private static int sleeve(float f, float g, float h) {
        float f2 = Math.max(0.0f, Math.min(1.0f, (21.9f - g) / 6.9f));
        float g2 = Mika.smooth(0.25f, 0.85f, f + 2.8f + 0.94f * f2) * 0.5f;
        float h2 =
                (1.0f - Mika.smooth(0.2f, 0.55f, Math.abs(g - 18.25f)))
                        * Mika.smooth(0.2f, 0.7f, h)
                        * 0.45f;
        return Colors.mix(-4200487, -7092297, Math.max(g2, h2));
    }

    private static int cuff(float f, float g, float h) {
        float f2 = -0.06f;
        float g2 = -1.65f;
        float h2 = 0.22f;
        float f3 = (float) Math.sqrt(f2 * f2 + g2 * g2 + h2 * h2);
        float g3 = f + 3.8f;
        float h3 = g - 13.4f;
        float f4 = h - 0.69f;
        float g4 = -(g3 * f2 + h3 * g2 + f4 * h2) / f3;
        float h4 = g3 + g4 * f2 / f3;
        float f5 = h3 + g4 * g2 / f3;
        float g5 = f4 + g4 * h2 / f3;
        float h5 = (float) Math.sqrt(h4 * h4 + f5 * f5 + g5 * g5);
        return Colors.mix(
                -5710645,
                -9461870,
                (1.0f - Mika.smooth(0.04f, 0.12f, g4)) * (1.0f - Mika.smooth(0.55f, 0.68f, h5)));
    }

    private static Shape hand() {
        float[][] floatGrid =
                new float[][] {
                    {-3.47f, 0.86f, 13.06f}, {-3.79f, 0.95f, 12.97f}, {-4.1f, 0.86f, 13.04f}
                };
        Shape[] shapes = new Shape[floatGrid.length];
        for (int i = 0; i < floatGrid.length; ++i) {
            shapes[i] =
                    Shapes.capsule(
                            floatGrid[i][0],
                            13.5f,
                            floatGrid[i][1] - 0.12f,
                            floatGrid[i][0],
                            floatGrid[i][2],
                            floatGrid[i][1],
                            0.16f);
        }
        Shape shape = Shapes.capsule(-3.36f, 13.48f, 0.64f, -3.3f, 13.2f, 0.76f, 0.15f);
        return Shapes.union(Shapes.union(shapes), shape).paint(Paint.of(-9532));
    }

    private static void legs(Rig rig) {
        Shape shape = Shapes.sphere(-1.12f, 13.2f, -0.05f, 1.0f);
        Shape shape2 = Shapes.cone(-1.18f, 12.8f, 0.0f, 1.13f, -1.1f, 7.9f, 0.1f, 0.88f);
        Shape shape3 = Shapes.sphere(-1.08f, 7.45f, 0.12f, 0.82f);
        Shape shape4 = Shapes.sphere(-1.07f, 7.5f, 0.48f, 0.5f);
        Strand strand =
                Shapes.tube(
                        -1.08f, 7.2f, 0.1f, 0.84f, -1.1f, 5.6f, -0.15f, 0.93f, -1.13f, 3.6f, -0.02f,
                        0.64f, -1.15f, 2.2f, 0.0f, 0.52f);
        Shape shape5 =
                Shapes.blend(0.45f, shape, shape2, shape3, shape4, strand)
                        .paint(Paint.field(Mika::tights));
        Part part =
                rig.part(Bone.RIGHT_LEG)
                        .pivot(-1.2f, 13.7f, 0.0f)
                        .shape(shape5)
                        .detail(0.85f)
                        .resolution(1.1f)
                        .outline(-15068908, 0.12f)
                        .occlusion(0.35f)
                        .swing(0.55f, 30.0f)
                        .joint(-1.08f, 7.45f, 0.12f, 0.8f);
        rig.part(Bone.LEFT_LEG).mirrorOf(part);
        Part part2 =
                rig.extra("shoe_r", Bone.RIGHT_LEG)
                        .shape(Mika.sneaker().rotate(0.0f, -7.0f, 0.0f, -1.2f, 0.0f, 0.2f))
                        .resolution(0.75f)
                        .detail(1.0f)
                        .outline(-6659470, 0.12f)
                        .decal(Mika.laces());
        rig.extra("shoe_l", Bone.LEFT_LEG).mirrorOf(part2);
    }

    private static int tights(float f, float g, float h) {
        double d = Math.atan2(-(f + 1.1f), h - 0.02f) - 0.35;
        float f2 =
                Mika.smooth(0.6f, 0.95f, (float) Math.cos(d)) * Mika.smooth(2.4f, 4.5f, g) * 0.5f;
        float g2 = (float) Math.sqrt((g - 7.5f) * (g - 7.5f) + (f + 1.07f) * (f + 1.07f) * 1.4f);
        float h2 = (1.0f - Mika.smooth(0.3f, 0.8f, g2)) * Mika.smooth(0.2f, 0.7f, h) * 0.55f;
        return Colors.mix(-12965330, -9808288, Math.max(f2, h2));
    }

    private static Shape sneaker() {
        Shape shape =
                Shapes.blend(
                                0.25f,
                                Shapes.box(-1.2f, 0.3f, 0.5f, 0.9f, 0.3f, 1.84f, 0.26f),
                                Shapes.ellipsoid(-1.2f, 0.36f, 2.05f, 0.82f, 0.34f, 0.5f))
                        .intersect(Shapes.above(0.0f))
                        .paint(
                                Paint.of(-264207)
                                        .band(
                                                -1011800, 0.0f, 0.46f, 0.0f, 0.0f, 1.0f, 0.0f,
                                                0.07f, 0.05f));
        Shape shape2 = Shapes.ellipsoid(-1.2f, 0.9f, 1.15f, 0.74f, 0.55f, 1.12f);
        Shape shape3 = Shapes.ellipsoid(-1.2f, 1.28f, -0.48f, 0.75f, 0.95f, 0.84f);
        Shape shape4 = Shapes.blend(0.45f, shape2, shape3).color(-542005);
        Shape shape5 = Shapes.ellipsoid(-1.2f, 0.72f, 1.86f, 0.68f, 0.36f, 0.5f).color(-264207);
        Shape shape6 = Shapes.torus(-1.2f, 2.1f, -0.4f, 0.6f, 0.26f).color(-5839921);
        Shape shape7 =
                Shapes.box(-1.2f, 1.75f, 0.35f, 0.42f, 0.42f, 0.3f, 0.18f)
                        .rotate(-25.0f, 0.0f, 0.0f, -1.2f, 1.75f, 0.35f)
                        .color(-207907);
        Shape shape8 =
                Shapes.capsule(-1.2f, 1.8f, -1.2f, -1.2f, 2.45f, -1.02f, 0.17f).color(-5839921);
        return Shapes.union(shape, Shapes.blend(0.12f, shape4, shape5), shape6, shape7, shape8);
    }

    private static void tail(Rig rig) {
        float[] floats =
                new float[] {
                    0.0f, 13.95f, -1.45f, 0.58f, 0.15f, 13.35f, -2.7f, 0.76f, 0.6f, 13.2f, -4.1f,
                    0.92f, 1.6f, 14.0f, -5.15f, 1.06f, 2.6f, 15.9f, -5.5f, 1.15f, 3.2f, 18.1f,
                    -5.15f, 1.15f, 3.35f, 19.9f, -4.35f, 1.04f, 3.0f, 21.0f, -3.45f, 0.72f, 2.55f,
                    21.45f, -2.85f, 0.26f
                };
        Curve curve = new Curve(floats);
        Shape shape =
                Shapes.union(
                        Shapes.strand(
                                0.24f, 0.04f, 2.68f, 21.32f, -3.02f, 2.36f, 21.62f, -2.68f, 1.96f,
                                21.62f, -2.62f),
                        Shapes.strand(
                                0.24f, 0.04f, 2.68f, 21.32f, -3.02f, 2.46f, 21.6f, -2.62f, 2.42f,
                                21.62f, -2.2f),
                        Shapes.strand(
                                0.24f, 0.04f, 2.68f, 21.32f, -3.02f, 2.44f, 21.7f, -2.72f, 2.4f,
                                22.12f, -2.62f));
        Part part =
                rig.extra("tail", Bone.BODY)
                        .pivot(0.0f, 13.95f, -1.45f)
                        .shape(
                                Shapes.union(Shapes.tube(floats), shape)
                                        .paint(Paint.field(curve::tailColor)))
                        .detail(1.0f)
                        .resolution(1.1f)
                        .flatLight(0.3f)
                        .outline(-6142180, 0.1f)
                        .motion(
                                Motion.tail()
                                        .idle(3.0f, 14.0f, 4.0f, 0.4f)
                                        .walk(4.0f, 12.0f, 3.0f, 1.0f)
                                        .spring(30.0f, 4.0f)
                                        .bend(1.4f));
        Canvas canvas = new Canvas(128, 96);
        canvas.fill(
                Shape2D.union(
                        Shape2D.line(10.0f, 80.0f, 64.0f, 26.0f, 24.0f),
                        Shape2D.line(64.0f, 26.0f, 118.0f, 80.0f, 24.0f)),
                -1999082968,
                6.0f);
        for (float f : new float[] {0.3f, 0.43f, 0.56f}) {
            part.decal(curve.chevron(canvas, f));
        }
    }

    private static float smooth(float f, float g, float h) {
        float f2 = Math.max(0.0f, Math.min(1.0f, (h - f) / (g - f)));
        return f2 * f2 * (3.0f - 2.0f * f2);
    }

    private static Decal face() {
        int i;
        Canvas canvas = new Canvas(256, 200);
        float f = 13.356522f;
        Shape2D[] shape2Ds = new Shape2D[FRINGE.length];
        for (i = 0; i < FRINGE.length; ++i) {
            float[] floats = FRINGE[i];
            float g = floats[7] + (floats[5] - floats[7]) * 0.12f;
            float h = floats[8] + (floats[6] - floats[8]) * 0.12f;
            shape2Ds[i] =
                    Shape2D.path(
                            floats[0] * 1.7f * 55.652176f,
                            9.0f,
                            Mika.u(floats[3]),
                            Mika.v(floats[4]) + f,
                            Mika.u(floats[5]),
                            Mika.v(floats[6]) + f,
                            Mika.u(g),
                            Mika.v(h) + f);
        }
        canvas.fill(Shape2D.union(shape2Ds), -1713793398, 3.0f);
        for (i = -1; i <= 1; i += 2) {
            Mika.eye(canvas, 128.0f + (float) i * 53.0f, 114.0f, i);
        }
        canvas.fill(
                Shape2D.curve(125.5f, 146.5f, 128.5f, 150.5f, 131.5f, 149.0f, 2.2f, 1.4f),
                -1713795968);
        canvas.fill(
                Shape2D.path(
                        3.0f, 3.0f, 117.0f, 177.0f, 122.5f, 182.5f, 128.0f, 178.5f, 133.5f, 182.5f,
                        139.0f, 177.0f),
                -7455954);
        return Decal.front(canvas, 0.0f, Mika.wy(27.203125f), Mika.wz(2.1f), 5.152f, 4.0249996f)
                .depth(1.4f, 1.4f)
                .maxAngle(72.0f)
                .flatLight(1.0f)
                .occlusion(0.3f);
    }

    private static void eye(Canvas canvas, float f, float g, int i) {
        Shape2D shape2D =
                Shape2D.ellipse(f, g + 10.0f, 31.0f, 40.0f).rotate((float) i * 12.0f, f, g + 10.0f);
        Shape2D shape2D2 = Shape2D.ellipse(f, g + 2.0f, 26.0f, 31.0f).intersect(shape2D);
        canvas.fill(shape2D2, Paint2D.linear(f, g - 28.0f, f, g + 4.0f, -2571046, -776));
        Shape2D shape2D3 = Shape2D.ellipse(f + (float) i, g + 5.0f, 21.0f, 27.0f);
        canvas.fill(
                shape2D3.intersect(shape2D2),
                Paint2D.linear(f, g - 22.0f, f, g + 32.0f, -11920890, -5748206, -1535446, -11414));
        canvas.fill(
                Shape2D.ellipse(f + (float) i, g + 6.0f, 9.0f, 21.0f).intersect(shape2D2),
                -1711287446,
                4.0f);
        canvas.fill(shape2D3.outline(2.5f).intersect(shape2D2), -1338371580);
        float h = f + (float) i;
        float f2 = g + 6.0f;
        canvas.fill(
                Shape2D.circle(h - 42.2f, f2, 46.7f)
                        .intersect(Shape2D.circle(h + 42.2f, f2, 46.7f)),
                -14414846);
        canvas.fill(
                Shape2D.ellipse(f - 8.0f, g - 8.0f, 7.5f, 9.0f).rotate(-20.0f, f - 8.0f, g - 8.0f),
                -1);
        canvas.fill(Shape2D.circle(f + 9.0f, g + 17.0f, 3.5f), -419430401);
        canvas.fill(
                Shape2D.curve(
                        f - (float) i * 28.0f,
                        g - 8.0f,
                        f - (float) i * 2.0f,
                        g - 42.0f,
                        f + (float) i * 30.0f,
                        g - 24.0f,
                        4.0f,
                        9.5f),
                -13887470);
        canvas.fill(
                Shape2D.taper(
                        f + (float) i * 24.0f,
                        g - 24.0f,
                        7.0f,
                        f + (float) i * 40.0f,
                        g - 36.0f,
                        1.5f),
                -13887470);
        canvas.fill(
                Shape2D.curve(
                        f + (float) i * 8.0f,
                        g + 34.0f,
                        f + (float) i * 21.0f,
                        g + 31.0f,
                        f + (float) i * 26.0f,
                        g + 17.0f,
                        1.2f,
                        3.0f),
                -866506210);
        canvas.fill(
                Shape2D.curve(
                        f - (float) i * 17.0f,
                        g - 38.0f,
                        f + (float) i * 2.0f,
                        g - 48.0f,
                        f + (float) i * 24.0f,
                        g - 38.0f,
                        1.5f,
                        2.2f),
                1721786442);
        canvas.fill(
                Shape2D.curve(
                        f - (float) i * 16.0f,
                        g - 50.0f,
                        f + (float) i * 2.0f,
                        g - 58.0f,
                        f + (float) i * 22.0f,
                        g - 53.0f,
                        3.0f,
                        2.0f),
                -4691398);
        float g2 = f + (float) i * 10.0f;
        float h2 = g + 42.0f;
        canvas.fill(Shape2D.ellipse(g2, h2, 20.0f, 9.0f), 1627357068, 9.0f);
        for (int j = 0; j < 3; ++j) {
            canvas.fill(
                    Shape2D.line(
                            g2 - 8.0f + (float) j * 7.0f,
                            h2 - 4.0f,
                            g2 - 12.0f + (float) j * 7.0f,
                            h2 + 4.0f,
                            1.6f),
                    1894277230);
        }
    }

    private static Decal pocketDecal() {
        int j;
        Canvas canvas = new Canvas(240, 144);
        int k = -1722777742;
        for (j = 0; j < 18; ++j) {
            canvas.fill(Shape2D.box(43.5f + (float) j * 9.0f, 16.5f, 2.6f, 0.75f, 0.7f), k);
        }
        for (j = 0; j < 23; ++j) {
            canvas.fill(Shape2D.box(21.0f + (float) j * 9.0f, 129.0f, 2.6f, 0.75f, 0.7f), k);
        }
        for (int i = -1; i <= 1; i += 2) {
            float f = 120.0f + (float) i * 78.0f;
            float g = 120.0f + (float) i * 102.75f;
            canvas.fill(Shape2D.taper(f, 13.5f, 6.0f, g, 132.0f, 3.75f), -1872719998);
            canvas.fill(
                    Shape2D.taper(
                            f + (float) i * 2.25f,
                            13.5f,
                            2.25f,
                            g + (float) i * 2.25f,
                            132.0f,
                            1.5f),
                    -1605733274);
            canvas.fill(
                    Shape2D.taper(
                            f - (float) i * 4.5f, 16.5f, 1.9f, g - (float) i * 4.5f, 129.0f, 1.5f),
                    0x60FFFFFF);
        }
        return Decal.front(canvas, 0.0f, 16.03f, 2.3f, 5.0f, 3.0f)
                .depth(1.2f, 1.2f)
                .maxAngle(75.0f);
    }

    private static Decal pawDecal() {
        Canvas canvas = new Canvas(128, 128);
        int i = -877910;
        Shape2D shape2D =
                Shape2D.blend(
                        8.0f,
                        Shape2D.ellipse(64.0f, 90.0f, 27.0f, 19.0f),
                        Shape2D.circle(48.0f, 78.0f, 15.0f),
                        Shape2D.circle(80.0f, 78.0f, 15.0f));
        canvas.fill(shape2D, i);
        canvas.fill(Shape2D.ellipse(28.0f, 52.0f, 11.0f, 14.0f).rotate(-25.0f, 28.0f, 52.0f), i);
        canvas.fill(Shape2D.ellipse(51.0f, 33.0f, 11.0f, 14.5f).rotate(-8.0f, 51.0f, 33.0f), i);
        canvas.fill(Shape2D.ellipse(77.0f, 33.0f, 11.0f, 14.5f).rotate(8.0f, 77.0f, 33.0f), i);
        canvas.fill(Shape2D.ellipse(100.0f, 52.0f, 11.0f, 14.0f).rotate(25.0f, 100.0f, 52.0f), i);
        return Decal.front(canvas, 1.2f, 20.6f, 1.6f, 1.5f, 1.5f).depth(1.5f, 1.5f).maxAngle(70.0f);
    }

    private static Canvas ribs(int i, int j) {
        Canvas canvas = new Canvas(j, 16);
        float f = (float) j / (float) i;
        for (int k = 0; k < i; ++k) {
            float g = (float) k * f + f * 0.5f;
            canvas.fill(Shape2D.box(g, 8.0f, f * 0.18f, 8.0f, 0.0f), 1078491756);
        }
        return canvas;
    }

    private static Decal hemRib() {
        return Decal.wrap(
                        Mika.ribs(64, 506),
                        0.0f,
                        14.2f,
                        -0.05f,
                        0.0f,
                        1.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        1.0f,
                        360.0f,
                        0.62f)
                .radius(1.2f, 3.3f);
    }

    private static Decal cuffRib() {
        return Decal.wrap(
                        Mika.ribs(24, 256),
                        -3.77f,
                        14.3f,
                        0.57f,
                        0.06f,
                        1.65f,
                        -0.22f,
                        0.0f,
                        0.0f,
                        1.0f,
                        360.0f,
                        1.2f)
                .radius(0.6f, 1.4f);
    }

    private static Decal bellDecal(float f) {
        Canvas canvas = new Canvas(64, 64);
        canvas.fill(Shape2D.box(32.0f, 30.0f, 30.0f, 2.0f, 1.0f), -5737444);
        canvas.fill(Shape2D.circle(32.0f, 40.0f, 4.5f), -11915254);
        canvas.fill(Shape2D.line(32.0f, 40.0f, 32.0f, 60.0f, 3.5f), -11915254);
        canvas.fill(Shape2D.ellipse(24.0f, 20.0f, 5.0f, 3.5f), -1426063361);
        return Decal.front(canvas, f, 19.6f, 2.5f, 1.0f, 1.0f).depth(0.6f, 0.6f).maxAngle(70.0f);
    }

    private static Decal laces() {
        Canvas canvas = new Canvas(96, 128);
        int j = -7710102;
        for (int i = 0; i < 4; ++i) {
            float f = 30.0f + (float) i * 22.0f;
            canvas.fill(Shape2D.line(24.0f, f, 72.0f, f + 14.0f, 6.0f), -1);
            canvas.fill(Shape2D.line(72.0f, f, 24.0f, f + 14.0f, 6.0f), -1);
            canvas.fill(Shape2D.circle(20.0f, f, 3.5f), j);
            canvas.fill(Shape2D.circle(76.0f, f, 3.5f), j);
        }
        canvas.fill(Shape2D.ellipse(36.0f, 14.0f, 11.0f, 6.0f).outline(4.0f), -1);
        canvas.fill(Shape2D.ellipse(60.0f, 14.0f, 11.0f, 6.0f).outline(4.0f), -1);
        canvas.fill(Shape2D.circle(48.0f, 16.0f, 4.0f), -1);
        Vec3 vec3 = new Vec3(-1.2f, 0.0f, 0.2f);
        Vec3 vec32 = new Vec3(-1.2f, 1.62f, 0.72f).rotate(0.0f, -7.0f, 0.0f, vec3);
        Vec3 vec33 = new Vec3(0.0f, 0.8f, 0.6f).rotate(0.0f, -7.0f, 0.0f);
        Vec3 vec34 = new Vec3(0.0f, 0.6f, -0.8f).rotate(0.0f, -7.0f, 0.0f);
        return Decal.project(
                        canvas, vec32.x(), vec32.y(), vec32.z(), vec33.x(), vec33.y(), vec33.z(),
                        vec34.x(), vec34.y(), vec34.z(), 0.95f, 1.25f)
                .depth(0.6f, 0.6f)
                .maxAngle(65.0f);
    }

    @Environment(value = EnvType.CLIENT)
    private static final class Curve {

        private final float[] xs;

        private final float[] ys;

        private final float[] zs;

        private final float[] len;

        private final int n;

        Curve(float[] floats) {
            int k = floats.length / 4 - 2;
            int m = 12;
            this.n = k * m + 1;
            this.xs = new float[this.n];
            this.ys = new float[this.n];
            this.zs = new float[this.n];
            this.len = new float[this.n];
            int o = 0;
            for (int i = 1; i <= k; ++i) {
                int p = (i - 1) * 4;
                int q = i * 4;
                int r = (i + 1) * 4;
                float f = i == 1 ? floats[0] : (floats[p] + floats[q]) * 0.5f;
                float g = i == 1 ? floats[1] : (floats[p + 1] + floats[q + 1]) * 0.5f;
                float h = i == 1 ? floats[2] : (floats[p + 2] + floats[q + 2]) * 0.5f;
                float f2 = i == k ? floats[r] : (floats[q] + floats[r]) * 0.5f;
                float g2 = i == k ? floats[r + 1] : (floats[q + 1] + floats[r + 1]) * 0.5f;
                float h2 = i == k ? floats[r + 2] : (floats[q + 2] + floats[r + 2]) * 0.5f;
                for (int j = 0; j < m || i == k && j == m; ++j) {
                    float f3 = (float) j / (float) m;
                    float g3 = 1.0f - f3;
                    this.xs[o] = g3 * g3 * f + 2.0f * g3 * f3 * floats[q] + f3 * f3 * f2;
                    this.ys[o] = g3 * g3 * g + 2.0f * g3 * f3 * floats[q + 1] + f3 * f3 * g2;
                    this.zs[o] = g3 * g3 * h + 2.0f * g3 * f3 * floats[q + 2] + f3 * f3 * h2;
                    if (o > 0) {
                        float h3 = this.xs[o] - this.xs[o - 1];
                        float f4 = this.ys[o] - this.ys[o - 1];
                        float g4 = this.zs[o] - this.zs[o - 1];
                        this.len[o] =
                                this.len[o - 1] + (float) Math.sqrt(h3 * h3 + f4 * f4 + g4 * g4);
                    }
                    ++o;
                }
            }
        }

        float param(float f, float g, float h) {
            int j = 0;
            float mAX_VALUE = Float.MAX_VALUE;
            for (int i = 0; i < this.n; ++i) {
                float f2 = f - this.xs[i];
                float g2 = g - this.ys[i];
                float h2 = h - this.zs[i];
                float f3 = f2 * f2 + g2 * g2 + h2 * h2;
                if (!(f3 < mAX_VALUE)) continue;
                mAX_VALUE = f3;
                j = i;
            }
            return this.len[j] / this.len[this.n - 1];
        }

        int tailColor(float f, float g, float h) {
            float param = this.param(f, g, h);
            return Colors.mix(
                    Colors.mix(-3647457, -681403, Mika.smooth(0.04f, 0.22f, param)),
                    -3618,
                    Mika.smooth(0.72f, 0.8f, param));
        }

        Decal chevron(Canvas canvas, float f) {
            int i;
            float g = f * this.len[this.n - 1];
            for (i = 1; i < this.n - 2 && this.len[i] < g; ++i) {}
            float h = this.xs[i + 1] - this.xs[i - 1];
            float f2 = this.ys[i + 1] - this.ys[i - 1];
            float g2 = this.zs[i + 1] - this.zs[i - 1];
            return Decal.wrap(
                            canvas,
                            this.xs[i],
                            this.ys[i],
                            this.zs[i],
                            h,
                            f2,
                            g2,
                            this.xs[i] - 1.8f,
                            this.ys[i] - 17.6f,
                            this.zs[i] + 3.3f,
                            140.0f,
                            0.9f)
                    .radius(0.3f, 1.6f);
        }
    }
}
