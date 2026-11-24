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
import java.util.List;

@Environment(value = EnvType.CLIENT)
public final class Luna extends CharacterDesign {

    private static final int SKIN = -6958;

    private static final int SKIN_SHADE = -1331558;

    private static final int SKIN_LINE = -5737368;

    private static final int HAIR = -1912065;

    private static final int HAIR_MID = -4940554;

    private static final int HAIR_TIP = -7706904;

    private static final int HAIR_ROOT = -6519080;

    private static final int HAIR_LIGHT = -1647873;

    private static final int HAIR_LINE = -10794346;

    private static final int NAVY = -14603682;

    private static final int NAVY_DARK = -15460295;

    private static final int NAVY_LINE = -15921370;

    private static final int BODICE = -13945223;

    private static final int TIER = -12693598;

    private static final int LINING = -7571752;

    private static final int RIBBON = -4544530;

    private static final int PETTICOAT = -4611086;

    private static final int LACE = -1036;

    private static final int GOLD = -1326758;

    private static final int GOLD_DARK = -5011921;

    private static final int GOLD_PALE = -2868;

    private static final int GOLD_LINE = -8760296;

    private static final int TIGHTS = -12959880;

    private static final int TIGHTS_SHEEN = -10656082;

    private static final int BOOT = -7710146;

    private static final int BOOT_DARK = -9550799;

    private static final int BOOT_CUFF = -6395064;

    private static final int LEATHER = -8762574;

    private static final int LEATHER_DARK = -11653344;

    private static final int MOON = -4688;

    private static final float HEAD_K = 1.05f;

    private static final float NECK_Y = 24.7f;

    private static final float NECK_Z = -0.1f;

    private static final float AXIS_Z = -0.3f;

    private static final float HAT_TILT_X = -8.0f;

    private static final float HAT_TILT_Z = 4.0f;

    private static final Vec3 HAT_CENTRE = new Vec3(0.0f, 29.15f, -0.25f);

    private static final Shape2D BRIM = Shape2D.taper(2.9f, 29.25f, 0.82f, 5.72f, 29.0f, 0.5f);

    private static final float BRIM_WAVE = 0.3f;

    private static final float RIM_R = 5.86f;

    private static final Vec3 ELBOW = new Vec3(-3.72f, 18.4f, -0.05f);

    private static final float ELBOW_PITCH = -12.0f;

    private static final float ELBOW_ROLL = 3.0f;

    private static final float FACE_PPU = 41.48148f;

    private static final float FACE_LEFT = -2.3142858f;

    private static final float FACE_TOP = 28.810715f;

    private static final float[] CAPE_Y = new float[] {7.0f, 10.0f, 13.5f, 17.0f, 20.5f, 23.4f};

    private static final float[] CAPE_RX = new float[] {5.15f, 4.85f, 4.15f, 3.45f, 3.2f, 3.15f};

    private static final float[] CAPE_RZ = new float[] {4.45f, 4.2f, 3.5f, 2.3f, 1.62f, 1.3f};

    private static final float[] CAPE_CZ =
            new float[] {-0.8f, -0.85f, -0.85f, -0.6f, -0.45f, -0.35f};

    private static final float[] CAPE_WRAP =
            new float[] {104.0f, 102.0f, 100.0f, 105.0f, 118.0f, 132.0f};

    private static final int CAPE_STEPS = 96;

    private static final float[] CAPE_LUT = Luna.capeTable();

    private static final float[] CURTAIN_Y = new float[] {17.0f, 20.0f, 22.5f, 24.5f, 27.6f, 29.3f};

    private static final float[] CURTAIN_RX = new float[] {3.2f, 3.05f, 2.95f, 2.95f, 3.12f, 2.35f};

    private static final float[] CURTAIN_RZ = new float[] {3.0f, 2.65f, 2.25f, 2.55f, 3.05f, 2.35f};

    private static final float[] CURTAIN_CZ =
            new float[] {-0.95f, -0.95f, -0.9f, -0.65f, -0.3f, -0.3f};

    public Luna() {
        super("luna", "Luna");
    }

    @Override
    protected void define(Rig rig) {
        rig.scale(1.0f).outline(-15396047, 0.15f).occlusion(0.35f);
        Canvas canvas2 = new Canvas(128, 12);
        Luna.paintBand(canvas2);
        Shape shape2 = Luna.faceShape();
        Shape shape3 =
                Shapes.ellipsoid(0.0f, 27.55f, -0.35f, 2.82f, 2.94f, 2.88f)
                        .intersect(Shapes.plane(0.0f, 24.6f, -2.7f, 0.0f, -1.0f, 0.86f));
        List<Lock> list = Luna.bangs(shape2);
        Luna.head(rig, shape2, list);
        Luna.hair(rig, shape3, list);
        Luna.hat(rig);
        Luna.body(rig);
        Luna.cape(rig);
        Luna.skirt(rig, canvas2);
        Luna.arms(rig, canvas2);
        Luna.legs(rig);
    }

    private static Shape faceShape() {
        Shape shape2 = Shapes.ellipsoid(0.0f, 27.5f, -0.3f, 2.38f, 2.5f, 2.42f);
        Shape shape3 = Shapes.ellipsoid(0.0f, 26.3f, 0.3f, 1.84f, 1.32f, 1.84f);
        Shape shape4 = Shapes.cone(0.0f, 25.75f, 0.4f, 1.28f, 0.0f, 24.74f, 1.02f, 0.34f);
        return Shapes.blend(1.0f, shape2, shape3, shape4);
    }

    private static void head(Rig rig, Shape shape2, List<Lock> list) {
        rig.part(Bone.HEAD)
                .pivot(0.0f, 24.7f, -0.1f)
                .shape(Luna.hs(shape2.paint(Paint.of(-6958))))
                .resolution(0.9f)
                .detail(1.5f)
                .flatLight(1.0f)
                .occlusion(0.2f)
                .outline(-5737368, 0.08f)
                .decal(Luna.faceDecal(list));
    }

    private static Shape hs(Shape shape2) {
        return shape2.scale(1.05f, 0.0f, 24.7f, -0.1f);
    }

    private static Vec3 hp(float f, float g, float h) {
        return new Vec3(f * 1.05f, 24.7f + (g - 24.7f) * 1.05f, -0.1f + (h - -0.1f) * 1.05f);
    }

    private static Part at(Part part, Vec3 vec3) {
        return part.pivot(vec3.x(), vec3.y(), vec3.z());
    }

    private static List<Lock> bangs(Shape shape2) {
        float[][] floatGrid =
                new float[][] {
                    {-50.0f, 26.5f, 0.75f, 0.3f, 0.0f},
                    {-24.0f, 27.6f, 0.72f, 0.25f, 0.0f},
                    {4.0f, 26.9f, 0.8f, -0.2f, 0.0f},
                    {27.0f, 27.8f, 0.7f, -0.25f, 0.0f},
                    {52.0f, 26.62f, 0.75f, -0.3f, 0.0f},
                    {-37.0f, 27.2f, 0.36f, 0.2f, 1.0f},
                    {40.0f, 27.02f, 0.36f, -0.2f, 1.0f}
                };
        ArrayList<Lock> list = new ArrayList<Lock>();
        for (float[] floats : floatGrid) {
            float[] floats2 = new float[10];
            float[] floats3 = new float[10];
            for (int i = 0; i < 10; ++i) {
                floats2[i] = floats[1] - 0.3f + (29.6f - floats[1] + 0.3f) * (float) i / 9.0f;
                floats3[i] = Luna.surfaceR(shape2, floats[0], floats2[i]);
            }
            list.add(
                    floats[4] > 0.0f
                            ? new Lock(
                                    floats[0], 29.4f, floats[1], floats[2], floats[3], 0.7f, 0.75f,
                                    0.1f, 0.3f, 0.2f, floats2, floats3)
                            : new Lock(
                                    floats[0], 29.4f, floats[1], floats[2], floats[3], 0.5f, 0.45f,
                                    0.05f, 0.4f, 0.22f, floats2, floats3));
        }
        return list;
    }

    private static float interp(float[] floats, float[] floats2, float f) {
        int i = floats.length;
        if (f <= floats[0]) {
            return floats2[0];
        }
        if (f >= floats[i - 1]) {
            return floats2[i - 1];
        }
        int j = 0;
        while (f > floats[j + 1]) {
            ++j;
        }
        return floats2[j]
                + (floats2[j + 1] - floats2[j]) * (f - floats[j]) / (floats[j + 1] - floats[j]);
    }

    private static float surfaceR(Shape shape2, float f, float g) {
        float distance;
        double d = Math.toRadians(f);
        float h = (float) Math.sin(d);
        float f2 = (float) Math.cos(d);
        float g2 = 4.5f;
        for (int i = 0;
                i < 80
                        && !(Math.abs(distance = shape2.distance(h * g2, g, -0.3f + f2 * g2))
                                < 0.001f);
                ++i) {
            g2 -= distance;
        }
        return g2;
    }

    private static void hair(Rig rig, Shape shape2, List<Lock> list) {
        int j;
        Lock lock;
        ArrayList<Shape> list2 = new ArrayList<Shape>();
        list2.add(shape2);
        for (int i = -1; i <= 1; i += 2) {
            list2.add(
                    Shapes.strand(
                                    0.74f,
                                    0.24f,
                                    (float) i * 2.35f,
                                    29.2f,
                                    -0.9f,
                                    (float) i * 3.04f,
                                    27.6f,
                                    -0.72f,
                                    (float) i * 3.02f,
                                    26.0f,
                                    -0.5f,
                                    (float) i * 2.78f,
                                    24.5f,
                                    -0.35f)
                            .flattenFrom(1.5f, 0.0f, 26.8f, -0.6f));
        }
        Paint paint =
                Paint.field(
                        (value, value2, value3) ->
                                Luna.shine(
                                        Colors.mix(
                                                -4940554,
                                                -1912065,
                                                Luna.clamp01((value2 - 24.6f) / 3.6f)),
                                        value,
                                        value2,
                                        value3,
                                        28.1f,
                                        0.26f,
                                        7,
                                        0.3f));
        rig.extra("hair", Bone.HEAD)
                .shape(Luna.hs(Shapes.union(list2.toArray(new Shape[0])).paint(paint)))
                .resolution(1.3f)
                .detail(0.6f)
                .flatLight(0.4f)
                .noOutline();
        ArrayList<Shape> list3 = new ArrayList<Shape>();
        for (Lock lock2 : list) {
            list3.add(lock2.shape());
        }
        Paint paint2 =
                Paint.field(
                        (value4, value5, value6) ->
                                Luna.shine(
                                        Colors.mix(
                                                -1647873,
                                                -6519080,
                                                Luna.clamp01((value5 - 27.1f) / 1.9f)),
                                        value4,
                                        value5,
                                        value6,
                                        28.45f,
                                        0.14f,
                                        11,
                                        0.9f));
        rig.extra("bangs", Bone.HEAD)
                .shape(Luna.hs(Shapes.union(list3.toArray(new Shape[0])).paint(paint2)))
                .resolution(0.45f)
                .detail(1.3f)
                .flatLight(0.7f)
                .occlusion(0.15f)
                .outline(-10794346, 0.08f);
        lock =
                new Lock(
                        -72.0f,
                        29.4f,
                        24.3f,
                        0.62f,
                        0.9f,
                        0.0f,
                        0.0f,
                        0.0f,
                        0.45f,
                        0.3f,
                        new float[] {24.0f, 25.0f, 26.2f, 27.6f, 28.8f, 29.6f},
                        new float[] {2.26f, 2.54f, 2.72f, 2.7f, 2.42f, 2.0f});
        Paint paint3 =
                Paint.field(
                        (value7, value8, value9) ->
                                value8 > 27.2f
                                        ? Colors.mix(
                                                -1647873,
                                                -6519080,
                                                Luna.clamp01((value8 - 27.2f) / 1.8f))
                                        : Colors.mix(
                                                -4940554,
                                                -1647873,
                                                Luna.clamp01((value8 - 24.3f) / 2.9f)));
        rig.extra("locks", Bone.HEAD)
                .shape(Luna.hs(lock.shape().mirrorX().paint(paint3)))
                .resolution(0.45f)
                .detail(0.7f)
                .flatLight(0.5f)
                .occlusion(0.3f)
                .outline(-10794346, 0.08f);
        ArrayList<Shape> list4 = new ArrayList<Shape>();
        ArrayList<Shape> list5 = new ArrayList<Shape>();
        for (j = 0; j < 5; ++j) {
            list4.add(
                    Luna.backLock(
                            118.0f + 31.0f * (float) j,
                            -0.22f,
                            18.2f + (float) (j % 2) * 0.35f,
                            1.25f,
                            0.5f,
                            2.6f,
                            (float) j * 1.7f));
        }
        for (j = 0; j < 7; ++j) {
            float f = (float) j / 6.0f;
            float g =
                    16.9f
                            + 1.4f * (float) Math.pow(Math.abs(f - 0.5f) * 2.0f, 1.5)
                            + (float) (j % 3) * 0.3f;
            list5.add(
                    Luna.backLock(
                            116.0f + 128.0f * f,
                            j % 2 == 0 ? 0.14f : 0.06f,
                            g,
                            0.66f,
                            0.12f,
                            1.7f,
                            (float) j * 2.3f));
        }
        Shape shape3 =
                Shapes.union(list4.toArray(new Shape[0]))
                        .paint(Paint.vertical(-9152312, 18.0f, -5993238, 26.0f));
        Shape shape4 =
                Shapes.union(list5.toArray(new Shape[0]))
                        .paint(
                                Paint.field(
                                        (value10, value11, value12) ->
                                                Luna.shine(
                                                        Luna.backHair(value11),
                                                        value10,
                                                        value11,
                                                        value12,
                                                        27.25f,
                                                        0.3f,
                                                        6,
                                                        1.1f)));
        Luna.at(rig.extra("hair_back", Bone.HEAD), Luna.hp(0.0f, 28.2f, -1.6f))
                .shape(Luna.hs(Shapes.union(shape3, shape4)))
                .motion(Motion.hair().hang(0.8f).drag(18.0f))
                .resolution(1.15f)
                .detail(0.85f)
                .flatLight(0.35f)
                .outline(-10794346, 0.1f);
        Strand strand =
                Shapes.tube(
                                -2.3f, 29.0f, -0.2f, 0.52f, -2.8f, 27.4f, -0.05f, 0.58f, -2.95f,
                                25.6f, 0.25f, 0.56f, -2.8f, 23.8f, 1.0f, 0.5f, -2.66f, 22.2f, 1.7f,
                                0.46f, -2.62f, 20.8f, 2.02f, 0.42f)
                        .flattenFrom(1.45f, 0.0f, 22.0f, -0.5f);
        int m = 20;
        float[] floats = new float[m * 4];
        for (int i = 0; i < m; ++i) {
            float h = (float) i / ((float) m - 1.0f);
            double d = Math.PI + (double) h * Math.PI * 5.0;
            float f2 = 0.8f - 0.22f * h;
            floats[i * 4] = -1.82f + f2 * (float) Math.cos(d);
            floats[i * 4 + 1] = 20.8f - 3.3f * h;
            floats[i * 4 + 2] = 2.02f + f2 * (float) Math.sin(d);
            floats[i * 4 + 3] = 0.42f - 0.18f * h;
        }
        Shape shape5 =
                Shapes.union(strand, Shapes.tube(floats))
                        .paint(Paint.vertical(-7706904, 17.4f, -1912065, 25.0f));
        Part part =
                Luna.at(rig.extra("curl_r", Bone.HEAD), Luna.hp(-2.35f, 28.7f, -0.15f))
                        .shape(Luna.hs(shape5))
                        .resolution(0.9f)
                        .motion(
                                Motion.hair()
                                        .drag(4.0f)
                                        .walk(3.0f, 0.0f, 3.5f, 2.0f)
                                        .hang(0.7f)
                                        .turn(10.0f, 0.0f)
                                        .bend(1.4f))
                        .detail(0.9f)
                        .flatLight(0.4f)
                        .outline(-10794346, 0.1f);
        rig.extra("curl_l", Bone.HEAD).mirrorOf(part);
    }

    private static int backHair(float f) {
        if (f > 26.2f) {
            return Colors.mix(-1912065, -6519080, Luna.clamp01((f - 26.2f) / 1.9f));
        }
        if (f > 22.0f) {
            return Colors.mix(-4940554, -1912065, Luna.clamp01((f - 22.0f) / 4.2f));
        }
        return Colors.mix(-7706904, -4940554, Luna.clamp01((f - 17.2f) / 4.8f));
    }

    private static int shine(
            int i, float f, float g, float h, float f2, float g2, int j, float h2) {
        float f3 = (float) Math.atan2(f, h - -0.3f);
        float g3 = Math.abs(g - f2 + 0.25f * (1.0f - (float) Math.cos(f3)));
        if (g3 > g2) {
            return i;
        }
        float h3 = (float) Math.sin(f3 * (float) j + h2) + 0.2f;
        if (h3 <= 0.0f) {
            return i;
        }
        float f4 = (1.0f - g3 / g2) * Math.min(1.0f, h3 * 3.0f) * 0.8f;
        return Colors.mix(i, Colors.mix(i, -1, 0.5f), Math.min(1.0f, f4 * 1.6f));
    }

    private static Shape backLock(
            float f, float g, float h, float f2, float g2, float h2, float f3) {
        double d = Math.toRadians(f);
        float g3 = (float) Math.sin(d);
        float h3 = (float) Math.cos(d);
        float[] floats = new float[] {29.3f, 27.6f, 25.3f, 23.0f, 20.8f, 18.9f, h};
        float[] floats2 = new float[floats.length * 3];
        for (int i2 = 0; i2 < floats.length; ++i2) {
            float f4 = floats[i2];
            float g4 = Luna.spline(CURTAIN_Y, CURTAIN_RX, f4) + g;
            float h4 = Luna.spline(CURTAIN_Y, CURTAIN_RZ, f4) + g;
            float spline = Luna.spline(CURTAIN_Y, CURTAIN_CZ, f4);
            float f5 =
                    0.32f * Luna.clamp01((26.5f - f4) / 5.0f) * (float) Math.sin(f4 * 0.95f + f3);
            float g5 = i2 == floats.length - 1 ? 0.16f : 0.0f;
            floats2[i2 * 3] = (g4 + g5) * g3 + f5 * h3;
            floats2[i2 * 3 + 1] = f4;
            floats2[i2 * 3 + 2] = spline + (h4 + g5) * h3 - f5 * g3;
        }
        return Shapes.strand(f2, g2, floats2).flatten(h2, g3, 0.0f, h3);
    }

    private static void hat(Rig rig) {
        Paint paint = Paint.vertical(-15460295, 28.6f, -14603682, 31.0f);
        Shape shape2 =
                Shapes.field(Luna::brimDistance, -6.1f, 28.1f, -6.35f, 6.1f, 30.1f, 5.85f)
                        .paint(paint);
        rig.extra("brim", Bone.HEAD)
                .shape(Luna.worn(shape2))
                .resolution(1.2f)
                .detail(0.65f)
                .outline(-15921370, 0.12f);
        Shape shape3 =
                Shapes.field(Luna::rimDistance, -6.2f, 28.35f, -6.45f, 6.2f, 29.65f, 5.95f)
                        .paint(Paint.of(-1326758).grain(0.6f, 0.08f));
        Shape shape4 =
                Shapes.cone(0.0f, 28.7f, -0.25f, 3.43f, 0.0f, 31.2f, -0.3f, 3.08f)
                        .intersect(Shapes.box(0.0f, 29.95f, -0.27f, 3.6f, 0.5f, 3.6f))
                        .subtract(Shapes.cone(0.0f, 28.7f, -0.25f, 2.75f, 0.0f, 31.2f, -0.3f, 2.4f))
                        .paint(Paint.vertical(-6650664, 29.45f, -4544530, 30.3f));
        rig.extra("hat_trim", Bone.HEAD)
                .shape(Luna.worn(Shapes.union(shape3, shape4)))
                .resolution(0.95f)
                .detail(0.35f)
                .noOutline();
        Shape shape5 =
                Shapes.cone(0.0f, 28.7f, -0.25f, 3.2f, 0.0f, 31.2f, -0.3f, 2.85f)
                        .intersect(Shapes.box(0.0f, 29.6f, -0.27f, 3.5f, 0.75f, 3.5f));
        rig.extra("hat", Bone.HEAD)
                .shape(Luna.worn(shape5.paint(paint)))
                .resolution(1.2f)
                .detail(0.3f)
                .outline(-15921370, 0.15f);
        rig.extra("hat_pin", Bone.HEAD)
                .shape(Luna.worn(Luna.pin()))
                .resolution(0.4f)
                .detail(0.9f)
                .outline(-8760296, 0.06f);
        Shape shape6 =
                Shapes.tube(
                                0.0f, 30.2f, -0.29f, 2.8f, 0.0f, 32.2f, -0.42f, 2.24f, 0.0f, 34.1f,
                                -0.74f, 1.56f, 0.04f, 35.3f, -1.42f, 1.02f, 0.14f, 36.0f, -2.45f,
                                0.64f, 0.3f, 36.15f, -3.65f, 0.36f)
                        .intersect(Shapes.above(30.15f))
                        .subtract(0.25f, Shapes.ellipsoid(0.0f, 34.45f, -2.3f, 0.9f, 0.35f, 0.45f))
                        .paint(Paint.vertical(-14603682, 30.0f, -14011792, 37.0f));
        Vec3 vec3 = Luna.hatPoint(0.0f, 30.4f, -0.3f);
        Part part =
                Luna.at(
                                rig.extra("hat_tip", rig.part(Bone.HEAD)),
                                Luna.hp(vec3.x(), vec3.y(), vec3.z()))
                        .shape(Luna.worn(shape6))
                        .resolution(1.25f)
                        .motion(
                                new Motion()
                                        .idle(2.0f, 0.0f, 2.0f, 0.3f)
                                        .walk(6.0f, 0.0f, 4.0f, 2.0f)
                                        .drag(7.0f)
                                        .hang(0.25f)
                                        .turn(10.0f, 0.0f)
                                        .spring(38.0f, 4.5f)
                                        .bend(1.7f)
                                        .limit(35.0f))
                        .detail(0.75f)
                        .outline(-15921370, 0.15f);
        Vec3 vec32 = Luna.hatPoint(0.28f, 35.9f, -3.5f);
        Shape shape7 =
                Shapes.strand(
                                0.08f,
                                0.08f,
                                vec32.x(),
                                vec32.y(),
                                vec32.z(),
                                vec32.x(),
                                vec32.y() - 0.8f,
                                vec32.z())
                        .color(-5011921);
        Shape shape8 =
                Shapes.extrude(
                                Shape2D.star(0.0f, 0.0f, 0.62f, 0.27f, 5)
                                        .rotate(180.0f, 0.0f, 0.0f),
                                0.14f,
                                0.05f)
                        .rotate(0.0f, 90.0f, 0.0f)
                        .move(vec32.x(), vec32.y() - 1.3f, vec32.z())
                        .paint(Paint.of(-9872).glow());
        Luna.at(rig.extra("hat_charm", part), Luna.hp(vec32.x(), vec32.y(), vec32.z()))
                .shape(Luna.hs(Shapes.union(shape7, shape8)))
                .motion(Motion.hair().hang(0.9f).spring(60.0f, 6.0f))
                .resolution(0.45f)
                .detail(1.2f)
                .outline(-8760296, 0.05f);
    }

    private static float brimDistance(float f, float g, float h) {
        float f2 = h + 0.25f;
        float g2 = (float) Math.sqrt(f * f + f2 * f2);
        return BRIM.distance(g2, g - Luna.wave(f, f2, g2, Luna.clamp01((g2 - 2.9f) / 2.9f)));
    }

    private static float rimDistance(float f, float g, float h) {
        float f2 = h + 0.25f;
        float g2 = (float) Math.sqrt(f * f + f2 * f2);
        float h2 = g2 - 5.86f;
        float f3 = g - 29.0f - Luna.wave(f, f2, g2, 1.0f);
        return (float) Math.sqrt(h2 * h2 + f3 * f3) - 0.31f;
    }

    private static float wave(float f, float g, float h, float f2) {
        if (h < 0.001f) {
            return 0.0f;
        }
        return 0.3f * f2 * f2 * (f * f - g * g) / (h * h);
    }

    private static Shape pin() {
        Shape2D shape2D =
                Shape2D.circle(0.0f, 0.0f, 0.68f).subtract(Shape2D.circle(0.34f, 0.18f, 0.52f));
        Shape2D shape2D2 = Shape2D.star(0.46f, 0.14f, 0.38f, 0.17f, 5).rotate(180.0f, 0.46f, 0.14f);
        Shape shape2 =
                Shapes.union(
                        Shapes.extrude(shape2D, 0.14f, 0.06f)
                                .paint(
                                        Paint.radial(-4688, -0.3f, -0.2f, 0.0f, 0.8f, -1326758)
                                                .glow()),
                        Shapes.extrude(shape2D2, 0.12f, 0.05f)
                                .move(0.0f, 0.0f, 0.04f)
                                .paint(Paint.of(-9872).glow()));
        double d = Math.toRadians(38.0);
        float f = 3.38f;
        return shape2.rotate(-8.0f, 38.0f, 0.0f)
                .move(f * (float) Math.sin(d), 29.95f, -0.25f + f * (float) Math.cos(d));
    }

    private static Shape worn(Shape shape2) {
        return Luna.hs(
                shape2.rotate(-8.0f, 0.0f, 4.0f, HAT_CENTRE.x(), HAT_CENTRE.y(), HAT_CENTRE.z()));
    }

    private static Vec3 hatPoint(float f, float g, float h) {
        return new Vec3(f, g, h).rotate(-8.0f, 0.0f, 4.0f, HAT_CENTRE);
    }

    private static void body(Rig rig) {
        Shape shape2 = Shapes.ellipsoid(0.0f, 21.35f, 0.05f, 2.2f, 1.75f, 1.4f);
        Shape shape3 = Shapes.capsule(0.0f, 22.95f, -0.15f, -2.45f, 22.4f, -0.15f, 0.72f).mirrorX();
        Shape shape4 = Shapes.ellipsoid(0.0f, 19.0f, 0.0f, 1.62f, 1.5f, 1.15f);
        Shape shape5 = Shapes.ellipsoid(0.0f, 16.8f, -0.05f, 2.2f, 1.75f, 1.5f);
        Shape shape6 = Shapes.blend(1.1f, shape2, shape3, shape4, shape5);
        Shape shape7 = shape6.paint(Paint.vertical(-14933168, 16.5f, -13945223, 22.5f));
        Shape shape8 =
                Shapes.capsule(0.0f, 22.9f, -0.15f, 0.0f, 25.1f, -0.05f, 0.6f)
                        .paint(
                                Paint.vertical(-6958, 23.7f, -1331558, 24.2f)
                                        .flatLight(1.0f)
                                        .occlusion(0.25f));
        Shape shape9 =
                Shapes.cone(0.0f, 22.5f, -0.12f, 1.02f, 0.0f, 24.3f, -0.1f, 0.78f)
                        .intersect(Shapes.box(0.0f, 23.45f, -0.1f, 1.3f, 0.63f, 1.3f));
        Shape shape10 =
                shape6.inflate(0.22f)
                        .intersect(Shapes.box(0.0f, 18.45f, 0.0f, 3.0f, 0.6f, 3.0f))
                        .paint(Paint.vertical(-11653344, 17.9f, -8762574, 18.9f));
        rig.part(Bone.BODY)
                .pivot(0.0f, 24.5f, 0.0f)
                .shape(Shapes.union(shape7, shape8, shape10))
                .resolution(1.2f)
                .detail(0.9f)
                .occlusion(0.3f)
                .outline(-15921370, 0.14f)
                .decal(Luna.bodiceTrim());
        rig.extra("collar", Bone.BODY)
                .shape(shape9.paint(Paint.vertical(-15460295, 22.9f, -13945223, 24.05f)))
                .resolution(0.6f)
                .detail(0.35f)
                .occlusion(0.3f)
                .outline(-15921370, 0.08f);
        ArrayList<Shape> list = new ArrayList<Shape>();
        float surfaceZ = Luna.surfaceZ(shape6, 0.0f, 23.1f, 4.0f);
        float f = Math.max(surfaceZ, Luna.surfaceZ(shape9, 0.0f, 23.1f, 4.0f));
        list.add(Shapes.ellipsoid(0.0f, 23.08f, f + 0.12f, 0.3f, 0.26f, 0.2f).color(-4544530));
        list.add(
                Shapes.ellipsoid(0.6f, 23.14f, f + 0.02f, 0.5f, 0.3f, 0.18f)
                        .rotate(0.0f, -18.0f, -14.0f, 0.6f, 23.14f, f)
                        .mirrorX()
                        .color(-4544530));
        for (int i = -1; i <= 1; i += 2) {
            float surfaceZ2 = Luna.surfaceZ(shape6, (float) i * 0.35f, 22.3f, 4.0f);
            float surfaceZ3 = Luna.surfaceZ(shape6, (float) i * 0.55f, 21.6f, 4.0f);
            list.add(
                    Shapes.strand(
                                    0.22f,
                                    0.16f,
                                    (float) i * 0.12f,
                                    22.95f,
                                    f + 0.08f,
                                    (float) i * 0.35f,
                                    22.3f,
                                    surfaceZ2 + 0.14f,
                                    (float) i * 0.55f,
                                    21.6f,
                                    surfaceZ3 + 0.12f)
                            .flatten(1.7f, 0.0f, 0.0f, 1.0f)
                            .color(-4544530));
        }
        list.add(Shapes.sphere(0.0f, 23.08f, f + 0.3f, 0.17f).paint(Paint.of(-4678401).glow()));
        float surfaceZ4 = Luna.surfaceZ(shape10, 0.0f, 18.45f, 4.0f);
        list.add(
                Shapes.extrude(
                                Shape2D.star(0.0f, 0.0f, 0.56f, 0.25f, 5)
                                        .rotate(180.0f, 0.0f, 0.0f),
                                0.17f,
                                0.05f)
                        .move(0.0f, 18.45f, surfaceZ4 + 0.08f)
                        .color(-1326758));
        rig.extra("trim", Bone.BODY)
                .shape(Shapes.union(list.toArray(new Shape[0])))
                .resolution(0.5f)
                .detail(0.55f)
                .noOutline();
    }

    private static void cape(Rig rig) {
        Shape shape2 =
                Shapes.field(Luna::capeDistance, -6.0f, 6.3f, -6.2f, 6.0f, 25.4f, 1.3f)
                        .paint(Paint.field(Luna::capeColor));
        rig.extra("cape", Bone.BODY)
                .pivot(0.0f, 23.0f, -1.0f)
                .shape(shape2)
                .resolution(1.35f)
                .motion(Motion.cloth().drag(12.0f).hang(0.6f))
                .detail(0.45f)
                .occlusion(0.3f)
                .noOutline()
                .decal(Luna.capeStars())
                .decal(Luna.capeHem());
    }

    private static float capeAt(int i, float f) {
        float g = (f - CAPE_Y[0]) / (CAPE_Y[CAPE_Y.length - 1] - CAPE_Y[0]) * 96.0f;
        g = g < 0.0f ? 0.0f : Math.min(g, 96.0f);
        int j = Math.min((int) g, 95);
        float h = CAPE_LUT[j * 4 + i];
        return h + (CAPE_LUT[j * 4 + 4 + i] - h) * (g - (float) j);
    }

    private static float[] capeTable() {
        float[] floats = new float[388];
        for (int i = 0; i <= 96; ++i) {
            float f = CAPE_Y[0] + (CAPE_Y[CAPE_Y.length - 1] - CAPE_Y[0]) * (float) i / 96.0f;
            floats[i * 4] = Luna.spline(CAPE_Y, CAPE_RX, f);
            floats[i * 4 + 1] = Luna.spline(CAPE_Y, CAPE_RZ, f);
            floats[i * 4 + 2] = Luna.spline(CAPE_Y, CAPE_CZ, f);
            floats[i * 4 + 3] = Luna.spline(CAPE_Y, CAPE_WRAP, f);
        }
        return floats;
    }

    private static float capeDistance(float f, float g, float h) {
        float f2;
        float g2;
        float cape = Luna.capeAt(0, g);
        float cape2 = Luna.capeAt(1, g);
        float cape3 = Luna.capeAt(2, g);
        float h2 = Math.abs(Luna.ellipse(f, h - cape3, cape, cape2)) - 0.43f;
        if (h2 < 0.6f) {
            g2 = (float) Math.toDegrees(Math.atan2(Math.abs(f) / cape, -(h - cape3) / cape2));
            h2 = Math.max(h2, (g2 - Luna.capeAt(3, g)) * 0.0174533f * (cape + cape2) * 0.5f);
        }
        h2 = Math.max(h2, Math.max(g - 23.4f, 7.0f - g));
        if (g < 22.5f) {
            return h2;
        }
        g2 = Luna.clamp01((g - 22.9f) / 2.3f);
        float f3 = 1.45f + 0.35f * g2;
        float g3 = Math.abs(Luna.ellipse(f, h + 0.3f, f3, f2 = 1.3f + 0.3f * g2)) - 0.38f;
        if (g3 < 0.6f) {
            float h3 = (float) Math.toDegrees(Math.atan2(Math.abs(f) / f3, -(h + 0.3f) / f2));
            g3 = Math.max(g3, (h3 - 118.0f) * 0.0174533f * 1.5f);
        }
        g3 = Math.max(g3, Math.max(g - 25.2f, 22.8f - g));
        return Math.min(h2, g3);
    }

    private static int capeColor(float f, float g, float h) {
        float ellipse;
        float cape = Luna.capeAt(0, g);
        float cape2 = Luna.capeAt(1, g);
        float cape3 = Luna.capeAt(2, g);
        if (g > 22.9f) {
            float f2 = Luna.clamp01((g - 22.9f) / 2.3f);
            float ellipse2 = Luna.ellipse(f, h + 0.3f, 1.45f + 0.35f * f2, 1.3f + 0.3f * f2);
            if (Math.abs(ellipse2) < Math.abs(Luna.ellipse(f, h - cape3, cape, cape2))) {
                // CFR emitted f6; bytecode uses this ellipse value
                return ellipse2 > 0.0f ? -14603682 : -7571752;
            }
        }
        if ((ellipse = Luna.ellipse(f, h - cape3, cape, cape2)) < 0.0f) {
            return -7571752;
        }
        return Colors.mix(-15460295, -14603682, Luna.clamp01((g - 7.0f) / 14.0f));
    }

    private static Decal capeStars() {
        Canvas canvas2 = new Canvas(176, 176);
        long l = 7L;
        for (int i = 0; i < 36; ++i) {
            l = l * 6364136223846793005L + 1442695040888963407L;
            float f = 7.0f + (float) ((l >>> 33) % 162L);
            l = l * 6364136223846793005L + 1442695040888963407L;
            float g = 8.0f + (float) ((l >>> 33) % 160L);
            l = l * 6364136223846793005L + 1442695040888963407L;
            float h = 2.8f + (float) ((l >>> 33) % 3L);
            boolean bl = (l >>> 40) % 3L == 0L;
            Shape2D shape2D =
                    bl
                            ? Shape2D.star(f, g, h * 1.3f, h * 0.3f, 4)
                            : Shape2D.star(f, g, h, h * 0.46f, 5);
            canvas2.fill(shape2D, bl ? -598638 : -1326758);
        }
        canvas2.fill(
                Shape2D.circle(88.0f, 92.0f, 19.0f).subtract(Shape2D.circle(97.0f, 86.0f, 16.0f)),
                -1326758);
        canvas2.fill(Shape2D.star(97.0f, 88.0f, 7.5f, 3.2f, 5), -598638);
        return Decal.back(canvas2, 0.0f, 12.5f, -3.0f, 11.0f, 11.0f)
                .depth(4.0f, 4.0f)
                .maxAngle(62.0f);
    }

    private static Decal capeHem() {
        Canvas canvas2 = new Canvas(192, 12);
        Luna.paintBand(canvas2);
        return Decal.wrap(
                        canvas2, 0.0f, 7.27f, -0.8f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, -1.0f, 230.0f,
                        0.48f)
                .offset(0.03f);
    }

    private static void paintBand(Canvas canvas2) {
        float height = canvas2.height();
        float f = height * 2.0f;
        canvas2.fillAll(
                (value, i) -> {
                    float g = i / height;
                    if (g < 0.1f || g > 0.9f) {
                        return 0;
                    }
                    if (g < 0.24f || g > 0.76f) {
                        return -5011921;
                    }
                    float h = ((float) Math.floor(value / f) + 0.5f) * f;
                    return Math.abs(value - h) + Math.abs(i - height * 0.5f) < height * 0.22f
                            ? -2868
                            : -1326758;
                });
    }

    private static Canvas lace(int i, int j) {
        Canvas canvas2 = new Canvas(i, 16);
        float f = (float) i / (float) j;
        canvas2.fill(Shape2D.box((float) i * 0.5f, 3.0f, (float) i * 0.5f, 3.0f, 0.0f), -1036);
        for (int k = 0; k < j; ++k) {
            float g = ((float) k + 0.5f) * f;
            canvas2.fill(Shape2D.circle(g, 6.5f, f * 0.52f), -1036);
            canvas2.erase(Shape2D.circle(g, 6.8f, f * 0.16f));
        }
        return canvas2;
    }

    private static void skirt(Rig rig, Canvas canvas2) {
        Canvas canvas3 = Luna.lace(448, 34);
        for (int i = -1; i <= 1; i += 2) {
            Bone bone = i < 0 ? Bone.RIGHT_LEG : Bone.LEFT_LEG;
            Shape shape2 =
                    Shapes.union(
                            Luna.tier(
                                            i, 18.5f, 12.4f, 1.75f, 3.45f, 2.5f, 11.0f, 0.24f,
                                            0.78f, 6.0f, 16.0f)
                                    .shape(-13945223, -15460295),
                            Luna.tier(
                                            i, 13.4f, 9.1f, 3.1f, 3.95f, 1.3f, 14.0f, 0.28f, 0.74f,
                                            14.0f, 22.0f)
                                    .shape(-12693598, -14603682),
                            Luna.tier(
                                            i, 9.55f, 8.6f, 3.7f, 4.05f, 1.0f, 20.0f, 0.16f, 0.74f,
                                            20.0f, 24.0f)
                                    .shape(-4611086, -7571752));
            rig.extra(i < 0 ? "skirt_r" : "skirt_l", Bone.BODY)
                    .pivot(0.0f, 18.2f, 0.0f)
                    .shape(shape2)
                    .resolution(1.15f)
                    .motion(Motion.skirt(bone).follow(bone, 0.3f, 12.0f))
                    .detail(0.4f)
                    .occlusion(0.3f)
                    .noOutline()
                    .decal(Luna.hemBand(canvas2, 12.58f, (float) i * 76.0f, 140.0f, 0.36f))
                    .decal(Luna.hemBand(canvas2, 9.28f, (float) i * 79.0f, 134.0f, 0.36f))
                    .decal(Luna.hemBand(canvas3, 8.8f, (float) i * 80.0f, 132.0f, 0.34f));
        }
        Tier tier =
                new Tier(
                        18.2f, 8.7f, 1.6f, 2.9f, 1.6f, 16.0f, 0.12f, 0.66f, -42.0f, -42.0f, 42.0f,
                        42.0f);
        rig.extra("underskirt", Bone.BODY)
                .pivot(0.0f, 18.2f, 0.2f)
                .shape(tier.shape(-4611086, -7571752))
                .resolution(1.1f)
                .motion(
                        new Motion()
                                .drag(-8.0f)
                                .walk(-5.0f, 0.0f, 0.0f, 2.0f)
                                .walkPhase(90.0f)
                                .spring(120.0f, 14.0f)
                                .bend(1.0f))
                .detail(0.4f)
                .occlusion(0.3f)
                .noOutline()
                .decal(Luna.underskirtStars());
    }

    private static Tier tier(
            int i,
            float f,
            float g,
            float h,
            float f2,
            float g2,
            float h2,
            float f3,
            float g3,
            float h3,
            float f4) {
        if (i > 0) {
            return new Tier(f, g, h, f2, g2, h2, f3, g3, h3, f4, 188.0f, 188.0f);
        }
        return new Tier(f, g, h, f2, g2, h2, f3, g3, -188.0f, -188.0f, -h3, -f4);
    }

    private static Decal hemBand(Canvas canvas2, float f, float g, float h, float f2) {
        double d = Math.toRadians(g);
        return Decal.wrap(
                        canvas2,
                        0.0f,
                        f,
                        0.0f,
                        0.0f,
                        1.0f,
                        0.0f,
                        (float) Math.sin(d),
                        0.0f,
                        (float) Math.cos(d),
                        h,
                        f2)
                .offset(0.03f);
    }

    private static Decal underskirtStars() {
        float f;
        int i;
        Canvas canvas2 = new Canvas(192, 288);
        long l = 3L;
        for (i = 0; i < 20; ++i) {
            l = l * 6364136223846793005L + 1442695040888963407L;
            f = 12.0f + (float) ((l >>> 33) % 168L);
            l = l * 6364136223846793005L + 1442695040888963407L;
            float g = 88.0f + (float) ((l >>> 33) % 170L);
            canvas2.fill(Shape2D.star(f, g, 5.4f, 1.7f, 4), -1326758);
        }
        for (i = 0; i < 18; ++i) {
            f = ((float) i + 0.5f) * 192.0f / 18.0f;
            canvas2.fill(Shape2D.circle(f, 278.0f, 6.2f), -1036);
            canvas2.erase(Shape2D.circle(f, 278.5f, 1.9f));
        }
        canvas2.fill(Shape2D.box(96.0f, 272.0f, 96.0f, 4.0f, 0.0f), -1036);
        return Decal.front(canvas2, 0.0f, 13.5f, 3.0f, 6.4f, 9.6f)
                .depth(3.5f, 3.0f)
                .maxAngle(66.0f);
    }

    private static Decal bodiceTrim() {
        int i;
        Canvas canvas2 = new Canvas(64, 96);
        for (i = -1; i <= 1; i += 2) {
            canvas2.fill(
                    Shape2D.path(
                            2.0f,
                            2.0f,
                            32.0f + (float) i * 10.5f,
                            2.0f,
                            32.0f + (float) i * 9.0f,
                            48.0f,
                            32.0f + (float) i * 9.8f,
                            94.0f),
                    -1326758);
        }
        for (i = 0; i < 4; ++i) {
            float f = 13.0f + (float) i * 21.0f;
            canvas2.fill(Shape2D.circle(32.0f, f, 5.0f), -5011921);
            canvas2.fill(Shape2D.circle(32.0f, f, 4.0f), -1326758);
            canvas2.fill(Shape2D.circle(30.8f, f - 1.3f, 1.5f), -2868);
        }
        return Decal.front(canvas2, 0.0f, 20.6f, 3.0f, 2.0f, 3.0f)
                .depth(2.5f, 2.0f)
                .maxAngle(70.0f);
    }

    private static Shape fore(Shape shape2, int i) {
        return shape2.rotate(
                -12.0f, 0.0f, (float) i * 3.0f, (float) i * ELBOW.x(), ELBOW.y(), ELBOW.z());
    }

    private static Vec3 fore(float f, float g, float h) {
        return new Vec3(f, g, h).rotate(-12.0f, 0.0f, 3.0f, ELBOW);
    }

    private static void arms(Rig rig, Canvas canvas2) {
        Paint paint = Paint.of(-6958).flatLight(1.0f).occlusion(0.25f);
        Shape shape2 = Shapes.ellipsoid(-3.5f, 22.0f, -0.1f, 0.92f, 1.0f, 0.9f);
        Shape shape3 = Shapes.cone(-3.55f, 21.5f, -0.1f, 0.66f, -3.72f, 18.45f, -0.05f, 0.55f);
        Shape shape4 = Shapes.sphere(-3.72f, 18.4f, -0.05f, 0.55f);
        Shape shape5 =
                Luna.fore(Shapes.cone(-3.72f, 18.3f, -0.05f, 0.54f, -3.84f, 15.6f, 0.12f, 0.5f), 1);
        Shape shape6 =
                Shapes.blend(0.35f, shape2, shape3, shape4, shape5)
                        .paint(Paint.vertical(-14867372, 15.0f, -13945223, 22.0f));
        Shape shape7 =
                Luna.fore(
                                Shapes.cone(-3.84f, 16.1f, 0.12f, 0.52f, -3.92f, 14.3f, 0.2f, 1.02f)
                                        .intersect(Shapes.above(14.3f)),
                                1)
                        .color(-13945223);
        Shape shape8 =
                Luna.fore(Shapes.cone(-3.84f, 15.7f, 0.12f, 0.2f, -3.93f, 14.0f, 0.2f, 0.5f), 1)
                        .color(-7571752);
        Shape shape9 = Shapes.union(shape6, Shapes.subtract(shape7, shape8));
        Vec3 vec3 = Luna.fore(-4.0f, 13.3f, 0.42f);
        Vec3 vec32 = Luna.fore(-3.91f, 14.52f, 0.2f);
        Vec3 vec33 = new Vec3(-0.04f, 1.0f, 0.05f).rotate(-12.0f, 0.0f, 3.0f);
        Vec3 vec34 = new Vec3(0.0f, 0.0f, 1.0f).rotate(-12.0f, 0.0f, 3.0f);
        Part part =
                rig.part(Bone.RIGHT_ARM)
                        .pivot(-3.5f, 22.3f, -0.1f)
                        .grip(vec3.x(), vec3.y(), vec3.z())
                        .shape(shape9)
                        .resolution(0.95f)
                        .detail(0.75f)
                        .occlusion(0.3f)
                        .outline(-15921370, 0.13f)
                        .swing(0.8f)
                        .joint(ELBOW.x(), ELBOW.y(), ELBOW.z(), 0.55f)
                        .decal(
                                Decal.wrap(
                                                canvas2, vec32.x(), vec32.y(), vec32.z(), vec33.x(),
                                                vec33.y(), vec33.z(), vec34.x(), vec34.y(),
                                                vec34.z(), 360.0f, 0.36f)
                                        .offset(0.03f));
        rig.part(Bone.LEFT_ARM).mirrorOf(part);
        ArrayList<Shape> list = new ArrayList<Shape>();
        list.add(Shapes.cone(3.92f, 14.6f, 0.22f, 0.33f, 3.97f, 13.8f, 0.28f, 0.33f));
        list.add(Shapes.ellipsoid(4.0f, 13.25f, 0.32f, 0.27f, 0.6f, 0.52f));
        float[] floats = new float[] {-24.0f, -19.0f, -14.0f, -8.0f};
        for (int i = 0; i < 4; ++i) {
            float f = 0.02f + (float) i * 0.21f;
            float g = i == 0 || i == 3 ? 0.62f : 0.8f;
            list.add(
                    Shapes.capsule(4.02f, 12.8f, f, 3.94f, 12.8f - g, f + 0.1f, 0.12f)
                            .rotate(0.0f, 0.0f, floats[i], 4.02f, 12.85f, f));
        }
        list.add(Shapes.capsule(3.86f, 13.35f, 0.76f, 3.74f, 12.75f, 0.96f, 0.14f));
        Part part2 =
                rig.extra("hand_l", Bone.LEFT_ARM)
                        .shape(
                                Luna.fore(
                                        Shapes.blend(0.18f, list.toArray(new Shape[0]))
                                                .paint(paint),
                                        -1))
                        .resolution(0.5f)
                        .detail(0.6f)
                        .outline(-5737368, 0.08f);
        rig.extra("hand_r", Bone.RIGHT_ARM).mirrorOf(part2);
    }

    private static void legs(Rig rig) {
        Shape shape2 = Shapes.cone(-1.3f, 15.3f, 0.0f, 1.18f, -1.1f, 8.6f, 0.08f, 0.82f);
        Shape shape3 = Shapes.sphere(-1.08f, 8.25f, 0.14f, 0.78f);
        Shape shape4 = Shapes.sphere(-1.08f, 8.3f, 0.45f, 0.55f);
        Shape shape5 = Shapes.cone(-1.08f, 8.0f, 0.1f, 0.8f, -1.14f, 2.1f, -0.1f, 0.5f);
        Shape shape6 = Shapes.ellipsoid(-1.12f, 5.9f, -0.3f, 0.85f, 1.5f, 0.9f);
        Shape shape7 =
                Shapes.blend(0.45f, shape2, shape3, shape4, shape5, shape6)
                        .paint(Paint.field(Luna::tights));
        Shape shape8 =
                Shapes.cone(-1.12f, 4.9f, -0.08f, 0.8f, -1.1f, 1.5f, -0.1f, 0.62f)
                        .intersect(Shapes.below(5.0f));
        Shape shape9 = Shapes.ellipsoid(-1.1f, 0.48f, 1.0f, 0.6f, 0.48f, 0.82f);
        Shape shape10 = Shapes.ellipsoid(-1.1f, 1.0f, 0.25f, 0.64f, 0.62f, 0.9f);
        Shape shape11 =
                Shapes.blend(0.35f, shape8, shape9, shape10)
                        .paint(Paint.vertical(-9550799, 0.5f, -7710146, 4.6f));
        Shape shape12 = Shapes.box(-1.1f, 0.42f, -0.5f, 0.4f, 0.42f, 0.36f, 0.14f).color(-12704232);
        Shape shape13 = Shapes.box(-1.1f, 0.12f, 0.97f, 0.64f, 0.12f, 0.8f, 0.1f).color(-12966888);
        Shape shape14 = Shapes.torus(-1.1f, 1.75f, -0.05f, 0.62f, 0.26f).color(-11653344);
        Shape shape15 =
                Shapes.ellipsoid(-1.72f, 1.75f, 0.57f, 0.24f, 0.3f, 0.14f)
                        .rotate(0.0f, -45.0f, 0.0f, -1.72f, 1.75f, 0.57f)
                        .color(-1326758);
        Shape shape16 =
                Shapes.union(shape11, shape12, shape13, shape14, shape15)
                        .rotate(0.0f, -7.0f, 0.0f, -1.12f, 0.0f, -0.1f);
        Shape shape17 = Shapes.union(shape7, shape16).intersect(Shapes.above(0.0f));
        Part part =
                rig.part(Bone.RIGHT_LEG)
                        .pivot(-1.3f, 15.3f, 0.0f)
                        .shape(shape17)
                        .resolution(1.2f)
                        .detail(0.75f)
                        .occlusion(0.3f)
                        .outline(-14411226, 0.13f)
                        .swing(0.55f, 30.0f)
                        .joint(-1.08f, 8.25f, 0.14f, 0.8f);
        rig.part(Bone.LEFT_LEG).mirrorOf(part);
        Shape shape18 =
                Shapes.cylinder(-1.12f, 4.45f, -0.08f, -1.12f, 5.18f, -0.08f, 0.94f, 0.18f)
                        .paint(Paint.vertical(-7710146, 4.5f, -6395064, 5.1f));
        Part part2 =
                rig.extra("cuff_r", Bone.RIGHT_LEG)
                        .shape(shape18)
                        .resolution(0.7f)
                        .detail(0.9f)
                        .occlusion(0.3f)
                        .outline(-12966888, 0.08f);
        rig.extra("cuff_l", Bone.LEFT_LEG).mirrorOf(part2);
    }

    private static int tights(float f, float g, float h) {
        float f2 = -1.1f - 0.2f * Luna.clamp01((g - 8.0f) / 7.0f);
        float g2 = 0.05f;
        float h2 = (float) Math.atan2(f - f2, h - g2);
        float f3 = (float) Math.exp(-(h2 + 0.35f) * (h2 + 0.35f) / 0.16f);
        int i =
                Colors.mix(
                        Colors.shade(-12959880, 0.82f), -12959880, Luna.clamp01((g - 3.0f) / 6.0f));
        return Colors.mix(i, -10656082, 0.55f * f3);
    }

    private static Decal faceDecal(List<Lock> list) {
        Canvas canvas2 = new Canvas(192, 158);
        Shape2D shape2D = null;
        for (Lock lock : list) {
            Shape2D shape2D2 = lock.shadow(0.28f).inflate(3.7333333f);
            shape2D = shape2D == null ? shape2D2 : shape2D.union(shape2D2);
        }
        canvas2.fill(shape2D, Colors.withAlpha(-1199194, 0.55f), 4.0f);
        canvas2.draw(Luna.features(), 12, 44);
        Vec3 vec3 = Luna.hp(0.0f, 26.90625f, 3.0f);
        return Decal.front(canvas2, vec3.x(), vec3.y(), vec3.z(), 4.8599997f, 3.9993749f)
                .depth(3.0f, 3.0f)
                .maxAngle(74.0f)
                .occlusion(0.3f)
                .flatLight(1.0f);
    }

    private static Canvas features() {
        float f = 0.5833333f;
        Canvas canvas2 = new Canvas(168, 114);
        Canvas canvas3 = new Canvas(64, 80);
        Luna.eye(new Pen(canvas3, f, 60.0f, 96.0f), 122.0f, 181.0f, -1);
        canvas2.draw(canvas3, 7, 0);
        canvas2.draw(canvas3.mirrored(), 97, 0);
        Pen pen = new Pen(canvas2, f, 48.0f, 96.0f);
        for (int i = -1; i <= 1; i += 2) {
            float g = 192.0f + (float) i * 70.0f;
            float h = 181.0f;
            pen.fill(Shape2D.star(g - 10.0f, h - 13.0f, 14.0f, 3.8f, 4), -1);
            pen.fill(Shape2D.circle(g - 10.0f, h - 13.0f, 4.8f), -1);
            pen.fill(Shape2D.star(g + 12.0f, h + 17.0f, 7.0f, 2.1f, 4), -285212673);
            pen.fill(Shape2D.circle(g + 10.0f, h - 2.0f, 2.3f), -1191182337);
            pen.fill(
                    Shape2D.ellipse(192.0f + (float) i * 96.0f, 226.0f, 27.0f, 12.0f),
                    1895793815,
                    12.0f);
        }
        pen.fill(
                Shape2D.curve(191.0f, 232.0f, 195.0f, 238.0f, 190.0f, 242.0f, 2.4f, 1.6f),
                -2135459206);
        pen.fill(
                Shape2D.curve(180.0f, 275.0f, 192.0f, 284.0f, 204.0f, 275.0f, 3.2f, 3.2f),
                -7060390);
        pen.fill(
                Shape2D.curve(188.0f, 285.0f, 192.0f, 287.0f, 196.0f, 285.0f, 1.8f, 1.8f),
                1438670960);
        return canvas2;
    }

    private static void eye(Pen pen, float f, float g, int j) {
        Shape2D shape2D = Shape2D.ellipse(f, g + 2.0f, 37.0f, 46.0f);
        pen.fill(shape2D, Paint2D.linear(f, g - 44.0f, f, g - 8.0f, -3160852, -1));
        Paint2D paint2D =
                Paint2D.linear(
                        f, g - 36.0f, f, g + 44.0f, -14282676, -11194192, -6656782, -2044673);
        pen.fill(
                Shape2D.ellipse(f, g + 4.0f, 29.0f, 41.0f).intersect(shape2D),
                (value, i) -> {
                    float h = (value - f) / 32.0f;
                    float f2 = (i - g + 28.0f) / 19.0f;
                    float g2 = (value - f) / 18.0f;
                    float h2 = (i - g - 27.0f) / 10.0f;
                    float f3 = (value - f) / 29.0f;
                    float g3 = (i - g - 4.0f) / 41.0f;
                    float h3 =
                            Luna.clamp01((1.0f - (float) Math.sqrt(h * h + f2 * f2)) * 8.0f)
                                    * 0.47f;
                    float f4 =
                            Luna.clamp01((1.0f - (float) Math.sqrt(g2 * g2 + h2 * h2)) * 5.0f)
                                    * 0.5f;
                    float g4 =
                            Luna.clamp01(((float) Math.sqrt(f3 * f3 + g3 * g3) - 0.88f) / 0.06f)
                                    * 0.82f;
                    int m =
                            Colors.mix(
                                    Colors.mix(paint2D.argb(value, i), -14676416, h3), -726785, f4);
                    return Colors.mix(m, -14479041, g4);
                });
        pen.fill(Shape2D.ellipse(f, g + 5.0f, 12.0f, 17.0f), -15136210);
        pen.fill(
                Shape2D.path(
                        6.0f,
                        12.0f,
                        f - (float) j * 37.0f,
                        g - 6.0f,
                        f - (float) j * 24.0f,
                        g - 36.0f,
                        f + (float) j * 2.0f,
                        g - 48.0f,
                        f + (float) j * 25.0f,
                        g - 42.0f,
                        f + (float) j * 39.0f,
                        g - 26.0f,
                        f + (float) j * 50.0f,
                        g - 32.0f),
                -14938064);
        pen.fill(
                Shape2D.curve(
                        f + (float) j * 40.0f,
                        g - 28.0f,
                        f + (float) j * 52.0f,
                        g - 40.0f,
                        f + (float) j * 58.0f,
                        g - 50.0f,
                        5.0f,
                        1.0f),
                -14938064);
        pen.fill(
                Shape2D.curve(
                        f + (float) j * 30.0f,
                        g - 38.0f,
                        f + (float) j * 38.0f,
                        g - 50.0f,
                        f + (float) j * 40.0f,
                        g - 58.0f,
                        3.5f,
                        1.0f),
                -14938064);
        pen.fill(
                Shape2D.curve(
                        f + (float) j * 6.0f,
                        g + 48.0f,
                        f + (float) j * 26.0f,
                        g + 44.0f,
                        f + (float) j * 35.0f,
                        g + 28.0f,
                        1.5f,
                        4.0f),
                -1069931696);
        pen.fill(
                Shape2D.path(
                        2.0f,
                        2.0f,
                        f - (float) j * 18.0f,
                        g - 55.0f,
                        f + (float) j * 4.0f,
                        g - 60.0f,
                        f + (float) j * 27.0f,
                        g - 53.0f),
                1883909461);
        pen.fill(
                Shape2D.path(
                        5.5f,
                        2.0f,
                        f - (float) j * 28.0f,
                        g - 70.0f,
                        f + (float) j * 2.0f,
                        g - 82.0f,
                        f + (float) j * 36.0f,
                        g - 76.0f),
                -7703368);
    }

    private static float ellipse(float f, float g, float h, float f2) {
        float g2 = f / h;
        float h2 = g / f2;
        float f3 = (float) Math.sqrt(g2 * g2 + h2 * h2);
        float g3 = (float) Math.sqrt(g2 * g2 / (h * h) + h2 * h2 / (f2 * f2));
        if (g3 < 1.0E-6f) {
            return -Math.min(h, f2);
        }
        return f3 * (f3 - 1.0f) / g3;
    }

    private static float spline(float[] floats, float[] floats2, float f) {
        int i;
        int j = floats.length;
        if (f <= floats[0]) {
            return floats2[0];
        }
        if (f >= floats[j - 1]) {
            return floats2[j - 1];
        }
        for (i = 0; i < j - 2 && f > floats[i + 1]; ++i) {}
        float g = (f - floats[i]) / (floats[i + 1] - floats[i]);
        float h = floats[i + 1] - floats[i];
        float f2 =
                i == 0
                        ? (floats2[1] - floats2[0]) / (floats[1] - floats[0])
                        : (floats2[i + 1] - floats2[i - 1]) / (floats[i + 1] - floats[i - 1]);
        float g2 =
                i + 2 >= j
                        ? (floats2[j - 1] - floats2[j - 2]) / (floats[j - 1] - floats[j - 2])
                        : (floats2[i + 2] - floats2[i]) / (floats[i + 2] - floats[i]);
        float h2 = g * g;
        float f3 = h2 * g;
        return (2.0f * f3 - 3.0f * h2 + 1.0f) * floats2[i]
                + (f3 - 2.0f * h2 + g) * h * f2
                + (-2.0f * f3 + 3.0f * h2) * floats2[i + 1]
                + (f3 - h2) * h * g2;
    }

    private static float surfaceZ(Shape shape2, float f, float g, float h) {
        float distance;
        float f2 = h;
        for (int i = 0; i < 80 && !(Math.abs(distance = shape2.distance(f, g, f2)) < 0.001f); ++i) {
            f2 -= distance;
        }
        return f2;
    }

    private static float clamp01(float f) {
        return f < 0.0f ? 0.0f : Math.min(f, 1.0f);
    }

    @Environment(value = EnvType.CLIENT)
    private static final class Lock {

        private static final float REF = 2.6f;

        private final float rad;

        private final float top;

        private final float tip;

        private final float width;

        private final float hook;

        private final float g0;

        private final float lift;

        private final float g1;

        private final float t0;

        private final float t1;

        private final float[] ys;

        private final float[] rs;

        private final float[] poly;

        private final Shape2D outline;

        Lock(
                float f,
                float top2,
                float tip2,
                float width2,
                float hook2,
                float g02,
                float lift2,
                float g12,
                float t02,
                float t12,
                float[] ys2,
                float[] rs2) {
            float g;
            int i;
            this.rad = (float) Math.toRadians(f);
            this.top = top2;
            this.tip = tip2;
            this.width = width2;
            this.hook = hook2;
            this.g0 = g02;
            this.lift = lift2;
            this.g1 = g12;
            this.t0 = t02;
            this.t1 = t12;
            this.ys = ys2;
            this.rs = rs2;
            int j = 7;
            float h = hook2 > 0.0f ? -1.0f : 1.0f;
            this.poly = new float[(2 * j + 3) * 2];
            int m = 0;
            this.poly[m++] = h * width2;
            this.poly[m++] = top2 + 0.5f;
            for (i = 0; i <= j; ++i) {
                g = 1.0f - (float) Math.pow(1.0f - (float) i / (float) j, 1.4f);
                this.poly[m++] = hook2 * g * g + h * width2 * (float) Math.pow(1.0f - g, 0.8f);
                this.poly[m++] = top2 + (tip2 - top2) * g;
            }
            for (i = j - 1; i >= 0; --i) {
                g = 1.0f - (float) Math.pow(1.0f - (float) i / (float) j, 1.4f);
                this.poly[m++] = hook2 * g * g - h * width2 * (float) Math.pow(1.0f - g, 1.25);
                this.poly[m++] = top2 + (tip2 - top2) * g;
            }
            this.poly[m++] = -h * width2;
            this.poly[m] = top2 + 0.5f;
            this.outline = Shape2D.polygon(this.poly);
        }

        private float gap(float f) {
            float g = 1.0f - f;
            return this.g0 * g * g + 2.0f * this.lift * f * g + this.g1 * f * f;
        }

        float distance(float f, float g, float h) {
            float f2;
            float g2 = h - -0.3f;
            float h2 = (float) Math.sqrt(f * f + g2 * g2);
            float f3 = Luna.clamp01((this.top - g) / (this.top - this.tip));
            float g3 = Luna.interp(this.ys, this.rs, g) + this.gap(f3);
            float h3 = Math.max(g3 - h2, h2 - g3 - (f2 = this.t0 + (this.t1 - this.t0) * f3));
            if (h3 > 0.8f) {
                return h3;
            }
            float f4 = 2.6f * ((float) Math.atan2(f, g2) - this.rad);
            float distance = this.outline.distance(f4, g);
            if (distance > 0.8f) {
                return distance;
            }
            float g4 =
                    Math.min(
                            1.0f,
                            Math.abs(f4 - this.hook * f3 * f3)
                                    / (this.width * (1.0f - f3) + 0.05f));
            h3 = Math.max(g3 - h2, h2 - g3 - f2 * (1.0f - 0.35f * g4 * g4));
            float h4 = Luna.clamp01(0.5f - 0.5f * (h3 - distance) / 0.07f);
            return h3 + (distance - h3) * h4 + 0.07f * h4 * (1.0f - h4);
        }

        Shape shape() {
            float f;
            float mAX_VALUE = Float.MAX_VALUE;
            float g = -3.4028235E38f;
            float mAX_VALUE2 = Float.MAX_VALUE;
            float h = 0.0f;
            for (int i = 0; i < this.poly.length; i += 2) {
                mAX_VALUE = Math.min(mAX_VALUE, this.poly[i]);
                g = Math.max(g, this.poly[i]);
            }
            for (float f2 : this.rs) {
                mAX_VALUE2 = Math.min(mAX_VALUE2, f2);
                h = Math.max(h, f2);
            }
            mAX_VALUE2 = Math.max(0.2f, mAX_VALUE2 + Math.min(this.g0, this.g1) - 0.15f);
            h += Math.max(this.g0, Math.max(this.lift, this.g1)) + this.t0 + 0.15f;
            float mAX_VALUE3 = Float.MAX_VALUE;
            float g2 = -3.4028235E38f;
            float mAX_VALUE4 = Float.MAX_VALUE;
            f = -3.4028235E38f;
            for (int i = 0; i <= 12; ++i) {
                double d = this.rad + (mAX_VALUE + (g - mAX_VALUE) * (float) i / 12.0f) / 2.6f;
                for (float h2 : new float[] {mAX_VALUE2, h}) {
                    mAX_VALUE3 = Math.min(mAX_VALUE3, h2 * (float) Math.sin(d));
                    g2 = Math.max(g2, h2 * (float) Math.sin(d));
                    mAX_VALUE4 = Math.min(mAX_VALUE4, -0.3f + h2 * (float) Math.cos(d));
                    f = Math.max(f, -0.3f + h2 * (float) Math.cos(d));
                }
            }
            return Shapes.field(
                    this::distance,
                    mAX_VALUE3 - 0.1f,
                    this.tip - 0.15f,
                    mAX_VALUE4 - 0.1f,
                    g2 + 0.1f,
                    this.top + 0.6f,
                    f + 0.1f);
        }

        Shape2D shadow(float f) {
            float[] floats = (float[]) this.poly.clone();
            for (int i = 0; i < floats.length; i += 2) {
                float g = floats[i + 1];
                float h =
                        Luna.interp(this.ys, this.rs, g)
                                + this.gap(Luna.clamp01((this.top - g) / (this.top - this.tip)))
                                + this.t0 * 0.5f;
                floats[i] =
                        (h * (float) Math.sin(this.rad + floats[i] / 2.6f) - -2.3142858f)
                                * 41.48148f;
                floats[i + 1] = (28.810715f - g + f) * 41.48148f;
            }
            return Shape2D.polygon(floats);
        }
    }

    @Environment(value = EnvType.CLIENT)
    private static final class Tier {

        private static final int STEPS = 48;

        private final float yTop;

        private final float yBot;

        private final float rTop;

        private final float rBot;

        private final float folds;

        private final float amp;

        private final float half;

        private final float a0Top;

        private final float a0Bot;

        private final float a1Top;

        private final float a1Bot;

        private final float[] shape = new float[98];

        Tier(
                float yTop2,
                float yBot2,
                float rTop2,
                float rBot2,
                float f,
                float folds2,
                float amp2,
                float g,
                float a0Top2,
                float a0Bot2,
                float a1Top2,
                float a1Bot2) {
            this.yTop = yTop2;
            this.yBot = yBot2;
            this.rTop = rTop2;
            this.rBot = rBot2;
            this.folds = folds2;
            this.amp = amp2;
            this.half = g * 0.5f;
            this.a0Top = a0Top2;
            this.a0Bot = a0Bot2;
            this.a1Top = a1Top2;
            this.a1Bot = a1Bot2;
            for (int i2 = 0; i2 <= 48; ++i2) {
                float h = Math.min((float) i2 / 48.0f, 0.999f);
                this.shape[i2 * 2] = 1.0f - (float) Math.pow(1.0f - h, f);
                this.shape[i2 * 2 + 1] = f * (float) Math.pow(1.0f - h, f - 1.0f);
            }
        }

        private float u(float f) {
            return Luna.clamp01((this.yTop - f) / (this.yTop - this.yBot));
        }

        private float table(int i, float f) {
            float g = f * 48.0f;
            int j = Math.min((int) g, 47);
            float h = this.shape[j * 2 + i];
            return h + (this.shape[j * 2 + 2 + i] - h) * (g - (float) j);
        }

        float radius(float f, float g, float h) {
            float f2 = this.u(g);
            return this.rTop
                    + (this.rBot - this.rTop) * this.table(0, f2)
                    + this.amp * f2 * (float) Math.cos((double) this.folds * Math.atan2(f, h));
        }

        float distance(float f, float g, float h) {
            double d;
            float f2;
            float g2 = this.u(g);
            float h2 = this.yTop - this.yBot;
            float f3 = (float) Math.sqrt(f * f + h * h);
            float g3 =
                    Math.abs(f3 - (f2 = this.rTop + (this.rBot - this.rTop) * this.table(0, g2)))
                            - this.half
                            - this.amp;
            if (g3 > 0.6f) {
                return Math.max(g3 * 0.7f, Math.max(g - this.yTop, this.yBot - g));
            }
            double e = Math.atan2(f, h);
            float h3 = (float) Math.cos((double) this.folds * e);
            float f4 = (float) Math.sin((double) this.folds * e);
            float g4 = f2 + this.amp * g2 * h3;
            float h4 = ((this.rBot - this.rTop) * this.table(1, g2) + this.amp * h3) / h2;
            float f5 = this.amp * g2 * this.folds * f4 / Math.max(f3, 0.5f);
            float g5 =
                    (Math.abs(f3 - g4) - this.half) / (float) Math.sqrt(1.0f + h4 * h4 + f5 * f5);
            g5 = Math.max(g5, Math.max(g - this.yTop, this.yBot - g));
            double d2 = Math.toRadians(this.a0Top + (this.a0Bot - this.a0Top) * g2);
            double e2 = Math.toRadians(this.a1Top + (this.a1Bot - this.a1Top) * g2);
            for (d = e - (d2 + e2) * 0.5; d > Math.PI; d -= Math.PI * 2) {}
            while (d < -Math.PI) {
                d += Math.PI * 2;
            }
            return Math.max(g5, (float) (Math.abs(d) - (e2 - d2) * 0.5) * f3);
        }

        Shape shape(int i, int j) {
            float f = Math.max(this.rTop, this.rBot) + this.amp + this.half + 0.2f;
            float g = Math.min(this.a0Top, this.a0Bot);
            float h = Math.max(this.a1Top, this.a1Bot);
            float f2 = 0.0f;
            float g2 = 0.0f;
            float h2 = 0.0f;
            float f3 = 0.0f;
            for (int k = 0; k <= 64; ++k) {
                double d = Math.toRadians(g + (h - g) * (float) k / 64.0f);
                f2 = Math.min(f2, (float) Math.sin(d) * f);
                g2 = Math.max(g2, (float) Math.sin(d) * f);
                h2 = Math.min(h2, (float) Math.cos(d) * f);
                f3 = Math.max(f3, (float) Math.cos(d) * f);
            }
            Shape shape2 =
                    Shapes.field(
                            this::distance,
                            f2 - 0.3f,
                            this.yBot - 0.2f,
                            h2 - 0.3f,
                            g2 + 0.3f,
                            this.yTop + 0.2f,
                            f3 + 0.3f);
            return shape2.paint(
                    Paint.field(
                            (value, value2, value3) -> {
                                float g3 = (float) Math.sqrt(value * value + value3 * value3);
                                if (g3 < this.radius(value, value2, value3)) {
                                    return j;
                                }
                                return Colors.mix(
                                        Colors.shade(i, 0.8f),
                                        i,
                                        Luna.clamp01(
                                                (value2 - this.yBot)
                                                        / (this.yTop - this.yBot)
                                                        * 1.6f));
                            }));
        }
    }

    @Environment(value = EnvType.CLIENT)
    private static final class Pen {

        private final Canvas canvas;

        private final float k;

        private final float ox;

        private final float oy;

        Pen(Canvas canvas2, float f, float ox2, float oy2) {
            this.canvas = canvas2;
            this.k = f;
            this.ox = ox2;
            this.oy = oy2;
        }

        private Shape2D map(Shape2D shape2D) {
            float f = this.k / (this.k - 1.0f);
            return shape2D.scale(this.k, this.ox * f, this.oy * f);
        }

        void fill(Shape2D shape2D, int i) {
            this.canvas.fill(this.map(shape2D), i);
        }

        void fill(Shape2D shape2D, int i, float f) {
            this.canvas.fill(this.map(shape2D), i, f * this.k);
        }

        void fill(Shape2D shape2D, Paint2D paint2D) {
            this.canvas.fill(
                    this.map(shape2D),
                    (value, value2) ->
                            paint2D.argb(value / this.k + this.ox, value2 / this.k + this.oy));
        }
    }
}
