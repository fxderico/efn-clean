package net.efn.client.module.impl.combat;

import net.efn.client.event.EventHandler;
import net.efn.client.event.EventPriority;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.impl.combat.melee.SlotReturn;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public final class ShieldBreakerModule extends ClientModule {

    private static final String MODE_PASSIVE = "Passive";

    private static final String MODE_AUTO = "Auto";

    private static final int REARM_TICKS = 10;

    private static final int MAX_LEAD_TICKS = 4;

    private static final int HOLD_TICKS = 2;

    private final ModeSetting mode =
            this.addSetting("mode", new ModeSetting("Mode", "Passive", "Passive", "Auto"));

    private final BooleanSetting switchBack =
            this.addSetting("switch-back", new BooleanSetting("Switch Back", true));

    private final SlotReturn slots = new SlotReturn(this);

    private int ticks;

    private int rearmTick;

    private int takenTick;

    public ShieldBreakerModule() {
        super(
                "shield-breaker",
                "Shield Breaker",
                "Swaps to an axe for any hit into a raised shield. Auto also makes the hit.",
                ModuleCategory.COMBAT);
    }

    @Override
    protected void onEnable() {
        this.ticks = 0;
        this.rearmTick = 0;
    }

    @Override
    protected void onDisable() {
        this.slots.drop(MinecraftClient.getInstance(), this.switchBack.get());
    }

    public void beforeAttack(ClientPlayerEntity player, Entity entity) {
        if (entity instanceof PlayerEntity player2
                && !MeleeAttacks.disablesShields(player.getMainHandStack())) {
            if (fightingHand(player)) {
                if (!MeleeAttacks.shieldBroken(player2) && blocksSoon(player, player2)) {
                    int bestAxe = bestAxe(player);
                    if (bestAxe >= 0 && this.slots.take(player, bestAxe)) {
                        this.takenTick = this.ticks;
                        MeleeAttacks.noteShieldBroken(player2);
                    }
                }
            }
        }
    }

    private static boolean fightingHand(ClientPlayerEntity player) {
        ItemStack mainHandStack = player.getMainHandStack();
        return mainHandStack.isEmpty() || MeleeAttacks.isMeleeWeapon(mainHandStack);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTick(TickEvent event) {
        this.ticks++;
        if (this.ticks - this.takenTick >= 2) {
            this.slots.tick(MinecraftClient.getInstance(), this.switchBack.get());
        }
    }

    @EventHandler
    public void onAutoTick(TickEvent event) {
        if (this.mode.is("Auto")) {
            this.tryHit(MinecraftClient.getInstance());
        }
    }

    private void tryHit(MinecraftClient client) {
        if (this.ticks >= this.rearmTick && MeleeAttacks.canAct(client)) {
            if (!CombatMacroLock.heldByOther(this) && !AutoInvTotemModule.wantsHotbar()) {
                ClientPlayerEntity player = client.player;
                if (!MeleeAttacks.actedThisTick(player)
                        && !ServerRotation.pending()
                        && !MeleeAttacks.clickPending(client)) {
                    EntityHitResult entityHitResult = MeleeAttacks.crosshairHit(client);
                    PlayerEntity player2;
                    Entity entity;
                    if (entityHitResult != null
                            && (entity = entityHitResult.getEntity()) instanceof PlayerEntity
                            && MeleeAttacks.isEnemy(player, player2 = (PlayerEntity) entity)) {
                        if (!MeleeAttacks.shieldBroken(player2) && blocksSoon(player, player2)) {
                            if (MeleeAttacks.disablesShields(player.getMainHandStack())
                                    || bestAxe(player) >= 0 && fightingHand(player)) {
                                if (MeleeAttacks.attack(client, entityHitResult)) {
                                    this.rearmTick = this.ticks + 10;
                                    MeleeAttacks.claimTick(client);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean blocksSoon(ClientPlayerEntity player, PlayerEntity player2) {
        if (!player2.isUsingItem()) {
            return false;
        } else {
            ItemStack activeItem = player2.getActiveItem();
            BlocksAttacksComponent blocksAttacksComponent =
                    activeItem.get(DataComponentTypes.BLOCKS_ATTACKS);
            return blocksAttacksComponent != null
                            && player2.getItemUseTime() + leadTicks(player)
                                    >= blocksAttacksComponent.getBlockDelayTicks()
                    ? MeleeAttacks.shieldBlocks(player2, player, activeItem)
                    : false;
        }
    }

    private static int leadTicks(ClientPlayerEntity player) {
        PlayerListEntry playerListEntry =
                player.networkHandler.getPlayerListEntry(player.getUuid());
        return playerListEntry == null
                ? 0
                : MathHelper.clamp(Math.round(playerListEntry.getLatency() / 50.0F), 0, 4);
    }

    static int bestAxe(ClientPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        int i = -1;
        double d = -1.0 / 0.0;
        for (int j = 0; j < PlayerInventory.getHotbarSize(); j++) {
            ItemStack stack = inventory.getStack(j);
            double attackDamage;
            if (MeleeAttacks.disablesShields(stack)
                    && !stack.willBreakNextUse()
                    && (attackDamage = MeleeAttacks.attackDamage(stack)) > d) {
                i = j;
                d = attackDamage;
            }
        }
        return i;
    }
}
