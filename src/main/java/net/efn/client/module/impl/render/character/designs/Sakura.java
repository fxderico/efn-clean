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

@Environment(EnvType.CLIENT)
public final class Sakura extends CharacterDesign {

    private static final int SKIN = -9016;

    private static final int SKIN_SHADE = -1331558;

    private static final int SKIN_LINE = -5740452;

    private static final int HAIR_ROOT = -2592100;

    private static final int HAIR = -677434;

    private static final int HAIR_TIP = -12321;

    private static final int HAIR_SHADE = -3908470;

    private static final int HAIR_LINE = -6864801;

    private static final int SHINE = -855643406;

    private static final int WHITE = -263689;

    private static final int WHITE_SHADE = -2305053;

    private static final int SLEEVE_IN = -2502430;

    private static final int CLOTH_LINE = -7568744;

    private static final int RED = -2674372;

    private static final int RED_DARK = -5955793;

    private static final int RED_IN = -10087904;

    private static final int RED_LINE = -8514270;

    private static final int CORD = -1816774;

    private static final int GOLD = -1459895;

    private static final int BEAD = -2608577;

    private static final int STRAW = -1320012;

    private static final int SOLE = -10600144;

    private static final int PETAL = -8984;

    private static final int PETAL_CORE = -2078596;

    private static final float HAKAMA_TOP = 20.0F;

    private static final float HAKAMA_HEM = 2.5F;

    private static final float HS = 1.12F;

    private static final float DROP = 0.4F;

    private static final float HX = 0.0F;

    private static final float HY = 28.9F;

    private static final float HZ = -0.3F;

    private static final float PPU = 56.88889F;

    private static final float FACE_TOP = 29.95F;

    private static final float[] PLEATS = new float[] {0.21F, 0.7F, 2.76F};

    private static final float[][] LOCKS =
            new float[][] {
                {-0.08F, 28.38F, -2.0F, 0.95F, 0.12F, 0.34F},
                {-0.98F, 28.82F, -25.0F, 0.95F, 0.28F, 0.3F},
                {-1.92F, 28.5F, -47.0F, 0.92F, 0.26F, 0.3F},
                {0.92F, 28.9F, 24.0F, 0.95F, 0.28F, 0.3F},
                {1.9F, 28.62F, 46.0F, 0.92F, 0.25F, 0.3F},
                {-0.55F, 28.58F, -13.0F, 0.45F, 0.16F, 0.44F},
                {0.46F, 28.7F, 11.0F, 0.45F, 0.18F, 0.44F},
                {-1.46F, 28.72F, -36.0F, 0.42F, 0.2F, 0.42F},
                {1.42F, 28.8F, 35.0F, 0.42F, 0.2F, 0.42F}
            };

    private static final Shape2D SLEEVE_SIDE =
            Shape2D.polygon(
                            0.85F, 23.95F, 0.62F, 19.0F, 0.12F, 15.7F, -0.2F, 14.7F, -1.2F, 14.25F,
                            -2.25F, 14.0F, -2.9F, 14.2F, -3.2F, 14.8F, -3.25F, 16.0F, -2.2F, 22.4F,
                            -1.0F, 24.2F)
                    .inflate(0.3F);

    public Sakura() {
        super("sakura", "Sakura");
    }

    @Override
    protected void define(Rig rig) {
        rig.scale(1.0F).outline(-11914176, 0.12F).occlusion(0.3F).flatLight(0.55F);
        float[][] floatGrid = lockPaths();
        Paint paint = hairPaint();
        head(rig, floatGrid);
        hair(rig, paint);
        fringe(rig, floatGrid);
        kanzashi(rig);
        body(rig);
        hakama(rig);
        arms(rig);
        legs(rig);
    }

    private static Shape up(Shape shape) {
        return shape.scale(1.12F, 0.0F, 24.6F, -0.2F).move(0.0F, -0.4F, 0.0F);
    }

    private static float tx(float f) {
        return f * 1.12F;
    }

    private static float ty(float f) {
        return 24.6F + (f - 24.6F) * 1.12F - 0.4F;
    }

    private static float tz(float f) {
        return -0.2F + (f + 0.2F) * 1.12F;
    }

    private static Shape headBase() {
        return Shapes.blend(
                0.85F,
                Shapes.ellipsoid(0.0F, 28.72F, -0.15F, 2.28F, 2.4F, 2.35F),
                Shapes.ellipsoid(0.0F, 27.75F, 0.55F, 2.05F, 1.3F, 1.75F),
                Shapes.cone(0.0F, 27.3F, 0.62F, 1.35F, 0.0F, 26.05F, 1.2F, 0.3F));
    }

    private static void head(Rig rig, float[][] floatGrid) {
        Shape shape = Shapes.capsule(0.0F, 23.3F, -0.45F, 0.0F, 25.9F, -0.3F, 0.6F);
        float ty = ty(25.82F);
        float tz = tz(1.2F);
        Paint paint =
                Paint.field(
                        (value, i, j) -> {
                            float f = ty + Math.max(0.0F, tz - j) * 0.62F;
                            float g =
                                    smooth(f + 0.05F, f - 0.2F, i)
                                            * (0.6F + 0.4F * smooth(23.4F, 24.8F, i));
                            return Colors.mix(-9016, -1331558, g);
                        });
        Shape shape2 = up(headBase().intersect(Shapes.below(30.2F)));
        rig.part(Bone.HEAD)
                .pivot(0.0F, 24.6F, -0.2F)
                .shape(Shapes.blend(0.45F, shape2, shape).paint(paint))
                .detail(2.0F)
                .flatLight(1.0F)
                .occlusion(0.22F)
                .outline(-5740452, 0.1F)
                .decal(face(floatGrid));
    }

    private static float[][] lockPaths() {
        Shape shape = headBase();
        float[][] floatGrid = new float[LOCKS.length][];
        for (int i = 0; i < LOCKS.length; i++) {
            float[] floats = LOCKS[i];
            float[] floats2 = onHead(36.0F, floats[2], -0.15F);
            float[] floats3 = onHead(58.0F, floats[2], 0.05F);
            float f = floats3[0] + (floats[0] - floats3[0]) * 0.75F;
            float g = floats[1] + 0.68F;
            float h = floats[0] - Math.signum(floats[0]) * floats[4];
            floatGrid[i] =
                    new float[] {
                        floats2[0],
                        floats2[1],
                        floats2[2],
                        floats3[0],
                        floats3[1],
                        floats3[2],
                        f,
                        g,
                        frontZ(shape, f, g) + floats[5],
                        h,
                        floats[1],
                        frontZ(shape, h, floats[1]) + 0.22F,
                        floats[3]
                    };
        }
        return floatGrid;
    }

    private static float frontZ(Shape shape, float f, float g) {
        float h = 3.5F;
        while (h > -1.0F && shape.distance(f, g, h) > 0.0F) {
            h -= 0.02F;
        }
        return h;
    }

    private static void fringe(Rig rig, float[][] floatGrid) {
        Shape[] shapes = new Shape[floatGrid.length];
        Shape[] shapes2 = new Shape[floatGrid.length];
        for (int i = 0; i < floatGrid.length; i++) {
            float[] floats = floatGrid[i];
            float f = floats[12];
            Strand strand =
                    Shapes.tube(
                                    floats[0],
                                    floats[1],
                                    floats[2],
                                    0.9F * f,
                                    floats[3],
                                    floats[4],
                                    floats[5],
                                    1.15F * f,
                                    floats[6],
                                    floats[7],
                                    floats[8],
                                    0.95F * f,
                                    floats[9],
                                    floats[10],
                                    floats[11],
                                    0.12F)
                            .flattenFrom(3.0F, 0.0F, 28.7F, -0.15F);
            shapes2[i] = Shapes.extrude(Shape2D.polygon(blade(floats)), 5.0F, 0.0F);
            shapes[i] = Shapes.intersect(strand, shapes2[i]);
        }
        Shape shape = Shapes.intersect(Shapes.blend(0.15F, shapes), Shapes.union(shapes2));
        Shape shape2 = headBase();
        Paint paint =
                Paint.field(
                        (value, value2, value3) -> {
                            int j =
                                    Colors.mix(
                                            Colors.mix(-2592100, -677434, 0.45F),
                                            -677434,
                                            smooth(30.9F, 29.7F, value2));
                            j = Colors.mix(j, -12321, 0.5F * smooth(29.5F, 28.5F, value2));
                            return Colors.mix(
                                    j,
                                    -3908470,
                                    0.6F
                                            * (1.0F
                                                    - smooth(
                                                            0.12F,
                                                            0.42F,
                                                            shape2.distance(
                                                                    value, value2, value3))));
                        });
        rig.extra("fringe", Bone.HEAD)
                .shape(up(shape.paint(paint)))
                .resolution(0.45F)
                .detail(1.05F)
                .flatLight(0.7F)
                .occlusion(0.1F)
                .outline(-6864801, 0.09F)
                .decal(shine());
    }

    private static float[] blade(float[] floats) {
        float f = floats[12];
        boolean bl = floats[9] < 0.0F;
        float g = floats[3];
        float h = 31.6F;
        float f2 = floats[6];
        float g2 = floats[7];
        float h2 = floats[9];
        float f3 = floats[10];
        byte b = 12;
        float[] floats2 = new float[(2 * b + 1) * 2];
        for (int i = 0; i <= b; i++) {
            float g3 = (float) i / b;
            float h3 = 1.0F - g3;
            float f4 = h3 * h3 * g + 2.0F * h3 * g3 * f2 + g3 * g3 * h2;
            float g4 = h3 * h3 * h + 2.0F * h3 * g3 * g2 + g3 * g3 * f3;
            float h4 = h3 * (f2 - g) + g3 * (h2 - f2);
            float f5 = h3 * (g2 - h) + g3 * (f3 - g2);
            float g5 = (float) Math.sqrt(h4 * h4 + f5 * f5);
            float h5 = -f5 / g5;
            float f6 = h4 / g5;
            if (h5 > 0.0F != bl) {
                h5 = -h5;
                f6 = -f6;
            }
            float g6 = f * (1.0F - smooth(0.6F, 1.0F, g3));
            float h6 = f * (1.0F - (float) Math.pow(smooth(0.3F, 1.0F, g3), 0.75));
            floats2[i * 2] = f4 + h5 * g6;
            floats2[i * 2 + 1] = g4 + f6 * g6;
            if (i < b) {
                floats2[(2 * b - i) * 2] = f4 - h5 * h6;
                floats2[(2 * b - i) * 2 + 1] = g4 - f6 * h6;
            }
        }
        return floats2;
    }

    private static Decal shine() {
        Canvas canvas = new Canvas(280, 22);
        for (float[] floats :
                new float[][] {
                    {-68.0F, -54.0F},
                    {-45.0F, -25.0F},
                    {-13.0F, 9.0F},
                    {20.0F, 38.0F},
                    {48.0F, 64.0F}
                }) {
            float f = floats[0];
            float g = floats[1];
            float h = (f + g) * 0.5F;
            float f2 = 6.5F - Math.abs(h) * 0.025F;
            canvas.fill(
                    Shape2D.curve(
                            140.0F + f * 2.0F,
                            ringY(f),
                            140.0F + f + h,
                            ringY((f + h) * 0.5F),
                            140.0F + h * 2.0F,
                            ringY(h),
                            1.0F,
                            f2),
                    -855643406);
            canvas.fill(
                    Shape2D.curve(
                            140.0F + h * 2.0F,
                            ringY(h),
                            140.0F + h + g,
                            ringY((h + g) * 0.5F),
                            140.0F + g * 2.0F,
                            ringY(g),
                            f2,
                            1.0F),
                    -855643406);
        }
        return Decal.wrap(
                        canvas,
                        0.0F,
                        ty(30.16F),
                        tz(-0.3F),
                        0.0F,
                        1.0F,
                        0.0F,
                        0.0F,
                        0.0F,
                        1.0F,
                        140.0F,
                        0.6272F)
                .radius(1.2F, 3.8F)
                .offset(0.03F);
    }

    private static float ringY(float f) {
        float g = 30.4F - 0.36F * (1.0F - (float) Math.cos(Math.toRadians(f)));
        return (30.44F - g) * 40.0F;
    }

    private static void hair(Rig rig, Paint paint) {
        ArrayList list = new ArrayList();
        list.add(Shapes.ellipsoid(0.0F, 28.9F, -0.3F, 2.58F, 2.62F, 2.7F));
        for (float f : new float[] {64.0F, 88.0F, 272.0F, 296.0F}) {
            list.add(capLock(f, 5.0F, 98.0F, 0.8F, 0.62F));
        }
        Shape shape = Shapes.box(0.0F, 26.3F, 2.3F, 2.95F, 3.3F, 2.8F, 0.9F);
        Shape shape2 =
                Shapes.blend(
                                0.3F,
                                Shapes.strand(
                                                0.62F, 0.44F, -2.3F, 29.6F, -0.05F, -2.48F, 28.1F,
                                                -0.12F, -2.45F, 26.5F, -0.22F)
                                        .flatten(1.6F, 1.0F, 0.0F, 0.0F),
                                Shapes.strand(
                                                0.68F, 0.44F, -2.25F, 29.5F, -0.95F, -2.46F, 27.9F,
                                                -1.05F, -2.45F, 26.3F, -1.12F)
                                        .flatten(1.6F, 1.0F, 0.0F, 0.0F))
                        .mirrorX();
        Shape shape3 =
                Shapes.blend(
                        0.25F,
                        Shapes.subtract(
                                        Shapes.blend(0.3F, ((Shape[]) list.toArray(new Shape[0]))),
                                        shape)
                                .paint(paint),
                        shape2.paint(paint.occlusion(0.35F)));
        rig.extra("hair", Bone.HEAD)
                .shape(up(shape3))
                .detail(1.1F)
                .flatLight(0.55F)
                .occlusion(0.22F)
                .outline(-6864801, 0.09F)
                .decal(shine());
        Strand strand =
                Shapes.tube(
                                -1.95F, 30.4F, 0.92F, 0.55F, -2.3F, 29.3F, 1.12F, 0.6F, -2.42F,
                                27.8F, 1.0F, 0.55F, -2.41F, 25.9F, 0.72F, 0.3F, -2.3F, 25.1F, 0.74F,
                                0.36F, -2.12F, 24.7F, 0.78F, 0.32F)
                        .flatten(1.8F, 1.0F, 0.0F, 0.3F);
        Strand strand2 =
                Shapes.tube(
                                -2.2F, 30.2F, 0.1F, 0.58F, -2.52F, 28.9F, 0.24F, 0.62F, -2.58F,
                                27.3F, 0.32F, 0.55F, -2.41F, 25.9F, 0.56F, 0.3F, -2.28F, 25.0F,
                                0.54F, 0.36F, -2.2F, 24.35F, 0.56F, 0.32F)
                        .flatten(1.8F, 1.0F, 0.0F, 0.15F);
        Shape shape4 =
                Shapes.cylinder(-2.41F, 25.38F, 0.64F, -2.41F, 25.94F, 0.64F, 0.5F, 0.12F)
                        .color(-263689);
        Shape shape5 = Shapes.union(Shapes.blend(0.25F, strand, strand2).paint(paint), shape4);
        Motion motion =
                new Motion()
                        .idle(1.5F, 0.0F, 0.6F, 0.35F)
                        .walk(3.0F, 0.0F, 1.0F, 2.0F)
                        .drag(8.0F)
                        .hang(0.6F)
                        .spring(60.0F, 8.0F)
                        .bend(1.4F);
        Part part =
                rig.extra("sidelock_r", Bone.HEAD)
                        .pivot(tx(-2.3F), ty(30.0F), tz(0.6F))
                        .shape(up(shape5))
                        .motion(motion)
                        .resolution(0.7F)
                        .detail(0.6F)
                        .flatLight(0.5F)
                        .occlusion(0.35F)
                        .outline(-6864801, 0.09F)
                        .decal(tieCord());
        rig.extra("sidelock_l", Bone.HEAD).mirrorOf(part);
        backHair(rig, paint);
    }

    private static Decal tieCord() {
        Canvas canvas = new Canvas(40, 20);
        canvas.fill(Shape2D.box(20.0F, 10.0F, 20.0F, 2.8F, 0.0F), -1816774);
        canvas.fill(Shape2D.ellipse(20.0F, 10.0F, 4.0F, 5.0F), -1816774);
        return Decal.wrap(
                        canvas,
                        tx(-2.41F),
                        ty(25.67F),
                        tz(0.63F),
                        0.0F,
                        1.0F,
                        0.0F,
                        -1.0F,
                        0.0F,
                        0.45F,
                        220.0F,
                        0.56F)
                .radius(0.2F, 0.9F);
    }

    private static void backHair(Rig rig, Paint paint) {
        ArrayList list = new ArrayList();
        ArrayList list2 = new ArrayList();
        for (int i = 0; i < 7; i++) {
            float f = (i - 3.0F) / 3.0F;
            float g = 180.0F - f * 72.0F;
            float h = i % 2 == 0 ? 0.1F : 0.0F;
            float[] floats = onHead(26.0F, g, -0.02F);
            float[] floats2 = onHead(60.0F, g, 0.22F + h);
            float[] floats3 = onHead(88.0F, g, 0.34F + h);
            float[] floats4 = onHead(114.0F, g, 0.2F + h);
            list.add(
                    Shapes.strand(
                                    0.78F,
                                    0.72F,
                                    floats[0],
                                    floats[1],
                                    floats[2],
                                    floats2[0],
                                    floats2[1],
                                    floats2[2],
                                    floats3[0],
                                    floats3[1],
                                    floats3[2],
                                    floats4[0],
                                    floats4[1],
                                    floats4[2])
                            .flattenFrom(2.4F, 0.0F, 28.9F, -0.3F));
            float[] floats5 = onHead(104.0F, g, 0.32F);
            list2.add(
                    Shapes.strand(
                                    0.8F,
                                    0.64F,
                                    floats5[0],
                                    floats5[1],
                                    floats5[2],
                                    f * 2.85F,
                                    25.0F,
                                    -2.52F - 0.3F * (1.0F - f * f),
                                    f * 1.02F,
                                    21.9F,
                                    -2.8F)
                            .flatten(2.0F, 0.0F, 0.0F, 1.0F));
        }
        ArrayList list3 = new ArrayList();
        float[] floats6 = new float[] {14.1F, 13.72F, 13.58F, 13.78F, 14.16F};
        for (int j = 0; j < 5; j++) {
            float f2 = (j - 2.0F) / 2.0F;
            list3.add(
                    Shapes.strand(
                                    0.8F,
                                    0.14F,
                                    f2 * 0.72F,
                                    22.3F,
                                    -2.85F,
                                    f2 * 1.02F,
                                    18.9F,
                                    -3.08F,
                                    f2 * 0.84F,
                                    16.0F,
                                    -3.25F,
                                    f2 * 0.2F,
                                    floats6[j],
                                    -3.32F)
                            .flatten(1.6F, 0.0F, 0.0F, 1.0F));
        }
        Shape shape =
                Shapes.union(
                                Shapes.blend(0.1F, ((Shape[]) list.toArray(new Shape[0]))),
                                Shapes.blend(0.2F, ((Shape[]) list2.toArray(new Shape[0]))),
                                Shapes.blend(0.15F, ((Shape[]) list3.toArray(new Shape[0]))))
                        .paint(paint);
        Shape shape2 = Shapes.box(0.0F, 21.9F, -2.82F, 1.42F, 0.45F, 0.62F, 0.32F).color(-263689);
        Shape shape3 = Shapes.box(0.0F, 21.9F, -2.82F, 1.5F, 0.2F, 0.7F, 0.17F).color(-1816774);
        Shape shape4 =
                Shapes.ellipsoid(-1.4F, 22.2F, -3.5F, 1.2F, 0.66F, 0.38F)
                        .paint(Paint.radial(-2305053, -0.3F, 22.0F, -3.6F, 1.6F, -263689))
                        .rotate(0.0F, 0.0F, -16.0F, -1.4F, 22.2F, -3.5F);
        Shape shape5 = Shapes.ellipsoid(0.0F, 22.0F, -3.58F, 0.48F, 0.52F, 0.36F).color(-1816774);
        Shape shape6 =
                Shapes.strand(
                                0.44F, 0.38F, -0.3F, 21.6F, -3.5F, -0.8F, 20.5F, -3.45F, -1.2F,
                                19.4F, -3.4F)
                        .flatten(2.2F, 0.0F, 0.0F, 1.0F)
                        .color(-263689);
        Shape shape7 =
                Shapes.union(
                        shape2,
                        shape3,
                        shape4,
                        shape4.mirrored(),
                        shape5,
                        shape6,
                        shape6.mirrored());
        rig.extra("hair_back", Bone.HEAD)
                .pivot(0.0F, ty(29.0F), tz(-1.6F))
                .shape(up(Shapes.union(shape, shape7)))
                .motion(Motion.hair().hang(0.0F).follow(Bone.BODY, 0.85F).drag(12.0F))
                .detail(0.58F)
                .flatLight(0.5F)
                .occlusion(0.35F)
                .outline(-6864801, 0.09F)
                .decal(tailLines());
    }

    private static Decal tailLines() {
        Canvas canvas = new Canvas(48, 108);
        float f = 12.0F;
        float g = 22.0F;
        int i = 1890076012;
        for (int j = 0; j < 4; j++) {
            float h = (j - 1.5F) / 2.0F;
            float f2 = h * 0.72F;
            float g2 = h * 1.02F;
            float h2 = h * 0.84F;
            float f3 = h * 0.2F;
            float g3 = (g2 + h2) * 0.5F;
            float h3 = 14.6F + Math.abs(h) * 0.4F;
            canvas.fill(
                    Shape2D.curve(
                            24.0F - f * f2,
                            (g - 21.4F) * f,
                            24.0F - f * g2,
                            (g - 18.9F) * f,
                            24.0F - f * g3,
                            (g - 17.45F) * f,
                            0.4F,
                            1.5F),
                    i);
            canvas.fill(
                    Shape2D.curve(
                            24.0F - f * g3,
                            (g - 17.45F) * f,
                            24.0F - f * h2,
                            (g - 16.0F) * f,
                            24.0F - f * f3,
                            (g - h3) * f,
                            1.5F,
                            0.3F),
                    i);
        }
        return Decal.back(canvas, 0.0F, ty(g - 54.0F / f), tz(-3.4F), 4.48F, 10.08F)
                .depth(0.9F, 1.0F)
                .maxAngle(70.0F);
    }

    private static void kanzashi(Rig rig) {
        Shape shape =
                Shapes.union(
                        blossom(1.6F).rotate(-22.0F, 42.0F, 0.0F).move(2.08F, 30.9F, 1.18F),
                        blossom(0.95F).rotate(-12.0F, 70.0F, 0.0F).move(2.66F, 30.02F, 0.12F));
        rig.extra("kanzashi", Bone.HEAD)
                .shape(up(shape))
                .resolution(0.42F)
                .detail(0.5F)
                .flatLight(0.4F)
                .outline(-6669732, 0.08F);
        ArrayList list = new ArrayList();
        float[][] floatGrid =
                new float[][] {{2.86F, 29.84F, 1.06F, 2.0F}, {3.2F, 29.74F, 0.52F, 3.0F}};
        for (int i = 0; i < floatGrid.length; i++) {
            float f = floatGrid[i][0];
            float g = floatGrid[i][1];
            float h = floatGrid[i][2];
            int j = (int) floatGrid[i][3];
            float f2 = g - 0.55F * j;
            list.add(
                    Shapes.capsule(f, g + 0.05F, h, f + 0.03F, f2, h + 0.02F, 0.07F)
                            .color(-1459895));
            for (int k = 0; k < j; k++) {
                float g2 = k == j - 1 ? 0.23F : 0.17F;
                list.add(
                        Shapes.sphere(f + 0.01F * k, g - 0.55F * (k + 1), h, g2)
                                .color((k + i) % 2 == 0 ? -2608577 : -1459895));
            }
        }
        rig.extra("bira", Bone.HEAD)
                .pivot(tx(3.08F), ty(29.86F), tz(0.8F))
                .shape(up(Shapes.union(((Shape[]) list.toArray(new Shape[0])))))
                .motion(Motion.hair().hang(1.0F).spring(60.0F, 6.0F))
                .resolution(0.4F)
                .detail(1.5F)
                .flatLight(0.5F)
                .outline(-8767416, 0.05F);
    }

    private static Shape blossom(float f) {
        ArrayList list = new ArrayList();
        for (int i = 0; i < 5; i++) {
            float g = 90.0F + 72.0F * i;
            double d = Math.toRadians(g);
            float h = (float) Math.cos(d);
            float f2 = (float) Math.sin(d);
            float g2 = h * 0.47F * f;
            float h2 = f2 * 0.47F * f;
            Shape shape =
                    Shapes.ellipsoid(g2, h2, 0.0F, 0.3F * f, 0.44F * f, 0.2F)
                            .rotate(0.0F, 0.0F, g - 90.0F, g2, h2, 0.0F);
            list.add(
                    Shapes.subtract(
                            shape, Shapes.sphere(h * 0.93F * f, f2 * 0.93F * f, 0.0F, 0.12F * f)));
        }
        Shape shape2 =
                Shapes.union(((Shape[]) list.toArray(new Shape[0])))
                        .paint(Paint.radial(-2078596, 0.0F, 0.0F, 0.1F, 0.4F * f, -8984));
        return Shapes.union(shape2, Shapes.sphere(0.0F, 0.0F, 0.16F, 0.15F * f).color(-1459895));
    }

    private static Shape capLock(float f, float g, float h, float f2, float g2) {
        float[] floats = new float[15];
        for (int k = 0; k < 5; k++) {
            float[] floats2 = onHead(g + (h - g) * k / 4.0F, f, 0.02F);
            System.arraycopy(floats2, 0, floats, k * 3, 3);
        }
        return Shapes.strand(f2, g2, floats).flattenFrom(2.8F, 0.0F, 28.9F, -0.3F);
    }

    private static float[] onHead(float f, float g, float h) {
        double d = Math.toRadians(f);
        double e = Math.toRadians(g);
        float f2 = (float) Math.cos(d);
        float g2 = (float) Math.sin(d);
        float h2 = (float) Math.cos(e);
        float f3 = (float) Math.sin(e);
        float g3 = f3 * g2;
        float h3 = 0.97F * f2 + 0.24F * h2 * g2;
        float f4 = -0.24F * f2 + 0.97F * h2 * g2;
        float g4 =
                1.0F
                                / (float)
                                        Math.sqrt(
                                                g3 * g3 / 7.3984003F
                                                        + h3 * h3 / 7.3984003F
                                                        + f4 * f4 / 8.065599F)
                        + h;
        return new float[] {0.0F + g3 * g4, 28.9F + h3 * g4, -0.3F + f4 * g4};
    }

    private static Paint hairPaint() {
        return Paint.field(
                (value, value2, value3) -> {
                    float f = value2 - 31.2F;
                    float g = value3 + 0.7F;
                    float h = (float) Math.sqrt(value * value + f * f + g * g);
                    int i = Colors.mix(-2592100, -677434, smooth(0.8F, 2.9F, h));
                    return Colors.mix(i, -12321, smooth(5.5F, 17.5F, h) * 0.9F);
                });
    }

    private static void body(Rig rig) {
        Shape shape =
                Shapes.union(
                        Shapes.capsule(0.0F, 23.95F, -0.15F, -2.55F, 23.3F, -0.15F, 0.95F),
                        Shapes.capsule(0.0F, 23.95F, -0.15F, 2.55F, 23.3F, -0.15F, 0.95F));
        Shape shape2 = Shapes.ellipsoid(0.0F, 21.7F, 0.05F, 2.55F, 2.1F, 1.7F);
        Shape shape3 = Shapes.ellipsoid(0.86F, 21.6F, 0.6F, 0.92F, 0.82F, 0.98F).mirrorX();
        Shape shape4 = Shapes.ellipsoid(0.0F, 19.6F, 0.0F, 2.15F, 1.7F, 1.5F);
        Shape shape5 = Shapes.ellipsoid(0.0F, 17.8F, 0.0F, 2.2F, 1.6F, 1.55F);
        Shape shape6 =
                Shapes.blend(1.0F, shape, Shapes.blend(0.8F, shape2, shape3), shape4, shape5)
                        .intersect(Shapes.above(17.2F));
        Strand strand =
                Shapes.strand(
                                0.42F, 0.4F, 0.0F, 24.75F, -1.1F, -0.95F, 24.62F, -0.72F, -1.22F,
                                24.3F, 0.35F, -0.75F, 23.35F, 1.45F, 0.12F, 22.45F, 1.92F)
                        .flattenFrom(2.2F, 0.0F, 22.5F, -1.2F);
        Strand strand2 =
                Shapes.strand(
                                0.42F, 0.4F, 0.0F, 24.75F, -1.1F, 0.95F, 24.62F, -0.72F, 1.22F,
                                24.3F, 0.35F, 0.75F, 23.35F, 1.5F, -0.3F, 22.15F, 2.02F, -1.35F,
                                20.6F, 1.88F)
                        .flattenFrom(2.2F, 0.0F, 22.5F, -1.2F);
        Paint paint =
                Paint.field(
                        (value, value2, value3) -> {
                            float f =
                                    smooth(0.5F, 1.3F, value3)
                                            * smooth(19.9F, 20.5F, value2)
                                            * (1.0F - smooth(20.6F, 21.1F, value2));
                            return Colors.mix(-263689, -2305053, f * 0.55F);
                        });
        rig.part(Bone.BODY)
                .shape(Shapes.blend(0.14F, shape6, strand, strand2).paint(paint))
                .outline(-7568744, 0.1F)
                .detail(0.85F)
                .occlusion(0.2F)
                .decal(collar());
        Shape shape7 =
                Shapes.subtract(
                        Shapes.ellipsoid(0.0F, 19.2F, 0.0F, 2.82F, 2.6F, 2.32F)
                                .intersect(Shapes.box(0.0F, 19.2F, 0.0F, 3.2F, 0.3F, 2.7F, 0.12F)),
                        Shapes.ellipsoid(0.0F, 19.2F, 0.0F, 2.4F, 2.6F, 1.9F));
        Shape shape8 = Shapes.ellipsoid(0.0F, 19.22F, 2.36F, 0.36F, 0.36F, 0.24F);
        Shape shape9 =
                Shapes.ellipsoid(-1.0F, 19.4F, 2.28F, 1.0F, 0.44F, 0.2F)
                        .rotate(0.0F, -8.0F, -12.0F, -1.0F, 19.4F, 2.28F);
        Strand strand3 =
                Shapes.strand(
                                0.27F, 0.22F, -0.12F, 19.0F, 2.38F, -0.42F, 18.1F, 2.44F, -0.64F,
                                17.2F, 2.48F)
                        .flatten(2.2F, 0.0F, 0.0F, 1.0F);
        Shape shape10 =
                Shapes.union(shape8, shape9, shape9.mirrored(), strand3, strand3.mirrored());
        rig.extra("himo", Bone.BODY)
                .shape(Shapes.union(shape7, shape10).color(-263689))
                .resolution(0.6F)
                .detail(0.36F)
                .outline(-7568744, 0.09F);
    }

    private static Decal collar() {
        Canvas canvas = new Canvas(92, 78);
        float[] floats = uv(0.0F, 25.1F);
        float[] floats2 = uv(0.0F, 23.2F);
        canvas.fill(
                Shape2D.polygon(
                        uv(
                                -0.95F, 25.1F, -0.84F, 24.49F, -0.41F, 23.59F, 0.02F, 23.16F, 0.4F,
                                23.59F, 0.84F, 24.49F, 0.95F, 25.1F)),
                Paint2D.linear(0.0F, floats[1], 0.0F, floats2[1], -9016, -9016, -1331558));
        segments(
                canvas, 6.6F, -2674372, -0.9F, 25.1F, -0.84F, 24.49F, -0.41F, 23.59F, 0.05F,
                23.12F);
        segments(canvas, 6.6F, -2674372, 0.9F, 25.1F, 0.84F, 24.49F, 0.4F, 23.59F, -0.02F, 23.1F);
        float[] floats3 = uv(0.02F, 23.12F);
        canvas.fill(Shape2D.circle(floats3[0], floats3[1], 4.2F), -2674372);
        int i = 1484418696;
        segments(canvas, 1.8F, i, -1.65F, 24.25F, -1.6F, 24.11F, -1.13F, 23.16F, -0.6F, 22.6F);
        segments(canvas, 1.8F, i, 1.65F, 24.25F, 1.6F, 24.11F, 1.09F, 23.12F, 0.55F, 22.45F);
        return Decal.front(canvas, 0.0F, 23.8F, 2.0F, 3.8333333F, 3.25F)
                .depth(1.6F, 1.2F)
                .maxAngle(86.0F)
                .flatLight(0.9F)
                .occlusion(0.4F);
    }

    private static float[] uv(float... f) {
        float[] floats = new float[f.length];
        for (int i = 0; i < f.length; i += 2) {
            floats[i] = (f[i] + 1.9166666F) * 24.0F;
            floats[i + 1] = (25.425F - f[i + 1]) * 24.0F;
        }
        return floats;
    }

    private static void segments(Canvas canvas, float f, int i, float... g) {
        float[] floats = uv(g);
        for (int j = 0; j + 3 < floats.length; j += 2) {
            canvas.fill(Shape2D.line(floats[j], floats[j + 1], floats[j + 2], floats[j + 3], f), i);
        }
    }

    private static void hakama(Rig rig) {
        Shape shape =
                Shapes.extrude(Shape2D.polygon(-0.3F, 20.4F, 0.3F, 20.4F, 0.0F, 16.3F), 0.7F, 0.04F)
                        .rotate(0.0F, 90.0F, 0.0F)
                        .move(-2.95F, 0.0F, 0.0F)
                        .color(-263689);
        Shape shape2 =
                Shapes.subtract(
                        Shapes.field(Sakura::hakamaField, -5.1F, 2.25F, -3.9F, 2.1F, 20.25F, 3.9F)
                                .paint(Paint.field(Sakura::hakamaColor)),
                        shape);
        Motion motion =
                new Motion()
                        .follow(Bone.RIGHT_LEG, 0.62F, 13.0F)
                        .drag(3.0F)
                        .hang(0.5F)
                        .spring(80.0F, 11.0F)
                        .bend(1.6F)
                        .limit(24.0F);
        Part part =
                rig.extra("hakama_r", Bone.BODY)
                        .pivot(-1.0F, 19.6F, 0.0F)
                        .shape(shape2)
                        .motion(motion)
                        .resolution(1.1F)
                        .detail(0.42F)
                        .flatLight(0.35F)
                        .occlusion(0.2F)
                        .outline(-8514270, 0.12F);
        part.decal(pleatLine(PLEATS[0])).decal(pleatLine(PLEATS[1]));
        rig.extra("hakama_l", Bone.BODY).mirrorOf(part);
    }

    private static Decal pleatLine(float f) {
        Canvas canvas = new Canvas(10, 96);
        canvas.fill(
                Shape2D.taper(5.0F, 1.0F, 1.2F, 5.0F, 95.0F, 6.0F),
                Paint2D.linear(0.0F, 0.0F, 0.0F, 40.0F, 8262946, -1719790302));
        float g = (float) Math.sin(f);
        float h = (float) Math.cos(f);
        return Decal.wrap(canvas, -1.14F, 8.0F, 0.0F, 1.15F, 17.5F, 0.0F, -g, 0.0F, h, 3.6F, 11.0F);
    }

    private static float hakamaField(float f, float g, float h) {
        float f2 = clamp01((20.0F - g) / 17.5F);
        float g2 = (float) Math.pow(f2, 0.8);
        float h2 = -(0.35F + 1.15F * f2);
        float f3 = 2.05F + 1.15F * g2;
        float g3 = 1.9F + 1.5F * g2;
        float h3 = f - h2;
        float ellipse = ellipse(h3, h, f3, g3);
        float f4 = (float) Math.atan2(-h3, h);
        float g4 = 0.05F + 0.17F * f2;
        for (float h4 : PLEATS) {
            ellipse -= g4 * pleat(f4 - h4);
        }
        float f5 = Math.max(g - 20.0F, 2.5F - g);
        return smax(ellipse, f5, 0.2F) * 0.9F;
    }

    private static int hakamaColor(float f, float g, float h) {
        float f2 = clamp01((20.0F - g) / 17.5F);
        float g2 = (float) Math.pow(f2, 0.8);
        float h2 = f + 0.35F + 1.15F * f2;
        if (g < 2.8F && ellipse(h2, h, 2.05F + 1.15F * g2, 1.9F + 1.5F * g2) < -0.2F) {
            return -10087904;
        } else {
            float f3 = (float) Math.atan2(-h2, h);
            float g3 = 0.0F;
            for (float h3 : PLEATS) {
                float f4 = f3 - h3;
                if (f4 >= 0.0F && f4 < 0.16F) {
                    g3 = Math.max(g3, 1.0F - smooth(0.02F, 0.16F, f4));
                }
            }
            int i = Colors.mix(-2674372, -5955793, smooth(9.0F, 2.5F, g) * 0.45F);
            return Colors.mix(i, -5955793, g3 * 0.35F * smooth(0.02F, 0.2F, f2));
        }
    }

    private static float pleat(float f) {
        if (!(f < -0.4F) && !(f > 0.07F)) {
            return f < 0.0F ? smooth(-0.4F, 0.0F, f) : 1.0F - smooth(0.0F, 0.07F, f);
        } else {
            return 0.0F;
        }
    }

    private static float ellipse(float f, float g, float h, float f2) {
        float g2 = f / h;
        float h2 = g / f2;
        float f3 = (float) Math.sqrt(g2 * g2 + h2 * h2);
        float g3 = f / (h * h);
        float h3 = g / (f2 * f2);
        float f4 = (float) Math.sqrt(g3 * g3 + h3 * h3);
        return f4 < 1.0E-6F ? -Math.min(h, f2) : f3 * (f3 - 1.0F) / f4;
    }

    private static void arms(Rig rig) {
        float f = -3.62F;
        float g = 23.0F;
        float h = -0.12F;
        float f2 = -4.05F;
        float g2 = 19.45F;
        float h2 = -0.3F;
        float f3 = -4.28F;
        float g3 = 15.95F;
        float h3 = 0.45F;
        float f4 =
                (float)
                        Math.sqrt(
                                (f3 - f2) * (f3 - f2)
                                        + (g3 - g2) * (g3 - g2)
                                        + (h3 - h2) * (h3 - h2));
        float g4 = (f3 - f2) / f4;
        float h4 = (g3 - g2) / f4;
        float f5 = (h3 - h2) / f4;
        Shape shape =
                Shapes.blend(
                        0.35F,
                        Shapes.cone(f, g, h, 0.66F, f2, g2, h2, 0.58F),
                        Shapes.cone(f2, g2, h2, 0.58F, f3, g3, h3, 0.66F));
        Shape shape2 =
                Shapes.sphere(f3 + 0.3F * g4, g3 + 0.3F * h4, h3 + 0.3F * f5, 0.46F)
                        .color(-2502430);
        Part part =
                rig.part(Bone.RIGHT_ARM)
                        .pivot(-3.5F, 23.25F, -0.1F)
                        .grip(-4.32F, 15.05F, 0.72F)
                        .shape(Shapes.subtract(0.08F, shape.color(-263689), shape2))
                        .resolution(0.7F)
                        .detail(0.7F)
                        .outline(-7568744, 0.1F)
                        .swing(0.55F)
                        .joint(f2, g2, h2, 0.6F);
        rig.part(Bone.LEFT_ARM).mirrorOf(part);
        Shape shape3 =
                Shapes.field(
                                (value, value2, value3) -> {
                                    float distance = SLEEVE_SIDE.distance(value3, value2);
                                    float g5 = -4.12F - 0.085F * (23.8F - value2);
                                    float h5 =
                                            0.3F
                                                    + 0.16F * clamp01((22.0F - value2) / 8.0F)
                                                    + 0.26F * smooth(0.0F, 1.3F, -distance)
                                                    + 0.07F
                                                            * (float) Math.sin(value3 * 2.3F + 0.6F)
                                                            * smooth(22.0F, 17.0F, value2);
                                    return smax(distance, Math.abs(value - g5) - h5, 0.28F);
                                },
                                -6.1F,
                                13.2F,
                                -3.9F,
                                -3.3F,
                                24.7F,
                                1.6F)
                        .paint(Paint.vertical(-1053966, 13.5F, -263689, 19.0F));
        Shape shape4 =
                Shapes.union(
                        Shapes.sphere(-5.0F, 14.14F, -3.42F, 0.23F).color(-1816774),
                        Shapes.cone(-5.0F, 13.98F, -3.44F, 0.15F, -5.01F, 13.58F, -3.46F, 0.27F)
                                .paint(Paint.vertical(-4705748, 13.3F, -1816774, 13.9F)));
        Motion motion =
                Motion.cloth().hang(0.7F).drag(8.0F).spring(40.0F, 5.0F).bend(1.3F).limit(35.0F);
        Part part2 =
                rig.extra("sleeve_r", Bone.RIGHT_ARM)
                        .shape(Shapes.union(shape3, shape4))
                        .motion(motion)
                        .resolution(1.0F)
                        .detail(0.7F)
                        .outline(-7568744, 0.1F)
                        .decal(sleeveTrim());
        rig.extra("sleeve_l", Bone.LEFT_ARM).mirrorOf(part2);
        hands(rig, f3, g3, h3);
    }

    private static void hands(Rig rig, float f, float g, float h) {
        float f2 = 4.3F;
        float g2 = 15.25F;
        float h2 = 0.64F;
        ArrayList list = new ArrayList();
        list.add(Shapes.ellipsoid(f2, g2, h2, 0.33F, 0.6F, 0.52F));
        list.add(Shapes.capsule(f2 - 0.02F, g2 + 0.45F, h2 - 0.12F, -f, g + 0.1F, h, 0.32F));
        list.add(
                Shapes.strand(
                        0.15F,
                        0.13F,
                        f2 + 0.02F,
                        g2 - 0.4F,
                        h2 + 0.36F,
                        f2 - 0.02F,
                        g2 - 0.86F,
                        h2 + 0.44F,
                        f2 - 0.2F,
                        g2 - 1.1F,
                        h2 + 0.4F));
        list.add(
                Shapes.strand(
                        0.15F,
                        0.13F,
                        f2 + 0.03F,
                        g2 - 0.44F,
                        h2 + 0.12F,
                        f2,
                        g2 - 0.98F,
                        h2 + 0.14F,
                        f2 - 0.22F,
                        g2 - 1.26F,
                        h2 + 0.11F));
        list.add(
                Shapes.strand(
                        0.14F,
                        0.12F,
                        f2 + 0.03F,
                        g2 - 0.42F,
                        h2 - 0.12F,
                        f2,
                        g2 - 0.9F,
                        h2 - 0.13F,
                        f2 - 0.2F,
                        g2 - 1.16F,
                        h2 - 0.14F));
        list.add(
                Shapes.strand(
                        0.13F,
                        0.11F,
                        f2 + 0.02F,
                        g2 - 0.36F,
                        h2 - 0.34F,
                        f2,
                        g2 - 0.74F,
                        h2 - 0.36F,
                        f2 - 0.16F,
                        g2 - 0.95F,
                        h2 - 0.35F));
        list.add(
                Shapes.strand(
                        0.17F,
                        0.14F,
                        f2 - 0.18F,
                        g2 + 0.1F,
                        h2 + 0.42F,
                        f2 - 0.3F,
                        g2 - 0.3F,
                        h2 + 0.58F,
                        f2 - 0.32F,
                        g2 - 0.54F,
                        h2 + 0.5F));
        Part part =
                rig.extra("hand_l", Bone.LEFT_ARM)
                        .shape(
                                Shapes.blend(0.12F, ((Shape[]) list.toArray(new Shape[0])))
                                        .color(-9016))
                        .resolution(0.45F)
                        .detail(0.35F)
                        .flatLight(1.0F)
                        .occlusion(0.25F)
                        .outline(-5740452, 0.1F);
        rig.extra("hand_r", Bone.RIGHT_ARM).mirrorOf(part);
    }

    private static Decal sleeveTrim() {
        Canvas canvas = new Canvas(64, 24);
        float f = 16.0F;
        float[] floats =
                new float[] {
                    -0.3F, 14.75F, -1.2F, 14.3F, -2.25F, 14.05F, -2.85F, 14.25F, -3.12F, 14.8F
                };
        for (int i = 0; i + 3 < floats.length; i += 2) {
            canvas.fill(
                    Shape2D.line(
                            (floats[i] + 3.75F) * f,
                            (15.3F - floats[i + 1]) * f,
                            (floats[i + 2] + 3.75F) * f,
                            (15.3F - floats[i + 3]) * f,
                            3.0F),
                    -1816774);
        }
        return Decal.right(
                        canvas, -6.1F, 15.3F - 12.0F / f, -3.75F + 32.0F / f, 64.0F / f, 24.0F / f)
                .depth(1.3F, 1.2F)
                .maxAngle(48.0F);
    }

    private static void legs(Rig rig) {
        float f = -1.3F;
        Shape shape =
                Shapes.blend(
                        0.35F,
                        Shapes.sphere(f, 1.62F, -0.12F, 0.52F),
                        Shapes.ellipsoid(f, 0.98F, -0.45F, 0.56F, 0.54F, 0.62F),
                        Shapes.ellipsoid(f, 0.84F, 0.55F, 0.6F, 0.43F, 1.22F),
                        Shapes.ellipsoid(f + 0.33F, 0.76F, 1.57F, 0.25F, 0.31F, 0.43F),
                        Shapes.ellipsoid(f - 0.18F, 0.73F, 1.47F, 0.37F, 0.29F, 0.46F));
        Shape shape2 = Shapes.box(f + 0.1F, 0.8F, 1.97F, 0.05F, 0.4F, 0.55F, 0.02F).color(-2305053);
        Shape shape3 = Shapes.cone(f, 1.7F, -0.1F, 0.5F, f + 0.02F, 3.3F, -0.08F, 0.54F);
        Shape shape4 = Shapes.subtract(Shapes.blend(0.3F, shape, shape3).color(-263689), shape2);
        Shape shape5 =
                Shapes.box(f, 0.22F, 0.52F, 0.74F, 0.22F, 1.64F, 0.2F)
                        .paint(
                                Paint.field(
                                        (value, value2, value3) ->
                                                value2 > 0.36F ? -1320012 : -10600144));
        Shape shape6 =
                Shapes.union(
                                Shapes.strand(
                                        0.2F, 0.2F, f + 0.13F, 0.5F, 1.5F, f + 0.13F, 0.95F, 1.3F),
                                Shapes.strand(
                                        0.2F, 0.2F, f + 0.13F, 0.95F, 1.3F, f - 0.35F, 1.08F, 0.85F,
                                        f - 0.62F, 0.55F, 0.35F),
                                Shapes.strand(
                                        0.2F, 0.2F, f + 0.13F, 0.95F, 1.3F, f + 0.35F, 1.08F, 0.85F,
                                        f + 0.6F, 0.55F, 0.35F))
                        .color(-1816774);
        Shape shape7 =
                Shapes.union(shape4, shape5, shape6).rotate(0.0F, -7.0F, 0.0F, f, 0.0F, -0.1F);
        Part part =
                rig.part(Bone.RIGHT_LEG)
                        .pivot(f, 14.2F, 0.0F)
                        .shape(shape7)
                        .resolution(0.6F)
                        .detail(0.5F)
                        .flatLight(0.4F)
                        .outline(-10860992, 0.1F)
                        .swing(0.38F, 18.0F);
        rig.part(Bone.LEFT_LEG).mirrorOf(part);
    }

    private static float px(float f) {
        return 128.0F + f * 56.88889F;
    }

    private static float py(float f) {
        return (29.95F - f) * 56.88889F;
    }

    private static Decal face(float[][] floatGrid) {
        Canvas canvas = new Canvas(88, 136);
        float f = 40.0F;
        float g = 70.0F;
        Shape2D shape2D = Shape2D.ellipse(f, g, 27.5F, 33.0F);
        canvas.fill(shape2D, Paint2D.linear(f, g - 33.0F, f, g + 10.0F, -923920, -1));
        canvas.fill(
                Shape2D.ellipse(f, g - 34.0F, 34.0F, 22.0F).intersect(shape2D), -1713846054, 2.0F);
        Shape2D shape2D2 = Shape2D.ellipse(f, g + 3.5F, 20.5F, 27.5F);
        Shape2D shape2D3 = shape2D2.intersect(shape2D);
        canvas.fill(
                shape2D3, Paint2D.linear(f, g - 24.0F, f, g + 30.0F, -11923926, -2996102, -19508));
        canvas.fill(shape2D2.outline(2.6F).intersect(shape2D), -1337323990);
        canvas.fill(Shape2D.ellipse(f, g + 1.5F, 9.0F, 12.5F), -14023146);
        canvas.fill(
                Shape2D.ellipse(f, g + 17.0F, 12.0F, 6.5F).intersect(shape2D3), -2130716956, 3.0F);
        canvas.fill(
                Shape2D.curve(
                        f + 26.5F, g - 13.0F, f + 2.0F, g - 46.0F, f - 30.0F, g - 6.0F, 4.2F, 8.4F),
                -13757408);
        canvas.fill(Shape2D.taper(f - 25.0F, g - 9.0F, 6.0F, f - 35.0F, g - 6.0F, 1.4F), -13757408);
        canvas.fill(
                Shape2D.curve(
                        f - 5.0F, g + 32.0F, f - 20.0F, g + 31.0F, f - 26.5F, g + 16.0F, 1.6F,
                        2.8F),
                -864407480);
        canvas.fill(
                Shape2D.curve(f + 15.0F, g - 38.0F, f, g - 48.0F, f - 23.0F, g - 33.0F, 1.5F, 1.5F),
                2009102984);
        canvas.fill(
                Shape2D.curve(
                        f + 20.0F, g - 48.0F, f + 2.0F, g - 57.0F, f - 22.0F, g - 51.0F, 4.0F,
                        2.6F),
                -4892806);
        canvas.fill(Shape2D.ellipse(f - 4.0F, g + 46.0F, 21.0F, 9.0F), 1728023196, 10.0F);
        Canvas canvas2 = new Canvas(256, 220);
        int i = Math.round(py(28.0F)) - 70;
        ArrayList list = new ArrayList();
        for (float[] floats : floatGrid) {
            float[] floats2 = blade(floats);
            for (int j = 0; j < floats2.length; j += 2) {
                floats2[j] = px(floats2[j]);
                floats2[j + 1] = py(floats2[j + 1]) + 14.0F;
            }
            list.add(Shape2D.polygon(floats2));
        }
        list.add(Shape2D.box(128.0F, py(29.75F), 130.0F, 14.222222F, 0.0F));
        canvas2.fill(Shape2D.union(((Shape2D[]) list.toArray(new Shape2D[0]))), -1930845035, 3.0F);
        canvas2.draw(canvas, 28, i);
        canvas2.draw(canvas.mirrored(), 140, i);
        for (int k = -1; k <= 1; k += 2) {
            float h = 128.0F + k * 60.0F;
            canvas2.fill(Shape2D.ellipse(h - 7.5F, i + 60.0F, 7.0F, 8.8F), -1);
            canvas2.fill(Shape2D.circle(h + 8.0F, i + 83.0F, 3.0F), -570425345);
            canvas2.fill(Shape2D.circle(h - 12.0F, i + 76.0F, 1.6F), -1426063361);
        }
        canvas2.fill(Shape2D.line(130.0F, i + 119.0F, 127.0F, i + 124.5F, 2.4F), -1713795968);
        canvas2.fill(
                Shape2D.curve(
                        119.0F, i + 157.0F, 128.0F, i + 163.0F, 137.0F, i + 157.0F, 2.6F, 2.6F),
                -4896678);
        return Decal.front(canvas2, 0.0F, ty(28.016407F), tz(2.1F), 5.04F, 4.33125F)
                .depth(2.0F, 1.8F)
                .maxAngle(72.0F)
                .flatLight(1.0F)
                .occlusion(0.3F);
    }

    private static float smax(float f, float g, float h) {
        float f2 = clamp01(0.5F - 0.5F * (g - f) / h);
        return g + (f - g) * f2 + h * f2 * (1.0F - f2);
    }

    private static float smooth(float f, float g, float h) {
        float f2 = clamp01((h - f) / (g - f));
        return f2 * f2 * (3.0F - 2.0F * f2);
    }

    private static float clamp01(float f) {
        return f < 0.0F ? 0.0F : Math.min(f, 1.0F);
    }
}
