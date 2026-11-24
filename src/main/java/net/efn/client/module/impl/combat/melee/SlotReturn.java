package net.efn.client.module.impl.combat.melee;

import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.impl.combat.AutoInvTotemModule;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;

@Environment(EnvType.CLIENT)
public final class SlotReturn {

    private static final int NONE = -1;

    private final Object owner;

    private int previous = -1;

    private int taken = -1;

    public SlotReturn(Object owner2) {
        this.owner = owner2;
    }

    public boolean take(ClientPlayerEntity player, int taken2) {
        PlayerInventory inventory = player.getInventory();
        if (inventory.getSelectedSlot() == taken2 && !this.holding()) {
            return true;
        } else if (PlayerInventory.isValidHotbarIndex(taken2)
                && !AutoInvTotemModule.wantsHotbar()
                && CombatMacroLock.tryAcquire(this.owner)) {
            if (!this.holding()) {
                this.previous = inventory.getSelectedSlot();
            }
            this.taken = taken2;
            inventory.setSelectedSlot(taken2);
            return true;
        } else {
            return false;
        }
    }

    public boolean holding() {
        return this.taken != -1;
    }

    public void tick(MinecraftClient client, boolean bl) {
        if (this.holding()) {
            ClientPlayerEntity player = client.player;
            if (player == null) {
                this.finish(null, false);
            } else if (AutoInvTotemModule.wantsHotbar()) {
                CombatMacroLock.release(this.owner);
            } else if (client.currentScreen == null && !MeleeAttacks.actedThisTick(player)) {
                this.finish(player, bl);
            }
        }
    }

    public void drop(MinecraftClient client, boolean bl) {
        if (this.holding()) {
            this.finish(client.player, bl);
        }
    }

    private void finish(ClientPlayerEntity player, boolean bl) {
        if (bl
                && player != null
                && player.getInventory().getSelectedSlot() == this.taken
                && this.previous != this.taken) {
            player.getInventory().setSelectedSlot(this.previous);
        }
        CombatMacroLock.release(this.owner);
        this.previous = -1;
        this.taken = -1;
    }
}
