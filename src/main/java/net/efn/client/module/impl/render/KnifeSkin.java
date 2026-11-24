package net.efn.client.module.impl.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
final class KnifeSkin {

    static final String NONE = "None";

    static final String BUTTERFLY = "Butterfly";

    static final String KARAMBIT = "Karambit";

    static final String KATANA = "Katana";

    static final int MAX_STREAKS = 2;

    private static final Vector3f OFF_FOREARM = new Vector3f(0.4F, -0.85F, 0.35F).normalize();

    private static final Vector3f OFF_AWAY = new Vector3f(-0.3F, -0.75F, 0.1F);

    private static final float WIPE_LIFT = 3.0F;

    private static final float WIPE_TAIL = 16.0F;

    private static final Vector3f SCABBARD_FALL = new Vector3f(-0.3F, -0.9F, 0.25F);

    private static final float SCABBARD_TURN = 0.6981317F;

    private static final float[][] BUTTERFLY_INSPECT =
            new float[][] {
                {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
                {
                    0.08F, -0.07F, 0.09F, 0.05F, -8.0F, 8.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                    8.0F
                },
                {
                    0.14F, -0.09F, 0.12F, 0.06F, -12.0F, 8.0F, 14.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                    12.0F
                },
                {
                    0.19F, -0.09F, 0.12F, 0.06F, -10.0F, 10.0F, 2.0F, -18.0F, 0.0F, 90.0F, 180.0F,
                    0.0F, 12.0F
                },
                {
                    0.24F, -0.09F, 0.13F, 0.06F, -12.0F, 8.0F, 10.0F, 0.0F, 0.0F, 180.0F, 360.0F,
                    0.0F, 12.0F
                },
                {
                    0.29F, -0.09F, 0.12F, 0.06F, -10.0F, 10.0F, 2.0F, 18.0F, 0.0F, 270.0F, 540.0F,
                    0.0F, 12.0F
                },
                {
                    0.34F, -0.09F, 0.12F, 0.06F, -12.0F, 8.0F, 14.0F, 0.0F, 0.0F, 360.0F, 720.0F,
                    0.0F, 12.0F
                },
                {
                    0.39F, -0.09F, 0.12F, 0.06F, -10.0F, 10.0F, 2.0F, -18.0F, 0.0F, 450.0F, 900.0F,
                    0.0F, 12.0F
                },
                {
                    0.44F, -0.09F, 0.13F, 0.06F, -12.0F, 8.0F, 10.0F, 0.0F, 0.0F, 540.0F, 1080.0F,
                    0.0F, 12.0F
                },
                {
                    0.49F, -0.09F, 0.12F, 0.06F, -10.0F, 10.0F, 2.0F, 18.0F, 0.0F, 630.0F, 1260.0F,
                    0.0F, 12.0F
                },
                {
                    0.54F, -0.09F, 0.12F, 0.06F, -12.0F, 8.0F, 14.0F, 0.0F, 0.0F, 720.0F, 1440.0F,
                    0.0F, 12.0F
                },
                {
                    0.62F, -0.1F, 0.12F, 0.07F, -35.0F, 6.0F, 20.0F, 0.0F, 0.0F, 720.0F, 1440.0F,
                    0.0F, 0.0F
                },
                {
                    0.76F, -0.1F, 0.13F, 0.07F, 25.0F, 6.0F, 16.0F, 0.0F, 0.0F, 720.0F, 1440.0F,
                    1.0F, 0.0F
                },
                {
                    0.88F, -0.05F, 0.06F, 0.03F, 8.0F, 3.0F, 6.0F, 0.0F, 0.0F, 720.0F, 1440.0F,
                    1.0F, 0.0F
                },
                {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 720.0F, 1440.0F, 1.0F, 0.0F}
            };

    private static final float[][] BUTTERFLY_DEPLOY =
            new float[][] {
                {
                    0.0F, 0.02F, -0.14F, 0.02F, 0.0F, -30.0F, -20.0F, 0.0F, 0.0F, 180.0F, 0.0F,
                    0.0F, 10.0F
                },
                {
                    0.3F, 0.01F, -0.04F, 0.01F, 0.0F, -10.0F, -10.0F, 0.0F, 0.0F, 220.0F, 100.0F,
                    0.0F, 10.0F
                },
                {
                    0.62F, 0.0F, 0.01F, 0.0F, 0.0F, 4.0F, 6.0F, 0.0F, 0.0F, 350.0F, 340.0F, 0.0F,
                    3.0F
                },
                {
                    0.8F, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F, -2.0F, 0.0F, 0.0F, 365.0F, 368.0F, 0.0F,
                    0.0F
                },
                {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 360.0F, 360.0F, 0.0F, 0.0F}
            };

    private static final float[][] KARAMBIT_INSPECT =
            new float[][] {
                {0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
                {0.1F, -0.12F, 0.14F, 0.06F, -10.0F, 14.0F, 10.0F, 0.0F, 0.0F, -25.0F, 0.0F, 0.0F},
                {0.14F, -0.14F, 0.16F, 0.06F, -10.0F, 14.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F},
                {
                    0.205F, -0.14F, 0.17F, 0.06F, -10.0F, 14.0F, 10.0F, 0.0F, 0.0F, 180.0F, 0.0F,
                    0.0F
                },
                {0.27F, -0.14F, 0.16F, 0.06F, -10.0F, 14.0F, 10.0F, 0.0F, 0.0F, 360.0F, 0.0F, 0.0F},
                {
                    0.335F, -0.14F, 0.17F, 0.06F, -10.0F, 14.0F, 10.0F, 0.0F, 0.0F, 540.0F, 0.0F,
                    0.0F
                },
                {0.4F, -0.14F, 0.16F, 0.06F, -10.0F, 14.0F, 10.0F, 0.0F, 0.0F, 720.0F, 0.0F, 0.0F},
                {0.48F, -0.13F, 0.16F, 0.06F, -10.0F, 12.0F, 10.0F, 0.0F, 0.0F, 750.0F, 0.0F, 0.0F},
                {0.56F, -0.1F, 0.12F, 0.06F, 20.0F, 10.0F, -70.0F, 0.0F, 0.0F, 720.0F, 0.0F, 0.0F},
                {0.72F, -0.1F, 0.13F, 0.06F, -10.0F, 10.0F, -60.0F, 0.0F, 0.0F, 720.0F, 0.0F, 1.0F},
                {0.86F, -0.06F, 0.08F, 0.05F, -6.0F, 4.0F, 8.0F, 0.0F, 0.0F, 360.0F, 0.0F, 1.0F},
                {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 360.0F, 0.0F, 1.0F}
            };

    private static final float[][] KARAMBIT_DEPLOY =
            new float[][] {
                {0.0F, 0.02F, -0.14F, 0.02F, 0.0F, -30.0F, -20.0F, 0.0F, 0.0F, -540.0F, 0.0F, 0.0F},
                {0.55F, 0.0F, 0.0F, 0.0F, 0.0F, 4.0F, 4.0F, 0.0F, 0.0F, -60.0F, 0.0F, 0.0F},
                {0.78F, 0.0F, 0.0F, 0.0F, 0.0F, -2.0F, -2.0F, 0.0F, 0.0F, 12.0F, 0.0F, 0.0F},
                {1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F}
            };

    private static final float[][] KATANA_INSPECT =
            new float[][] {
                {
                    0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                    0.0F, 0.0F, 0.0F, 4.0F
                },
                {
                    0.1F, -0.06F, 0.12F, 0.06F, 0.0F, 0.0F, 60.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                    0.0F, 0.0F, 0.0F, 0.6F, 4.0F
                },
                {
                    0.15F, -0.07F, 0.14F, 0.08F, -2.0F, 0.0F, 60.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                    0.0F, 0.0F, 0.0F, 1.0F, 4.0F
                },
                {
                    0.2F, -0.07F, 0.14F, 0.08F, -2.0F, 0.0F, 60.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                    0.0F, 0.0F, 0.0F, 1.0F, 12.0F
                },
                {
                    0.36F, -0.07F, 0.14F, 0.08F, 0.0F, 0.0F, 58.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                    0.0F, 0.0F, 0.0F, 1.0F, 44.0F
                },
                {
                    0.4F, -0.07F, 0.14F, 0.08F, 0.0F, 0.0F, 58.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                    0.0F, 0.0F, 0.0F, 1.0F, 53.0F
                },
                {
                    0.44F, -0.06F, 0.13F, 0.06F, 2.0F, 1.0F, 54.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                    0.0F, 0.0F, 0.0F, 0.6F, 58.0F
                },
                {
                    0.48F, -0.05F, 0.11F, 0.03F, 4.0F, 2.0F, 42.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                    0.0F, 0.0F, 0.0F, 0.0F, 62.0F
                },
                {
                    0.52F, -0.04F, 0.1F, 0.02F, 0.0F, 4.0F, 20.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                    0.0F, 0.0F, 0.0F, 0.0F, 62.0F
                },
                {
                    0.6F, -0.04F, 0.12F, 0.02F, 0.0F, 4.0F, -160.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                    0.0F, 0.0F, 0.0F, 0.0F, 62.0F
                },
                {
                    0.68F, -0.04F, 0.1F, 0.02F, 0.0F, 4.0F, -340.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                    0.0F, 0.0F, 0.0F, 0.0F, 62.0F
                },
                {
                    0.78F, 0.04F, -0.03F, 0.0F, 0.0F, -6.0F, -400.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                    0.0F, 0.0F, 0.0F, 0.0F, 62.0F
                },
                {
                    0.88F, 0.0F, 0.02F, 0.0F, 0.0F, 2.0F, -365.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                    0.0F, 0.0F, 0.0F, 0.0F, 62.0F
                },
                {
                    1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -360.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                    0.0F, 0.0F, 0.0F, 62.0F
                }
            };

    private static final float[][] KATANA_DEPLOY =
            new float[][] {
                {
                    0.0F, 0.06F, 0.0F, 0.02F, -10.0F, 0.0F, 45.0F, 180.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                    0.0F, 0.0F, 1.0F, 0.0F
                },
                {
                    0.05F, 0.05F, 0.01F, 0.01F, -12.0F, 0.0F, 47.0F, 180.0F, 0.0F, 0.0F, 0.0F, 0.0F,
                    0.0F, 2.0F, 1.0F, 0.0F
                },
                {
                    0.18F, 0.15F, 0.0F, 0.05F, -10.0F, 0.0F, 45.0F, 180.0F, 0.0F, 0.0F, 0.0F, 0.3F,
                    0.0F, 16.0F, 1.0F, 0.0F
                },
                {
                    0.3F, 0.3F, -0.02F, 0.1F, -8.0F, 0.0F, 43.0F, 180.0F, 0.0F, 0.0F, 0.0F, 0.9F,
                    0.0F, 50.0F, 1.0F, 0.0F
                },
                {
                    0.33F, 0.3F, 0.0F, 0.1F, -4.0F, 4.0F, 36.0F, 180.0F, 0.0F, 0.0F, 0.0F, 1.0F,
                    0.0F, 52.0F, 1.0F, 0.0F
                },
                {
                    0.41F, 0.08F, 0.12F, 0.04F, 0.0F, 10.0F, -40.0F, 180.0F, 0.0F, 0.0F, 0.0F, 1.0F,
                    0.0F, 52.0F, 0.25F, 0.0F
                },
                {
                    0.47F, -0.02F, 0.1F, 0.02F, 5.0F, 8.0F, -100.0F, 180.0F, 0.0F, 0.0F, 0.0F, 1.0F,
                    0.0F, 52.0F, 0.0F, 0.0F
                },
                {
                    0.62F, -0.02F, 0.08F, 0.02F, 0.0F, 6.0F, -460.0F, 180.0F, 0.0F, 0.0F, 0.0F,
                    1.0F, 0.0F, 52.0F, 0.0F, 0.0F
                },
                {
                    0.71F, 0.04F, -0.04F, 0.0F, 0.0F, -6.0F, -520.0F, 180.0F, 0.0F, 0.0F, 0.0F,
                    1.0F, 0.0F, 52.0F, 0.0F, 0.0F
                },
                {
                    0.82F, 0.0F, 0.02F, 0.0F, 0.0F, 2.0F, -370.0F, 300.0F, 0.0F, 0.0F, 0.0F, 2.0F,
                    0.0F, 52.0F, 0.0F, 0.0F
                },
                {
                    1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -360.0F, 360.0F, 0.0F, 0.0F, 0.0F, 3.0F,
                    0.0F, 52.0F, 0.0F, 0.0F
                }
            };

    private static KnifeSkin butterfly;

    private static KnifeSkin karambit;

    private static KnifeSkin katana;

    final String name;

    final InspectAnimation.Clip inspect;

    final InspectAnimation.Clip deploy;

    private final KnifeSkin.Part[] parts;

    private final KnifeSkin.Streak[] streaks;

    private final float voxel;

    private final float gripX;

    private final float gripY;

    private final Vector3f fist;

    private final Vector3f elbow = new Vector3f();

    private final Quaternionf rest;

    private KnifeMesh scabbard;

    private float saddleX;

    private float saddleY;

    private float[] letGo;

    private KnifeSkin.Centre centre;

    private float bladeFrom;

    private float bladeTo;

    private KnifeSkin(
            String name2,
            KnifeSkin.Part[] parts2,
            KnifeSkin.Streak[] streaks2,
            float voxel2,
            float gripX2,
            float gripY2,
            Vector3f fist2,
            Vector3f vec,
            Vector3f vec2,
            InspectAnimation.Clip inspect2,
            InspectAnimation.Clip deploy2) {
        this.name = name2;
        this.parts = parts2;
        this.streaks = streaks2;
        this.voxel = voxel2;
        this.gripX = gripX2;
        this.gripY = gripY2;
        this.fist = fist2;
        this.inspect = inspect2;
        this.deploy = deploy2;
        Vector3f vec3 = new Vector3f(vec).normalize();
        Vector3f vec4 = new Vector3f(vec2).fma(-vec2.dot(vec3), vec3).normalize();
        this.rest =
                new Quaternionf()
                        .setFromNormalized(
                                new Matrix3f(vec3, vec4, new Vector3f(vec3).cross(vec4)));
        fist2.fma(0.6F, FirstPersonArm.FOREARM, this.elbow);
    }

    static KnifeSkin of(String string) {
        return switch (string) {
            case "Butterfly" -> butterfly != null ? butterfly : (butterfly = butterfly());
            case "Karambit" -> karambit != null ? karambit : (karambit = karambit());
            case "Katana" -> katana != null ? katana : (katana = katana());
            default -> null;
        };
    }

    private static KnifeSkin butterfly() {
        float f = -1.5F;
        float g = 0.5F;
        KnifeSkin.Part[] parts2 =
                new KnifeSkin.Part[] {
                    new KnifeSkin.Part(KnifeModels.butterflyHandle(1), -1, f, g),
                    new KnifeSkin.Part(KnifeModels.butterflyBlade(), 8, f, g),
                    new KnifeSkin.Part(KnifeModels.butterflyHandle(-1), 9, f, g)
                };
        KnifeSkin.Streak[] streaks2 =
                new KnifeSkin.Streak[] {
                    new KnifeSkin.Streak(1, 2.5F, 0.5F, 21.5F, -0.5F, 1.0F),
                    new KnifeSkin.Streak(2, -4.0F, 0.5F, -27.5F, 0.5F, 0.8F)
                };
        return new KnifeSkin(
                "Butterfly",
                parts2,
                streaks2,
                0.021F,
                -10.0F,
                0.5F,
                new Vector3f(-0.06F, 0.06F, 0.02F),
                new Vector3f(-0.15F, 1.0F, -0.1F),
                new Vector3f(1.0F, 0.15F, 0.3F),
                new InspectAnimation.Clip(BUTTERFLY_INSPECT, 3.8F, 0.14F, 0.54F),
                new InspectAnimation.Clip(BUTTERFLY_DEPLOY, 0.9F));
    }

    private static KnifeSkin karambit() {
        float f = -16.0F;
        float g = -1.0F;
        KnifeSkin.Part[] parts2 =
                new KnifeSkin.Part[] {new KnifeSkin.Part(KnifeModels.karambit(), 8, f, g)};
        float[] kARAMBIT_TIP = KnifeModels.KARAMBIT_TIP;
        byte b = 2;
        KnifeSkin.Streak[] streaks2 =
                new KnifeSkin.Streak[] {
                    new KnifeSkin.Streak(
                            0, 6.0F * b, 0.5F * b, kARAMBIT_TIP[0], kARAMBIT_TIP[1], 1.0F)
                };
        return new KnifeSkin(
                "Karambit",
                parts2,
                streaks2,
                0.022F / b,
                f + 2.0F,
                g,
                new Vector3f(-0.06F, 0.1F, 0.02F),
                new Vector3f(-0.5F, -0.8F, -0.3F),
                new Vector3f(0.8F, -0.5F, 0.35F),
                new InspectAnimation.Clip(KARAMBIT_INSPECT, 3.2F, 0.14F, 0.4F),
                new InspectAnimation.Clip(KARAMBIT_DEPLOY, 0.8F));
    }

    private static KnifeSkin katana() {
        KnifeSkin.Part[] parts2 =
                new KnifeSkin.Part[] {new KnifeSkin.Part(KnifeModels.katana(), -1, 0.0F, 0.0F)};
        KnifeSkin.Streak[] streaks2 =
                new KnifeSkin.Streak[] {
                    new KnifeSkin.Streak(
                            0,
                            6.0F,
                            KnifeModels.katanaBow(6) + 0.5F,
                            48.5F,
                            KnifeModels.katanaBow(48) + 1.5F,
                            1.0F)
                };
        KnifeSkin knifeSkin =
                new KnifeSkin(
                        "Katana",
                        parts2,
                        streaks2,
                        0.028F,
                        -5.0F,
                        0.5F,
                        new Vector3f(-0.06F, 0.02F, 0.02F),
                        new Vector3f(-0.6F, 0.8F, -0.2F),
                        new Vector3f(0.6F, 0.45F, 0.1F),
                        new InspectAnimation.Clip(KATANA_INSPECT, 4.4F, 0.52F, 0.68F),
                        new InspectAnimation.Clip(KATANA_DEPLOY, 2.0F));
        knifeSkin.scabbard = KnifeModels.katanaScabbard();
        knifeSkin.saddleX = 20.0F;
        knifeSkin.saddleY = 0.0F;
        knifeSkin.centre = value -> value < 3.0F ? 0.5F : KnifeModels.katanaCentre(value);
        knifeSkin.bladeFrom = 3.0F;
        knifeSkin.bladeTo = 49.0F;
        knifeSkin.letGo = knifeSkin.findLetGo();
        return knifeSkin;
    }

    private float[] findLetGo() {
        float[] floats = new float[16];
        float[] floats2 = new float[16];
        for (int i = 0; i <= 2000; i++) {
            this.deploy.sample(i / 2000.0F, floats);
            if (floats[13] < 1.0F) {
                break;
            }
            System.arraycopy(floats, 0, floats2, 0, 16);
        }
        return floats2;
    }

    int streakCount() {
        return this.streaks.length;
    }

    float streakStrength(int i) {
        return this.streaks[i].strength;
    }

    void draw(
            MatrixStack matrices,
            float[] floats,
            float f,
            boolean bl,
            float g,
            KnifeSkin.Sink sink,
            float[] floats2) {
        Matrix4f matrix = this.knifeMatrix(floats, f, g, new Matrix4f());
        Vector3f vec = new Vector3f(this.fist).add(floats[0] * f, floats[1] * f, floats[2] * f);
        FirstPersonArm.place(
                matrices, vec, this.elbow, bl, matrixStackx -> sink.arm(matrixStackx, false));
        Matrix4f matrix2 =
                this.scabbard != null && f >= 0.999F && floats[13] > 0.01F
                        ? this.scabbardMatrix(floats, g)
                        : null;
        Vector3f vec2 = null;
        if (matrix2 != null) {
            vec2 = matrix2.transformPosition(this.saddleX, this.saddleY, 0.5F, new Vector3f());
        } else if (this.centre != null && floats[14] * f > 0.01F) {
            float h = MathHelper.clamp(floats[14] * f, 0.0F, 1.0F);
            float f2 = floats[15];
            float g2 = this.centre.y(f2) + (f2 >= this.bladeFrom ? 3.0F : 0.0F);
            vec2 =
                    matrix.transformPosition(f2, g2, 0.5F, new Vector3f())
                            .fma(1.0F - h * h * (3.0F - 2.0F * h), OFF_AWAY);
        }
        if (vec2 != null) {
            matrices.push();
            matrices.scale(-1.0F, 1.0F, 1.0F);
            Vector3f vec3 = new Vector3f(-vec2.x, vec2.y, vec2.z);
            FirstPersonArm.place(
                    matrices,
                    vec3,
                    new Vector3f(vec3).fma(0.6F, OFF_FOREARM),
                    bl,
                    matrixStackx -> sink.arm(matrixStackx, true));
            matrices.pop();
        }
        KnifeMesh.Shine shine;
        KnifeMesh.Shine shine2;
        if ((shine2 = shine = this.wipe(floats, f)) == null) {
            float shinePosition = shinePosition(floats[10]);
            shine2 =
                    new KnifeMesh.Shine(
                            shinePosition,
                            shinePosition > 0.0F
                                    ? MathHelper.sin(3.1415927F * shinePosition) * f
                                    : 0.0F,
                            0.0F);
        }
        matrices.push();
        matrices.multiplyPositionMatrix(matrix);
        for (int i = 0; i < this.parts.length; i++) {
            KnifeSkin.Part part2 = this.parts[i];
            matrices.push();
            float h2;
            if (part2.channel >= 0
                    && (h2 = MathHelper.wrapDegrees(floats[part2.channel]) * f) != 0.0F) {
                matrices.multiply(
                        RotationAxis.POSITIVE_Z.rotationDegrees(h2),
                        part2.pivotX,
                        part2.pivotY,
                        0.5F);
            }
            sink.part(matrices, part2.mesh, shine2);
            if (shine != null) {
                sink.glow(matrices, part2.mesh, shine);
            }
            if (floats2 != null) {
                this.streaks(i, matrices.peek().getPositionMatrix(), floats2);
            }
            matrices.pop();
        }
        matrices.pop();
        if (matrix2 != null) {
            matrices.push();
            matrices.multiplyPositionMatrix(matrix2);
            sink.part(matrices, this.scabbard, KnifeMesh.Shine.NONE);
            matrices.pop();
        }
    }

    private void streaks(int i, Matrix4f matrix, float[] floats) {
        Vector3f vec = new Vector3f();
        for (int j = 0; j < this.streaks.length; j++) {
            KnifeSkin.Streak streak = this.streaks[j];
            if (streak.part == i) {
                matrix.transformPosition(streak.baseX, streak.baseY, 0.5F, vec);
                floats[j * 6] = vec.x;
                floats[j * 6 + 1] = vec.y;
                floats[j * 6 + 2] = vec.z;
                matrix.transformPosition(streak.tipX, streak.tipY, 0.5F, vec);
                floats[j * 6 + 3] = vec.x;
                floats[j * 6 + 4] = vec.y;
                floats[j * 6 + 5] = vec.z;
            }
        }
    }

    private Matrix4f knifeMatrix(float[] floats, float f, float g, Matrix4f matrix) {
        float h = 0.017453292F * f;
        float f2 = this.voxel * g;
        matrix.translation(
                        this.fist.x + floats[0] * f,
                        this.fist.y + floats[1] * f,
                        this.fist.z + floats[2] * f)
                .rotate(
                        new Quaternionf()
                                .rotateY(MathHelper.wrapDegrees(floats[3]) * h)
                                .rotateX(MathHelper.wrapDegrees(floats[4]) * h)
                                .rotateZ(MathHelper.wrapDegrees(floats[5]) * h))
                .rotate(this.rest);
        float g2 = MathHelper.wrapDegrees(floats[6]) * h;
        if (g2 != 0.0F) {
            matrix.rotateX(g2);
        }
        return matrix.scale(f2).translate(floats[11] * f - this.gripX, -this.gripY, -0.5F);
    }

    private Matrix4f scabbardMatrix(float[] floats, float f) {
        boolean bl = floats[13] >= 1.0F;
        float[] floats2 = bl ? floats : this.letGo;
        float g = 312.5F;
        Matrix4f matrix =
                this.knifeMatrix(floats2, 1.0F, f, new Matrix4f())
                        .rotateAround(new Quaternionf().rotationZ(floats2[12] / g), 3.0F, g, 0.5F)
                        .translate(0.05F, 0.0F, 0.0F);
        if (!bl) {
            float h = 1.0F - Math.max(floats[13], 0.0F);
            h *= h;
            Vector3f vec =
                    matrix.transformPosition(this.saddleX, this.saddleY, 0.5F, new Vector3f());
            matrix.translateLocal(-vec.x, -vec.y, -vec.z)
                    .rotateLocalZ(0.6981317F * h)
                    .translateLocal(
                            vec.x + SCABBARD_FALL.x * h,
                            vec.y + SCABBARD_FALL.y * h,
                            vec.z + SCABBARD_FALL.z * h);
        }
        return matrix;
    }

    private KnifeMesh.Shine wipe(float[] floats, float f) {
        float g =
                this.centre != null
                        ? MathHelper.clamp((floats[14] * f - 0.7F) / 0.3F, 0.0F, 1.0F)
                        : 0.0F;
        if (!(g <= 0.01F) && !(floats[15] < this.bladeFrom)) {
            float h = (floats[15] - this.bladeFrom) / (this.bladeTo - this.bladeFrom);
            return new KnifeMesh.Shine(h, g * g * (3.0F - 2.0F * g), 16.0F);
        } else {
            return null;
        }
    }

    float sparkle(float[] floats, float f) {
        float sparkle = sparkle(floats[10]);
        if (this.centre != null && floats[15] >= this.bladeFrom) {
            float g = (floats[15] - this.bladeFrom) / (this.bladeTo - this.bladeFrom);
            float h =
                    MathHelper.clamp((g - 0.96F) / 0.06F, 0.0F, 1.0F)
                            * MathHelper.clamp((1.3F - g) / 0.15F, 0.0F, 1.0F);
            sparkle = Math.max(sparkle, h * MathHelper.clamp(floats[14] * 1.6F, 0.0F, 1.0F));
        }
        return sparkle * f;
    }

    static float shinePosition(float f) {
        if (f <= 0.0F) {
            return 0.0F;
        } else {
            float g = f - 2.0F * MathHelper.floor(f / 2.0F);
            return g < 1.0F ? g : 0.0F;
        }
    }

    static float sparkle(float f) {
        float shinePosition = shinePosition(f);
        return MathHelper.clamp((shinePosition - 0.72F) / 0.16F, 0.0F, 1.0F)
                * MathHelper.clamp((1.0F - shinePosition) / 0.07F, 0.0F, 1.0F);
    }

    @Environment(EnvType.CLIENT)
    private interface Centre {

        float y(float f);
    }

    @Environment(EnvType.CLIENT)
    private record Part(KnifeMesh mesh, int channel, float pivotX, float pivotY) {}

    @Environment(EnvType.CLIENT)
    interface Sink {

        void part(MatrixStack matrices, KnifeMesh knifeMesh, KnifeMesh.Shine shine);

        void glow(MatrixStack matrices, KnifeMesh knifeMesh, KnifeMesh.Shine shine);

        void arm(MatrixStack matrices, boolean bl);
    }

    @Environment(EnvType.CLIENT)
    record Streak(int part, float baseX, float baseY, float tipX, float tipY, float strength) {}
}
