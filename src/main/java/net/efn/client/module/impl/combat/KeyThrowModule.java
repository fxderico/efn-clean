package net.efn.client.module.impl.combat;

import net.efn.client.event.EventPriority;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.KeyInputEvent;
import net.efn.client.event.impl.MouseInputEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.input.ModuleKeybinds;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.impl.combat.melee.SlotReturn;
import net.efn.client.module.setting.KeySetting;
import net.efn.client.util.pvp.InputControl;
import net.efn.client.util.pvp.Rotation;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResult.Success;
import net.minecraft.util.ActionResult.SwingSource;
import net.minecraft.util.Hand;

@Environment(EnvType.CLIENT)
public final class KeyThrowModule extends ClientModule {

    private static final int BUFFER_TICKS = 10;

    private final KeySetting pearlKey =
            this.addSetting("pearl-key", new KeySetting("Pearl Key", -1));

    private final KeySetting windChargeKey =
            this.addSetting("wind-charge-key", new KeySetting("Wind Charge Key", -1));

    private final SlotReturn slots = new SlotReturn(this);

    private Item pending;

    private int pendingTicks;

    public KeyThrowModule() {
        super(
                "key-throw",
                "Key Throw",
                "Throws a pearl or a wind charge with a key and swaps back.",
                ModuleCategory.COMBAT);
    }

    @Override
    protected void onEnable() {
        this.pending = null;
        this.on(KeyInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onKey);
        this.on(MouseInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onMouse);
        this.on(TickEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onTick);
    }

    @Override
    protected void onDisable() {
        this.pending = null;
        this.slots.drop(MinecraftClient.getInstance(), true);
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
        if (ModuleKeybinds.isBindable(i)) {
            Item item =
                    i == this.pearlKey.value()
                            ? Items.ENDER_PEARL
                            : (i == this.windChargeKey.value() ? Items.WIND_CHARGE : null);
            MinecraftClient client = MinecraftClient.getInstance();
            if (item != null && client.currentScreen == null && client.player != null) {
                this.pending = item;
                this.pendingTicks = 0;
            }
        }
    }

    private void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        this.slots.tick(client, true);
        if (client.player == null) {
            this.pending = null;
        } else if (this.pending != null) {
            this.tryThrow(client, client.player);
        }
    }

    private void tryThrow(MinecraftClient client, ClientPlayerEntity player) {
        if (++this.pendingTicks > 10) {
            this.pending = null;
        } else if (MeleeAttacks.canAct(client)
                && MeleeAttacks.handsFree(client, player)
                && !MeleeAttacks.actedThisTick(player)) {
            if (!CombatMacroLock.heldByOther(this) && !AutoInvTotemModule.wantsHotbar()) {
                PlayerInventory inventory = player.getInventory();
                boolean of = player.getOffHandStack().isOf(this.pending);
                int i =
                        player.getMainHandStack().isOf(this.pending)
                                ? inventory.getSelectedSlot()
                                : Crystals.hotbarSlotOf(inventory, this.pending);
                if (!of && i < 0) {
                    this.pending = null;
                } else {
                    ItemStack stack = of ? player.getOffHandStack() : inventory.getStack(i);
                    if (!player.getItemCooldownManager().isCoolingDown(stack)) {
                        boolean bl = this.pending == Items.WIND_CHARGE;
                        if (!ServerRotation.pending()
                                && (!bl || ServerRotation.canRotate(player))) {
                            if (of || this.slots.take(player, i)) {
                                this.pending = null;
                                if (bl) {
                                    ServerRotation.request(new Rotation(player.getYaw(), 90.0F));
                                    if (player.isOnGround()) {
                                        InputControl.jump();
                                    }
                                }
                                Hand hand;
                                ActionResult actionResult;
                                if ((actionResult =
                                                client.interactionManager.interactItem(
                                                        player,
                                                        hand = of ? Hand.OFF_HAND : Hand.MAIN_HAND))
                                        instanceof Success) {
                                    Success success = (Success) actionResult;
                                    if (success.swingSource() == SwingSource.CLIENT) {
                                        player.swingHand(hand);
                                    }
                                    client.gameRenderer.firstPersonRenderer.resetEquipProgress(
                                            hand);
                                }
                                MeleeAttacks.claimTick(client);
                            }
                        }
                    }
                }
            }
        }
    }
}
