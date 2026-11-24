package net.efn.client.module.impl.movement;

import com.example.client.mixin.AccessorElytraFlyMultiPlayerGameMode;
import com.example.client.mixin.AccessorLocalPlayer;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.EventPriority;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.KeyInputEvent;
import net.efn.client.event.impl.MouseInputEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.input.ModuleKeybinds;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.AutoInvTotemModule;
import net.efn.client.module.impl.movement.elytrafly.ElytraFlyInventory;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.KeySetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.InputControl;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public final class ElytraSwapModule extends ClientModule {

    private static final int[] SOURCE_INDICES = sourceIndices();

    private static final int MAX_WAIT_TICKS = 20;

    private static final int CHEST_SLOT_ID = 6;

    private static final Text SWAP_FAILED =
            ModuleChat.body()
                    .danger("Swap failed")
                    .text(", the item didn't move into the chest slot")
                    .build();

    private final KeySetting key = this.addSetting("key", new KeySetting("Key", -1));

    private final BooleanSetting swapBackOnLanding =
            this.addSetting(
                    "swap-back-on-landing", new BooleanSetting("Swap Back on Landing", true));

    private final NumberSetting landingDelay =
            this.addSetting(
                    "landing-delay",
                    new NumberSetting("Landing Delay", 4.0F, 0.0F, 20.0F).step(1.0F));

    private boolean pending;

    private int pendingTicks;

    private boolean armed;

    private boolean glided;

    private int groundTicks;

    private int settleTicks;

    private int storedIndex = -1;

    private Item storedItem;

    public ElytraSwapModule() {
        super(
                "elytra-swap",
                "Elytra Swap",
                "Swaps between your chestplate and elytra with one key.",
                ModuleCategory.MOVEMENT);
        this.usesNotifications();
        this.landingDelay.visibleWhen(this.swapBackOnLanding::get);
    }

    @Override
    protected void onEnable() {
        this.reset();
        this.on(KeyInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onKey);
        this.on(MouseInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onMouse);
        this.on(TickEvent.TYPE, Stage.PRE, EventPriority.LOWEST, this::onTick);
    }

    @Override
    protected void onDisable() {
        this.reset();
    }

    private void reset() {
        this.pending = false;
        this.pendingTicks = 0;
        this.disarm();
    }

    private void disarm() {
        this.armed = false;
        this.glided = false;
        this.groundTicks = 0;
        this.settleTicks = 0;
        this.storedIndex = -1;
        this.storedItem = null;
    }

    private void onKey(KeyInputEvent event) {
        if (event.action == 1) {
            this.onPress(event.key);
        }
    }

    private void onMouse(MouseInputEvent event) {
        if (event.action == 1) {
            this.onPress(1000 + event.button);
        }
    }

    private void onPress(int i) {
        int key2 = this.key.value();
        if (ModuleKeybinds.isBindable(key2) && i == key2) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.currentScreen == null && client.player != null) {
                this.pending = true;
                this.pendingTicks = 0;
            }
        }
    }

    private void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || client.interactionManager == null) {
            this.reset();
        } else if (!this.pending) {
            if (this.armed) {
                this.followFlight(client, player);
            }
        } else {
            String busyReason = this.busyReason(player);
            if (busyReason == null) {
                if (client.currentScreen != null || this.swap(client, player)) {
                    this.pending = false;
                    return;
                }
                busyReason = "you kept moving";
            }
            if (++this.pendingTicks > 20) {
                this.pending = false;
                ModuleChat.send(
                        this, ModuleChat.body().warn("Swap skipped").text(", " + busyReason));
            }
        }
    }

    private String busyReason(ClientPlayerEntity player) {
        if (CombatMacroLock.heldByOther(this)) {
            return "another macro kept the inventory busy";
        } else if (AutoInvTotemModule.busy()) {
            return "the totem refill kept the inventory busy";
        } else if (player.isUsingItem()) {
            return "you kept using an item";
        } else {
            return ((AccessorLocalPlayer) player).efn$lastSentInput().sneak()
                    ? "you kept sneaking"
                    : null;
        }
    }

    private void followFlight(MinecraftClient client, ClientPlayerEntity player) {
        if (!this.swapBackOnLanding.get()
                || !player.getEquippedStack(EquipmentSlot.CHEST)
                        .contains(DataComponentTypes.GLIDER)) {
            this.disarm();
        } else if (player.isGliding()) {
            this.glided = true;
            this.groundTicks = 0;
        } else if (this.glided) {
            if (!player.isOnGround() && !player.isTouchingWater()) {
                this.groundTicks = 0;
            } else if (this.groundTicks++ >= this.landingDelay.getInt()) {
                if (client.currentScreen == null
                        && inventoryReady(player)
                        && this.busyReason(player) == null) {
                    if (!isBound(player, player.getEquippedStack(EquipmentSlot.CHEST))) {
                        int chestplateToEquip = this.chestplateToEquip(player);
                        if (chestplateToEquip == -1) {
                            ModuleChat.send(
                                    this,
                                    ModuleChat.body()
                                            .danger("Can't swap back")
                                            .text(", no ")
                                            .name("chestplate")
                                            .text(" left in your inventory"));
                        } else if (!AutoInvTotemModule.clickSafe(player)) {
                            if (++this.settleTicks <= 20) {
                                InputControl.idle();
                                return;
                            }
                            ModuleChat.send(
                                    this,
                                    ModuleChat.body()
                                            .warn("Swap back skipped")
                                            .text(", you kept moving"));
                        } else if (!equip(client, player, chestplateToEquip)) {
                            ModuleChat.send(this, SWAP_FAILED);
                        }
                    }
                    this.disarm();
                }
            }
        }
    }

    private boolean swap(MinecraftClient client, ClientPlayerEntity player) {
        if (player.isSpectator()) {
            return true;
        } else if (!inventoryReady(player)) {
            ModuleChat.send(
                    this,
                    ModuleChat.body()
                            .warn("Swap skipped")
                            .text(", close the open container first"));
            return true;
        } else {
            ItemStack equippedStack = player.getEquippedStack(EquipmentSlot.CHEST);
            if (isBound(player, equippedStack)) {
                ModuleChat.send(
                        this,
                        ModuleChat.body()
                                .danger("Can't swap")
                                .text(", your chest piece has ")
                                .name("Curse of Binding"));
                return true;
            } else if (equippedStack.contains(DataComponentTypes.GLIDER)) {
                int chestplateToEquip = this.chestplateToEquip(player);
                if (chestplateToEquip == -1) {
                    ModuleChat.send(
                            this,
                            ModuleChat.body()
                                    .danger("Can't swap")
                                    .text(", no ")
                                    .name("chestplate")
                                    .text(" in your inventory"));
                } else {
                    if (!AutoInvTotemModule.clickSafe(player)) {
                        InputControl.idle();
                        return false;
                    }
                    if (!equip(client, player, chestplateToEquip)) {
                        ModuleChat.send(this, SWAP_FAILED);
                    }
                }
                this.disarm();
                return true;
            } else {
                int elytra = findElytra(player);
                if (elytra == -1) {
                    ModuleChat.send(
                            this,
                            ModuleChat.body()
                                    .danger("Can't swap")
                                    .text(", no usable ")
                                    .name("elytra")
                                    .text(" in your inventory"));
                    return true;
                } else if (!AutoInvTotemModule.clickSafe(player)) {
                    InputControl.idle();
                    return false;
                } else {
                    Item item = isChestplate(equippedStack) ? equippedStack.getItem() : null;
                    if (!equip(client, player, elytra)) {
                        ModuleChat.send(this, SWAP_FAILED);
                        return true;
                    } else {
                        this.disarm();
                        if (item != null) {
                            this.armed = this.swapBackOnLanding.get();
                            this.storedIndex = elytra;
                            this.storedItem = item;
                        }
                        return true;
                    }
                }
            }
        }
    }

    private int chestplateToEquip(ClientPlayerEntity player) {
        ItemStack stack;
        return this.storedIndex >= 0
                        && this.storedItem != null
                        && (stack = player.getInventory().getStack(this.storedIndex))
                                .isOf(this.storedItem)
                        && !isBound(player, stack)
                ? this.storedIndex
                : findBestChestplate(player);
    }

    private static boolean equip(MinecraftClient client, ClientPlayerEntity player, int i) {
        Item item = player.getInventory().getStack(i).getItem();
        ((AccessorElytraFlyMultiPlayerGameMode) client.interactionManager)
                .efn$elytraFlySyncSelected();
        if (!PlayerInventory.isValidHotbarIndex(i) && i != 40) {
            ElytraFlyInventory.moveToChest(i);
        } else {
            client.interactionManager.clickSlot(
                    player.playerScreenHandler.syncId, 6, i, SlotActionType.SWAP, player);
        }
        player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(0));
        return player.getEquippedStack(EquipmentSlot.CHEST).isOf(item);
    }

    private static boolean inventoryReady(ClientPlayerEntity player) {
        return player.currentScreenHandler == player.playerScreenHandler
                && player.currentScreenHandler.getCursorStack().isEmpty();
    }

    private static boolean isBound(ClientPlayerEntity player, ItemStack stack) {
        return !player.isCreative()
                && EnchantmentHelper.hasAnyEnchantmentsWith(
                        stack, EnchantmentEffectComponentTypes.PREVENT_ARMOR_CHANGE);
    }

    private static int findElytra(ClientPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        int i = -1;
        int j = -1;
        for (int k : SOURCE_INDICES) {
            ItemStack stack = inventory.getStack(k);
            int durabilityLeft;
            if (LivingEntity.canGlideWith(stack, EquipmentSlot.CHEST)
                    && !isBound(player, stack)
                    && (durabilityLeft = durabilityLeft(stack)) > j) {
                i = k;
                j = durabilityLeft;
            }
        }
        return i;
    }

    private static int findBestChestplate(ClientPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        int i = -1;
        double d = -1.0;
        int j = -1;
        for (int k : SOURCE_INDICES) {
            ItemStack stack = inventory.getStack(k);
            if (isChestplate(stack) && !isBound(player, stack)) {
                double armorScore = armorScore(stack);
                int durabilityLeft = durabilityLeft(stack);
                if (armorScore > d || armorScore == d && durabilityLeft > j) {
                    i = k;
                    d = armorScore;
                    j = durabilityLeft;
                }
            }
        }
        return i;
    }

    private static boolean isChestplate(ItemStack stack) {
        if (!stack.isEmpty() && !stack.contains(DataComponentTypes.GLIDER)) {
            EquippableComponent equippableComponent = stack.get(DataComponentTypes.EQUIPPABLE);
            return equippableComponent != null && equippableComponent.slot() == EquipmentSlot.CHEST;
        } else {
            return false;
        }
    }

    private static double armorScore(ItemStack stack) {
        AttributeModifiersComponent attributeModifiersComponent =
                stack.getOrDefault(
                        DataComponentTypes.ATTRIBUTE_MODIFIERS,
                        AttributeModifiersComponent.DEFAULT);
        return attributeModifiersComponent.applyOperations(
                        EntityAttributes.ARMOR, 0.0, EquipmentSlot.CHEST)
                + attributeModifiersComponent.applyOperations(
                        EntityAttributes.ARMOR_TOUGHNESS, 0.0, EquipmentSlot.CHEST);
    }

    private static int durabilityLeft(ItemStack stack) {
        return stack.isDamageable() ? stack.getMaxDamage() - stack.getDamage() : 2147483647;
    }

    private static int[] sourceIndices() {
        int[] ints = new int[37];
        int i = 0;
        while (i < 36) {
            ints[i] = i++;
        }
        ints[36] = 40;
        return ints;
    }
}
