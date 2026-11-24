package net.efn.client.module.impl.render.character.designs;

import net.efn.client.module.impl.render.character.kit.Bone;
import net.efn.client.module.impl.render.character.kit.Canvas;
import net.efn.client.module.impl.render.character.kit.CharacterDesign;
import net.efn.client.module.impl.render.character.kit.Decal;
import net.efn.client.module.impl.render.character.kit.Motion;
import net.efn.client.module.impl.render.character.kit.Noise;
import net.efn.client.module.impl.render.character.kit.Paint;
import net.efn.client.module.impl.render.character.kit.Paint2D;
import net.efn.client.module.impl.render.character.kit.Part;
import net.efn.client.module.impl.render.character.kit.Rig;
import net.efn.client.module.impl.render.character.kit.Sample;
import net.efn.client.module.impl.render.character.kit.Shape;
import net.efn.client.module.impl.render.character.kit.Shape2D;
import net.efn.client.module.impl.render.character.kit.Shapes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class TungTungSahur extends CharacterDesign {

    private static final float R = 5.0F;

    private static final float BOTTOM = 12.0F;

    private static final float TOP = 34.0F;

    private static final float RIM = 0.85F;

    private static final float FIST_X = -7.0F;

    private static final float FIST_Y = 11.6F;

    private static final float FIST_Z = 0.72F;

    private static final float[] KNUCKLE_DIR = unit(-0.16F, 0.86F, 0.48F);

    private static final int WOOD = -1063542;

    private static final int WOOD_DARK = -4158378;

    private static final int WOOD_WORN = -5868993;

    private static final int FISSURE = -9551070;

    private static final int KNOT = -11653867;

    private static final int CUT = -1389672;

    private static final int LIMB = -3567528;

    private static final int LIMB_DARK = -7053254;

    private static final int INK = -15003638;

    private static final int LINE = -12967406;

    public TungTungSahur() {
        super("tung_tung_sahur", "Tung Tung Tung Sahur");
    }

    @Override
    protected void define(Rig rig) {
        rig.scale(1.0F).outline(-12967406, 0.16F).occlusion(0.3F).firstPersonArms(false);
        rig.part(Bone.HEAD).pivot(0.0F, 12.5F, 0.0F);
        Part part = rig.extra("upright", Bone.HEAD).motion(Motion.none().hang(1.0F).limit(100.0F));
        rig.extra("log", part)
                .shape(log())
                .detail(1.5F)
                .resolution(1.2F)
                .flatLight(0.35F)
                .neck(0.0F, 22.5F, 0.0F, 0.0F, 1.0F, 0.0F, 4.0F)
                .look(0.6F, 0.0F, 40.0F)
                .motion(Motion.none().walk(0.0F, 4.0F, -6.0F, 1.0F).drag(6.0F))
                .decal(eyes())
                .decal(mouth())
                .decal(rings());
        Part part2 =
                rig.part(Bone.RIGHT_ARM)
                        .pivot(-5.3F, 22.8F, 0.2F)
                        .grip(-7.0F, 11.6F, 0.72F)
                        .shape(arm())
                        .joint(-6.57F, 17.1F, 0.14F, 0.6F);
        rig.part(Bone.LEFT_ARM).mirrorOf(part2);
        Part part3 =
                rig.extra("hand_r", Bone.RIGHT_ARM).shape(hand()).resolution(0.6F).detail(0.8F);
        rig.extra("hand_l", Bone.LEFT_ARM).mirrorOf(part3);
        Part part4 =
                rig.part(Bone.RIGHT_LEG)
                        .pivot(-2.2F, 12.8F, 0.0F)
                        .shape(leg())
                        .swing(0.6F, 35.0F)
                        .joint(-2.3F, 6.75F, 0.16F, 0.7F);
        rig.part(Bone.LEFT_LEG).mirrorOf(part4);
        Part part5 =
                rig.extra("foot_r", Bone.RIGHT_LEG).shape(foot()).resolution(0.7F).detail(0.8F);
        rig.extra("foot_l", Bone.LEFT_LEG).mirrorOf(part5);
    }

    private static Shape log() {
        Shape shape = Shapes.field(TungTungSahur::trunk, -5.5F, 11.8F, -5.5F, 5.5F, 34.2F, 5.5F);
        TungTungSahur.Bark bark =
                new TungTungSahur.Bark(
                        Paint.wood(-1063542, -4158378, 0.3F, 0.0F, -0.2F, 0.0F, 1.0F, 0.0F, 0.8F)
                                .noise(-5868993, 3.5F, 0.08F)
                                .grain(0.35F, 0.04F));
        Shape shape2 =
                Shapes.blend(
                        0.35F,
                        shape.paint(bark),
                        knot(38.0F, 17.2F, 0.8F),
                        knot(165.0F, 27.5F, 0.95F),
                        knot(-120.0F, 19.5F, 0.7F),
                        knot(-58.0F, 14.2F, 0.55F),
                        stub(128.0F, 25.2F),
                        eye(1.0F, bark),
                        eye(-1.0F, bark));
        Shape shape3 =
                Shapes.ellipsoid(
                        0.0F, 25.35F, radius(0.0F, 25.35F, 1.0F) + 0.12F, 2.7F, 1.05F, 0.55F);
        return Shapes.subtract(
                0.12F,
                Shapes.subtract(0.35F, shape2, shape3),
                Shapes.union(hole(165.0F, 27.5F, 0.32F), hole(-120.0F, 19.5F, 0.24F)));
    }

    private static Shape stub(float f, float g) {
        float h = (float) Math.sin(Math.toRadians(f));
        float f2 = (float) Math.cos(Math.toRadians(f));
        float g2 = (float) Math.cos(Math.toRadians(32.0));
        float h2 = (float) Math.sin(Math.toRadians(32.0));
        float f3 = h * g2;
        float g3 = f2 * g2;
        float h3 = h * 3.9F;
        float f4 = f2 * 3.9F;
        float g4 = h3 + f3 * 3.05F;
        float h4 = g + h2 * 3.05F;
        float f5 = f4 + g3 * 3.05F;
        Shape shape =
                Shapes.cone(h3, g, f4, 0.98F, g4 + f3 * 0.4F, h4 + h2 * 0.4F, f5 + g3 * 0.4F, 0.64F)
                        .paint(Paint.wood(-1063542, -4158378, h3, g, f4, f3, h2, g3, 0.3F));
        Paint paint = Paint.radial(-6264264, g4, h4, f5, 0.75F, -1389672);
        return Shapes.subtract(shape, Shapes.plane(g4, h4, f5, -f3, -h2, -g3).paint(paint));
    }

    private static Shape eye(float f, Paint paint) {
        float g = f * 2.14F;
        float h = 28.72F;
        float f2 = g / 5.0F;
        float g2 = (float) Math.sqrt(1.0F - f2 * f2);
        float h2 = radius(f2, h, g2) - 0.5F;
        float f3 = (float) Math.toDegrees(Math.asin(f2));
        return Shapes.ellipsoid(f2 * h2, h, g2 * h2, 1.85F, 1.8F, 0.85F)
                .rotate(0.0F, f3, 0.0F, f2 * h2, h, g2 * h2)
                .paint(paint);
    }

    private static Shape knot(float f, float g, float h) {
        float f2 = (float) Math.sin(Math.toRadians(f));
        float g2 = (float) Math.cos(Math.toRadians(f));
        float h2 = radius(f2, g, g2) + 0.28F;
        float f3 = h2 - h;
        return Shapes.ellipsoid(f2 * f3, g, g2 * f3, h, h * 1.35F, h)
                .paint(Paint.radial(-11653867, f2 * h2, g, g2 * h2, h * 1.05F, -4158378));
    }

    private static Shape hole(float f, float g, float h) {
        float f2 = (float) Math.sin(Math.toRadians(f));
        float g2 = (float) Math.cos(Math.toRadians(f));
        float h2 = radius(f2, g, g2) + 0.28F;
        return Shapes.ellipsoid(f2 * h2, g, g2 * h2, h, h * 1.3F, h).color(-11653867);
    }

    private static float radius(float f, float g, float h) {
        float f2 = 0.17F * Noise.noise(f * 1.1F + 2.3F, g * 0.1F, h * 1.1F - 1.3F);
        return 5.0F + f2 - 0.17F * fissure(f, g, h);
    }

    private static float fissure(float f, float g, float h) {
        float f2 = Math.abs(Noise.noise(f * 3.0F - 5.1F, g * 0.08F, h * 3.0F + 0.7F));
        float g2 = smooth(0.3F, 0.62F, h) * smooth(21.8F, 23.6F, g);
        return Math.max(0.0F, 1.0F - f2 / 0.28F) * (1.0F - 0.88F * g2);
    }

    private static float trunk(float f, float g, float h) {
        float f2 = (float) Math.sqrt(f * f + h * h);
        float g2 = f2 > 1.0E-4F ? f / f2 : 0.0F;
        float h2 = f2 > 1.0E-4F ? h / f2 : 1.0F;
        float f3 = f2 - radius(g2, g, h2) + 0.85F;
        float g3 = Math.abs(g - 23.0F) - 11.0F + 0.85F;
        float h3 = Math.max(f3, 0.0F);
        float f4 = Math.max(g3, 0.0F);
        return (float) Math.sqrt(h3 * h3 + f4 * f4) + Math.min(Math.max(f3, g3), 0.0F) - 0.85F;
    }

    private static Shape arm() {
        Paint paint = stick(-5.4F, 22.6F, 0.2F, -7.0F, 11.6F, 0.72F);
        Shape shape =
                Shapes.blend(
                        0.2F,
                        Shapes.sphere(-5.3F, 22.8F, 0.2F, 0.7F),
                        Shapes.capsule(-5.3F, 22.8F, 0.2F, -3.9F, 22.8F, 0.2F, 0.55F));
        Shape shape2 = Shapes.cone(-5.4F, 22.5F, 0.2F, 0.64F, -6.55F, 17.3F, 0.12F, 0.54F);
        Shape shape3 = Shapes.sphere(-6.57F, 17.1F, 0.14F, 0.6F);
        Shape shape4 = Shapes.cone(-6.6F, 16.85F, 0.14F, 0.55F, -6.93F, 12.6F, 0.5F, 0.5F);
        return Shapes.blend(0.3F, shape, shape2, shape3, shape4).paint(paint);
    }

    private static Shape hand() {
        float f = KNUCKLE_DIR[0];
        float g = KNUCKLE_DIR[1];
        float h = KNUCKLE_DIR[2];
        Shape shape = Shapes.ellipsoid(-6.92F, 11.650001F, 0.70000005F, 0.86F, 1.05F, 0.9F);
        Shape shape2 = Shapes.cone(-6.93F, 12.75F, 0.5F, 0.5F, -6.95F, 12.150001F, 0.72F, 0.64F);
        Shape[] shapes = new Shape[4];
        for (int i = 0; i < 4; i++) {
            float f2 = -0.66F + i * 0.44F;
            float g2 = -7.0F + f * f2 - 0.28F;
            float h2 = 11.6F + g * f2;
            float f3 = 0.72F + h * f2 + 0.72F;
            shapes[i] =
                    Shapes.capsule(
                            g2 - 0.35F,
                            h2,
                            f3 - 0.3F,
                            g2 + 0.3F,
                            h2,
                            f3 + 0.1F,
                            0.26F + 0.02F * (3 - i));
        }
        Shape shape3 = Shapes.capsule(-6.38F, 12.1F, 0.92F, -6.82F, 12.320001F, 1.6700001F, 0.27F);
        return Shapes.blend(0.18F, Shapes.blend(0.3F, shape, shape2), Shapes.union(shapes), shape3)
                .paint(stick(-6.9F, 12.8F, 0.5F, -7.0F, 10.6F, 0.72F));
    }

    private static Shape leg() {
        Paint paint = stick(-2.2F, 12.8F, 0.0F, -2.35F, 1.4F, 0.0F);
        Shape shape = Shapes.sphere(-2.2F, 12.6F, 0.0F, 0.8F);
        Shape shape2 = Shapes.cone(-2.2F, 12.4F, 0.0F, 0.74F, -2.3F, 7.0F, 0.12F, 0.62F);
        Shape shape3 = Shapes.sphere(-2.3F, 6.75F, 0.16F, 0.68F);
        Shape shape4 = Shapes.cone(-2.3F, 6.5F, 0.12F, 0.62F, -2.36F, 1.9F, 0.0F, 0.55F);
        return Shapes.blend(0.3F, shape, shape2, shape3, shape4).paint(paint);
    }

    private static Shape foot() {
        Shape shape =
                Shapes.blend(
                        0.5F,
                        Shapes.ellipsoid(-2.42F, 0.55F, 1.0F, 1.2F, 0.55F, 1.9F),
                        Shapes.ellipsoid(-2.36F, 0.95F, -0.1F, 0.8F, 0.85F, 1.0F),
                        Shapes.sphere(-2.36F, 1.75F, 0.0F, 0.55F));
        Shape shape2 =
                Shapes.union(
                        Shapes.sphere(-1.7F, 0.38F, 2.55F, 0.4F),
                        Shapes.sphere(-2.2F, 0.35F, 2.8F, 0.36F),
                        Shapes.sphere(-2.68F, 0.34F, 2.76F, 0.33F),
                        Shapes.sphere(-3.12F, 0.33F, 2.52F, 0.31F));
        return Shapes.intersect(Shapes.blend(0.22F, shape, shape2), Shapes.above(0.0F))
                .paint(stick(-2.4F, 0.6F, -1.0F, -2.4F, 0.6F, 3.0F));
    }

    private static Paint stick(float f, float g, float h, float f2, float g2, float h2) {
        return Paint.wood(-3567528, -7053254, f, g, h, f2 - f, g2 - g, h2 - h, 0.35F)
                .grain(0.3F, 0.05F);
    }

    private static Decal eyes() {
        Canvas canvas = new Canvas(336, 208);
        for (int i = -1; i <= 1; i += 2) {
            float f = 168.0F + i * 85.5F;
            float g = 122.0F;
            canvas.fill(Shape2D.circle(f, g, 75.0F), 808458248, 13.0F);
            canvas.fill(Shape2D.circle(f, g, 66.0F), -15003638);
            canvas.fill(
                    Shape2D.circle(f, g, 60.75F),
                    Paint2D.radial(f - 15.0F, g - 18.0F, 78.0F, -1, -132105, -1054240));
            float h = f - i * 1.5F;
            float f2 = g + 1.0F;
            canvas.fill(Shape2D.circle(h, f2, 12.0F), -15857658);
            canvas.fill(Shape2D.circle(h - 3.9F, f2 - 4.4F, 3.6F), -285212673);
        }
        Shape2D shape2D = Shape2D.curve(15.0F, 48.0F, 74.0F, 18.0F, 147.0F, 71.0F, 21.0F, 34.5F);
        canvas.fill(shape2D, -15003638);
        canvas.fill(
                shape2D.mirrorX(168.0F).rotate(6.0F, 321.0F, 48.0F).move(0.0F, -7.0F), -15003638);
        return Decal.front(canvas, 0.0F, 29.175F, 5.0F, 8.4F, 5.2F)
                .depth(3.6F, 1.5F)
                .maxAngle(72.0F)
                .flatLight(1.0F)
                .occlusion(0.0F);
    }

    private static Decal mouth() {
        Canvas canvas = new Canvas(272, 104);
        Shape2D shape2D =
                Shape2D.ellipse(136.0F, 19.0F, 124.5F, 78.0F)
                        .subtract(Shape2D.circle(136.0F, -482.0F, 507.0F));
        canvas.fill(shape2D.inflate(6.0F), -15003638);
        canvas.fill(shape2D, Paint2D.linear(136.0F, 10.0F, 136.0F, 95.5F, -15071227, -12970996));
        canvas.fill(Shape2D.ellipse(136.0F, 98.5F, 75.0F, 39.0F).intersect(shape2D), -3646898);
        return Decal.front(canvas, 0.0F, 24.675F, 5.0F, 6.8F, 2.6F)
                .depth(3.6F, 1.5F)
                .maxAngle(72.0F);
    }

    private static Decal rings() {
        Canvas canvas = new Canvas(168, 168);
        canvas.fill(
                Shape2D.circle(84.0F, 84.0F, 72.0F),
                Paint2D.radial(81.0F, 86.0F, 72.0F, -3565470, -1917818, -1521774, -2709138),
                4.0F);
        Shape2D shape2D =
                Shape2D.field(
                        (value, value2) -> {
                            float f = value - 81.0F;
                            float g = value2 - 86.0F;
                            float h = (float) Math.sqrt(f * f + g * g) + 0.001F;
                            float f2 =
                                    h + 2.5F * Noise.noise(f / h * 1.3F, g / h * 1.3F, h * 0.028F);
                            float g2 = f2 / 8.25F;
                            float h2 = g2 - Math.round(g2);
                            return Math.max(
                                    Math.max(Math.abs(h2) * 8.25F - 0.82F, h - 67.5F), 6.7F - h);
                        },
                        12.0F,
                        17.0F,
                        150.0F,
                        155.0F);
        canvas.fill(shape2D, -1602927328);
        canvas.fill(Shape2D.circle(81.0F, 86.0F, 3.6F), -9813984);
        canvas.fill(Shape2D.taper(87.0F, 81.0F, 3.0F, 133.0F, 52.0F, 1.0F), -1069538288);
        canvas.fill(Shape2D.taper(77.0F, 91.0F, 2.2F, 47.0F, 126.0F, 1.0F), -1337973744);
        return Decal.top(canvas, 0.0F, 34.0F, 0.0F, 9.678F, 9.678F)
                .depth(0.6F, 0.6F)
                .maxAngle(35.0F);
    }

    private static float smooth(float f, float g, float h) {
        float f2 = Math.max(0.0F, Math.min(1.0F, (h - f) / (g - f)));
        return f2 * f2 * (3.0F - 2.0F * f2);
    }

    private static float[] unit(float f, float g, float h) {
        float f2 = (float) Math.sqrt(f * f + g * g + h * h);
        return new float[] {f / f2, g / f2, h / f2};
    }

    @Environment(EnvType.CLIENT)
    private static final class Bark extends Paint {

        private final Paint base;

        Bark(Paint base2) {
            this.base = base2;
        }

        @Override
        public void apply(float f, float g, float h, Sample sample) {
            this.base.apply(f, g, h, sample);
            float f2 = (float) Math.sqrt(f * f + h * h);
            if (!(f2 < 0.5F)) {
                float g2 =
                        TungTungSahur.smooth(3.95F, 4.7F, f2)
                                * TungTungSahur.smooth(12.0F, 12.6F, g)
                                * (1.0F - TungTungSahur.smooth(33.4F, 34.0F, g));
                float h2 = TungTungSahur.fissure(f / f2, g, h / f2) * g2;
                if (h2 > 0.0F) {
                    sample.toward(-9551070, Math.min(1.0F, h2 * 0.62F));
                }
            }
        }
    }
}
