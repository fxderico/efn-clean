package net.efn.client.module.impl.combat.melee;

import com.example.client.mixin.AccessorCombatMinecraft;
import com.example.client.mixin.AccessorLocalPlayer;
import com.example.client.mixin.AccessorTriggerbotKeyMapping;

import net.efn.client.config.FriendStore;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.combat.ShieldBreakerModule;
import net.efn.client.module.impl.misc.AutoEatModule;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.component.type.PiercingWeaponComponent;
import net.minecraft.component.type.WeaponComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
public final class MeleeAttacks {

    public static final float CRIT_CHARGE = 0.9F;

    private static final double LINE_EPSILON_SQ = 0.001;

    private static final int SHIELD_BROKEN_TICKS = 20;

    private static int actorId;

    private static int actionTick = -1;

    private static int shieldBrokenId;

    private static int shieldBrokenUntil;

    private MeleeAttacks() {}

    public static void noteAction(PlayerEntity player) {
        actorId = player.getId();
        actionTick = player.age;
    }

    public static boolean actedThisTick(PlayerEntity player) {
        return actionTick == player.age && actorId == player.getId();
    }

    public static boolean handsFree(MinecraftClient client, ClientPlayerEntity player) {
        return !player.isUsingItem()
                && !player.isRiding()
                && !client.interactionManager.isBreakingBlock()
                && !rowing(player)
                && !clickPending(client.options.swapHandsKey)
                && !clickPending(client.options.dropKey);
    }

    private static boolean rowing(ClientPlayerEntity player) {
        PlayerInput playerInput = ((AccessorLocalPlayer) player).efn$lastSentInput();
        return player.getVehicle() instanceof AbstractBoatEntity
                && (playerInput.forward() != playerInput.backward()
                        || playerInput.left() != playerInput.right());
    }

    public static boolean clickPending(MinecraftClient client) {
        return clickPending(client.options.attackKey);
    }

    public static boolean clickPending(KeyBinding keyBinding) {
        return ((AccessorTriggerbotKeyMapping) keyBinding).efn$triggerbotClickCount() > 0;
    }

    public static boolean rightClickComing(MinecraftClient client) {
        return client.options.useKey.isPressed() || clickPending(client.options.useKey);
    }

    public static void takeAttackClicks(MinecraftClient client) {
        KeyBinding attackKey = client.options.attackKey;
        while (attackKey.wasPressed()) {}
    }

    public static void claimTick(MinecraftClient client) {
        takeAttackClicks(client);
        KeyBinding useKey = client.options.useKey;
        while (useKey.wasPressed()) {}
        client.attackCooldown = Math.max(client.attackCooldown, 1);
        AccessorCombatMinecraft accessorCombatMinecraft = (AccessorCombatMinecraft) client;
        accessorCombatMinecraft.efn$setRightClickDelay(
                Math.max(accessorCombatMinecraft.efn$rightClickDelay(), 2));
    }

    public static boolean canAct(MinecraftClient client) {
        if (client.player == null
                || client.world == null
                || client.interactionManager == null
                || client.currentScreen != null) {
            return false;
        } else if (AutoEatModule.isEating()) {
            return false;
        } else {
            FreecamModule module = ModuleRegistry.get(FreecamModule.class);
            return module == null || !module.isEnabled();
        }
    }

    public static EntityHitResult crosshairHit(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player != null && client.world != null) {
            HitResult crosshairTarget = player.getCrosshairTarget(1.0F, player);
            EntityHitResult entityHitResult;
            if (crosshairTarget instanceof EntityHitResult
                    && (entityHitResult = (EntityHitResult) crosshairTarget).getType()
                            == Type.ENTITY) {
                return lineIsClear(player, player.getEyePos(), entityHitResult.getPos())
                        ? entityHitResult
                        : null;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static boolean lineIsClear(ClientPlayerEntity player, Vec3d vec, Vec3d vec2) {
        BlockHitResult blockHitResult =
                player.getEntityWorld()
                        .raycast(
                                new RaycastContext(
                                        vec, vec2, ShapeType.COLLIDER, FluidHandling.NONE, player));
        return blockHitResult.getType() == Type.MISS
                || blockHitResult.getPos().squaredDistanceTo(vec)
                        >= vec.squaredDistanceTo(vec2) - 0.001;
    }

    public static boolean attack(MinecraftClient client, EntityHitResult entityHitResult) {
        return attack(client, entityHitResult, 0.0);
    }

    public static boolean attack(
            MinecraftClient client, EntityHitResult entityHitResult, double d) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || client.interactionManager == null) {
            return false;
        } else if (client.attackCooldown > 0 || actedThisTick(player)) {
            return false;
        } else if (handsFree(client, player) && !player.isSpectator()) {
            if (player.isSprinting() != ((AccessorLocalPlayer) player).efn$wasSprinting()) {
                return false;
            } else {
                ItemStack mainHandStack = player.getMainHandStack();
                if (mainHandStack.contains(DataComponentTypes.PIERCING_WEAPON)) {
                    return false;
                } else if (mainHandStack.isItemEnabled(client.world.getEnabledFeatures())
                        && !player.isBelowMinimumAttackCharge(mainHandStack, 0)) {
                    Entity entity = entityHitResult.getEntity();
                    if (entity.isRemoved()) {
                        return false;
                    } else {
                        AttackRangeComponent attackRangeComponent =
                                mainHandStack.get(DataComponentTypes.ATTACK_RANGE);
                        boolean bl =
                                attackRangeComponent != null
                                        ? attackRangeComponent.isWithinRange(
                                                player, entityHitResult.getPos())
                                        : player.canInteractWithEntityIn(
                                                entity.getBoundingBox(), d);
                        if (!bl) {
                            return false;
                        } else {
                            client.interactionManager.attackEntity(player, entity);
                            if (!actedThisTick(player)) {
                                return false;
                            } else {
                                player.swingHand(Hand.MAIN_HAND);
                                return true;
                            }
                        }
                    }
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public static boolean stab(MinecraftClient client, EntityHitResult entityHitResult) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || client.interactionManager == null) {
            return false;
        } else if (client.attackCooldown > 0 || actedThisTick(player)) {
            return false;
        } else if (handsFree(client, player) && !player.isSpectator()) {
            if (player.isSprinting() != ((AccessorLocalPlayer) player).efn$wasSprinting()) {
                return false;
            } else {
                ItemStack mainHandStack = player.getMainHandStack();
                PiercingWeaponComponent piercingWeaponComponent =
                        mainHandStack.get(DataComponentTypes.PIERCING_WEAPON);
                if (piercingWeaponComponent == null) {
                    return false;
                } else if (mainHandStack.isItemEnabled(client.world.getEnabledFeatures())
                        && !player.isBelowMinimumAttackCharge(mainHandStack, 0)) {
                    Entity entity = entityHitResult.getEntity();
                    if (entity.isRemoved()) {
                        return false;
                    } else {
                        AttackRangeComponent attackRangeComponent =
                                mainHandStack.get(DataComponentTypes.ATTACK_RANGE);
                        boolean bl =
                                attackRangeComponent != null
                                        ? attackRangeComponent.isWithinRange(
                                                player, entityHitResult.getPos())
                                        : player.canInteractWithEntityIn(
                                                entity.getBoundingBox(), 0.0);
                        if (!bl) {
                            return false;
                        } else {
                            client.interactionManager.attackWithPiercingWeapon(
                                    piercingWeaponComponent);
                            if (!actedThisTick(player)) {
                                return false;
                            } else {
                                player.swingHand(Hand.MAIN_HAND);
                                return true;
                            }
                        }
                    }
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public static Entity stabTarget(ClientPlayerEntity player, Predicate<Entity> predicate) {
        AttackRangeComponent attackRange = player.getAttackRange();
        Rotation rotation = ServerRotation.sent();
        Vec3d vec =
                rotation != null
                        ? Vec3d.fromPolar(rotation.pitch(), rotation.yaw())
                        : player.getRotationVec(1.0F);
        Vec3d eyePos = player.getEyePos();
        Vec3d vec2 = eyePos.add(vec.multiply(attackRange.getEffectiveMinRange(player)));
        Vec3d vec3 =
                eyePos.add(
                        vec.multiply(
                                attackRange.getEffectiveMaxRange(player)
                                        + Math.max(0.0, player.getMovement().dotProduct(vec))));
        BlockHitResult blockHitResult =
                player.getEntityWorld()
                        .raycast(
                                new RaycastContext(
                                        eyePos,
                                        vec3,
                                        ShapeType.COLLIDER,
                                        FluidHandling.NONE,
                                        player));
        if (blockHitResult.getType() != Type.MISS) {
            vec3 = blockHitResult.getPos();
        }
        float hitboxMargin = attackRange.hitboxMargin();
        for (Entity entity :
                player.getEntityWorld()
                        .getOtherEntities(
                                player,
                                new Box(eyePos, vec3).expand(hitboxMargin + 1.0),
                                predicate)) {
            Box box = entity.getBoundingBox().expand(hitboxMargin);
            if (box.contains(vec2) || box.raycast(vec2, vec3).isPresent()) {
                return entity;
            }
        }
        return null;
    }

    private static boolean isHittable(ClientPlayerEntity player, Entity entity) {
        if (!(entity instanceof LivingEntity entity2
                && entity != player
                && !(entity instanceof ArmorStandEntity))) {
            return false;
        } else {
            PlayerEntity player2;
            return entity2.isAlive() && entity.isAttackable() && player.getVehicle() != entity
                    ? !(entity instanceof PlayerEntity)
                            || !(player2 = (PlayerEntity) entity).isSpectator()
                                    && !player2.isCreative()
                    : false;
        }
    }

    public static boolean isEnemy(ClientPlayerEntity player, Entity entity) {
        PlayerEntity player2;
        return isHittable(player, entity) && !isFriend(entity)
                ? !(entity instanceof PlayerEntity)
                        || player.shouldDamagePlayer(player2 = (PlayerEntity) entity)
                                && player.networkHandler.getPlayerListEntry(player2.getUuid())
                                        != null
                : false;
    }

    public static boolean isHostile(LivingEntity entity) {
        MobEntity mobEntity;
        return entity instanceof Monster
                && (!(entity instanceof Angerable)
                        || entity instanceof MobEntity
                                && (mobEntity = (MobEntity) entity).isAttacking());
    }

    public static float charge(PlayerEntity player) {
        return player.getAttackCooldownProgress(0.5F);
    }

    public static int ticksToFull(PlayerEntity player) {
        return Math.max(
                0,
                MathHelper.ceil(
                        (1.0F - charge(player)) * player.getAttackCooldownProgressPerTick()));
    }

    public static boolean airborne(PlayerEntity player) {
        return !player.isOnGround()
                && !player.isClimbing()
                && !player.isTouchingWater()
                && !player.hasBlindnessEffect()
                && !player.hasVehicle()
                && !player.getAbilities().flying
                && !player.isGliding();
    }

    public static boolean wouldCrit(PlayerEntity player, Entity entity) {
        return charge(player) > 0.9F && player.fallDistance > 0.0 && critStance(player, entity);
    }

    public static boolean critStance(PlayerEntity player, Entity entity) {
        return !player.isOnGround()
                && !player.isClimbing()
                && !player.isTouchingWater()
                && !player.hasBlindnessEffect()
                && !player.hasVehicle()
                && !player.isSprinting()
                && entity instanceof LivingEntity;
    }

    public static boolean shieldBlocks(LivingEntity entity, PlayerEntity player) {
        return shieldBlocks(entity, player, entity.getBlockingItem());
    }

    public static boolean shieldBlocks(LivingEntity entity, PlayerEntity player, ItemStack stack) {
        if (stack == null) {
            return false;
        } else {
            BlocksAttacksComponent blocksAttacksComponent =
                    stack.get(DataComponentTypes.BLOCKS_ATTACKS);
            if (blocksAttacksComponent == null) {
                return false;
            } else {
                DamageSource damageSource =
                        player.getEntityWorld().getDamageSources().playerAttack(player);
                if (blocksAttacksComponent
                        .bypassedBy()
                        .map(tagKey -> damageSource.isIn((TagKey<DamageType>) tagKey))
                        .orElse(false)) {
                    return false;
                } else {
                    Vec3d rotationVector = entity.getRotationVector(0.0F, entity.getHeadYaw());
                    Vec3d vec = player.getEntityPos().subtract(entity.getEntityPos());
                    Vec3d vec2 = new Vec3d(vec.x, 0.0, vec.z).normalize();
                    double d =
                            Math.acos(MathHelper.clamp(vec2.dotProduct(rotationVector), -1.0, 1.0));
                    return blocksAttacksComponent.getDamageReductionAmount(damageSource, 1.0F, d)
                            > 0.0F;
                }
            }
        }
    }

    public static boolean wastedOnShield(ClientPlayerEntity player, LivingEntity entity) {
        if (shieldBlocks(entity, player)
                && !shieldBroken(entity)
                && !disablesShields(player.getMainHandStack())) {
            ShieldBreakerModule module = ModuleRegistry.get(ShieldBreakerModule.class);
            if (module != null && module.isEnabled()) {
                PlayerInventory inventory = player.getInventory();
                for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
                    if (disablesShields(inventory.getStack(i))) {
                        return false;
                    }
                }
                return true;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public static void noteShieldBroken(LivingEntity entity) {
        shieldBrokenId = entity.getId();
        shieldBrokenUntil = entity.age + 20;
    }

    public static boolean shieldBroken(LivingEntity entity) {
        return entity.getId() == shieldBrokenId && entity.age < shieldBrokenUntil;
    }

    public static boolean disablesShields(ItemStack stack) {
        WeaponComponent weaponComponent = stack.get(DataComponentTypes.WEAPON);
        return weaponComponent != null && weaponComponent.disableBlockingForSeconds() > 0.0F;
    }

    public static boolean isMeleeWeapon(ItemStack stack) {
        return stack.isIn(ItemTags.SWORDS)
                || stack.isIn(ItemTags.AXES)
                || stack.isIn(ItemTags.SPEARS)
                || stack.isOf(Items.MACE)
                || stack.isOf(Items.TRIDENT);
    }

    public static double attackDamage(ItemStack stack) {
        double[] doubles = new double[] {0.0};
        stack.applyAttributeModifiers(
                EquipmentSlot.MAINHAND,
                (registryEntry, entityAttributeModifier) -> {
                    if (EntityAttributes.ATTACK_DAMAGE.equals(registryEntry)
                            && entityAttributeModifier.operation() == Operation.ADD_VALUE) {
                        doubles[0] += entityAttributeModifier.value();
                    }
                });
        return doubles[0];
    }

    public static boolean isFriend(Entity entity) {
        return !(entity instanceof PlayerEntity player)
                ? false
                : FriendStore.getInstance().isFriend(player.getGameProfile().name());
    }

    public static int spread(int i) {
        return i > 0 ? ThreadLocalRandom.current().nextInt(i + 1) : 0;
    }
}
