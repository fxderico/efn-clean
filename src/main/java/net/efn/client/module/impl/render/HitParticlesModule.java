package net.efn.client.module.impl.render;

import net.efn.client.event.EventPriority;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PlayerAttackEntityEvent;
import net.efn.client.gui.ThemeTint;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ColorSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

@Environment(EnvType.CLIENT)
public final class HitParticlesModule extends ClientModule {

    private static final String TYPE_CRIT = "Crit";

    private static final String TYPE_MAGIC_CRIT = "Magic Crit";

    private static final String TYPE_SPARK = "Spark";

    private static final String TYPE_FLAME = "Flame";

    private static final String TYPE_SOUL_FLAME = "Soul Flame";

    private static final String TYPE_END_ROD = "End Rod";

    private static final String TYPE_TOTEM = "Totem";

    private static final String TYPE_HEART = "Heart";

    private static final String TYPE_DUST = "Dust";

    private static final String ORIGIN_IMPACT = "Impact";

    private static final String ORIGIN_BODY = "Body";

    private static final String TARGETS_LIVING = "Living";

    private static final String TARGETS_PLAYERS = "Players";

    private static final String TARGETS_ALL = "All";

    private static final String COLOR_CUSTOM = "Custom";

    private static final String COLOR_ACCENT = "Accent";

    private static final double LIFT = 0.25;

    private static final int PEEP_DUST = 16743112;

    private final ModeSetting type =
            this.addSetting(
                    "type",
                    new ModeSetting(
                            "Type",
                            "Crit",
                            "Crit",
                            "Magic Crit",
                            "Spark",
                            "Flame",
                            "Soul Flame",
                            "End Rod",
                            "Totem",
                            "Heart",
                            "Dust"));

    private final NumberSetting amount =
            this.addSetting("amount", new NumberSetting("Amount", 10.0F, 1.0F, 40.0F).step(1.0F));

    private final NumberSetting spread =
            this.addSetting("spread", new NumberSetting("Spread", 1.0F, 0.25F, 3.0F).step(0.25F));

    private final ModeSetting origin =
            this.addSetting("origin", new ModeSetting("Origin", "Impact", "Impact", "Body"));

    private final BooleanSetting onlyCrits =
            this.addSetting("only-crits", new BooleanSetting("Only Crits", false));

    private final ModeSetting targets =
            this.addSetting(
                    "targets", new ModeSetting("Targets", "Living", "Living", "Players", "All"));

    private final ModeSetting dustColorMode =
            this.addSetting(
                    "dust-color-mode", new ModeSetting("Color", "Accent", "Custom", "Accent"));

    private final ColorSetting dustColor =
            this.addSetting("dust-color", new ColorSetting("Custom Color", -657929));

    private final NumberSetting dustSize =
            this.addSetting(
                    "dust-size", new NumberSetting("Dust Size", 1.0F, 0.5F, 2.0F).step(0.1F));

    private final Random random = Random.create();

    public HitParticlesModule() {
        super(
                "hit-particles",
                "Hit Particles",
                "Spawns particles where your hits land.",
                ModuleCategory.VISUALS);
        this.dustColorMode.visibleWhen(() -> this.type.is("Dust"));
        this.dustColor.visibleWhen(() -> this.type.is("Dust") && this.dustColorMode.is("Custom"));
        this.dustSize.visibleWhen(() -> this.type.is("Dust"));
    }

    @Override
    protected void onEnable() {
        this.on(PlayerAttackEntityEvent.TYPE, Stage.PRE, EventPriority.LOWEST, this::onAttack);
    }

    private void onAttack(PlayerAttackEntityEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.isOnThread()) {
            ClientPlayerEntity player = client.player;
            ClientWorld world = client.world;
            Entity target = event.target;
            if (player != null && world != null && target != null && target != player) {
                if (target.isAttackable() && this.accepts(target)) {
                    if (!this.onlyCrits.get() || isCritical(player, target)) {
                        this.spawnBurst(client, world, player, target);
                    }
                }
            }
        }
    }

    private boolean accepts(Entity entity) {
        String targets2 = this.targets.get();
        return switch (targets2) {
            case "Players" -> entity instanceof PlayerEntity;
            case "All" -> true;
            default -> entity instanceof LivingEntity;
        };
    }

    private static boolean isCritical(ClientPlayerEntity player, Entity entity) {
        return player.getAttackCooldownProgress(0.5F) > 0.9F
                && player.fallDistance > 0.0
                && !player.isOnGround()
                && !player.isClimbing()
                && !player.isTouchingWater()
                && !player.hasBlindnessEffect()
                && !player.hasVehicle()
                && !player.isSprinting()
                && entity instanceof LivingEntity;
    }

    private void spawnBurst(
            MinecraftClient client, ClientWorld world, ClientPlayerEntity player, Entity entity) {
        HitParticlesModule.Burst burst = this.burst();
        Box boundingBox = entity.getBoundingBox();
        Vec3d center = boundingBox.getCenter();
        boolean is = this.origin.is("Body");
        Vec3d vec = is ? null : impactPoint(client, player, entity, boundingBox);
        Vec3d vec2 = player.getRotationVector().negate();
        double spread2 = this.spread.get();
        double d = burst.jitter() * spread2;
        int amount2 = this.amount.getInt();
        float f = this.type.is("Dust") ? 0.0F : ThemeTint.world();
        DustParticleEffect dustParticleEffect =
                f > 0.0F ? new DustParticleEffect(16743112, 0.9F) : null;
        for (int j = 0; j < amount2; j++) {
            Vec3d vec3 = is ? this.randomPointIn(boundingBox) : vec;
            Vec3d vec4 = vec3.subtract(center);
            vec4 = vec4.lengthSquared() < 1.0E-6 ? vec2 : vec4.normalize();
            Vec3d vec5 = vec4.multiply(0.8).add(this.randomUnit()).add(0.0, 0.25, 0.0).normalize();
            double e = burst.speed() * spread2 * (0.6 + 0.4 * this.random.nextDouble());
            Object object =
                    dustParticleEffect != null && this.random.nextFloat() < f
                            ? dustParticleEffect
                            : burst.particle();
            world.addParticleClient(
                    (ParticleEffect) object,
                    true,
                    false,
                    vec3.x + this.random.nextGaussian() * d,
                    vec3.y + this.random.nextGaussian() * d,
                    vec3.z + this.random.nextGaussian() * d,
                    vec5.x * e,
                    vec5.y * e,
                    vec5.z * e);
        }
    }

    private HitParticlesModule.Burst burst() {
        String type2 = this.type.get();
        return switch (type2) {
            case "Magic Crit" ->
                    new HitParticlesModule.Burst(ParticleTypes.ENCHANTED_HIT, 0.8, 0.1);
            case "Spark" -> new HitParticlesModule.Burst(ParticleTypes.ELECTRIC_SPARK, 0.16, 0.1);
            case "Flame" -> new HitParticlesModule.Burst(ParticleTypes.FLAME, 0.035, 0.12);
            case "Soul Flame" ->
                    new HitParticlesModule.Burst(ParticleTypes.SOUL_FIRE_FLAME, 0.035, 0.12);
            case "End Rod" -> new HitParticlesModule.Burst(ParticleTypes.END_ROD, 0.09, 0.1);
            case "Totem" -> new HitParticlesModule.Burst(ParticleTypes.TOTEM_OF_UNDYING, 0.45, 0.1);
            case "Heart" -> new HitParticlesModule.Burst(ParticleTypes.HEART, 0.0, 0.35);
            case "Dust" ->
                    new HitParticlesModule.Burst(
                            new DustParticleEffect(this.dustRgb(), this.dustSize.get()), 1.0, 0.3);
            default -> new HitParticlesModule.Burst(ParticleTypes.CRIT, 0.8, 0.1);
        };
    }

    private int dustRgb() {
        int i =
                this.dustColorMode.is("Accent")
                        ? CosmeticAccent.argb()
                        : ThemeTint.world(this.dustColor.argb());
        return i & 16777215;
    }

    private static Vec3d impactPoint(
            MinecraftClient client, ClientPlayerEntity player, Entity entity, Box box) {
        HitResult crosshairTarget = client.crosshairTarget;
        EntityHitResult entityHitResult;
        if (crosshairTarget instanceof EntityHitResult
                && (entityHitResult = (EntityHitResult) crosshairTarget).getEntity() == entity) {
            return entityHitResult.getPos();
        } else {
            Vec3d eyePos = player.getEyePos();
            return new Vec3d(
                    MathHelper.clamp(eyePos.x, box.minX, box.maxX),
                    MathHelper.clamp(eyePos.y, box.minY, box.maxY),
                    MathHelper.clamp(eyePos.z, box.minZ, box.maxZ));
        }
    }

    private Vec3d randomPointIn(Box box) {
        return new Vec3d(
                box.minX + box.getLengthX() * (0.1 + 0.8 * this.random.nextDouble()),
                box.minY + box.getLengthY() * (0.1 + 0.8 * this.random.nextDouble()),
                box.minZ + box.getLengthZ() * (0.1 + 0.8 * this.random.nextDouble()));
    }

    private Vec3d randomUnit() {
        Vec3d vec =
                new Vec3d(
                        this.random.nextGaussian(),
                        this.random.nextGaussian(),
                        this.random.nextGaussian());
        return vec.lengthSquared() < 1.0E-9 ? new Vec3d(0.0, 1.0, 0.0) : vec.normalize();
    }

    @Environment(EnvType.CLIENT)
    private record Burst(ParticleEffect particle, double speed, double jitter) {}
}
