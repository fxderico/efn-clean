package net.efn.client.util.pvp;

import net.efn.client.module.impl.movement.SprintModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.PlayerInput;

@Environment(EnvType.CLIENT)
public final class InputControl {

    private static boolean dropSprint;

    private static boolean releaseForward;

    private static boolean backward;

    private static boolean jump;

    private static boolean idle;

    private InputControl() {}

    public static void dropSprint() {
        dropSprint = true;
    }

    public static void releaseForward(boolean bl) {
        releaseForward = true;
        backward |= bl;
    }

    public static void jump() {
        jump = true;
    }

    public static void idle() {
        idle = true;
    }

    public static PlayerInput shape(ClientPlayerEntity player, PlayerInput playerInput) {
        boolean forward = playerInput.forward();
        boolean backward2 = playerInput.backward();
        boolean left = playerInput.left();
        boolean right = playerInput.right();
        boolean jump2 = playerInput.jump();
        boolean bl = playerInput.sprint() || SprintModule.holding();
        if (idle) {
            bl = false;
            jump2 = false;
            right = false;
            left = false;
            backward2 = false;
            forward = false;
        }
        if (releaseForward && !idle) {
            forward = false;
            backward2 |= backward;
        }
        if (jump && !idle) {
            jump2 = true;
        }
        if (dropSprint || idle) {
            bl = false;
            player.setSprinting(false);
        }
        idle = false;
        jump = false;
        backward = false;
        releaseForward = false;
        dropSprint = false;
        return forward == playerInput.forward()
                        && backward2 == playerInput.backward()
                        && left == playerInput.left()
                        && right == playerInput.right()
                        && jump2 == playerInput.jump()
                        && bl == playerInput.sprint()
                ? playerInput
                : new PlayerInput(forward, backward2, left, right, jump2, playerInput.sneak(), bl);
    }
}
