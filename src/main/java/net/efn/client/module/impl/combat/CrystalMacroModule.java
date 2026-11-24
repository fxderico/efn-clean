package net.efn.client.module.impl.combat;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import net.efn.client.event.EventHandler;
import net.efn.client.event.impl.PlayerAttackEntityEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.CombatMacroLock;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.crystal.CrystalDamage;
import net.efn.client.module.impl.combat.crystal.Crystals;
import net.efn.client.module.impl.combat.crystal.LootLatch;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.impl.misc.AutoEatModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.ServerRotation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public final class CrystalMacroModule extends ClientModule {

    private static final int COUNTER_CAP = 1000;

    private static final int WEAPON_SETTLE_TICKS = 2;

    private static final int MAX_BREAK_ATTEMPTS = 3;

    private static final int GHOST_REST_TICKS = 100;

    private final NumberSetting breakDelay =
            this.addSetting(
                    "break-delay", new NumberSetting("Break Delay", 0.0F, 0.0F, 10.0F).step(1.0F));

    private final NumberSetting placeDelay =
            this.addSetting(
                    "place-delay", new NumberSetting("Place Delay", 0.0F, 0.0F, 10.0F).step(1.0F));

    private final BooleanSetting autoObsidian =
            this.addSetting("auto-obsidian", new BooleanSetting("Auto Obsidian", true));

    private final NumberSetting obsidianDelay =
            this.addSetting(
                    "obsidian-delay",
                    new NumberSetting("Obsidian Delay", 2.0F, 0.0F, 10.0F).step(1.0F));

    private final BooleanSetting switchBack =
            this.addSetting("switch-back", new BooleanSetting("Switch Back", true));

    private final NumberSetting maxSelfDamage =
            this.addSetting(
                    "max-self-damage",
                    new NumberSetting("Max Self Damage", 8.0F, 0.0F, 20.0F).step(0.5F));

    private final BooleanSetting humanize =
            this.addSetting("humanize", new BooleanSetting("Human Pace", true));

    private boolean engaged;

    private int savedSlot = -1;

    private int ownSlot = -1;

    private int ticksSinceAction = 1000;

    private int actionGap;

    private final Int2IntOpenHashMap breakAttempts = new Int2IntOpenHashMap();

    private final Int2IntOpenHashMap restUntil = new Int2IntOpenHashMap();

    private ItemStack lastMain;

    private int mainAge = 1000;

    private int pendingBreakId = -1;

    private int pendingBreakAge = 1000;

    private BlockPos pendingCrystalBase;

    private int pendingCrystalAge = 1000;

    private BlockPos pendingObsidian;

    private int pendingObsidianAge = 1000;

    public CrystalMacroModule() {
        super(
                "crystal-macro",
                "Crystal Macro",
                "Breaks and places crystals while you hold right-click.",
                ModuleCategory.COMBAT);
        this.obsidianDelay.visibleWhen(this.autoObsidian::get);
    }

    @Override
    public int settingsVersion() {
        return 1;
    }

    @Override
    public void migrateSettings(int i) {
        if (i < 1) {
            this.breakDelay.set(this.breakDelay.get() - 1.0F);
            this.placeDelay.set(this.placeDelay.get() - 1.0F);
            this.obsidianDelay.set(this.obsidianDelay.get() - 1.0F);
        }
    }

    @Override
    protected void onEnable() {
        this.resetState();
    }

    @Override
    protected void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        this.endHold(client);
        ClientPlayerEntity player = client.player;
        if (player != null && !hotbarTakenByOther()) {
            this.restoreSlot(player);
        }
        this.resetState();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        this.ageCounters();
        ClientPlayerEntity player = client.player;
        if (player != null
                && client.world != null
                && client.interactionManager != null
                && !player.isDead()) {
            ItemStack mainHandStack = player.getMainHandStack();
            if (mainHandStack != this.lastMain) {
                this.lastMain = mainHandStack;
                this.mainAge = 0;
            }
            this.forgetGone(client.world);
            LootLatch.update(client, player);
            if (LootLatch.latched()) {
                this.endHold(client);
                if (!player.isUsingItem()
                        && !AutoEatModule.isEating()
                        && (this.ownsSelection(player) || activationAllows(player))) {
                    suppressVanillaUse(client);
                }
            } else if (!this.holdActive(client, player)) {
                this.endHold(client);
                if (client.currentScreen == null
                        && !hotbarTakenByOther()
                        && !MeleeAttacks.actedThisTick(player)) {
                    this.restoreSlot(player);
                }
            } else if (!AutoInvTotemModule.wantsHotbar() && !CombatMacroLock.heldByOther(this)) {
                if (this.engaged) {
                    if (!this.keepEngaged(player)) {
                        this.endHold(client);
                        return;
                    }
                } else if (player.isUsingItem()
                        || !this.ownsSelection(player) && !activationAllows(player)) {
                    return;
                }
                CrystalMacroModule.Plan plan = this.plan(client, player);
                if (this.engaged || plan != null || this.ownsSelection(player)) {
                    this.engaged = true;
                    CombatMacroLock.tryAcquire(this);
                    suppressVanillaUse(client);
                    if (plan != null
                            && MeleeAttacks.handsFree(client, player)
                            && !MeleeAttacks.actedThisTick(player)
                            && !ServerRotation.pending()
                            && !ServerRotation.desynced()) {
                        switch (plan.step()) {
                            case BREAK:
                                this.breakCrystal(client, player, plan);
                                break;
                            case CRYSTAL:
                                if (this.useOnCrosshair(client, player, this.placeDelay, plan)) {
                                    this.pendingCrystalBase = plan.target();
                                    this.pendingCrystalAge = 0;
                                }
                                break;
                            case OBSIDIAN:
                                if (this.useOnCrosshair(client, player, this.obsidianDelay, plan)) {
                                    this.pendingObsidian = plan.target();
                                    this.pendingObsidianAge = 0;
                                }
                        }
                    }
                }
            } else {
                CombatMacroLock.release(this);
                if (this.engaged) {
                    suppressVanillaUse(client);
                }
            }
        } else {
            this.endHold(client);
            this.forgetSlots();
        }
    }

    @EventHandler
    public void onAttack(PlayerAttackEntityEvent event) {
        Entity target2 = event.target;
        EndCrystalEntity endCrystalEntity;
        if (target2 instanceof EndCrystalEntity
                && (endCrystalEntity = (EndCrystalEntity) target2).getEntityWorld().isClient()) {
            this.markBreak(endCrystalEntity);
        }
    }

    private boolean holdActive(MinecraftClient client, ClientPlayerEntity player) {
        return MeleeAttacks.canAct(client)
                && client.isWindowFocused()
                && !player.isSpectator()
                && Crystals.isPhysicallyDown(client, client.options.useKey);
    }

    private static boolean hotbarTakenByOther() {
        return AutoEatModule.isEating() || AutoInvTotemModule.wantsHotbar();
    }

    private boolean keepEngaged(ClientPlayerEntity player) {
        if (this.ownsSelection(player)) {
            return true;
        } else {
            this.forgetSlots();
            return activationAllows(player);
        }
    }

    private boolean ownsSelection(ClientPlayerEntity player) {
        return this.ownSlot != -1 && player.getInventory().getSelectedSlot() == this.ownSlot;
    }

    private static boolean activationAllows(ClientPlayerEntity player) {
        ItemStack mainHandStack = player.getMainHandStack();
        ItemStack offHandStack = player.getOffHandStack();
        return !mainHandStack.isOf(Items.END_CRYSTAL)
                        && (!offHandStack.isOf(Items.END_CRYSTAL)
                                || !Crystals.isCombatItem(mainHandStack))
                ? (mainHandStack.isIn(ItemTags.SWORDS)
                                || mainHandStack.isIn(ItemTags.AXES)
                                || mainHandStack.isOf(Items.MACE))
                        && (offHandStack.isEmpty() || offHandStack.isOf(Items.TOTEM_OF_UNDYING))
                        && Crystals.hotbarSlotOf(player.getInventory(), Items.END_CRYSTAL) != -1
                : true;
    }

    private void endHold(MinecraftClient client) {
        CombatMacroLock.release(this);
        if (this.engaged) {
            this.engaged = false;
            client.options.useKey.setPressed(
                    client.currentScreen == null
                            && Crystals.isPhysicallyDown(client, client.options.useKey));
        }
    }

    private void restoreSlot(ClientPlayerEntity player) {
        if (this.ownSlot != -1) {
            PlayerInventory inventory = player.getInventory();
            if (this.switchBack.get()
                    && PlayerInventory.isValidHotbarIndex(this.savedSlot)
                    && this.savedSlot != this.ownSlot
                    && inventory.getSelectedSlot() == this.ownSlot) {
                inventory.setSelectedSlot(this.savedSlot);
            }
            this.forgetSlots();
        }
    }

    private void forgetSlots() {
        this.savedSlot = -1;
        this.ownSlot = -1;
    }

    private void resetState() {
        this.engaged = false;
        this.forgetSlots();
        this.ticksSinceAction = 1000;
        this.actionGap = 0;
        this.breakAttempts.clear();
        this.restUntil.clear();
        this.lastMain = null;
        this.mainAge = 1000;
        this.pendingBreakId = -1;
        this.pendingBreakAge = 1000;
        this.pendingCrystalBase = null;
        this.pendingCrystalAge = 1000;
        this.pendingObsidian = null;
        this.pendingObsidianAge = 1000;
        CombatMacroLock.release(this);
    }

    private static void suppressVanillaUse(MinecraftClient client) {
        KeyBinding useKey = client.options.useKey;
        useKey.setPressed(false);
        while (useKey.wasPressed()) {}
    }

    private CrystalMacroModule.Plan plan(MinecraftClient client, ClientPlayerEntity player) {
        HitResult crosshairTarget = player.getCrosshairTarget(1.0F, player);
        int confirmWindow = Crystals.confirmWindow(client, player);
        if (this.pendingCrystalBase != null
                && Crystals.hasCrystal(client.world, this.pendingCrystalBase, null)) {
            this.pendingCrystalBase = null;
        }
        if (crosshairTarget instanceof EntityHitResult entityHitResult) {
            Entity entity = entityHitResult.getEntity();
            EndCrystalEntity endCrystalEntity;
            if (!(entity instanceof EndCrystalEntity)
                    || !(endCrystalEntity = (EndCrystalEntity) entity).isAlive()) {
                return null;
            } else if (endCrystalEntity.getId() == this.pendingBreakId
                    && this.pendingBreakAge < confirmWindow) {
                return null;
            } else if (this.restUntil.get(endCrystalEntity.getId()) > player.age) {
                return null;
            } else {
                float estimate = CrystalDamage.estimate(endCrystalEntity.getEntityPos(), player);
                if (!(estimate > this.maxSelfDamage.get())
                        && !CrystalDamage.endangers(player, estimate)) {
                    int weaponRoute = Crystals.weaponRoute(player);
                    return weaponRoute == -1
                            ? null
                            : CrystalMacroModule.Plan.forBreak(endCrystalEntity, weaponRoute);
                } else {
                    return null;
                }
            }
        } else {
            BlockHitResult blockHitResult;
            if (crosshairTarget instanceof BlockHitResult
                    && (blockHitResult = (BlockHitResult) crosshairTarget).getType()
                            == Type.BLOCK) {
                ClientWorld world = client.world;
                BlockPos pos = blockHitResult.getBlockPos();
                if (!world.getWorldBorder().contains(pos)) {
                    return null;
                } else {
                    BlockState blockState = world.getBlockState(pos);
                    if (Crystals.isBase(blockState)) {
                        if (pos.equals(this.pendingCrystalBase)
                                && this.pendingCrystalAge < confirmWindow) {
                            return null;
                        } else if (!Crystals.canHost(world, pos, null)) {
                            return null;
                        } else {
                            int route2 =
                                    Crystals.route(
                                            player, Items.END_CRYSTAL, blockState, this.savedSlot);
                            return route2 == -1
                                    ? null
                                    : CrystalMacroModule.Plan.forPlace(
                                            CrystalMacroModule.Step.CRYSTAL,
                                            blockHitResult,
                                            pos,
                                            route2);
                        }
                    } else if (!this.autoObsidian.get()) {
                        return null;
                    } else if (Crystals.route(
                                    player,
                                    Items.END_CRYSTAL,
                                    Blocks.OBSIDIAN.getDefaultState(),
                                    this.savedSlot)
                            == -1) {
                        return null;
                    } else {
                        int route3 =
                                Crystals.route(player, Items.OBSIDIAN, blockState, this.savedSlot);
                        if (route3 == -1) {
                            return null;
                        } else {
                            BlockPos pos2 =
                                    Crystals.obsidianTarget(
                                            world, player, blockHitResult, blockState);
                            return pos2 != null
                                            && (!pos2.equals(this.pendingObsidian)
                                                    || this.pendingObsidianAge >= confirmWindow)
                                    ? CrystalMacroModule.Plan.forPlace(
                                            CrystalMacroModule.Step.OBSIDIAN,
                                            blockHitResult,
                                            pos2,
                                            route3)
                                    : null;
                        }
                    }
                }
            } else {
                return null;
            }
        }
    }

    private void breakCrystal(
            MinecraftClient client, ClientPlayerEntity player, CrystalMacroModule.Plan plan) {
        if (this.due(this.breakDelay)) {
            if (plan.route() != -2) {
                this.select(player, plan.route());
            }
            if (this.mainAge >= 2
                    || Crystals.enchantsAddDamage(player.getMainHandStack())
                    || Crystals.dealsDamage(player, ItemStack.EMPTY)) {
                if (Crystals.strike(client, player, plan.crystal())) {
                    MeleeAttacks.claimTick(client);
                    this.markBreak(plan.crystal());
                    int i = plan.crystal().getId();
                    if (this.breakAttempts.addTo(i, 1) + 1 >= 3) {
                        this.breakAttempts.remove(i);
                        this.restUntil.put(i, player.age + 100);
                    }
                    this.acted();
                }
            }
        }
    }

    private boolean useOnCrosshair(
            MinecraftClient client,
            ClientPlayerEntity player,
            NumberSetting setting,
            CrystalMacroModule.Plan plan) {
        if (!this.due(setting)) {
            return false;
        } else {
            if (plan.route() != -2) {
                this.select(player, plan.route());
            }
            if (!Crystals.rightClick(client, player, plan.blockHit())) {
                return false;
            } else {
                MeleeAttacks.claimTick(client);
                this.acted();
                return true;
            }
        }
    }

    private boolean due(NumberSetting setting) {
        return this.ticksSinceAction > Math.max(setting.getInt(), this.actionGap - 1);
    }

    private void acted() {
        this.ticksSinceAction = 0;
        this.actionGap = this.humanize.get() ? Crystals.humanGap() : 0;
    }

    private void forgetGone(ClientWorld world) {
        this.breakAttempts.keySet().removeIf(i -> world.getEntityById(i) == null);
        this.restUntil.keySet().removeIf(i -> world.getEntityById(i) == null);
    }

    private void select(ClientPlayerEntity player, int selectedSlot) {
        PlayerInventory inventory = player.getInventory();
        if (this.savedSlot == -1) {
            this.savedSlot = inventory.getSelectedSlot();
        }
        inventory.setSelectedSlot(selectedSlot);
        this.ownSlot = selectedSlot;
        this.lastMain = player.getMainHandStack();
        this.mainAge = 0;
    }

    private void markBreak(EndCrystalEntity endCrystalEntity) {
        this.pendingBreakId = endCrystalEntity.getId();
        this.pendingBreakAge = 0;
    }

    private void ageCounters() {
        this.ticksSinceAction = Math.min(this.ticksSinceAction + 1, 1000);
        this.mainAge = Math.min(this.mainAge + 1, 1000);
        this.pendingBreakAge = Math.min(this.pendingBreakAge + 1, 1000);
        this.pendingCrystalAge = Math.min(this.pendingCrystalAge + 1, 1000);
        this.pendingObsidianAge = Math.min(this.pendingObsidianAge + 1, 1000);
    }

    @Environment(EnvType.CLIENT)
    private record Plan(
            CrystalMacroModule.Step step,
            EndCrystalEntity crystal,
            BlockHitResult blockHit,
            BlockPos target,
            int route) {

        static CrystalMacroModule.Plan forBreak(EndCrystalEntity endCrystalEntity, int i) {
            return new CrystalMacroModule.Plan(
                    CrystalMacroModule.Step.BREAK, endCrystalEntity, null, null, i);
        }

        static CrystalMacroModule.Plan forPlace(
                CrystalMacroModule.Step step2, BlockHitResult blockHitResult, BlockPos pos, int i) {
            return new CrystalMacroModule.Plan(step2, null, blockHitResult, pos, i);
        }
    }

    @Environment(EnvType.CLIENT)
    private static enum Step {
        BREAK,
        CRYSTAL,
        OBSIDIAN;

        private Step() {}
    }
}
