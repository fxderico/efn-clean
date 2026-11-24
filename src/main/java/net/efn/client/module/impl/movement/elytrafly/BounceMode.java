package net.efn.client.module.impl.movement.elytrafly;

import net.efn.client.module.impl.movement.ElytraFlyModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.registry.tag.BlockTags;

@Environment(EnvType.CLIENT)
public final class BounceMode extends ElytraFlightMode {

    private boolean rubberbanded = false;

    private int tickDelay;

    private double prevFov;

    public BounceMode(ElytraFlyModule module) {
        super(module);
        this.tickDelay = module.restartDelay.getInt();
    }

    @Override
    public void onTick() {
        super.onTick();
        ClientPlayerEntity player = this.mc.player;
        if (player != null) {
            if (this.mc.options.jumpKey.isPressed()
                    && !player.isGliding()
                    && !this.elytraFly.manualTakeoff.get()) {
                sendStartFallFlying(player);
            }
            if (checkConditions(player)) {
                if (!this.rubberbanded) {
                    if (this.prevFov != 0.0 && !this.elytraFly.sprint.get()) {
                        this.mc.options.getFovEffectScale().setValue(0.0);
                    }
                    if (this.elytraFly.autoJump.get()) {
                        this.mc.options.jumpKey.setPressed(true);
                    }
                    this.mc.options.forwardKey.setPressed(true);
                    player.setYaw(this.getYawDirection(player));
                    if (this.elytraFly.lockPitch.get()) {
                        player.setPitch(this.elytraFly.pitch.get());
                    }
                }
                if (!this.elytraFly.sprint.get()) {
                    if (player.isGliding()) {
                        player.setSprinting(player.isOnGround());
                    } else {
                        player.setSprinting(true);
                    }
                }
                if (this.rubberbanded && this.elytraFly.restart.get()) {
                    if (this.tickDelay > 0) {
                        this.tickDelay--;
                    } else {
                        sendStartFallFlying(player);
                        this.rubberbanded = false;
                        this.tickDelay = this.elytraFly.restartDelay.getInt();
                    }
                }
            }
        }
    }

    @Override
    public void onPreTick() {
        super.onPreTick();
        ClientPlayerEntity player = this.mc.player;
        if (player != null && checkConditions(player) && this.elytraFly.sprint.get()) {
            player.setSprinting(true);
        }
    }

    private void unpress() {
        this.mc.options.forwardKey.setPressed(false);
        if (this.elytraFly.autoJump.get()) {
            this.mc.options.jumpKey.setPressed(false);
        }
    }

    @Override
    public void onPositionPacket() {
        this.rubberbanded = true;
        ClientPlayerEntity player = this.mc.player;
        if (player != null) {
            player.stopGliding();
        }
    }

    @Override
    public void onPacketSend(Packet<?> packet) {
        ClientCommandC2SPacket clientCommandPacket;
        if (packet instanceof ClientCommandC2SPacket
                && (clientCommandPacket = (ClientCommandC2SPacket) packet).getMode()
                        == Mode.START_FALL_FLYING
                && !this.elytraFly.sprint.get()) {
            this.mc.player.setSprinting(true);
        }
    }

    public static boolean recastElytra(ClientPlayerEntity player) {
        if (checkConditions(player) && startGliding(player)) {
            sendStartFallFlying(player);
            return true;
        } else {
            return false;
        }
    }

    public static boolean checkConditions(ClientPlayerEntity player) {
        BlockState blockStateAtPos = player.getBlockStateAtPos();
        boolean bl =
                blockStateAtPos.isIn(BlockTags.CLIMBABLE)
                        && !blockStateAtPos.isIn(BlockTags.CAN_GLIDE_THROUGH);
        return !player.getAbilities().flying
                && !player.hasVehicle()
                && !bl
                && !player.isTouchingWater()
                && !player.hasStatusEffect(StatusEffects.LEVITATION);
    }

    private static boolean startGliding(ClientPlayerEntity player) {
        for (EquipmentSlot equipmentSlot : EquipmentSlot.VALUES) {
            if (LivingEntity.canGlideWith(player.getEquippedStack(equipmentSlot), equipmentSlot)) {
                MinecraftClient.getInstance().submitAndJoin(() -> player.startGliding());
                return true;
            }
        }
        return false;
    }

    private static void sendStartFallFlying(ClientPlayerEntity player) {
        player.networkHandler.sendPacket(
                new ClientCommandC2SPacket(player, Mode.START_FALL_FLYING));
    }

    private float getYawDirection(ClientPlayerEntity player) {
        String yawLockMode = this.elytraFly.yawLockMode.get();
        return switch (yawLockMode) {
            case "Smart" -> Math.round((player.getYaw() + 1.0F) / 45.0F) * 45.0F;
            case "Simple" -> this.elytraFly.yaw.get();
            default -> player.getYaw();
        };
    }

    @Override
    public void onActivate() {
        this.prevFov = this.mc.options.getFovEffectScale().getValue();
    }

    @Override
    public void onDeactivate() {
        this.unpress();
        this.rubberbanded = false;
        if (this.prevFov != 0.0 && !this.elytraFly.sprint.get()) {
            this.mc.options.getFovEffectScale().setValue(this.prevFov);
        }
    }
}
