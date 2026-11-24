package net.efn.client.easyplace.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public final class InventoryCalm {

    private static final double STOPPED = 0.0025;

    private static final double HALTED = 1.0E-4;

    private InventoryCalm() {}

    public static boolean idle(ClientPlayerEntity player) {
        PlayerInput playerInput = player.input.playerInput;
        return !moving(playerInput) && !playerInput.sneak();
    }

    public static boolean still(ClientPlayerEntity player) {
        return idle(player)
                && player.isOnGround()
                && player.getVelocity().horizontalLengthSquared() < 0.0025;
    }

    public static boolean settle(MinecraftClient client, ClientPlayerEntity player) {
        if (still(player)) {
            return true;
        } else {
            GameOptions options = client.options;
            options.forwardKey.setPressed(false);
            options.backKey.setPressed(false);
            options.leftKey.setPressed(false);
            options.rightKey.setPressed(false);
            options.jumpKey.setPressed(false);
            options.sprintKey.setPressed(false);
            options.useKey.setPressed(false);
            options.attackKey.setPressed(false);
            PlayerInput playerInput = player.input.playerInput;
            if (moving(playerInput)
                    || !player.isOnGround()
                    || player.getVelocity().horizontalLengthSquared() >= 1.0E-4) {
                return false;
            } else if (!overGround(player)) {
                return true;
            } else {
                options.sneakKey.setPressed(false);
                return false;
            }
        }
    }

    private static boolean moving(PlayerInput playerInput) {
        return playerInput.forward()
                || playerInput.backward()
                || playerInput.left()
                || playerInput.right()
                || playerInput.jump()
                || playerInput.sprint();
    }

    private static boolean overGround(ClientPlayerEntity player) {
        BlockPos pos = BlockPos.ofFloored(player.getX(), player.getY() - 0.05, player.getZ());
        return !player.getEntityWorld()
                .getBlockState(pos)
                .getCollisionShape(player.getEntityWorld(), pos)
                .isEmpty();
    }
}
