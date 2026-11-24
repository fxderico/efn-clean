package net.efn.client.module.impl.movement.elytrafly;

import net.efn.client.event.impl.PlayerMoveEvent;
import net.efn.client.module.impl.movement.ElytraFlyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public abstract class ElytraFlightMode {

    protected final MinecraftClient mc = MinecraftClient.getInstance();

    protected final ElytraFlyModule elytraFly;

    protected boolean lastJumpPressed;

    protected boolean incrementJumpTimer;

    public boolean lastForwardPressed;

    protected int jumpTimer;

    public double velX;

    public double velY;

    public double velZ;

    protected double ticksLeft;

    public Vec3d forward;

    public Vec3d right;

    protected double acceleration;

    protected ElytraFlightMode(ElytraFlyModule elytraFly2) {
        this.elytraFly = elytraFly2;
    }

    public void onTick() {
        ClientPlayerEntity player = this.mc.player;
        if (player != null) {
            int find;
            if (this.elytraFly.autoReplenish.get()
                    && (find =
                                    ElytraFlyInventory.find(
                                            itemStack ->
                                                    itemStack.getItem() == Items.FIREWORK_ROCKET))
                            != -1
                    && !ElytraFlyInventory.isHotbar(find)) {
                ElytraFlyInventory.moveToHotbar(find, this.elytraFly.replenishSlot.getInt() - 1);
            }
            ItemStack equippedStack;
            if (this.elytraFly.replace.get()
                    && (equippedStack = player.getEquippedStack(EquipmentSlot.CHEST)).getItem()
                            == Items.ELYTRA) {
                int replaceDurability = this.elytraFly.replaceDurability.getInt();
                if (equippedStack.getMaxDamage() - equippedStack.getDamage() <= replaceDurability) {
                    int find2 =
                            ElytraFlyInventory.find(
                                    itemStack ->
                                            itemStack.getMaxDamage() - itemStack.getDamage()
                                                            > replaceDurability
                                                    && itemStack.getItem() == Items.ELYTRA);
                    ElytraFlyInventory.moveToChest(find2);
                }
            }
        }
    }

    public void onPreTick() {}

    public void onPacketSend(Packet<?> packet) {}

    public void onPositionPacket() {}

    public void onPlayerMove() {}

    public void onActivate() {
        this.lastJumpPressed = false;
        this.jumpTimer = 0;
        this.ticksLeft = 0.0;
        this.acceleration = 0.0;
    }

    public void onDeactivate() {}

    public void autoTakeoff() {
        ClientPlayerEntity player = this.mc.player;
        if (this.incrementJumpTimer) {
            this.jumpTimer++;
        }
        boolean pressed = this.mc.options.jumpKey.isPressed();
        ElytraFlightModes elytraFlightModes = this.elytraFly.flightModeType();
        if (this.elytraFly.autoTakeOff.get()
                        && elytraFlightModes != ElytraFlightModes.Pitch40
                        && elytraFlightModes != ElytraFlightModes.Bounce
                || !this.elytraFly.manualTakeoff.get()
                        && elytraFlightModes == ElytraFlightModes.Bounce
                        && pressed) {
            if (!this.lastJumpPressed && !player.isGliding()) {
                this.jumpTimer = 0;
                this.incrementJumpTimer = true;
            }
            if (this.jumpTimer >= 8) {
                this.jumpTimer = 0;
                this.incrementJumpTimer = false;
                player.setJumping(false);
                player.setSprinting(true);
                player.jump();
                player.networkHandler.sendPacket(
                        new ClientCommandC2SPacket(player, Mode.START_FALL_FLYING));
            }
        }
        this.lastJumpPressed = pressed;
    }

    public void handleAutopilot() {
        ClientPlayerEntity player = this.mc.player;
        if (player.isGliding()) {
            if (this.elytraFly.autoPilot.get()
                    && player.getY() > this.elytraFly.autoPilotMinimumHeight.get()
                    && this.elytraFly.flightModeType() != ElytraFlightModes.Bounce) {
                this.mc.options.forwardKey.setPressed(true);
                this.lastForwardPressed = true;
            }
            if (this.elytraFly.useFireworks.get()) {
                if (this.ticksLeft <= 0.0) {
                    this.ticksLeft = this.elytraFly.autoPilotFireworkDelay.get() * 20.0F;
                    int inHotbar =
                            ElytraFlyInventory.findInHotbar(
                                    itemStack -> itemStack.getItem() == Items.FIREWORK_ROCKET);
                    if (inHotbar == -1 || this.mc.interactionManager == null) {
                        return;
                    }
                    if (inHotbar == 40) {
                        this.mc.interactionManager.interactItem(player, Hand.OFF_HAND);
                        player.swingHand(Hand.OFF_HAND);
                    } else {
                        ElytraFlyInventory.swap(inHotbar, true);
                        this.mc.interactionManager.interactItem(player, Hand.MAIN_HAND);
                        player.swingHand(Hand.MAIN_HAND);
                        ElytraFlyInventory.swapBack();
                    }
                }
                this.ticksLeft--;
            }
        }
    }

    public void handleHorizontalSpeed(PlayerMoveEvent event) {
        boolean bl = false;
        boolean bl2 = false;
        if (this.mc.options.forwardKey.isPressed()) {
            this.velX = this.velX + this.forward.x * this.getSpeed() * 10.0;
            this.velZ = this.velZ + this.forward.z * this.getSpeed() * 10.0;
            bl = true;
        } else if (this.mc.options.backKey.isPressed()) {
            this.velX = this.velX - this.forward.x * this.getSpeed() * 10.0;
            this.velZ = this.velZ - this.forward.z * this.getSpeed() * 10.0;
            bl = true;
        }
        if (this.mc.options.rightKey.isPressed()) {
            this.velX = this.velX + this.right.x * this.getSpeed() * 10.0;
            this.velZ = this.velZ + this.right.z * this.getSpeed() * 10.0;
            bl2 = true;
        } else if (this.mc.options.leftKey.isPressed()) {
            this.velX = this.velX - this.right.x * this.getSpeed() * 10.0;
            this.velZ = this.velZ - this.right.z * this.getSpeed() * 10.0;
            bl2 = true;
        }
        if (bl && bl2) {
            double d = 1.0 / Math.sqrt(2.0);
            this.velX *= d;
            this.velZ *= d;
        }
    }

    public void handleVerticalSpeed(PlayerMoveEvent event) {
        if (this.mc.options.jumpKey.isPressed()) {
            this.velY = this.velY + 0.5 * this.elytraFly.verticalSpeed.get();
        } else if (this.mc.options.sneakKey.isPressed()) {
            this.velY = this.velY - 0.5 * this.elytraFly.verticalSpeed.get();
        }
    }

    public void handleFallMultiplier() {
        if (this.velY < 0.0) {
            this.velY = this.velY * this.elytraFly.fallMultiplier.get();
        } else if (this.velY > 0.0) {
            this.velY = 0.0;
        }
    }

    public void handleAcceleration() {
        if (this.elytraFly.acceleration.get()) {
            if (!this.isMoving()) {
                this.acceleration = 0.0;
            }
            this.acceleration =
                    Math.min(
                            this.acceleration
                                    + this.elytraFly.accelerationMin.get()
                                    + this.elytraFly.accelerationStep.get() * 0.1,
                            (double) this.elytraFly.horizontalSpeed.get());
        } else {
            this.acceleration = 0.0;
        }
    }

    public void zeroAcceleration() {
        this.acceleration = 0.0;
    }

    protected double getSpeed() {
        return this.elytraFly.acceleration.get()
                ? this.acceleration
                : this.elytraFly.horizontalSpeed.get();
    }

    private boolean isMoving() {
        ClientPlayerEntity player = this.mc.player;
        return player.forwardSpeed != 0.0F || player.sidewaysSpeed != 0.0F;
    }
}
