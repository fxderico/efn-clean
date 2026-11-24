package net.efn.client.module.impl.combat.totem;

import net.efn.client.module.impl.combat.crystal.CrystalDamage;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;

@Environment(EnvType.CLIENT)
public final class DoubleHand {

    private static final float DANGER_HEALTH = 6.0F;

    private static final double DANGER_RANGE = 6.0;

    private static final int SAFE_TICKS = 10;

    private boolean holding;

    private boolean rearmed = true;

    private int holdSlot = -1;

    private int returnSlot = -1;

    private int safeTicks;

    public DoubleHand() {}

    public boolean holding() {
        return this.holding;
    }

    public void tick(MinecraftClient client, ClientPlayerEntity player, boolean bl) {
        PlayerInventory inventory = player.getInventory();
        int selectedSlot = inventory.getSelectedSlot();
        boolean of = player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING);
        boolean bl2 = bl && inDanger(client.world, player);
        int i = this.safeTicks = bl2 ? 0 : Math.min(this.safeTicks + 1, 10);
        if (this.safeTicks == 10) {
            this.holding = false;
            this.rearmed = true;
        }
        if (this.holding && selectedSlot != this.holdSlot && !of) {
            this.holding = false;
            this.rearmed = false;
            this.returnSlot = -1;
        }
        boolean bl3 =
                client.currentScreen == null
                        && !player.isUsingItem()
                        && !MeleeAttacks.actedThisTick(player);
        if (this.holding) {
            if (of || !bl3) {
                return;
            }
            int hotbarTotem = hotbarTotem(inventory);
            if (hotbarTotem != -1) {
                this.holdSlot = hotbarTotem;
                inventory.setSelectedSlot(hotbarTotem);
                return;
            }
            this.holding = false;
        } else if (bl2 && this.rearmed) {
            if (of) {
                this.holding = true;
                if (selectedSlot != this.holdSlot) {
                    this.returnSlot = -1;
                }
                this.holdSlot = selectedSlot;
                return;
            }
            int hotbarTotem2 = hotbarTotem(inventory);
            if (hotbarTotem2 != -1
                    && bl3
                    && player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                this.holding = true;
                this.holdSlot = hotbarTotem2;
                this.returnSlot = selectedSlot;
                inventory.setSelectedSlot(hotbarTotem2);
                return;
            }
        }
        if (this.returnSlot != -1 && bl3) {
            this.putBack(inventory);
        }
    }

    public void putBack(PlayerInventory playerInventory) {
        if (this.returnSlot != -1) {
            if (playerInventory.getSelectedSlot() == this.holdSlot) {
                playerInventory.setSelectedSlot(this.returnSlot);
            }
            this.returnSlot = -1;
        }
    }

    public void reset() {
        this.holding = false;
        this.rearmed = true;
        this.holdSlot = -1;
        this.returnSlot = -1;
        this.safeTicks = 0;
    }

    private static boolean inDanger(ClientWorld world, ClientPlayerEntity player) {
        float health = CrystalDamage.health(player);
        if (health <= 6.0F) {
            return true;
        } else {
            for (EndCrystalEntity endCrystalEntity :
                    world.getNonSpectatingEntities(
                            EndCrystalEntity.class, player.getBoundingBox().expand(6.0))) {
                if (player.distanceTo(endCrystalEntity) <= 6.0
                        && CrystalDamage.estimate(endCrystalEntity.getEntityPos(), player)
                                >= health) {
                    return true;
                }
            }
            return false;
        }
    }

    private static int hotbarTotem(PlayerInventory playerInventory) {
        int selectedSlot = playerInventory.getSelectedSlot();
        for (int j = 0; j < 9; j++) {
            if (j != selectedSlot && playerInventory.getStack(j).isOf(Items.TOTEM_OF_UNDYING)) {
                return j;
            }
        }
        return playerInventory.getStack(selectedSlot).isOf(Items.TOTEM_OF_UNDYING)
                ? selectedSlot
                : -1;
    }
}
