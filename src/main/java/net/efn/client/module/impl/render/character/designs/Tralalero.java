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
import net.efn.client.module.impl.render.character.kit.Vec3;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Tralalero extends CharacterDesign {

    private static final float YJ = 17.9F;

    private static final float PIVOT_Z = -1.5F;

    private static final float SHIFT = 0.8F;

    private static final float LEG_X = 2.9F;

    private static final float HIP = 12.0F;

    private static final float LEG_Z = 3.4F;

    private static final float KNEE = 6.4F;

    private static final float MID_HIP = 14.2F;

    private static final float MID_Z = -7.0F;

    private static final float SHOE_SCALE = 1.15F;

    private static final float[] BODY_Z =
            new float[] {
                -12.4F, -10.7F, -8.4F, -5.7F, -2.9F, 0.0F, 2.9F, 5.8F, 8.3F, 10.3F, 11.8F, 12.8F,
                13.4F
            };

    private static final float[] BODY_Y =
            new float[] {
                1.4F, 1.15F, 0.8F, 0.4F, 0.1F, 0.0F, 0.05F, 0.25F, 0.5F, 0.75F, 0.95F, 1.05F, 1.1F
            };

    private static final float[] BODY_RY =
            new float[] {
                1.0F, 2.1F, 3.3F, 4.6F, 5.5F, 5.85F, 5.75F, 5.2F, 4.35F, 3.35F, 2.3F, 1.35F, 0.45F
            };

    private static final float[] BODY_RX =
            new float[] {
                0.9F, 1.8F, 2.9F, 4.2F, 5.1F, 5.45F, 5.35F, 4.8F, 3.95F, 3.0F, 2.1F, 1.25F, 0.45F
            };

    private static final float[] LINE_Z =
            new float[] {
                -14.5F, -11.0F, -8.0F, -5.0F, -2.0F, 1.0F, 3.5F, 5.5F, 7.0F, 9.0F, 11.0F, 14.5F
            };

    private static final float[] LINE_Y =
            new float[] {
                19.699999F,
                19.1F,
                18.1F,
                17.1F,
                16.4F,
                16.3F,
                16.9F,
                17.65F,
                17.4F,
                17.6F,
                18.1F,
                18.4F
            };

    private static final float MOUTH_DEG = 152.0F;

    private static final int MOUTH_W = 284;

    private static final int MOUTH_H = 120;

    private static final float MOUTH_PX = 25.0F;

    private static final float MOUTH_Z = 9.45F;

    private static final float CORNER = 74.0F;

    private static final float LIP_FRONT = 11.0F;

    private static final float LIP_BACK = 7.4F;

    private static final int BACK_TOP = -12621165;

    private static final int BACK = -10515274;

    private static final int BELLY = -920330;

    private static final int FIN_TIP = -13346176;

    private static final int SKIN = -10055494;

    private static final int SKIN_LIGHT = -8674358;

    private static final int SKIN_DARK = -11634525;

    private static final int SHOE = -13933608;

    private static final int SHOE_DARK = -14989656;

    private static final int SHOE_LIGHT = -9593609;

    private static final int SOLE = -657935;

    private static final int SOLE_GREY = -4604218;

    private static final int LINE = -15194566;

    private static final int MOUTH = -12972264;

    private static final int GUM = -10872282;

    private static final int TEETH = -263436;

    public Tralalero() {
        super("tralalero", "Tralalero Tralala");
    }

    @Override
    protected void define(Rig rig) {
        rig.scale(1.0F).outline(-15194566, 0.16F).occlusion(0.5F).firstPersonArms(false);
        rig.part(Bone.BODY).pivot(0.0F, 15.9F, 0.0F);
        Shape shape = Shapes.field(Tralalero::loft, -5.8F, 11.7F, -14.6F, 5.8F, 24.099998F, 14.2F);
        Shape shape2 =
                Shapes.field(
                        (value, value2, value3) -> bellyLine(value3) - value2,
                        -6.0F,
                        10.9F,
                        -14.0F,
                        6.0F,
                        24.9F,
                        15.0F);
        Shape shape3 =
                Shapes.blend(
                                1.1F,
                                Shapes.intersect(0.5F, shape, shape2)
                                        .paint(Paint.field(Tralalero::skin)),
                                dorsal())
                        .move(0.0F, 0.0F, 0.8F);
        Canvas canvas = eyeCanvas();
        Canvas canvas2 = gillCanvas();
        Canvas canvas3 = laceCanvas();
        Canvas canvas4 = stripeCanvas();
        Part part =
                rig.extra("shark", Bone.BODY)
                        .pivot(0.0F, 17.9F, -1.5F)
                        .shape(shape3)
                        .resolution(1.2F)
                        .detail(0.78F)
                        .outline(0.1F)
                        .neck(0.0F, 17.9F, 6.8F, 0.0F, 0.0F, 1.0F, 3.0F)
                        .look(0.45F, 0.45F, 28.0F)
                        .motion(
                                Motion.none()
                                        .idle(0.7F, 0.8F, 0.0F, 0.28F)
                                        .walk(2.0F, 0.0F, 4.0F, 1.0F)
                                        .follow(Bone.HEAD, 0.25F)
                                        .spring(70.0F, 10.0F)
                                        .limit(12.0F))
                        .decal(eye(canvas, 1.0F))
                        .decal(eye(canvas.mirrored(), -1.0F))
                        .decal(gills(canvas2, 1.0F))
                        .decal(gills(canvas2.mirrored(), -1.0F));
        Shape shape4 =
                Shapes.field(
                        (value4, value5, value6) -> value5 - bellyLine(value6) - 0.4F,
                        -6.0F,
                        10.9F,
                        -14.0F,
                        6.0F,
                        24.9F,
                        15.0F);
        Shape shape5 =
                Shapes.intersect(shape.inflate(-0.15F), shape4)
                        .paint(Paint.of(-920330).flatLight(0.4F).occlusion(0.35F))
                        .move(0.0F, 0.0F, 0.8F);
        rig.extra("belly", part).shape(shape5).resolution(1.0F).detail(1.0F).decal(mouth());
        rig.extra("tail", part)
                .pivot(0.0F, 19.199999F, -9.9F)
                .shape(caudal())
                .resolution(1.0F)
                .motion(
                        Motion.tail()
                                .idle(1.0F, 9.0F, 0.0F, 0.45F)
                                .walk(2.0F, 14.0F, 0.0F, 1.0F)
                                .drag(5.0F)
                                .hang(0.3F)
                                .turn(0.0F, 16.0F)
                                .spring(40.0F, 5.5F)
                                .bend(1.2F));
        Part part2 =
                rig.part(Bone.RIGHT_ARM)
                        .pivot(-4.3F, 15.4F, 4.0F)
                        .grip(-6.8F, 13.4F, 2.7F)
                        .shape(pectoral())
                        .resolution(0.9F)
                        .detail(0.8F)
                        .swing(0.35F);
        rig.part(Bone.LEFT_ARM).mirrorOf(part2);
        Part part3 =
                rig.part(Bone.RIGHT_LEG)
                        .pivot(-2.9F, 12.0F, 3.4F)
                        .shape(leg(-2.9F, 12.0F, 3.4F, -1.0F))
                        .resolution(1.0F)
                        .detail(1.4F)
                        .swing(0.5F, 35.0F)
                        .joint(-2.8000002F, 6.5F, 3.5500002F, 0.8F);
        rig.part(Bone.LEFT_LEG).mirrorOf(part3);
        Part part4 =
                rig.extra("shoe_r", Bone.RIGHT_LEG)
                        .shape(shoe(-2.9F, 3.4F))
                        .resolution(1.0F)
                        .detail(1.6F)
                        .decal(laces(canvas3, -2.9F, 3.4F))
                        .decal(stripe(canvas4, -2.9F, 3.4F, -1.0F));
        rig.extra("shoe_l", Bone.LEFT_LEG).mirrorOf(part4);
        Part part5 =
                rig.extra("leg_mid", Bone.BODY)
                        .pivot(0.0F, 14.2F, -7.0F)
                        .shape(leg(0.0F, 14.2F, -7.0F, 1.0F))
                        .resolution(1.0F)
                        .detail(1.4F)
                        .motion(
                                Motion.none()
                                        .follow(Bone.LEFT_LEG, 0.5F)
                                        .walk(22.0F, 0.0F, 0.0F, 1.0F)
                                        .hang(0.5F)
                                        .limit(45.0F));
        rig.extra("shoe_mid", part5)
                .shape(shoe(0.0F, -7.0F))
                .resolution(1.0F)
                .detail(1.6F)
                .decal(laces(canvas3, 0.0F, -7.0F))
                .decal(stripe(canvas4, 0.0F, -7.0F, -1.0F))
                .decal(stripe(canvas4.mirrored(), 0.0F, -7.0F, 1.0F));
    }

    private static float smooth(float f, float g, float h) {
        float f2 = (h - f) / (g - f);
        f2 = f2 < 0.0F ? 0.0F : Math.min(f2, 1.0F);
        return f2 * f2 * (3.0F - 2.0F * f2);
    }

    private static int segment(float[] floats, float f) {
        int i = 0;
        while (i < floats.length - 2 && f > floats[i + 1]) {
            i++;
        }
        return i;
    }

    private static float spline(float[] floats, float[] floats2, int i, float f, boolean bl) {
        int j = floats.length;
        float g = floats[i + 1] - floats[i];
        float h = Math.max(0.0F, Math.min(1.0F, (f - floats[i]) / g));
        float f2 = floats2[Math.max(0, i - 1)];
        float g2 = floats2[i];
        float h2 = floats2[i + 1];
        float f3 = floats2[Math.min(j - 1, i + 2)];
        float g3 = 2.0F * f2 - 5.0F * g2 + 4.0F * h2 - f3;
        float h3 = 3.0F * g2 - f2 - 3.0F * h2 + f3;
        return bl
                ? 0.5F * (h2 - f2 + 2.0F * g3 * h + 3.0F * h3 * h * h) / g
                : 0.5F * (2.0F * g2 + (h2 - f2) * h + g3 * h * h + h3 * h * h * h);
    }

    private static float bellyLine(float f) {
        return spline(LINE_Z, LINE_Y, segment(LINE_Z, f), f, false);
    }

    private static float loft(float f, float g, float h) {
        int i = BODY_Z.length;
        if (h > BODY_Z[i - 1]) {
            float f2 = g - 17.9F - BODY_Y[i - 1];
            float g2 = h - BODY_Z[i - 1];
            return (float) Math.sqrt(f * f + f2 * f2 + g2 * g2) - BODY_RY[i - 1];
        } else if (h < BODY_Z[0]) {
            float h2 = g - 17.9F - BODY_Y[0];
            float f3 = h - BODY_Z[0];
            return (float) Math.sqrt(f * f + h2 * h2 + f3 * f3) - BODY_RX[0];
        } else {
            int segment = segment(BODY_Z, h);
            float g3 = 17.9F + spline(BODY_Z, BODY_Y, segment, h, false);
            float spline = spline(BODY_Z, BODY_RY, segment, h, false);
            float spline2 = spline(BODY_Z, BODY_RX, segment, h, false);
            float spline3 = spline(BODY_Z, BODY_RY, segment, h, true);
            float spline4 = spline(BODY_Z, BODY_RX, segment, h, true);
            float spline5 = spline(BODY_Z, BODY_Y, segment, h, true);
            float h3 = Math.max(Math.abs(spline4), Math.abs(spline3)) + Math.abs(spline5);
            float f4 = f / spline2;
            float g4 = (g - g3) / spline;
            float h4 = (float) Math.sqrt(f4 * f4 + g4 * g4);
            float f5 =
                    (float) Math.sqrt(f4 * f4 / (spline2 * spline2) + g4 * g4 / (spline * spline));
            float g5 = f5 < 1.0E-6F ? -Math.min(spline2, spline) : h4 * (h4 - 1.0F) / f5;
            return g5 / (float) Math.sqrt(1.0F + h3 * h3);
        }
    }

    private static int skin(float f, float g, float h) {
        return Colors.mix(-10515274, -12621165, smooth(bellyLine(h) + 0.5F, 23.4F, g));
    }

    private static Shape2D outline(float f, float... g) {
        float[] floats = (float[]) g.clone();
        for (int i = 1; i < floats.length; i += 2) {
            floats[i] += f;
        }
        return Shape2D.polygon(floats);
    }

    private static Shape blade(Shape2D shape2D, float f, float g, float h, float f2) {
        Shape shape = Shapes.extrude(shape2D, h, h);
        float g2 = (float) Math.sqrt(1.0F + f2 * f2);
        return Shapes.field(
                (k, value, m) -> {
                    float h2 = (float) Math.sqrt((k - f) * (k - f) + (value - g) * (value - g));
                    return Math.max(shape.distance(k, value, m), (Math.abs(m) - h + f2 * h2) / g2);
                },
                shape2D.minX(),
                shape2D.minY(),
                -h,
                shape2D.maxX(),
                shape2D.maxY(),
                h);
    }

    private static Shape side(Shape shape) {
        return orient(shape, 0.0F, 0.0F, 0.0F, new Vec3(0.0F, 0.0F, -1.0F), Vec3.UP);
    }

    private static Shape dorsal() {
        Shape2D shape2D =
                outline(
                        17.9F, -2.6F, 4.2F, -1.9F, 6.2F, -1.0F, 7.9F, 0.1F, 9.4F, 1.4F, 10.6F, 2.6F,
                        11.5F, 3.7F, 12.1F, 3.2F, 11.2F, 2.75F, 10.0F, 2.45F, 8.7F, 2.4F, 7.4F,
                        2.6F, 6.2F, 3.1F, 5.0F, 3.6F, 4.2F);
        Shape2D shape2D2 =
                outline(
                        17.9F, 8.3F, 2.2F, 8.9F, 3.6F, 9.7F, 4.7F, 10.6F, 5.5F, 10.2F, 4.5F, 10.1F,
                        3.4F, 10.4F, 2.2F);
        Shape shape =
                Shapes.union(
                        side(blade(shape2D.inflate(0.1F), 0.4F, 22.5F, 1.5F, 0.125F)),
                        side(blade(shape2D2, 9.4F, 20.699999F, 0.72F, 0.14F)));
        return shape.paint(Paint.vertical(-12621165, 22.9F, -13346176, 29.4F));
    }

    private static Shape caudal() {
        Shape2D shape2D =
                outline(
                        17.9F, 12.0F, 1.4F, 11.7F, 2.2F, 12.4F, 3.8F, 13.2F, 5.4F, 14.0F, 7.0F,
                        14.8F, 8.3F, 15.4F, 9.1F, 15.0F, 7.9F, 14.6F, 6.3F, 14.3F, 4.6F, 14.1F,
                        3.0F, 14.2F, 1.6F, 14.4F, 0.2F, 14.7F, -1.3F, 14.95F, -2.7F, 15.2F, -3.9F,
                        14.5F, -3.2F, 13.6F, -2.2F, 12.8F, -1.0F, 11.9F, 0.4F);
        Shape shape = side(blade(shape2D.inflate(0.08F), 12.0F, 19.3F, 0.82F, 0.062F));
        Shape shape2 = Shapes.cone(0.0F, 19.1F, -10.3F, 1.5F, 0.0F, 19.3F, -12.5F, 1.0F);
        Shape shape3 = Shapes.ellipsoid(0.0F, 19.3F, -12.2F, 1.3F, 0.55F, 1.4F);
        return Shapes.blend(0.7F, shape2, shape3, shape)
                .paint(Paint.radial(-10515274, 0.0F, 19.3F, -12.2F, 5.5F, -13346176))
                .move(0.0F, 0.0F, 0.8F);
    }

    private static Shape pectoral() {
        Shape2D shape2D =
                Shape2D.polygon(
                        -1.2F, 2.0F, 0.5F, 2.1F, 2.0F, 1.7F, 3.5F, 1.0F, 5.0F, 0.1F, 6.3F, -0.9F,
                        7.0F, -1.6F, 6.0F, -1.5F, 4.6F, -1.35F, 3.2F, -1.3F, 1.8F, -1.5F, 0.4F,
                        -1.9F, -1.2F, -2.2F);
        Shape shape =
                orient(
                        blade(shape2D, 0.0F, 0.0F, 0.62F, 0.05F),
                        -4.3F,
                        15.4F,
                        3.2F,
                        new Vec3(-0.8F, -0.5F, -0.33F),
                        new Vec3(0.25F, 0.05F, 1.0F));
        return shape.paint(Paint.radial(-10515274, -4.3F, 15.4F, 3.2F, 6.0F, -13346176))
                .move(0.0F, 0.0F, 0.8F);
    }

    private static Paint legPaint(float f) {
        return Paint.field(
                (value, value2, i) -> {
                    float g = smooth(f + 0.3F, f + 1.2F, i);
                    float h =
                            smooth(1.3F, 0.3F, Math.abs(value2 - 6.4F))
                                    * smooth(f - 0.2F, f - 0.8F, i);
                    return Colors.mix(Colors.mix(-10055494, -8674358, g), -11634525, h);
                });
    }

    private static Shape leg(float f, float g, float h, float f2) {
        float g2 = 6.4F;
        float h2 = g - 6.4F;
        Shape shape = Shapes.capsule(f, g, h, f, g + 2.8F, h, 1.85F);
        Shape shape2 =
                Shapes.cone(f, g + 0.3F, h, 2.0F, f - f2 * 0.15F, g2 + 0.9F, h + 0.15F, 1.12F);
        Shape shape3 =
                Shapes.ellipsoid(
                        f + f2 * 0.05F, g2 + 0.56F * h2, h + 0.85F, 1.18F, 0.42F * h2, 1.08F);
        Shape shape4 =
                Shapes.ellipsoid(f + f2 * 0.7F, g2 + 0.55F * h2, h, 0.85F, 0.45F * h2, 1.05F);
        Shape shape5 = Shapes.ellipsoid(f - f2 * 0.6F, g2 + 1.35F, h + 0.55F, 0.8F, 1.0F, 0.78F);
        Shape shape6 = Shapes.ellipsoid(f, g2 + 0.55F * h2, h - 0.7F, 1.2F, 0.45F * h2, 0.95F);
        Shape shape7 = Shapes.sphere(f - f2 * 0.1F, g2 + 0.1F, h + 0.15F, 0.9F);
        Shape shape8 = Shapes.ellipsoid(f - f2 * 0.05F, g2 + 0.25F, h + 0.95F, 0.55F, 0.62F, 0.4F);
        Shape shape9 =
                Shapes.cone(f - f2 * 0.1F, g2 - 0.3F, h + 0.25F, 0.86F, f, 2.4F, h + 0.05F, 0.56F);
        Shape shape10 =
                Shapes.ellipsoid(f - f2 * 0.36F, g2 - 1.55F, h - 0.62F, 0.84F, 1.55F, 0.98F);
        Shape shape11 =
                Shapes.ellipsoid(f + f2 * 0.36F, g2 - 1.25F, h - 0.55F, 0.76F, 1.35F, 0.88F);
        Shape shape12 = Shapes.cone(f, g2 - 2.7F, h - 0.55F, 0.5F, f, 2.2F, h - 0.5F, 0.42F);
        Shape shape13 = Shapes.sphere(f, 2.3F, h, 0.58F);
        Shape shape14 =
                Shapes.union(
                                shape,
                                Shapes.blend(
                                        0.3F, shape2, shape3, shape4, shape5, shape6, shape7,
                                        shape8, shape9, shape10, shape11, shape12, shape13))
                        .paint(legPaint(h));
        Shape shape15 =
                flat(foot(0.0F), f, 0.6F, h, 0.6F, 0.52F)
                        .paint(Paint.vertical(-4604218, 0.2F, -657935, 0.3F));
        return Shapes.union(shape14, shape15.scale(1.15F, f, 0.0F, h));
    }

    private static Shape orient(Shape shape, float f, float g, float h, Vec3 vec3, Vec3 vec32) {
        Vec3 vec33 = vec3.normalize();
        Vec3 vec34 = vec33.cross(vec32).normalize();
        Vec3 vec35 = vec34.cross(vec33);
        float f2 =
                (float)
                        Math.acos(
                                Math.max(
                                        -1.0F,
                                        Math.min(
                                                1.0F,
                                                (vec33.x() + vec35.y() + vec34.z() - 1.0F)
                                                        * 0.5F)));
        Shape shape2 =
                f2 < 1.0E-4F
                        ? shape
                        : shape.rotateAxis(
                                vec35.z() - vec34.y(),
                                vec34.x() - vec33.z(),
                                vec33.y() - vec35.x(),
                                (float) Math.toDegrees(f2),
                                0.0F,
                                0.0F,
                                0.0F);
        return shape2.move(f, g, h);
    }

    private static Shape flat(Shape2D shape2D, float f, float g, float h, float f2, float g2) {
        return orient(Shapes.extrude(shape2D, f2, g2), f, g, h, Vec3.LEFT, Vec3.FORWARD);
    }

    private static Shape2D foot(float f) {
        Shape2D shape2D = Shape2D.circle(0.0F, -1.05F, 1.0F);
        Shape2D shape2D2 = Shape2D.circle(0.0F, 1.4F, 1.22F);
        Shape2D shape2D3 = Shape2D.circle(0.0F, 2.35F, 1.05F);
        Shape2D shape2D4 = Shape2D.blend(2.2F, shape2D, shape2D2, shape2D3);
        return f > 0.0F ? shape2D4.inflate(-f) : shape2D4;
    }

    private static Shape shoe(float f, float g) {
        Shape shape =
                Shapes.blend(
                        0.8F,
                        Shapes.cone(f, 1.55F, g - 0.85F, 1.3F, f, 1.25F, g + 1.1F, 1.3F),
                        Shapes.ellipsoid(f, 0.95F, g + 1.95F, 1.22F, 0.92F, 1.5F));
        Paint paint =
                Paint.field(
                        (gx, value, i) ->
                                Colors.mix(-13933608, -14989656, smooth(g - 0.6F, g - 1.3F, i)));
        Shape shape2 =
                Shapes.intersect(0.15F, shape, flat(foot(0.1F), f, 2.0F, g, 1.3F, 0.25F))
                        .paint(paint);
        Shape shape3 =
                Shapes.torus(f, 2.95F, g - 0.5F, 0.78F, 0.34F)
                        .rotate(-14.0F, 0.0F, 0.0F, f, 2.95F, g - 0.5F)
                        .color(-14989656);
        Shape shape4 =
                Shapes.ellipsoid(f, 2.9F, g + 0.35F, 0.52F, 0.4F, 0.7F)
                        .rotate(20.0F, 0.0F, 0.0F, f, 2.9F, g + 0.35F)
                        .color(-9593609);
        Shape shape5 = Shapes.ellipsoid(f, 3.15F, g - 1.25F, 0.26F, 0.45F, 0.2F).color(-14989656);
        return Shapes.blend(0.2F, shape2, shape3, shape4, shape5).scale(1.15F, f, 0.0F, g);
    }

    private static Canvas laceCanvas() {
        Canvas canvas = new Canvas(60, 76);
        for (int i = 0; i < 4; i++) {
            float f = 6.5F + i * 20.25F;
            float g = 22.5F - i * 1.9F;
            canvas.fill(Shape2D.box(30.0F, f, g, 4.1F, 4.1F), -1);
            canvas.fill(Shape2D.circle(30.0F - g - 2.3F, f, 2.4F), -14274500);
            canvas.fill(Shape2D.circle(30.0F + g + 2.3F, f, 2.4F), -14274500);
        }
        return canvas;
    }

    private static Canvas stripeCanvas() {
        Canvas canvas = new Canvas(160, 30);
        canvas.fill(Shape2D.line(20.0F, 9.0F, 140.0F, 21.0F, 13.0F), -1);
        return canvas;
    }

    private static Decal stripe(Canvas canvas, float f, float g, float h) {
        float f2 = 1.15F;
        return Decal.project(
                        canvas,
                        f + h * 1.3F * f2,
                        1.55F * f2,
                        g + 0.35F * f2,
                        h,
                        0.0F,
                        0.0F,
                        0.0F,
                        1.0F,
                        0.0F,
                        3.6F * f2,
                        0.68F * f2)
                .depth(0.9F, 0.9F)
                .maxAngle(70.0F)
                .flatLight(0.6F)
                .occlusion(0.4F);
    }

    private static Decal laces(Canvas canvas, float f, float g) {
        float h = 1.15F;
        return Decal.project(
                        canvas,
                        f,
                        2.7F * h,
                        g + 1.3F * h,
                        0.0F,
                        0.79F,
                        0.61F,
                        0.0F,
                        1.0F,
                        0.0F,
                        1.25F * h,
                        1.58F * h)
                .depth(0.9F, 0.9F)
                .flatLight(0.6F)
                .occlusion(0.4F);
    }

    private static Canvas eyeCanvas() {
        Canvas canvas = new Canvas(96, 96);
        float f = 48.0F;
        float g = 50.0F;
        Shape2D shape2D = Shape2D.ellipse(f, g, 21.0F, 20.0F);
        canvas.fill(shape2D.inflate(4.0F), 1428832344, 3.0F);
        canvas.fill(shape2D, Paint2D.radial(f - 4.5F, g - 6.0F, 26.0F, -14011322, -16316147));
        canvas.fill(Shape2D.circle(f - 7.5F, g - 7.0F, 5.6F), -1);
        canvas.fill(Shape2D.circle(f + 7.5F, g + 8.0F, 2.4F), -1325400065);
        canvas.fill(Shape2D.arc(f, g + 2.0F, 22.0F, 200.0F, 340.0F, 4.0F), -2144713630, 2.0F);
        return canvas;
    }

    private static Decal eye(Canvas canvas, float f) {
        return Decal.project(
                        canvas, f * 3.3F, 20.1F, 10.2F, f * 0.84F, 0.36F, 0.4F, 0.0F, 1.0F, 0.0F,
                        2.2F, 2.2F)
                .depth(1.2F, 1.2F)
                .maxAngle(75.0F)
                .flatLight(0.7F);
    }

    private static Canvas gillCanvas() {
        Canvas canvas = new Canvas(120, 120);
        for (int i = 0; i < 5; i++) {
            float f = 16.0F + i * 22.0F;
            float g = 42.0F - i * 3.0F;
            canvas.fill(
                    Shape2D.curve(f, 58.0F - g, f + 7.0F, 58.0F, f, 58.0F + g, 3.4F, 3.4F),
                    -802799780);
        }
        return canvas;
    }

    private static Decal gills(Canvas canvas, float f) {
        return Decal.project(
                        canvas, f * 5.1F, 18.5F, 6.0F, f, 0.05F, 0.2F, 0.0F, 1.0F, 0.0F, 2.8F, 2.8F)
                .depth(1.5F, 1.5F);
    }

    private static float mouthX(float f) {
        return 142.0F + f * 284.0F / 152.0F;
    }

    private static float arch(float f) {
        float g = Math.min(1.0F, Math.abs(f) / 74.0F);
        float h = 11.0F - 3.6F * (float) Math.pow(g, 1.8);
        return 60.0F - (h - 9.45F) * 25.0F;
    }

    private static float gape(float f) {
        float g = Math.min(1.0F, Math.abs(f) / 74.0F);
        return 15.000001F * (1.0F - g * g);
    }

    private static Decal mouth() {
        Canvas canvas = new Canvas(284, 120);
        float f = 0.53521127F;
        float[] floats = new float[284];
        float[] floats2 = new float[284];
        float[] floats3 = new float[284];
        for (int i = 0; i < 284; i++) {
            float g = (i + 0.5F - 142.0F) * f;
            float h = (arch(g + 0.5F) - arch(g - 0.5F)) * f;
            floats[i] = arch(g);
            floats2[i] = Math.abs(g) > 75.0F ? -1.0F : gape(g);
            floats3[i] = 1.0F / (float) Math.sqrt(1.0F + h * h);
        }
        Shape2D shape2D =
                Shape2D.field(
                        (value, value2) -> {
                            int j = Math.max(0, Math.min(283, (int) value));
                            return floats2[j] < 0.0F
                                    ? 4.0F
                                    : (Math.abs(value2 - floats[j]) - floats2[j]) * floats3[j];
                        },
                        mouthX(-76.0F),
                        3.0F,
                        mouthX(76.0F),
                        117.0F);
        canvas.fill(shape2D, Paint2D.linear(0.0F, 0.0F, 0.0F, 120.0F, -10872282, -12972264));
        float f2 = 0.75F;
        float g2 = 2.5F;
        for (int k = -4; k <= 4; k++) {
            float h2 = k * 15.0F;
            float gape = gape(h2);
            if (!(gape < g2)) {
                Shape2D shape2D2 =
                        Shape2D.polygon(
                                mouthX(h2 - 6.0F),
                                arch(h2 - 6.0F) - gape(h2 - 6.0F) - f2,
                                mouthX(h2 + 6.0F),
                                arch(h2 + 6.0F) - gape(h2 + 6.0F) - f2,
                                mouthX(h2),
                                arch(h2) + gape * 0.5F);
                canvas.fill(shape2D2.intersect(shape2D), -263436);
                float f3 = h2 + 7.5F;
                float gape2 = gape(f3);
                if (!(gape2 < g2) && k != 4) {
                    Shape2D shape2D3 =
                            Shape2D.polygon(
                                    mouthX(f3 - 5.0F),
                                    arch(f3 - 5.0F) + gape(f3 - 5.0F) + f2,
                                    mouthX(f3 + 5.0F),
                                    arch(f3 + 5.0F) + gape(f3 + 5.0F) + f2,
                                    mouthX(f3),
                                    arch(f3) - gape2 * 0.35F);
                    canvas.fill(shape2D3.intersect(shape2D), -1185056);
                }
            }
        }
        canvas.fill(shape2D.outline(2.5F), -15194566);
        return Decal.wrap(
                        canvas, 0.0F, 18.35F, 10.25F, 0.0F, 0.0F, 1.0F, 0.0F, -1.0F, 0.0F, 152.0F,
                        4.8F)
                .maxAngle(80.0F)
                .flatLight(1.0F)
                .occlusion(0.3F);
    }
}
