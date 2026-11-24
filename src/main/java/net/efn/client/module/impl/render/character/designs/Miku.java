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
public final class Miku extends CharacterDesign {

    private static final int SKIN = -8241;

    private static final int SKIN_SHADE = -1198942;

    private static final int SKIN_LINE = -5740452;

    private static final int HAIR_ROOT = -14705514;

    private static final int HAIR = -12793152;

    private static final int HAIR_TIP = -7936291;

    private static final int HAIR_SHADE = -15368325;

    private static final int HAIR_LINE = -15903662;

    private static final int SHINE = -857866245;

    private static final int SHIRT = -2499360;

    private static final int SHIRT_SHADE = -5525063;

    private static final int SHIRT_LINE = -10788500;

    private static final int BLACK = -13618373;

    private static final int BLACK_SHADE = -14934492;

    private static final int BLACK_LINE = -16119026;

    private static final int TEAL = -13645638;

    private static final int TEAL_DARK = -14971002;

    private static final int MAGENTA = -1882484;

    private static final int TATTOO = -2608065;

    private static final float SKIRT_TOP = 17.9F;

    private static final float SKIRT_HEM = 13.3F;

    private static final int PLEATS = 18;

    private static final float HS = 1.12F;

    private static final float DROP = 0.4F;

    private static final float HX = 0.0F;

    private static final float HY = 28.9F;

    private static final float HZ = -0.3F;

    private static final float PPU = 56.88889F;

    private static final float FACE_TOP = 29.95F;

    private static final float[][] LOCKS =
            new float[][] {
                {-0.1F, 28.3F, -2.0F, 0.95F, 0.12F, 0.34F},
                {-1.0F, 28.75F, -25.0F, 0.95F, 0.28F, 0.3F},
                {-1.92F, 28.4F, -47.0F, 0.92F, 0.26F, 0.3F},
                {0.9F, 28.85F, 24.0F, 0.95F, 0.28F, 0.3F},
                {1.9F, 28.5F, 46.0F, 0.92F, 0.25F, 0.3F},
                {-0.55F, 28.5F, -13.0F, 0.45F, 0.16F, 0.44F},
                {0.46F, 28.62F, 11.0F, 0.45F, 0.18F, 0.44F},
                {-1.46F, 28.65F, -36.0F, 0.42F, 0.2F, 0.42F},
                {1.42F, 28.7F, 35.0F, 0.42F, 0.2F, 0.42F}
            };

    public Miku() {
        super("miku", "Miku");
    }

    @Override
    protected void define(Rig rig) {
        rig.scale(1.0F).outline(-14011336, 0.12F).occlusion(0.3F).flatLight(0.55F);
        float[][] floatGrid = lockPaths();
        Paint paint = hairPaint();
        head(rig, floatGrid);
        hair(rig, paint);
        fringe(rig, floatGrid);
        twinTails(rig);
        headset(rig);
        body(rig);
        skirt(rig);
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
        Shape shape = Shapes.capsule(0.0F, 23.3F, -0.45F, 0.0F, 25.9F, -0.3F, 0.58F);
        float ty = ty(25.82F);
        float tz = tz(1.2F);
        Paint paint =
                Paint.field(
                        (value, i, j) -> {
                            float f = ty + Math.max(0.0F, tz - j) * 0.62F;
                            float g =
                                    smooth(f + 0.05F, f - 0.2F, i)
                                            * (0.6F + 0.4F * smooth(23.4F, 24.8F, i));
                            return Colors.mix(-8241, -1198942, g);
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
                                            Colors.mix(-14705514, -12793152, 0.45F),
                                            -12793152,
                                            smooth(30.9F, 29.7F, value2));
                            j = Colors.mix(j, -7936291, 0.45F * smooth(29.5F, 28.5F, value2));
                            return Colors.mix(
                                    j,
                                    -15368325,
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
                .outline(-15903662, 0.09F)
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
                    -857866245);
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
                    -857866245);
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
        for (float g = 130.0F; g <= 230.0F; g += 25.0F) {
            list.add(capLock(g, 20.0F, 118.0F, 0.85F, 0.5F));
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
        Strand strand =
                Shapes.strand(
                                0.9F, 0.3F, 0.0F, 28.0F, -2.35F, 0.0F, 26.6F, -2.2F, 0.0F, 25.6F,
                                -1.95F)
                        .flatten(2.2F, 0.0F, 0.0F, 1.0F);
        Shape shape3 =
                Shapes.blend(
                        0.25F,
                        Shapes.subtract(
                                        Shapes.blend(0.3F, ((Shape[]) list.toArray(new Shape[0]))),
                                        shape)
                                .paint(paint),
                        shape2.paint(paint.occlusion(0.35F)),
                        strand.paint(paint));
        rig.extra("hair", Bone.HEAD)
                .shape(up(shape3))
                .detail(1.1F)
                .flatLight(0.55F)
                .occlusion(0.22F)
                .outline(-15903662, 0.09F)
                .decal(shine());
        Strand strand2 =
                Shapes.tube(
                                -1.95F, 30.4F, 0.92F, 0.55F, -2.3F, 29.3F, 1.12F, 0.6F, -2.42F,
                                27.8F, 1.0F, 0.55F, -2.4F, 25.9F, 0.8F, 0.46F, -2.3F, 24.0F, 0.78F,
                                0.36F, -2.15F, 22.6F, 0.82F, 0.05F)
                        .flatten(1.8F, 1.0F, 0.0F, 0.3F);
        Strand strand3 =
                Shapes.tube(
                                -2.2F, 30.2F, 0.1F, 0.58F, -2.52F, 28.9F, 0.24F, 0.62F, -2.58F,
                                27.3F, 0.32F, 0.55F, -2.45F, 25.6F, 0.5F, 0.42F, -2.36F, 23.8F,
                                0.52F, 0.25F, -2.3F, 23.0F, 0.55F, 0.04F)
                        .flatten(1.8F, 1.0F, 0.0F, 0.15F);
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
                        .shape(up(Shapes.blend(0.25F, strand2, strand3).paint(paint)))
                        .motion(motion)
                        .resolution(0.7F)
                        .detail(0.6F)
                        .flatLight(0.5F)
                        .occlusion(0.35F)
                        .outline(-15903662, 0.09F);
        rig.extra("sidelock_l", Bone.HEAD).mirrorOf(part);
    }

    private static void twinTails(Rig rig) {
        float tx = tx(-2.62F);
        float ty = ty(30.2F);
        float tz = tz(-1.35F);
        float[][] floatGrid =
                new float[][] {
                    {
                        0.0F, 0.0F, 1.0F, -0.9F, -0.5F, 1.35F, -1.5F, -2.6F, 1.5F, -1.75F, -6.5F,
                        1.45F, -1.75F, -11.5F, 1.3F, -1.6F, -16.5F, 1.05F, -1.45F, -20.5F, 0.7F,
                        -1.35F, -23.4F, 0.06F
                    },
                    {
                        0.0F, 0.1F, 0.8F, -1.0F, -0.4F, 1.0F, -1.9F, -2.8F, 1.1F, -2.2F, -7.0F,
                        1.0F, -2.25F, -12.0F, 0.9F, -2.05F, -17.0F, 0.7F, -1.85F, -20.8F, 0.45F,
                        -1.95F, -22.8F, 0.04F
                    },
                    {
                        0.0F, 0.1F, 0.8F, -0.7F, -0.6F, 1.0F, -1.0F, -2.9F, 1.05F, -1.15F, -7.2F,
                        1.0F, -1.1F, -12.2F, 0.9F, -0.95F, -16.8F, 0.7F, -0.9F, -20.2F, 0.45F,
                        -0.75F, -22.2F, 0.04F
                    }
                };
        float[] floats = new float[] {0.0F, -0.25F, 0.2F};
        Shape[] shapes = new Shape[floatGrid.length];
        for (int m = 0; m < floatGrid.length; m++) {
            float[] floats2 = floatGrid[m];
            float[] floats3 = new float[floats2.length / 3 * 4];
            for (int n = 0; n < floats2.length / 3; n++) {
                float f = n / (floats2.length / 3.0F - 1.0F);
                floats3[n * 4] = tx + floats2[n * 3];
                floats3[n * 4 + 1] = ty + floats2[n * 3 + 1] * 1.12F;
                floats3[n * 4 + 2] =
                        tz
                                - 1.15F
                                        * (float)
                                                Math.sin(
                                                        Math.min(1.0F, f * 3.0F)
                                                                * 3.141592653589793
                                                                * 0.5)
                                + floats[m] * f;
                floats3[n * 4 + 3] = floats2[n * 3 + 2];
            }
            shapes[m] = Shapes.tube(floats3).flatten(1.7F, 0.25F, 0.0F, 1.0F);
        }
        float g = ty - 26.208F;
        Paint paint =
                Paint.field(
                        (i, j, k) -> {
                            float h = clamp01((ty - j) / (ty - g));
                            int o = Colors.mix(-14705514, -12793152, smooth(0.0F, 0.18F, h));
                            o = Colors.mix(o, -7936291, 0.6F * smooth(0.55F, 1.0F, h));
                            float f2 = smooth(0.4F, -0.6F, i - tx + 1.6F) * 0.35F;
                            return Colors.mix(o, -15368325, f2);
                        });
        Shape shape =
                Shapes.union(
                                Shapes.box(tx - 0.3F, ty, tz, 0.34F, 1.0F, 0.75F, 0.14F)
                                        .color(-13618373),
                                Shapes.box(tx - 0.62F, ty, tz + 0.38F, 0.06F, 0.86F, 0.09F, 0.03F)
                                        .color(-1882484),
                                Shapes.box(tx - 0.62F, ty, tz - 0.38F, 0.06F, 0.86F, 0.09F, 0.03F)
                                        .color(-1882484))
                        .rotate(0.0F, 0.0F, -12.0F, tx, ty, tz);
        Motion motion =
                Motion.hair()
                        .hang(0.25F)
                        .follow(Bone.BODY, 0.8F)
                        .drag(12.0F)
                        .turn(6.0F, 0.0F)
                        .spring(40.0F, 6.0F)
                        .bend(1.6F)
                        .limit(35.0F);
        Part part =
                rig.extra("tail_r", Bone.HEAD)
                        .pivot(tx, ty, tz)
                        .shape(Shapes.union(Shapes.blend(0.35F, shapes).paint(paint), shape))
                        .motion(motion)
                        .resolution(1.0F)
                        .detail(0.9F)
                        .flatLight(0.5F)
                        .occlusion(0.3F)
                        .outline(-15903662, 0.1F);
        rig.extra("tail_l", Bone.HEAD).mirrorOf(part);
    }

    private static void headset(Rig rig) {
        Shape shape =
                Shapes.union(
                        Shapes.cylinder(-2.45F, 28.0F, 0.05F, -2.85F, 28.0F, 0.05F, 0.62F, 0.12F)
                                .color(-13618373),
                        Shapes.cylinder(-2.8F, 28.0F, 0.05F, -2.95F, 28.0F, 0.05F, 0.38F, 0.06F)
                                .color(-13645638));
        Shape shape2 =
                Shapes.union(
                        Shapes.tube(
                                        -2.7F, 27.7F, 0.45F, 0.08F, -2.45F, 26.8F, 1.3F, 0.08F,
                                        -1.7F, 26.25F, 2.05F, 0.08F, -0.95F, 26.15F, 2.25F, 0.08F)
                                .color(-13618373),
                        Shapes.ellipsoid(-0.85F, 26.15F, 2.28F, 0.22F, 0.16F, 0.16F)
                                .color(-14934492));
        rig.extra("headset", Bone.HEAD)
                .shape(up(Shapes.union(shape, shape.mirrored(), shape2)))
                .resolution(0.4F)
                .detail(0.6F)
                .flatLight(0.4F)
                .outline(-16119026, 0.08F);
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
                    int i = Colors.mix(-14705514, -12793152, smooth(0.8F, 2.9F, h));
                    return Colors.mix(i, -7936291, smooth(5.5F, 17.5F, h) * 0.9F);
                });
    }

    private static void body(Rig rig) {
        Shape shape =
                Shapes.union(
                        Shapes.capsule(0.0F, 23.95F, -0.15F, -2.45F, 23.3F, -0.15F, 0.9F),
                        Shapes.capsule(0.0F, 23.95F, -0.15F, 2.45F, 23.3F, -0.15F, 0.9F));
        Shape shape2 = Shapes.ellipsoid(0.0F, 21.7F, 0.05F, 2.45F, 2.1F, 1.65F);
        Shape shape3 = Shapes.ellipsoid(0.84F, 21.6F, 0.6F, 0.9F, 0.8F, 0.95F).mirrorX();
        Shape shape4 = Shapes.ellipsoid(0.0F, 19.6F, 0.0F, 2.0F, 1.7F, 1.45F);
        Shape shape5 = Shapes.ellipsoid(0.0F, 17.8F, 0.0F, 2.15F, 1.6F, 1.5F);
        Shape shape6 =
                Shapes.blend(1.0F, shape, Shapes.blend(0.8F, shape2, shape3), shape4, shape5)
                        .intersect(Shapes.above(17.2F));
        Paint paint =
                Paint.field(
                        (value, value2, value3) -> {
                            float f =
                                    smooth(0.5F, 1.3F, value3)
                                            * smooth(19.9F, 20.5F, value2)
                                            * (1.0F - smooth(20.6F, 21.1F, value2));
                            float g = smooth(1.6F, 2.3F, Math.abs(value)) * 0.35F;
                            return Colors.mix(-2499360, -5525063, Math.max(f * 0.55F, g));
                        });
        rig.part(Bone.BODY)
                .shape(shape6.paint(paint))
                .outline(-10788500, 0.1F)
                .detail(0.85F)
                .occlusion(0.2F);
        Shape2D shape2D =
                Shape2D.polygon(
                        0.05F, 24.5F, 1.15F, 24.55F, 1.35F, 23.7F, 0.6F, 23.15F, 0.08F, 23.55F);
        Shape shape7 =
                Shapes.extrude(shape2D, 0.14F, 0.07F)
                        .rotate(-25.0F, -20.0F, 0.0F, 0.5F, 23.9F, 0.0F)
                        .move(0.0F, 0.0F, frontZ(shape6, 0.6F, 23.8F) + 0.02F);
        Shape shape8 = Shapes.union(shape7, shape7.mirrored()).color(-2499360);
        rig.extra("collar", Bone.BODY)
                .shape(shape8)
                .resolution(0.35F)
                .detail(0.8F)
                .flatLight(0.3F)
                .outline(-13645638, 0.1F);
        float[] floats = new float[] {23.35F, 22.4F, 21.3F, 20.2F, 19.3F};
        float[] floats2 = new float[] {0.26F, 0.32F, 0.38F, 0.42F, 0.44F};
        float[] floats3 = new float[floats.length * 4];
        for (int i = 0; i < floats.length; i++) {
            floats3[i * 4 + 1] = floats[i];
            floats3[i * 4 + 2] = frontZ(shape6, 0.0F, floats[i]) + 0.1F;
            floats3[i * 4 + 3] = floats2[i];
        }
        Shape2D shape2D2 =
                Shape2D.polygon(-1.0F, 24.0F, 1.0F, 24.0F, 1.0F, 19.7F, 0.0F, 19.1F, -1.0F, 19.7F);
        Shape shape9 =
                Shapes.intersect(
                        Shapes.tube(floats3).flatten(3.5F, 0.0F, 0.0F, 1.0F),
                        Shapes.extrude(shape2D2, 4.0F, 0.0F));
        Shape shape10 =
                Shapes.ellipsoid(
                        0.0F, 23.45F, frontZ(shape6, 0.0F, 23.45F) + 0.12F, 0.36F, 0.3F, 0.22F);
        rig.extra("tie", Bone.BODY)
                .pivot(0.0F, 23.45F, 1.6F)
                .shape(
                        Shapes.union(shape9, shape10)
                                .paint(Paint.vertical(-14971002, 19.3F, -13645638, 22.5F)))
                .motion(Motion.none().walk(3.0F, 0.0F, 2.0F, 2.0F).drag(4.0F).spring(80.0F, 8.0F))
                .resolution(0.4F)
                .detail(0.5F)
                .flatLight(0.4F)
                .outline(-15835560, 0.08F);
    }

    private static void skirt(Rig rig) {
        Shape shape =
                Shapes.field(Miku::skirtDistance, -3.4F, 13.1F, -3.0F, 3.4F, 18.1F, 3.0F)
                        .paint(Paint.field(Miku::skirtColor));
        rig.extra("skirt", Bone.BODY)
                .pivot(0.0F, 17.9F, 0.0F)
                .shape(shape)
                .resolution(0.9F)
                .detail(0.8F)
                .occlusion(0.35F)
                .outline(-16119026, 0.12F)
                .decal(stripe(13.650001F, 0.26F))
                .motion(
                        Motion.none()
                                .walk(0.0F, 8.0F, 0.0F, 1.0F)
                                .walkPhase(-90.0F)
                                .drag(2.0F)
                                .spring(80.0F, 10.0F));
    }

    private static float flare(float f) {
        float g = clamp01((17.6F - f) / 4.299999F);
        return g * (2.0F - g) * 0.8F + g * 0.2F;
    }

    private static float pleat(float f, float g, float h) {
        float f2 = 2.25F + 0.95F * h;
        float g2 = 1.62F + 0.95F * h;
        double d = Math.atan2(f / f2, g / g2) * 18.0 / 6.283185307179586;
        d -= Math.floor(d);
        return (float) Math.abs(2.0 * d - 1.0);
    }

    private static float skirtDistance(float f, float g, float h) {
        float flare = flare(g);
        float f2 = 2.25F + 0.95F * flare;
        float g2 = 1.62F + 0.95F * flare;
        float h2 = f / f2;
        float f3 = h / g2;
        float g3 = (float) Math.sqrt(h2 * h2 + f3 * f3);
        float h3 =
                (g3 - 1.0F) * Math.min(f2, g2) * 0.72F - 0.2F * flare * (pleat(f, h, flare) - 0.5F);
        return Math.max(h3, Math.max(g - 17.9F, 13.3F - g));
    }

    private static int skirtColor(float f, float g, float h) {
        float flare = flare(g);
        return Colors.mix(
                -14934492, -13618373, 0.35F + 0.65F * (1.0F - flare + flare * pleat(f, h, flare)));
    }

    private static Decal stripe(float f, float g) {
        Canvas canvas = new Canvas(512, 24);
        canvas.fill(Shape2D.box(256.0F, 12.0F, 256.0F, 8.0F, 0.0F), -13645638);
        return Decal.wrap(canvas, 0.0F, f, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 360.0F, g)
                .radius(1.8F, 3.4F);
    }

    private static void arms(Rig rig) {
        float f = -3.5F;
        float g = 23.1F;
        float h = -0.12F;
        float f2 = -3.95F;
        float g2 = 19.5F;
        float h2 = -0.3F;
        float f3 = -4.2F;
        float g3 = 16.0F;
        float h3 = 0.4F;
        Shape shape =
                Shapes.blend(
                                0.35F,
                                Shapes.sphere(f + 0.1F, g, h, 0.66F),
                                Shapes.cone(f, g, h, 0.62F, f2, g2, h2, 0.52F),
                                Shapes.cone(f2, g2, h2, 0.52F, f3, g3, h3, 0.44F))
                        .paint(
                                Paint.field(
                                        (value, value2, value3) -> {
                                            float f4 = smooth(23.4F, 21.8F, value2);
                                            return Colors.mix(-1198942, -8241, 0.4F + 0.6F * f4);
                                        }));
        Part part =
                rig.part(Bone.RIGHT_ARM)
                        .pivot(-3.5F, 23.25F, -0.1F)
                        .grip(-4.32F, 15.05F, 0.72F)
                        .shape(shape)
                        .resolution(0.7F)
                        .detail(0.8F)
                        .flatLight(0.8F)
                        .outline(-5740452, 0.1F)
                        .swing(0.55F)
                        .joint(f2, g2, h2, 0.55F);
        rig.part(Bone.LEFT_ARM).mirrorOf(part).decal(tattoo(-f, g, h, -f2, g2, h2));
        float g4 =
                (float)
                        Math.sqrt(
                                (f3 - f2) * (f3 - f2)
                                        + (g3 - g2) * (g3 - g2)
                                        + (h3 - h2) * (h3 - h2));
        float h4 = (f3 - f2) / g4;
        float f5 = (g3 - g2) / g4;
        float g5 = (h3 - h2) / g4;
        float h5 = f2 + 0.35F * (f3 - f2);
        float f6 = g2 + 0.35F * (g3 - g2) + 0.9F;
        float g6 = h2 + 0.35F * (h3 - h2);
        float h6 = f3 + 1.1F * h4;
        float f7 = g3 + 1.1F * f5;
        float g7 = h3 + 1.1F * g5;
        Shape shape2 = Shapes.cone(h5, f6 + 0.3F, g6, 0.62F, h6, f7, g7, 1.15F);
        Shape shape3 = Shapes.sphere(h6 + 0.25F * h4, f7 + 0.25F * f5, g7 + 0.25F * g5, 0.95F);
        float h7 = f6 + 0.3F;
        float f8 =
                (float)
                        Math.sqrt(
                                (h6 - h5) * (h6 - h5)
                                        + (f7 - h7) * (f7 - h7)
                                        + (g7 - g6) * (g7 - g6));
        Shape shape4 =
                Shapes.subtract(0.1F, shape2, shape3)
                        .paint(
                                Paint.field(
                                        (m, value4, o) -> {
                                            float g8 =
                                                    ((m - h5) * (h6 - h5)
                                                                    + (value4 - h7) * (f7 - h7)
                                                                    + (o - g6) * (g7 - g6))
                                                            / (f8 * f8);
                                            return Colors.mix(
                                                    -13618373,
                                                    -14934492,
                                                    smooth(0.2F, 1.0F, g8) * 0.5F);
                                        }));
        Canvas canvas = new Canvas(256, 16);
        canvas.fill(Shape2D.box(128.0F, 8.0F, 128.0F, 6.0F, 0.0F), -13645638);
        float h8 = h6 - h5;
        float f9 = f7 - h7;
        float g9 = g7 - g6;
        Decal decal =
                Decal.wrap(
                                canvas,
                                h5 + h8 * 0.93F,
                                h7 + f9 * 0.93F,
                                g6 + g9 * 0.93F,
                                h8,
                                f9,
                                g9,
                                0.0F,
                                0.0F,
                                1.0F,
                                360.0F,
                                f8 * 0.1F)
                        .radius(0.7F, 1.4F);
        Decal decal2 =
                Decal.wrap(
                                canvas,
                                h5 + h8 * 0.05F,
                                h7 + f9 * 0.05F,
                                g6 + g9 * 0.05F,
                                h8,
                                f9,
                                g9,
                                0.0F,
                                0.0F,
                                1.0F,
                                360.0F,
                                f8 * 0.08F)
                        .radius(0.4F, 1.0F);
        Part part2 =
                rig.extra("sleeve_r", Bone.RIGHT_ARM)
                        .shape(shape4)
                        .resolution(1.1F)
                        .detail(0.8F)
                        .flatLight(0.3F)
                        .occlusion(0.4F)
                        .outline(-16119026, 0.1F)
                        .decal(decal)
                        .decal(decal2);
        rig.extra("sleeve_l", Bone.LEFT_ARM).mirrorOf(part2);
        hands(rig, f3, g3, h3);
    }

    private static Decal tattoo(float f, float g, float h, float f2, float g2, float h2) {
        Canvas canvas = new Canvas(64, 40);
        canvas.fill(Shape2D.ellipse(44.0F, 20.0F, 9.0F, 14.0F).outline(4.5F), -2608065);
        canvas.fill(Shape2D.line(20.0F, 7.0F, 20.0F, 34.0F, 4.5F), -2608065);
        canvas.fill(Shape2D.line(20.0F, 7.0F, 14.0F, 13.0F, 4.0F), -2608065);
        float f3 = 0.22F;
        float g3 = f + (f2 - f) * f3;
        float h3 = g + (g2 - g) * f3;
        float f4 = h + (h2 - h) * f3;
        return Decal.left(canvas, g3 + 0.55F, h3, f4 + 0.05F, 1.0F, 0.62F)
                .depth(0.6F, 0.6F)
                .maxAngle(70.0F)
                .flatLight(0.8F);
    }

    private static void hands(Rig rig, float f, float g, float h) {
        float f2 = 4.3F;
        float g2 = 15.25F;
        float h2 = 0.6F;
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
                                        .color(-8241))
                        .resolution(0.45F)
                        .detail(0.35F)
                        .flatLight(1.0F)
                        .occlusion(0.25F)
                        .outline(-5740452, 0.1F);
        rig.extra("hand_r", Bone.RIGHT_ARM).mirrorOf(part);
    }

    private static void legs(Rig rig) {
        float f = -1.1F;
        Shape shape = Shapes.sphere(f, 14.6F, -0.05F, 1.02F);
        Shape shape2 = Shapes.cone(f - 0.05F, 14.3F, 0.0F, 1.02F, f, 8.4F, 0.08F, 0.76F);
        Shape shape3 = Shapes.sphere(f, 7.9F, 0.1F, 0.72F);
        Strand strand =
                Shapes.tube(
                        f, 7.6F, 0.08F, 0.72F, f - 0.02F, 5.8F, -0.12F, 0.8F, f - 0.04F, 3.6F,
                        -0.02F, 0.56F, f - 0.05F, 2.0F, 0.0F, 0.48F);
        Shape shape4 = Shapes.blend(0.45F, shape, shape2, shape3, strand);
        Shape shape5 =
                shape4.intersect(Shapes.above(10.6F))
                        .paint(Paint.vertical(-1198942, 10.8F, -8241, 12.5F));
        Shape shape6 =
                Shapes.cylinder(f, 11.05F, 0.03F, f, 10.35F, 0.05F, 0.9F, 0.12F).color(-13645638);
        Shape shape7 =
                Shapes.blend(0.3F, shape4.intersect(Shapes.below(10.4F)).inflate(0.09F), shoe(f))
                        .paint(
                                Paint.field(
                                        (value, value2, value3) ->
                                                Colors.mix(
                                                        -13618373,
                                                        -14934492,
                                                        smooth(-0.2F, -0.8F, value3) * 0.6F)));
        Part part =
                rig.part(Bone.RIGHT_LEG)
                        .pivot(f, 14.2F, 0.0F)
                        .shape(shape5)
                        .detail(0.6F)
                        .resolution(1.0F)
                        .flatLight(0.8F)
                        .outline(-5740452, 0.1F)
                        .occlusion(0.3F)
                        .swing(0.55F, 30.0F)
                        .joint(f, 7.9F, 0.1F, 0.8F);
        rig.part(Bone.LEFT_LEG).mirrorOf(part);
        Part part2 =
                rig.extra("boot_r", Bone.RIGHT_LEG)
                        .shape(shape7)
                        .detail(0.8F)
                        .resolution(1.0F)
                        .outline(-16119026, 0.11F)
                        .occlusion(0.3F);
        rig.extra("boot_l", Bone.LEFT_LEG).mirrorOf(part2);
        Part part3 =
                rig.extra("boot_trim_r", Bone.RIGHT_LEG)
                        .shape(Shapes.union(shape6, sole(f)))
                        .detail(0.4F)
                        .resolution(0.6F)
                        .flatLight(0.3F)
                        .outline(-15835560, 0.09F);
        rig.extra("boot_trim_l", Bone.LEFT_LEG).mirrorOf(part3);
    }

    private static Shape shoe(float f) {
        return Shapes.blend(
                0.4F,
                Shapes.ellipsoid(f, 1.1F, 0.95F, 0.62F, 0.6F, 1.15F),
                Shapes.ellipsoid(f, 1.4F, -0.35F, 0.62F, 0.9F, 0.72F),
                Shapes.cone(f, 2.3F, -0.05F, 0.5F, f, 1.2F, 0.0F, 0.6F));
    }

    private static Shape sole(float f) {
        Shape shape = Shapes.box(f, 0.62F, 0.85F, 0.64F, 0.14F, 1.3F, 0.12F);
        Shape shape2 = Shapes.box(f, 0.3F, -0.55F, 0.46F, 0.3F, 0.4F, 0.1F);
        return Shapes.blend(0.12F, shape, shape2)
                .intersect(Shapes.above(0.0F))
                .paint(Paint.vertical(-14971002, 0.0F, -13645638, 0.7F));
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
        canvas.fill(shape2D, Paint2D.linear(f, g - 33.0F, f, g + 10.0F, -1904912, -1));
        canvas.fill(
                Shape2D.ellipse(f, g - 34.0F, 34.0F, 22.0F).intersect(shape2D), -1715153190, 2.0F);
        Shape2D shape2D2 = Shape2D.ellipse(f, g + 3.5F, 20.5F, 27.5F);
        Shape2D shape2D3 = shape2D2.intersect(shape2D);
        canvas.fill(
                shape2D3,
                Paint2D.linear(f, g - 24.0F, f, g + 30.0F, -16106432, -14504024, -6491416));
        canvas.fill(shape2D2.outline(2.6F).intersect(shape2D), -1341506496);
        canvas.fill(Shape2D.ellipse(f, g + 1.5F, 9.0F, 12.5F), -16439768);
        canvas.fill(
                Shape2D.ellipse(f, g + 17.0F, 12.0F, 6.5F).intersect(shape2D3), -2133655560, 3.0F);
        canvas.fill(
                Shape2D.curve(
                        f + 26.5F, g - 13.0F, f + 2.0F, g - 46.0F, f - 30.0F, g - 6.0F, 4.2F, 8.4F),
                -14800336);
        canvas.fill(Shape2D.taper(f - 25.0F, g - 9.0F, 6.0F, f - 35.0F, g - 6.0F, 1.4F), -14800336);
        canvas.fill(
                Shape2D.curve(
                        f - 5.0F, g + 32.0F, f - 20.0F, g + 31.0F, f - 26.5F, g + 16.0F, 1.6F,
                        2.8F),
                -868591010);
        canvas.fill(
                Shape2D.curve(f + 15.0F, g - 38.0F, f, g - 48.0F, f - 23.0F, g - 33.0F, 1.5F, 1.5F),
                2005965984);
        canvas.fill(
                Shape2D.curve(
                        f + 20.0F, g - 48.0F, f + 2.0F, g - 57.0F, f - 22.0F, g - 51.0F, 4.0F,
                        2.6F),
                -13989243);
        canvas.fill(Shape2D.ellipse(f - 4.0F, g + 46.0F, 21.0F, 9.0F), 1442810524, 10.0F);
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
        canvas2.fill(Shape2D.union(((Shape2D[]) list.toArray(new Shape2D[0]))), -1931434602, 3.0F);
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
                Shape2D.path(
                        3.0F,
                        3.0F,
                        118.0F,
                        i + 156.0F,
                        123.0F,
                        i + 161.0F,
                        128.0F,
                        i + 157.5F,
                        133.0F,
                        i + 161.0F,
                        138.0F,
                        i + 156.0F),
                -6668728);
        return Decal.front(canvas2, 0.0F, ty(28.016407F), tz(2.1F), 5.04F, 4.33125F)
                .depth(2.0F, 1.8F)
                .maxAngle(72.0F)
                .flatLight(1.0F)
                .occlusion(0.3F);
    }

    private static float smooth(float f, float g, float h) {
        float f2 = clamp01((h - f) / (g - f));
        return f2 * f2 * (3.0F - 2.0F * f2);
    }

    private static float clamp01(float f) {
        return f < 0.0F ? 0.0F : Math.min(f, 1.0F);
    }
}
