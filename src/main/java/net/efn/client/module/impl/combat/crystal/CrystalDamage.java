package net.efn.client.module.impl.combat.crystal;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.ShapeContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class CrystalDamage {

    private static final double REACH = 12.0;

    private static final float SELF_SCALE = 1.15F;

    private static final float SELF_MARGIN = 2.0F;

    private static final EquipmentSlot[] ARMOR =
            new EquipmentSlot[] {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
            };

    private CrystalDamage() {}

    public static float estimate(Vec3d vec, LivingEntity entity2) {
        return new CrystalDamage.Victim(entity2, Vec3d.ZERO).damage(vec);
    }

    public static float estimate(Vec3d vec, LivingEntity entity2, float f, BlockPos pos) {
        return new CrystalDamage.Victim(entity2, Vec3d.ZERO).damage(vec, f, pos);
    }

    public static float health(LivingEntity entity2) {
        return entity2.getHealth() + entity2.getAbsorptionAmount();
    }

    public static boolean endangers(PlayerEntity player, float f) {
        return !player.isCreative() && f * 1.15F + 2.0F >= health(player);
    }

    private static float falloff(double d) {
        return falloff(d, 12.0);
    }

    private static float falloff(double d, double e) {
        return (float) ((d * d + d) / 2.0 * 7.0 * e + 1.0);
    }

    private static float protection(LivingEntity entity2) {
        Registry orThrow = entity2.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        int i = 0;
        for (EquipmentSlot equipmentSlot : ARMOR) {
            ItemStack equippedStack = entity2.getEquippedStack(equipmentSlot);
            if (!equippedStack.isEmpty()) {
                i +=
                        level(orThrow, Enchantments.PROTECTION, equippedStack)
                                + 2 * level(orThrow, Enchantments.BLAST_PROTECTION, equippedStack);
            }
        }
        return i;
    }

    private static int level(
            Registry<Enchantment> registry, RegistryKey<Enchantment> key, ItemStack stack) {
        return registry.getOptional(key)
                .map(reference -> EnchantmentHelper.getLevel(reference, stack))
                .orElse(0);
    }

    @Environment(EnvType.CLIENT)
    private record Blast(Vec3d center, BlockPos ignored) {}

    @Environment(EnvType.CLIENT)
    public static final class Victim {

        private final LivingEntity entity;

        private final Vec3d position;

        private final Box box;

        private final DamageSource source;

        private final float armor;

        private final float toughness;

        private final float protection;

        private final float resistance;

        private final BlocksAttacksComponent shield;

        private final Map<Vec3d, Float> exposures = new HashMap<>();

        private final Map<CrystalDamage.Blast, Float> blastExposures = new HashMap<>();

        public Victim(LivingEntity entity2, Vec3d vec) {
            this.entity = entity2;
            this.position = entity2.getEntityPos().add(vec);
            this.box = entity2.getBoundingBox().offset(vec);
            this.source = entity2.getDamageSources().explosion(null, null);
            this.armor = entity2.getArmor();
            this.toughness = (float) entity2.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS);
            this.protection = CrystalDamage.protection(entity2);
            StatusEffectInstance statusEffect = entity2.getStatusEffect(StatusEffects.RESISTANCE);
            this.resistance =
                    statusEffect == null
                            ? 1.0F
                            : Math.max(25 - (statusEffect.getAmplifier() + 1) * 5, 0) / 25.0F;
            ItemStack blockingItem = entity2.getBlockingItem();
            this.shield =
                    blockingItem == null
                            ? null
                            : blockingItem.get(DataComponentTypes.BLOCKS_ATTACKS);
        }

        public LivingEntity entity() {
            return this.entity;
        }

        public float damage(Vec3d vec) {
            return this.damage(vec, false);
        }

        public float damage(Vec3d vec, boolean bl) {
            return this.reduce(this.raw(vec, bl));
        }

        public float raw(Vec3d vec, boolean bl) {
            double d = Math.sqrt(this.position.squaredDistanceTo(vec)) / 12.0;
            if (d > 1.0) {
                return 0.0F;
            } else {
                float scaled =
                        this.scaled(CrystalDamage.falloff((1.0 - d) * this.exposure(vec, bl)));
                return scaled - this.blocked(vec, scaled);
            }
        }

        public float reduce(float f) {
            float damageLeft =
                    DamageUtil.getDamageLeft(
                            this.entity, f, this.source, this.armor, this.toughness);
            return DamageUtil.getInflictedDamage(damageLeft * this.resistance, this.protection);
        }

        public float damage(Vec3d vec, float f, BlockPos pos) {
            return this.reduce(this.raw(vec, f, pos));
        }

        public float raw(Vec3d vec, float f, BlockPos pos) {
            double d = f * 2.0;
            double e = Math.sqrt(this.position.squaredDistanceTo(vec)) / d;
            if (e > 1.0) {
                return 0.0F;
            } else {
                float scaled =
                        this.scaled(
                                CrystalDamage.falloff((1.0 - e) * this.blastExposure(vec, pos), d));
                return scaled - this.blocked(vec, scaled);
            }
        }

        private float blocked(Vec3d vec, float f) {
            if (this.shield != null && !(f <= 0.0F)) {
                Vec3d rotationVector =
                        this.entity.getRotationVector(0.0F, this.entity.getHeadYaw());
                Vec3d vec2 = vec.subtract(this.position);
                Vec3d vec3 = new Vec3d(vec2.x, 0.0, vec2.z).normalize();
                double d = Math.acos(MathHelper.clamp(vec3.dotProduct(rotationVector), -1.0, 1.0));
                return this.shield.getDamageReductionAmount(this.source, f, d);
            } else {
                return 0.0F;
            }
        }

        public float upperBound(Vec3d vec) {
            double d = Math.sqrt(this.position.squaredDistanceTo(vec)) / 12.0;
            return d > 1.0 ? 0.0F : this.reduce(this.scaled(CrystalDamage.falloff(1.0 - d)));
        }

        public float upperBound(Vec3d vec, float f) {
            double d = f * 2.0;
            double e = Math.sqrt(this.position.squaredDistanceTo(vec)) / d;
            return e > 1.0 ? 0.0F : this.reduce(this.scaled(CrystalDamage.falloff(1.0 - e, d)));
        }

        private float exposure(Vec3d vec, boolean bl) {
            Float exposure = this.exposures.get(vec);
            if (exposure != null) {
                return exposure;
            } else {
                Box box2 = bl ? new Box(BlockPos.ofFloored(vec.x, vec.y - 0.5, vec.z)) : null;
                float f =
                        !this.openAir(vec, null) || box2 != null && this.hull(vec).intersects(box2)
                                ? this.seenPercent(vec, box2, null)
                                : 1.0F;
                this.exposures.put(vec, f);
                return f;
            }
        }

        private float blastExposure(Vec3d vec, BlockPos pos) {
            CrystalDamage.Blast blast = new CrystalDamage.Blast(vec, pos);
            Float blastExposure = this.blastExposures.get(blast);
            if (blastExposure != null) {
                return blastExposure;
            } else {
                float f = this.openAir(vec, pos) ? 1.0F : this.seenPercent(vec, null, pos);
                this.blastExposures.put(blast, f);
                return f;
            }
        }

        private Box hull(Vec3d vec) {
            return this.box.union(new Box(vec, vec));
        }

        private boolean openAir(Vec3d vec, BlockPos pos) {
            World entityWorld = this.entity.getEntityWorld();
            ShapeContext shapeContext = ShapeContext.of(this.entity);
            int i = MathHelper.floor(Math.min(this.box.minX, vec.x));
            int j = MathHelper.floor(Math.min(this.box.minY, vec.y));
            int k = MathHelper.floor(Math.min(this.box.minZ, vec.z));
            int m = MathHelper.floor(Math.max(this.box.maxX, vec.x));
            int n = MathHelper.floor(Math.max(this.box.maxY, vec.y));
            int o = MathHelper.floor(Math.max(this.box.maxZ, vec.z));
            Mutable mutable = new Mutable();
            for (int p = i; p <= m; p++) {
                for (int q = j; q <= n; q++) {
                    for (int r = k; r <= o; r++) {
                        mutable.set(p, q, r);
                        if (!mutable.equals(pos)
                                && !entityWorld
                                        .getBlockState(mutable)
                                        .getCollisionShape(entityWorld, mutable, shapeContext)
                                        .isEmpty()) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        private float seenPercent(Vec3d vec, Box box2, BlockPos pos) {
            double d = 1.0 / ((this.box.maxX - this.box.minX) * 2.0 + 1.0);
            double e = 1.0 / ((this.box.maxY - this.box.minY) * 2.0 + 1.0);
            double d2 = 1.0 / ((this.box.maxZ - this.box.minZ) * 2.0 + 1.0);
            double e2 = (1.0 - Math.floor(1.0 / d) * d) / 2.0;
            double d3 = (1.0 - Math.floor(1.0 / d2) * d2) / 2.0;
            if (!(d < 0.0) && !(e < 0.0) && !(d2 < 0.0)) {
                World entityWorld = this.entity.getEntityWorld();
                int i = 0;
                int j = 0;
                for (double e3 = 0.0; e3 <= 1.0; e3 += d) {
                    for (double d4 = 0.0; d4 <= 1.0; d4 += e) {
                        for (double e4 = 0.0; e4 <= 1.0; e4 += d2) {
                            Vec3d vec2 =
                                    new Vec3d(
                                            MathHelper.lerp(e3, this.box.minX, this.box.maxX) + e2,
                                            MathHelper.lerp(d4, this.box.minY, this.box.maxY),
                                            MathHelper.lerp(e4, this.box.minZ, this.box.maxZ) + d3);
                            BlockHitResult blockHitResult =
                                    entityWorld.raycast(
                                            new RaycastContext(
                                                    vec2,
                                                    vec,
                                                    ShapeType.COLLIDER,
                                                    FluidHandling.NONE,
                                                    this.entity));
                            if ((blockHitResult.getType() == Type.MISS
                                            || blockHitResult.getBlockPos().equals(pos))
                                    && (box2 == null || !throughBlock(box2, vec2, vec))) {
                                i++;
                            }
                            j++;
                        }
                    }
                }
                return (float) i / j;
            } else {
                return 0.0F;
            }
        }

        private static boolean throughBlock(Box box2, Vec3d vec, Vec3d vec2) {
            Vec3d vec3 =
                    new Vec3d(
                            MathHelper.lerp(1.0E-7, vec2.x, vec.x),
                            MathHelper.lerp(1.0E-7, vec2.y, vec.y),
                            MathHelper.lerp(1.0E-7, vec2.z, vec.z));
            return box2.contains(vec) || box2.raycast(vec, vec3).isPresent();
        }

        private float scaled(float f) {
            if (!(this.entity instanceof PlayerEntity)) {
                return f;
            } else {
                return switch (this.entity.getEntityWorld().getDifficulty()) {
                    case PEACEFUL -> 0.0F;
                    case EASY -> Math.min(f / 2.0F + 1.0F, f);
                    case NORMAL -> f;
                    case HARD -> f * 1.5F;
                };
            }
        }
    }
}
