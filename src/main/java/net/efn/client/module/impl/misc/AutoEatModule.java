package net.efn.client.module.impl.misc;

import net.efn.client.event.EventHandler;
import net.efn.client.event.EventPriority;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.AutoInvTotemModule;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.impl.combat.melee.SlotReturn;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.Set;

@Environment(EnvType.CLIENT)
public final class AutoEatModule extends ClientModule {

    private static final Set<Item> NEVER_EAT =
            Set.of(
                    Items.ROTTEN_FLESH,
                    Items.SPIDER_EYE,
                    Items.POISONOUS_POTATO,
                    Items.PUFFERFISH,
                    Items.CHICKEN,
                    Items.CHORUS_FRUIT,
                    Items.SUSPICIOUS_STEW,
                    Items.ENCHANTED_GOLDEN_APPLE);

    private static final int NOT_FOUND = -1;

    private static final int OFFHAND_SLOT = -2;

    private static final float HEALING_BONUS = 100.0F;

    private static AutoEatModule instance;

    private final NumberSetting hunger =
            this.addSetting("hunger", new NumberSetting("Hunger", 16.0F, 0.0F, 20.0F).step(1.0F));

    private final NumberSetting health =
            this.addSetting("health", new NumberSetting("Health", 10.0F, 0.0F, 20.0F).step(1.0F));

    private final SlotReturn slots = new SlotReturn(this);

    private boolean eating;

    public AutoEatModule() {
        super(
                "auto-eat",
                "Auto Eat",
                "Eats hotbar or offhand food once hunger or health drops too low.",
                ModuleCategory.MISC);
        instance = this;
    }

    @Override
    protected void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        this.stop(client);
        this.slots.drop(client, true);
    }

    public static boolean isEating() {
        AutoEatModule module = instance;
        return module != null && module.isEnabled() && module.eating;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        this.eat(client);
        if (!this.eating) {
            this.slots.tick(client, true);
        }
    }

    private void eat(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player != null
                && client.world != null
                && client.interactionManager != null
                && client.currentScreen == null
                && !AutoInvTotemModule.wantsHotbar()) {
            if (player.isUsingItem() && player.getActiveItem().contains(DataComponentTypes.FOOD)) {
                if (this.eating) {
                    client.options.useKey.setPressed(true);
                }
            } else if (!this.shouldEat(player)) {
                this.stop(client);
            } else {
                int bestSlot = this.findBestSlot(player);
                if (bestSlot == -1
                        || MeleeAttacks.actedThisTick(player)
                        || !MeleeAttacks.handsFree(client, player)) {
                    this.stop(client);
                } else if (CombatMacroLock.heldByOther(this)) {
                    this.eating = true;
                } else {
                    if (bestSlot == -2) {
                        client.interactionManager.interactItem(player, Hand.OFF_HAND);
                        MeleeAttacks.claimTick(client);
                    } else if (!this.slots.take(player, bestSlot)) {
                        this.stop(client);
                        return;
                    }
                    this.eating = true;
                    MeleeAttacks.takeAttackClicks(client);
                    client.options.useKey.setPressed(true);
                }
            }
        } else {
            this.stop(client);
        }
    }

    private boolean hungry(ClientPlayerEntity player) {
        return player.getHungerManager().getFoodLevel() <= this.hunger.getInt();
    }

    private boolean shouldEat(ClientPlayerEntity player) {
        boolean bl = this.health.getInt() > 0 && player.getHealth() <= this.health.getInt();
        return this.hungry(player) || bl;
    }

    private int findBestSlot(ClientPlayerEntity player) {
        boolean bl = !this.hungry(player);
        PlayerInventory inventory = player.getInventory();
        int i = -1;
        float f = -1.0F;
        for (int j = 0; j < 9; j++) {
            FoodComponent foodComponent = foodOf(player, inventory.getStack(j));
            float score;
            if (foodComponent != null && (score = score(foodComponent, bl)) > f) {
                f = score;
                i = j;
            }
        }
        FoodComponent foodComponent2 = foodOf(player, player.getOffHandStack());
        if (foodComponent2 != null && score(foodComponent2, bl) > f) {
            i = -2;
        }
        return i;
    }

    private static float score(FoodComponent foodComponent, boolean bl) {
        float f = foodComponent.nutrition() + foodComponent.saturation();
        return bl && foodComponent.canAlwaysEat() ? f + 100.0F : f;
    }

    private static FoodComponent foodOf(ClientPlayerEntity player, ItemStack stack) {
        if (!stack.isEmpty()
                && !NEVER_EAT.contains(stack.getItem())
                && !player.getItemCooldownManager().isCoolingDown(stack)) {
            FoodComponent foodComponent = stack.get(DataComponentTypes.FOOD);
            return foodComponent != null && player.canConsume(foodComponent.canAlwaysEat())
                    ? foodComponent
                    : null;
        } else {
            return null;
        }
    }

    private void stop(MinecraftClient client) {
        if (this.eating) {
            this.eating = false;
            client.options.useKey.setPressed(
                    Crystals.isPhysicallyDown(client, client.options.useKey));
        }
    }
}
