package net.efn.client.module.impl.combat.crystal;

import com.example.client.mixin.AccessorLocalPlayer;

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import net.efn.client.input.EfnKeyPolling;
import net.efn.client.input.ModuleKeybinds;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.InputUtil.Key;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.effect.EnchantmentEffectEntry;
import net.minecraft.enchantment.effect.value.AddEnchantmentEffect;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResult.Success;
import net.minecraft.util.ActionResult.SwingSource;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Environment(EnvType.CLIENT)
public final class Crystals {

    public static final int READY = -2;

    public static final int NO_ROUTE = -1;

    private static final int MIN_CONFIRM_TICKS = 4;

    private static final int MAX_CONFIRM_TICKS = 20;

    private static final float RTT_SMOOTHING = 0.25F;

    private static final double RESERVE_RANGE = 12.0;

    private static final Set<Block> USABLE_BLOCKS =
            Set.of(
                    Blocks.RESPAWN_ANCHOR,
                    Blocks.NOTE_BLOCK,
                    Blocks.LEVER,
                    Blocks.REPEATER,
                    Blocks.REDSTONE_WIRE,
                    Blocks.CAKE,
                    Blocks.COMPOSTER,
                    Blocks.DRAGON_EGG);

    private static final Long2IntOpenHashMap PLACED_AT = new Long2IntOpenHashMap();

    private static float measuredRtt;

    private Crystals() {}

    public static boolean isBase(BlockState state) {
        return state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.BEDROCK);
    }

    public static Vec3d explosionAt(BlockPos pos) {
        return Vec3d.ofBottomCenter(pos.up());
    }

    public static Box boxOn(BlockPos pos) {
        return EntityType.END_CRYSTAL.getDimensions().getBoxAt(explosionAt(pos));
    }

    public static boolean hasCrystal(World world, BlockPos pos, Entity entity) {
        BlockPos pos2 = pos.up();
        for (EndCrystalEntity endCrystalEntity :
                world.getNonSpectatingEntities(EndCrystalEntity.class, new Box(pos2))) {
            if (endCrystalEntity != entity && endCrystalEntity.getBlockPos().equals(pos2)) {
                return true;
            }
        }
        return false;
    }

    public static boolean canHost(World world, BlockPos pos, Entity entity) {
        BlockPos pos2 = pos.up();
        if (!world.isAir(pos2)) {
            return false;
        } else {
            Box box =
                    new Box(
                            pos2.getX(),
                            pos2.getY(),
                            pos2.getZ(),
                            pos2.getX() + 1.0,
                            pos2.getY() + 2.0,
                            pos2.getZ() + 1.0);
            for (Entity entity2 : world.getOtherEntities(entity, box)) {
                if (entity2.isAlive()) {
                    return false;
                }
            }
            return true;
        }
    }

    public static BlockPos obsidianTarget(
            ClientWorld world,
            ClientPlayerEntity player,
            BlockHitResult blockHitResult,
            BlockState state) {
        BlockPos pos = placedObsidian(world, player, blockHitResult, state);
        return pos != null && canHost(world, pos, null) ? pos : null;
    }

    public static BlockPos placedObsidian(
            ClientWorld world,
            ClientPlayerEntity player,
            BlockHitResult blockHitResult,
            BlockState state) {
        if (!player.shouldCancelInteraction()
                && reactsToUse(world, blockHitResult.getBlockPos(), state)) {
            return null;
        } else {
            ItemPlacementContext itemPlacementContext =
                    new ItemPlacementContext(
                            player, Hand.MAIN_HAND, new ItemStack(Items.OBSIDIAN), blockHitResult);
            if (!itemPlacementContext.canPlace()) {
                return null;
            } else {
                BlockPos pos = itemPlacementContext.getBlockPos();
                if (world.isOutOfHeightLimit(pos) || !world.getWorldBorder().contains(pos)) {
                    return null;
                } else {
                    return !world.canPlace(
                                    Blocks.OBSIDIAN.getDefaultState(),
                                    pos,
                                    ShapeContext.ofPlacement(player))
                            ? null
                            : pos;
                }
            }
        }
    }

    private static boolean reactsToUse(World world, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof BlockEntityProvider || USABLE_BLOCKS.contains(block)) {
            return true;
        } else {
            return !state.isIn(BlockTags.DOORS)
                            && !state.isIn(BlockTags.TRAPDOORS)
                            && !state.isIn(BlockTags.FENCE_GATES)
                            && !state.isIn(BlockTags.BUTTONS)
                            && !state.isIn(BlockTags.CAULDRONS)
                            && !state.isIn(BlockTags.CANDLE_CAKES)
                            && !state.isIn(BlockTags.FLOWER_POTS)
                    ? state.createScreenHandlerFactory(world, pos) != null
                    : true;
        }
    }

    public static int route(ClientPlayerEntity player, Item item, BlockState state, int i) {
        ItemStack mainHandStack = player.getMainHandStack();
        if (mainHandStack.isOf(item)) {
            return -2;
        } else {
            boolean of = player.getOffHandStack().isOf(item);
            if (of && letsClickThrough(mainHandStack, state)) {
                return -2;
            } else {
                PlayerInventory inventory = player.getInventory();
                int hotbarSlot = hotbarSlotOf(inventory, item);
                if (hotbarSlot != -1) {
                    return hotbarSlot;
                } else if (!of) {
                    return -1;
                } else if (PlayerInventory.isValidHotbarIndex(i)
                        && letsClickThrough(inventory.getStack(i), state)) {
                    return i;
                } else {
                    for (int k = 0; k < PlayerInventory.getHotbarSize(); k++) {
                        if (letsClickThrough(inventory.getStack(k), state)) {
                            return k;
                        }
                    }
                    return -1;
                }
            }
        }
    }

    public static boolean letsClickThrough(ItemStack stack, BlockState state) {
        if (stack.isEmpty()) {
            return true;
        } else if (!stack.contains(DataComponentTypes.CONSUMABLE)
                && !stack.contains(DataComponentTypes.BLOCKS_ATTACKS)
                && !stack.contains(DataComponentTypes.KINETIC_WEAPON)) {
            EquippableComponent equippableComponent = stack.get(DataComponentTypes.EQUIPPABLE);
            return equippableComponent != null && equippableComponent.swappable()
                    ? false
                    : stack.isIn(ItemTags.SWORDS)
                            || stack.isOf(Items.MACE)
                            || stack.isOf(Items.TOTEM_OF_UNDYING)
                            || stack.isIn(ItemTags.AXES) && isBase(state);
        } else {
            return false;
        }
    }

    public static boolean isCombatItem(ItemStack stack) {
        return stack.isEmpty()
                || stack.isIn(ItemTags.SWORDS)
                || stack.isIn(ItemTags.AXES)
                || stack.isOf(Items.MACE)
                || stack.isOf(Items.TOTEM_OF_UNDYING);
    }

    public static int hotbarSlotOf(PlayerInventory playerInventory, Item item) {
        for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
            if (playerInventory.getStack(i).isOf(item)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean strike(MinecraftClient client, ClientPlayerEntity player, Entity entity) {
        if (player != null
                && client.interactionManager != null
                && entity != null
                && !entity.isRemoved()
                && !player.isSpectator()) {
            if (!MeleeAttacks.actedThisTick(player)
                    && MeleeAttacks.handsFree(client, player)
                    && player.isSprinting() == ((AccessorLocalPlayer) player).efn$wasSprinting()) {
                client.interactionManager.attackEntity(player, entity);
                player.swingHand(Hand.MAIN_HAND);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean rightClick(
            MinecraftClient client, ClientPlayerEntity player, BlockHitResult blockHitResult) {
        if (!MeleeAttacks.actedThisTick(player) && MeleeAttacks.handsFree(client, player)) {
            Hand hand =
                    letsClickThrough(
                                    player.getMainHandStack(),
                                    client.world.getBlockState(blockHitResult.getBlockPos()))
                            ? Hand.OFF_HAND
                            : Hand.MAIN_HAND;
            ItemStack stackInHand = player.getStackInHand(hand);
            if (!stackInHand.isItemEnabled(client.world.getEnabledFeatures())) {
                return false;
            } else {
                int count = stackInHand.getCount();
                ActionResult actionResult =
                        client.interactionManager.interactBlock(player, hand, blockHitResult);
                Success success;
                if (actionResult instanceof Success
                        && (success = (Success) actionResult).swingSource() == SwingSource.CLIENT) {
                    player.swingHand(hand);
                    if (!stackInHand.isEmpty()
                            && (stackInHand.getCount() != count || player.isInCreativeMode())) {
                        client.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                    }
                }
                return true;
            }
        } else {
            return false;
        }
    }

    public static int weaponRoute(ClientPlayerEntity player) {
        ItemStack mainHandStack = player.getMainHandStack();
        if (!swings(player, mainHandStack)
                || !enchantsAddDamage(mainHandStack) && !dealsDamage(player, mainHandStack)) {
            PlayerInventory inventory = player.getInventory();
            int i = -1;
            for (int j = 0; j < PlayerInventory.getHotbarSize(); j++) {
                ItemStack stack = inventory.getStack(j);
                if (swings(player, stack)) {
                    if (enchantsAddDamage(stack)) {
                        return j;
                    }
                    if (i == -1 && dealsDamage(player, stack)) {
                        i = j;
                    }
                }
            }
            return i;
        } else {
            return -2;
        }
    }

    private static boolean swings(ClientPlayerEntity player, ItemStack stack) {
        return !stack.contains(DataComponentTypes.PIERCING_WEAPON)
                && !player.isBelowMinimumAttackCharge(stack, 0);
    }

    public static boolean enchantsAddDamage(ItemStack stack) {
        float f = 0.0F;
        for (Entry enchantmentEntry : stack.getEnchantments().getEnchantmentEntries()) {
            Enchantment key = (Enchantment) ((RegistryEntry) enchantmentEntry.getKey()).value();
            for (EnchantmentEffectEntry enchantmentEffectEntry :
                    key.getEffect(EnchantmentEffectComponentTypes.DAMAGE)) {
                Object object;
                if (enchantmentEffectEntry.requirements().isPresent()
                        || !((object = enchantmentEffectEntry.effect())
                                instanceof AddEnchantmentEffect)) {
                    return false;
                }
                AddEnchantmentEffect addEnchantmentEffect = (AddEnchantmentEffect) object;
                f += addEnchantmentEffect.value().getValue(enchantmentEntry.getIntValue());
            }
        }
        return f > 0.0F;
    }

    public static boolean dealsDamage(LivingEntity entity, ItemStack stack) {
        EntityAttributeInstance entityAttributeInstance2 =
                new EntityAttributeInstance(
                        EntityAttributes.ATTACK_DAMAGE, entityAttributeInstance -> {});
        entityAttributeInstance2.setBaseValue(
                entity.getAttributeBaseValue(EntityAttributes.ATTACK_DAMAGE));
        for (StatusEffectInstance statusEffect : entity.getStatusEffects()) {
            statusEffect
                    .getEffectType()
                    .value()
                    .forEachAttributeModifier(
                            statusEffect.getAmplifier(),
                            (registryEntry, entityAttributeModifier) ->
                                    include(
                                            entityAttributeInstance2,
                                            registryEntry,
                                            entityAttributeModifier));
        }
        for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
            ItemStack stack2 =
                    equipmentSlot == EquipmentSlot.MAINHAND
                            ? stack
                            : entity.getEquippedStack(equipmentSlot);
            if (!stack2.isEmpty() && !stack2.shouldBreak()) {
                stack2.applyAttributeModifiers(
                        equipmentSlot,
                        (registryEntry, entityAttributeModifier) ->
                                include(
                                        entityAttributeInstance2,
                                        registryEntry,
                                        entityAttributeModifier));
            }
        }
        return (float) entityAttributeInstance2.getValue() > 0.0F;
    }

    private static void include(
            EntityAttributeInstance entityAttributeInstance,
            RegistryEntry<EntityAttribute> entry,
            EntityAttributeModifier entityAttributeModifier) {
        if (EntityAttributes.ATTACK_DAMAGE.equals(entry)) {
            entityAttributeInstance.removeModifier(entityAttributeModifier.id());
            entityAttributeInstance.addTemporaryModifier(entityAttributeModifier);
        }
    }

    public static int confirmWindow(MinecraftClient client, ClientPlayerEntity player) {
        return MathHelper.clamp(rttTicks(client, player) + 2, 4, 20);
    }

    public static int rttTicks(MinecraftClient client, ClientPlayerEntity player) {
        return measuredRtt > 0.0F ? Math.round(measuredRtt) : latencyMs(client, player) / 50 + 1;
    }

    public static void notePlace(ClientPlayerEntity player, BlockPos pos) {
        ObjectIterator objectIterator = PLACED_AT.long2IntEntrySet().iterator();
        while (objectIterator.hasNext()) {
            int i =
                    player.age
                            - ((it.unimi.dsi.fastutil.longs.Long2IntMap.Entry)
                                            objectIterator.next())
                                    .getIntValue();
            if (i < 0 || i > 20) {
                objectIterator.remove();
            }
        }
        PLACED_AT.put(pos.asLong(), player.age);
    }

    public static void noteSpawn(ClientPlayerEntity player, BlockPos pos) {
        long l = pos.asLong();
        if (PLACED_AT.containsKey(l)) {
            int i = player.age - PLACED_AT.remove(l);
            if (i > 0 && i <= 20) {
                measuredRtt = measuredRtt > 0.0F ? measuredRtt + (i - measuredRtt) * 0.25F : i;
            }
        }
    }

    public static boolean placing(ClientPlayerEntity player, BlockPos pos, int i) {
        long l = pos.asLong();
        return PLACED_AT.containsKey(l) && sent(player, PLACED_AT.get(l), i);
    }

    public static boolean anyPlacing(ClientPlayerEntity player, int i) {
        IntIterator intIterator = PLACED_AT.values().iterator();
        while (intIterator.hasNext()) {
            int j = intIterator.next();
            if (sent(player, j, i)) {
                return true;
            }
        }
        return false;
    }

    public static List<Box> reservedBoxes(ClientPlayerEntity player, int i) {
        ArrayList list = new ArrayList();
        for (EndCrystalEntity endCrystalEntity :
                player.getEntityWorld()
                        .getNonSpectatingEntities(
                                EndCrystalEntity.class, player.getBoundingBox().expand(12.0))) {
            if (endCrystalEntity.isAlive()) {
                list.add(endCrystalEntity.getBoundingBox());
            }
        }
        for (it.unimi.dsi.fastutil.longs.Long2IntMap.Entry long2IntEntry :
                PLACED_AT.long2IntEntrySet()) {
            if (sent(player, long2IntEntry.getIntValue(), i)) {
                list.add(boxOn(BlockPos.fromLong(long2IntEntry.getLongKey())));
            }
        }
        return list;
    }

    private static boolean sent(ClientPlayerEntity player, int i, int j) {
        int k = player.age - i;
        return k >= 0 && k < j;
    }

    public static int humanGap() {
        double d = ThreadLocalRandom.current().nextDouble();
        return d < 0.7 ? 2 : (d < 0.95 ? 3 : 4);
    }

    public static void forgetRtt() {
        PLACED_AT.clear();
        measuredRtt = 0.0F;
    }

    private static int latencyMs(MinecraftClient client, ClientPlayerEntity player) {
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (networkHandler == null) {
            return 0;
        } else {
            PlayerListEntry playerListEntry = networkHandler.getPlayerListEntry(player.getUuid());
            return playerListEntry == null ? 0 : Math.max(playerListEntry.getLatency(), 0);
        }
    }

    public static boolean isKeyDown(MinecraftClient client, int i) {
        if (!ModuleKeybinds.isBindable(i)) {
            return false;
        } else {
            return i >= 1000
                    ? GLFW.glfwGetMouseButton(client.getWindow().getHandle(), i - 1000) == 1
                    : EfnKeyPolling.isKeyDown(client.getWindow(), i);
        }
    }

    public static boolean isPhysicallyDown(MinecraftClient client, KeyBinding keyBinding) {
        Key key;
        try {
            key = InputUtil.fromTranslationKey(keyBinding.getBoundKeyTranslationKey());
        } catch (IllegalArgumentException exception) {
            return false;
        }
        int code = key.getCode();
        if (code < 0) {
            return false;
        } else {
            return switch (key.getCategory()) {
                case MOUSE -> GLFW.glfwGetMouseButton(client.getWindow().getHandle(), code) == 1;
                case KEYSYM -> EfnKeyPolling.isKeyDown(client.getWindow(), code);
                default -> false;
            };
        }
    }
}
