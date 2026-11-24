/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.easyplace.builder;

import fi.dy.masa.litematica.world.WorldSchematic;

import net.efn.client.chat.ModuleChat;
import net.efn.client.easyplace.EasyPlace;
import net.efn.client.easyplace.compat.BaritoneCompat;
import net.efn.client.easyplace.inventory.InventoryPickSourceMode;
import net.efn.client.easyplace.module.EasyPlaceModule;
import net.efn.client.easyplace.placement.BuildScope;
import net.efn.client.easyplace.placement.PlacementConfig;
import net.efn.client.easyplace.placement.PlacementManager;
import net.efn.client.easyplace.schematic.SchematicManager;
import net.efn.client.easyplace.util.BlockStateUtil;
import net.efn.client.easyplace.util.PlacementUtil;
import net.efn.client.easyplace.util.StateRules;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.donutmacros.AhPurchase;
import net.efn.client.module.impl.donutmacros.DonutSell;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.ObserverBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.FluidState;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Environment(value = EnvType.CLIENT)
public final class SchematicBuilderModule extends ClientModule {

    private static final int SCAN_TICKS = 20;

    private static final int SCAN_AFTER_PLACE_TICKS = 4;

    private static final int NO_TARGET_TICKS = 4;

    private static final int WALK_FAILS = 2;

    private static final int LEAVES_TO_BUY = 16;

    private static final int SCAFFOLD_DEPTH = 4;

    private static final int SWEEP_TICKS = 1200;

    private static final int NOTHING_MOVED_TICKS = 1200;

    private static final String GLASS_SEARCH = "glass stack";

    private static final int SMALL_NEED = 16;

    private static final int POUR_REST_TICKS = 1200;

    private static final int RELOG_REFUSALS = 8;

    private static final int RELOG_WINDOW_TICKS = 1200;

    private static final int RELOG_GAP_TICKS = 6000;

    private static final int LAVA_FIRST_RETRY = 200;

    private static final double LOW_WEIGHT = 40.0;

    private static final int WALK_CANDIDATES = 16;

    private static final int STUBBORN_TRIES = 3;

    private static final int GIVE_UP_TRIES = 8;

    private static final int BAD_CLIMB_TICKS = 1200;

    private static final float SWIM_DEPTH = 0.4f;

    private static final double CLOSE_REACH = 2.8;

    private static final double CLOSE_STAND = 2.2;

    private static final int TOTEM_POP = 35;

    private static final long SELL_GAP_TICKS = 200L;

    private static final long BUY_SETTLE_TICKS = 40L;

    private static final int BUSY_LIMIT_TICKS = 100;

    private static final int STRAY_MENU_TICKS = 100;

    private static final int RELAX_LIMIT = 32;

    private static final int DEFER_BATCH = 4;

    private static final long PLAN_BUDGET_NANOS = 3000000L;

    private static final int PLAN_TARGETS = 600;

    private static final int BUCKET_BATCH = 8;

    private static final int SPARE_SLOTS = 4;

    private static final int TOTEM_RETRY_TICKS = 1200;

    private static final int SPRINT_RUN = 3;

    private static final int EAT_GAP_TICKS = 60;

    private static final float LOW_HEALTH = 8.0f;

    private static final float SAFE_FALL_HEALTH = 15.0f;

    private static final double MAX_FALL = 5.0;

    private static final float FULL_FALL_HEALTH = 18.0f;

    private static final double FULL_FALL = 8.0;

    private static final int STUCK_GAP_TICKS = 60;

    private static final int ARRIVE_GRACE_TICKS = 30;

    private static final Predicate<ItemStack> TOTEM =
            itemStack -> itemStack.isOf(Items.TOTEM_OF_UNDYING);

    private static final Direction[] SUPPORT_SIDES =
            new Direction[] {
                Direction.DOWN,
                Direction.NORTH,
                Direction.SOUTH,
                Direction.EAST,
                Direction.WEST,
                Direction.UP
            };

    private final BooleanSetting buyFromAh =
            this.addSetting("buy-from-ah", new BooleanSetting("Buy From AH", true));

    private final StringSetting maxPerListing =
            this.addSetting(
                    "max-per-listing",
                    new StringSetting("Max Price Per Listing", "10m")
                            .hint("e.g. 10m, one whole listing"));

    private final StringSetting budget =
            this.addSetting(
                    "budget",
                    new StringSetting("Max Total Spend", "").hint("e.g. 500m, empty = no limit"));

    private final NumberSetting refresh =
            this.addSetting(
                    "refresh",
                    new NumberSetting("AH Refresh (ms)", 1000.0f, 300.0f, 3000.0f).step(100.0f));

    private final BooleanSetting climb =
            this.addSetting("climb", new BooleanSetting("Climb With Leaves", true));

    private final BooleanSetting supportLeaves =
            this.addSetting("support-leaves", new BooleanSetting("Support Leaves", true));

    private final BooleanSetting walk =
            this.addSetting("walk", new BooleanSetting("Walk With Baritone", false));

    private final BooleanSetting crafterSlots =
            this.addSetting("crafter-slots", new BooleanSetting("Crafter Slots", true));

    private final BooleanSetting clickStates =
            this.addSetting("click-states", new BooleanSetting("Repeaters & Comparators", true));

    private final BooleanSetting placeWater =
            this.addSetting("place-water", new BooleanSetting("Place Water & Lava", true));

    private final BooleanSetting sellLeftovers =
            this.addSetting("sell-leftovers", new BooleanSetting("Sell Leftovers", true));

    private final BooleanSetting keepTotem =
            this.addSetting("keep-totem", new BooleanSetting("Keep Totem In Offhand", true));

    private final BooleanSetting relogOnGhosts =
            this.addSetting("relog-ghosts", new BooleanSetting("Relog On Ghost Blocks", true));

    private final BooleanSetting placeTnt =
            this.addSetting("place-tnt", new BooleanSetting("Place TNT", false));

    private final BooleanSetting machineLast =
            this.addSetting("machine-last", new BooleanSetting("Pistons & Observers Last", true));

    private final BooleanSetting flipLevers =
            this.addSetting("flip-levers", new BooleanSetting("Flip Levers On", true));

    private final BooleanSetting relaxFacing =
            this.addSetting("relax-facing", new BooleanSetting("Relax Facing When Stuck", true));

    private final BooleanSetting eat =
            this.addSetting("eat", new BooleanSetting("Eat When Hungry", true));

    private final NumberSetting tickDelay =
            this.addSetting(
                    "tick-delay", new NumberSetting("Tick Delay", 1.0f, 0.0f, 10.0f).step(1.0f));

    private final BooleanSetting chatFeedback =
            this.addSetting("chat-feedback", new BooleanSetting("Chat Feedback", true));

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private final PlacementManager placement =
            new PlacementManager(
                    this.mc,
                    (value, value2, i) -> {
                        if (value.startsWith("missing-item-") || value.startsWith("unsupported-")) {
                            this.feedback(value, value2, i);
                        }
                    });

    private final SchematicManager schematics = new SchematicManager(this.mc);

    private final Walker walker = new Walker();

    private final Set<Long> scaffold = new HashSet<Long>();

    private final Set<Long> spentSupport = new HashSet<Long>();

    private long proppedAt = Long.MIN_VALUE;

    private boolean leftoversAdopted;

    private final ScaffoldStore scaffoldStore = new ScaffoldStore();

    private String scaffoldWorld;

    private final Set<Long> stuckScaffold = new HashSet<Long>();

    private final Map<Long, Long> supportFor = new HashMap<Long, Long>();

    private final Set<Long> craftersDone = new HashSet<Long>();

    private final Set<Long> clickFailed = new HashSet<Long>();

    private final Set<Long> deferred = new HashSet<Long>();

    private final Set<Long> missingKeys = new HashSet<Long>();

    private final Set<Long> stubborn = new HashSet<Long>();

    private final Map<Long, Integer> tries = new HashMap<Long, Integer>();

    private final Map<Long, Integer> walkFails = new HashMap<Long, Integer>();

    private final Map<Long, Long> badStands = new HashMap<Long, Long>();

    private final Map<Long, Integer> waterFails = new HashMap<Long, Integer>();

    private final Map<Long, Integer> scaffoldFails = new HashMap<Long, Integer>();

    private final Set<Long> badScaffold = new HashSet<Long>();

    private final Set<Long> buildScaffold = new HashSet<Long>();

    private final Map<Item, Integer> startCounts = new HashMap<Item, Integer>();

    private final Map<Item, Integer> bought = new HashMap<Item, Integer>();

    private final Map<String, Long> lastFeedbackTick = new HashMap<String, Long>();

    private BuildScan scan;

    private long tick;

    private long nextScan;

    private long lastScan;

    private long placedAtScan;

    private int stuckWalks;

    private Vec3d walkFrom;

    private BlockPos walkStand;

    private int climbFails;

    private long noClimbUntil;

    private final Map<Long, Long> badClimbs = new HashMap<Long, Long>();

    private long placingSince;

    private boolean placing;

    private AhPurchase purchase;

    private Item purchaseItem;

    private int purchaseNeed;

    private long lastPlaceTick = -4611686018427387904L;

    private int busyTicks;

    private StandPlanner planner;

    private List<StandPlanner.Step> route;

    private int routeIndex;

    private BlockPos routeTarget;

    private BlockPos routeStand;

    private BlockPos routeStart;

    private StandPlanner.Use routeUse;

    private int sweptLayer = Integer.MAX_VALUE;

    private int refusedSeen;

    private long lastLookUpTrace;

    private final Relogger relogger = new Relogger();

    private final ArrayDeque<Long> refusalTicks = new ArrayDeque();

    private int refusedTracked;

    private BlockPos refusedStand;

    private int refusedHere;

    private long lastRelogTick = -6000L;

    private ClientWorld seenLevel;

    private boolean sweepDue;

    private boolean sweepPlanning;

    private long sweepSince;

    private int climbTo = Integer.MIN_VALUE;

    private boolean fallsNext;

    private long lavaFirstUntil;

    private boolean lavaPlanning;

    private boolean planWithFalls;

    private int routeItemTicks;

    private long placedAtArrival;

    private long arrivedAt = -4611686018427387904L;

    private long noPlanUntil;

    private long lastStuckTick = -4611686018427387904L;

    private int strayMenuTicks;

    private String purchaseName = "";

    private DonutSell sell;

    private long nextSellTick;

    private Climber climber;

    private Hop hop;

    private Bridger bridger;

    private Breaker breaker;

    private BlockUser user;

    private Eater eater;

    private long nextEatTick;

    private CellFiller filler;

    private BlockPos walkTarget;

    private long spent;

    private long spendLimit;

    private long maxListing;

    private int leavesBought;

    private int bucketsEmptied;

    private boolean retriedDeferred;

    private boolean waterGivenUp;

    private boolean lavaGivenUp;

    private boolean latePhase;

    private long stallSince;

    private long stallProgress = -1L;

    private int leastLeft = Integer.MAX_VALUE;

    private BlockPos stallAt;

    private final Set<Long> lackingKeys = new HashSet<Long>();

    private final Map<Long, Long> pouredAt = new HashMap<Long, Long>();

    private long nextRefillTick;

    private boolean totemPopped;

    private long totemSwapTick;

    private long nextTotemBuyTick;

    private int shownLayer = Integer.MAX_VALUE;

    private boolean walking;

    private boolean armed;

    private int leversFlipped;

    private boolean verified;

    private boolean relaxTried;

    private final List<BlockPos> relaxed = new ArrayList<BlockPos>();

    private final List<Long> plannedKeys = new ArrayList<Long>();

    public SchematicBuilderModule() {
        super(
                "schematic-builder",
                "Schematic Builder",
                "Builds your active Litematica schematic layer by layer: buys missing blocks from"
                    + " the AH, walks with Baritone, climbs with leaves, sets crafter slots, puts"
                    + " the water and lava in and sells what is left over.",
                ModuleCategory.BOTS);
        this.buyFromAh.describe("Buys missing blocks from the AH.");
        this.maxPerListing.describe("Most it pays for one whole AH listing.");
        this.budget.describe("Most it spends in total, empty = no limit.");
        this.refresh.describe("How often it refreshes the AH (milliseconds).");
        this.sellLeftovers.describe("Sells leftover blocks when the build is done.");
        this.section(
                "Buying",
                this.buyFromAh,
                this.maxPerListing,
                this.budget,
                this.refresh,
                this.sellLeftovers);
        this.walk.describe("Walks to far blocks with Baritone (Baritone must be installed).");
        this.climb.describe("Pillars up with leaves to reach high layers.");
        this.supportLeaves.describe("Places temporary leaves under blocks that need support.");
        this.section("Movement", this.walk, this.climb, this.supportLeaves);
        this.tickDelay.describe("Ticks to wait between placements (0 = fastest).");
        this.machineLast.describe("Places pistons and observers last so nothing fires early.");
        this.relaxFacing.describe("When stuck, allows blocks to face a different way.");
        this.crafterSlots.describe("Sets crafter slots to match the schematic.");
        this.clickStates.describe("Clicks repeaters and comparators to the right setting.");
        this.flipLevers.describe("Turns levers on where the schematic has them on.");
        this.placeWater.describe("Also places water and lava sources.");
        this.placeTnt.describe("Also places TNT blocks.");
        this.section(
                "Placing",
                this.tickDelay,
                this.machineLast,
                this.relaxFacing,
                this.crafterSlots,
                this.clickStates,
                this.flipLevers,
                this.placeWater,
                this.placeTnt);
        this.keepTotem.describe("Keeps a totem in your offhand.");
        this.eat.describe("Eats when hungry.");
        this.relogOnGhosts.describe(
                "Rejoins if the server keeps rejecting placements (ghost blocks).");
        this.section("Safety", this.keepTotem, this.eat, this.relogOnGhosts);
        this.chatFeedback.describe("Says what it is doing in chat.");
        this.section("Display", this.chatFeedback);
        this.usesNotifications();
        this.maxPerListing.visibleWhen(this.buyFromAh::get);
        this.budget.visibleWhen(this.buyFromAh::get);
        this.refresh.visibleWhen(this.buyFromAh::get);
        this.placement.setOutcome(
                new PlacementManager.Outcome() {

                    @Override
                    public void placed(BlockPos pos) {
                        if (SchematicBuilderModule.this.mc.world != null
                                && SchematicBuilderModule.this
                                                .mc
                                                .world
                                                .getBlockState(pos)
                                                .getBlock()
                                        instanceof ChestBlock) {
                            SchematicBuilderModule.trace(
                                    "chest confirmed at "
                                            + pos.toShortString()
                                            + " as "
                                            + String.valueOf(
                                                    SchematicBuilderModule.this.mc.world
                                                            .getBlockState(pos)));
                        }
                        SchematicBuilderModule.this.tries.merge(pos.asLong(), 1, Integer::sum);
                        SchematicBuilderModule.this.lastPlaceTick =
                                SchematicBuilderModule.this.tick;
                        SchematicBuilderModule.this.verified = false;
                    }

                    @Override
                    public void failed(BlockPos pos2) {
                        SchematicBuilderModule.this.tries.merge(pos2.asLong(), 1, Integer::sum);
                        SchematicBuilderModule.this.lastPlaceTick =
                                SchematicBuilderModule.this.tick;
                    }
                });
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    protected void onEnable() {
        if (this.mc.player == null || this.mc.world == null) {
            this.say(ModuleChat.body().warn("Join a world first."));
            this.requestDisable();
            return;
        }
        if (!this.schematics.hasActiveSchematic()) {
            this.say(
                    ModuleChat.body()
                            .warn("No active Litematica placement")
                            .text(", load and place a schematic first."));
            this.requestDisable();
            return;
        }
        this.maxListing = AhPurchase.parseMoney(this.maxPerListing.get());
        if (this.buyFromAh.get() && this.maxListing <= 0L) {
            this.say(ModuleChat.body().warn("Set Max Price Per Listing to a price like 10m."));
            this.requestDisable();
            return;
        }
        this.spendLimit =
                this.budget.get().isBlank() ? 0L : AhPurchase.parseMoney(this.budget.get());
        long spendLimit2 = this.spendLimit;
        if (this.buyFromAh.get() && !this.budget.get().isBlank() && this.spendLimit <= 0L) {
            this.say(
                    ModuleChat.body()
                            .warn("Set Max Total Spend to a price like 500m, or leave it empty."));
            this.requestDisable();
            return;
        }
        EasyPlaceModule module = ModuleRegistry.get(EasyPlaceModule.class);
        if (module != null && module.isEnabled()) {
            module.setEnabled(false);
        }
        this.resetState();
        this.snapshotInventory(this.mc.player);
        this.placement.start();
        this.walking = this.walk.get() && BaritoneCompat.canWalk();
        boolean walking2 = this.walking;
        if (this.walk.get() && !this.walking) {
            this.say(
                    ModuleChat.body()
                            .warn("Baritone not found")
                            .text(", walking with the builder's own planner."));
        }
        if (this.walking) {
            BaritoneCompat.lockForBuild();
        }
        this.on(
                TickEvent.TYPE,
                Stage.PRE,
                tickEvent -> {
                    this.relogger.tick(this.mc);
                    if (!this.relogger.active()) {
                        this.placement.tick(this.config());
                        this.step();
                    }
                    if (this.placing && this.mc.currentScreen == null) {
                        this.placement.tickPre(this.config());
                    }
                });
        this.on(
                PacketAppliedEvent.TYPE,
                packetAppliedEvent -> {
                    EntityStatusS2CPacket entityStatusPacket;
                    Packet<?> packet = packetAppliedEvent.packet;
                    if (packet instanceof EntityStatusS2CPacket
                            && (entityStatusPacket = (EntityStatusS2CPacket) packet).getStatus()
                                    == 35
                            && this.mc.world != null
                            && this.mc.player != null
                            && entityStatusPacket.getEntity(this.mc.world) == this.mc.player) {
                        this.totemPopped = true;
                    }
                });
        this.say(ModuleChat.body().text("Building the schematic layer by layer."));
    }

    @Override
    protected void onDisable() {
        if (this.purchase != null) {
            this.purchase.cancel();
        }
        if (this.sell != null) {
            this.sell.cancel();
        }
        if (this.climber != null) {
            this.climber.release(this.mc);
        }
        if (this.hop != null) {
            this.hop.release(this.mc);
        }
        if (this.bridger != null) {
            this.bridger.release(this.mc);
        }
        if (this.breaker != null && this.mc.interactionManager != null) {
            this.mc.interactionManager.cancelBlockBreaking();
        }
        if (this.breaker != null) {
            this.breaker.release(this.mc);
        }
        if (this.user != null) {
            this.user.close(this.mc);
        }
        if (this.eater != null) {
            this.eater.release(this.mc);
        }
        this.walker.stop();
        this.placement.reset();
        if (this.walking) {
            BaritoneCompat.restoreSettings();
        }
        this.scaffoldStore.save(this.scaffoldWorld, this.scaffold);
        this.scaffoldWorld = null;
        this.resetState();
    }

    static boolean tracing() {
        return Boolean.getBoolean("easyplace.builder.debug");
    }

    static void trace(String string) {
        if (Boolean.getBoolean("easyplace.builder.debug")) {
            EasyPlace.LOG.info("[builder] " + string);
        }
    }

    private void resetState() {
        this.scan = null;
        this.tick = 0L;
        this.nextScan = 0L;
        this.lastScan = 0L;
        this.placedAtScan = 0L;
        this.stuckWalks = 0;
        this.placing = false;
        this.purchase = null;
        this.purchaseItem = null;
        this.sell = null;
        this.nextSellTick = 0L;
        this.climber = null;
        this.hop = null;
        this.bridger = null;
        this.breaker = null;
        this.user = null;
        this.eater = null;
        this.nextEatTick = 0L;
        this.filler = null;
        this.planner = null;
        this.route = null;
        this.routeTarget = null;
        this.noPlanUntil = 0L;
        this.lastStuckTick = -4611686018427387904L;
        this.walkTarget = null;
        this.spent = 0L;
        this.leavesBought = 0;
        this.bucketsEmptied = 0;
        this.retriedDeferred = false;
        this.armed = false;
        this.leversFlipped = 0;
        this.verified = false;
        this.relaxTried = false;
        this.relaxed.clear();
        this.waterGivenUp = false;
        this.lavaGivenUp = false;
        this.latePhase = false;
        this.nextRefillTick = 0L;
        this.totemPopped = false;
        this.totemSwapTick = 0L;
        this.nextTotemBuyTick = 0L;
        this.shownLayer = Integer.MAX_VALUE;
        this.sweptLayer = Integer.MAX_VALUE;
        this.refusedSeen = 0;
        this.refusalTicks.clear();
        this.refusedTracked = 0;
        this.refusedStand = null;
        this.refusedHere = 0;
        this.lastRelogTick = -6000L;
        this.seenLevel = null;
        this.climbTo = Integer.MIN_VALUE;
        this.fallsNext = false;
        this.planWithFalls = false;
        this.lavaFirstUntil = 0L;
        this.lavaPlanning = false;
        this.sweepDue = false;
        this.sweepPlanning = false;
        this.scaffold.clear();
        this.spentSupport.clear();
        this.proppedAt = Long.MIN_VALUE;
        this.leftoversAdopted = false;
        this.stuckScaffold.clear();
        this.supportFor.clear();
        this.craftersDone.clear();
        this.clickFailed.clear();
        this.deferred.clear();
        this.missingKeys.clear();
        this.stubborn.clear();
        this.tries.clear();
        this.walkFails.clear();
        this.badStands.clear();
        this.climbFails = 0;
        this.noClimbUntil = 0L;
        this.badClimbs.clear();
        this.pouredAt.clear();
        this.leastLeft = Integer.MAX_VALUE;
        this.stallAt = null;
        this.walkStand = null;
        this.waterFails.clear();
        this.scaffoldFails.clear();
        this.badScaffold.clear();
        this.buildScaffold.clear();
        this.startCounts.clear();
        this.bought.clear();
        this.lastFeedbackTick.clear();
        this.placement.setScope(null);
    }

    private boolean watchRefusals(ClientPlayerEntity player) {
        int refusedCount = this.placement.refusedCount();
        if (refusedCount < this.refusedTracked) {
            this.refusedTracked = refusedCount;
        }
        if (refusedCount > this.refusedTracked) {
            for (int i = this.refusedTracked; i < refusedCount; ++i) {
                this.refusalTicks.addLast(this.tick);
            }
            this.refusedTracked = refusedCount;
            BlockPos pos = player.getBlockPos();
            if (pos.equals(this.refusedStand)) {
                if (++this.refusedHere >= 2) {
                    this.badStands.put(pos.asLong(), this.tick + 600L);
                    this.stopMoving();
                    this.route = null;
                    this.noPlanUntil = this.tick;
                    this.refusedHere = 0;
                    this.feedback(
                            "refused-move",
                            "The server refuses blocks placed from this spot, trying from another",
                            100);
                }
            } else {
                this.refusedStand = pos;
                this.refusedHere = 1;
            }
        }
        while (!this.refusalTicks.isEmpty() && this.tick - this.refusalTicks.peekFirst() > 1200L) {
            this.refusalTicks.pollFirst();
        }
        if (!this.relogOnGhosts.get()
                || this.refusalTicks.size() < 8
                || this.tick - this.lastRelogTick < 6000L) {
            return false;
        }
        if (this.purchase != null || this.sell != null || this.user != null) {
            return false;
        }
        if (!this.relogger.start(this.mc)) {
            return false;
        }
        this.lastRelogTick = this.tick;
        this.refusalTicks.clear();
        this.say(
                ModuleChat.body()
                        .warn("The server keeps refusing blocks (ghost blocks)")
                        .text(": relogging to sync up, the build carries on after."));
        this.releaseMovement();
        return true;
    }

    private boolean watchStall(ClientPlayerEntity player) {
        if (this.purchase != null || this.sell != null || this.relogger.active()) {
            this.stallSince = this.tick;
            return false;
        }
        int i =
                this.scan == null
                        ? Integer.MAX_VALUE
                        : this.missingKeys.size() + this.scan.liquids.size() + this.scaffold.size();
        int j = i;
        if (this.placement.placedCount() != this.stallProgress
                || i < this.leastLeft
                || this.stallAt == null
                || !this.stallAt.isWithinDistance(player.getBlockPos(), 3.0)) {
            this.stallProgress = this.placement.placedCount();
            this.leastLeft = Math.min(this.leastLeft, i);
            this.stallAt = player.getBlockPos();
            this.stallSince = this.tick;
            return false;
        }
        if (this.tick - this.stallSince < 1200L) {
            return false;
        }
        this.stallSince = this.tick;
        SchematicBuilderModule.trace(
                "nothing moved for 1200 ticks at "
                        + player.getBlockPos().toShortString()
                        + ": hop "
                        + (this.hop != null)
                        + " climber "
                        + (this.climber != null)
                        + " bridger "
                        + (this.bridger != null)
                        + " breaker "
                        + (this.breaker == null ? "-" : this.breaker.pos().toShortString())
                        + " filler "
                        + (String)
                                (this.filler == null
                                        ? "-"
                                        : String.valueOf((Object) this.filler.kind())
                                                + " "
                                                + this.filler.cell().toShortString())
                        + " user "
                        + (this.user != null)
                        + " eater "
                        + (this.eater != null)
                        + " planner "
                        + (this.planner != null)
                        + " route "
                        + (String)
                                (this.route == null
                                        ? "-"
                                        : this.routeIndex + "/" + this.route.size())
                        + " walker "
                        + this.walker.active()
                        + " placing "
                        + this.placing
                        + " noPlan "
                        + (this.noPlanUntil - this.tick)
                        + " layer "
                        + String.valueOf(this.scan == null ? "?" : Integer.valueOf(this.scan.layer))
                        + " "
                        + this.placement.debugState(player.getBlockPos())
                        + " | "
                        + this.scanState());
        this.feedback("stall", "Nothing moved for a minute, starting over from here", 100);
        if (this.filler != null) {
            this.waterFails.put(this.filler.cell().asLong(), 2);
        }
        if (this.routeTarget != null) {
            this.deferred.add(this.routeTarget.asLong());
        }
        this.releaseMovement();
        if (this.user != null) {
            this.user.close(this.mc);
        }
        this.user = null;
        this.filler = null;
        this.planner = null;
        this.route = null;
        this.routeTarget = null;
        this.stopMoving();
        this.placement.abort();
        this.noPlanUntil = this.tick;
        this.rescanSoon();
        return true;
    }

    private String scanState() {
        if (this.scan == null) {
            return "no scan";
        }
        StringBuilder sb =
                new StringBuilder(
                        "missing "
                                + this.scan.missing.size()
                                + " waiting "
                                + this.scan.waiting.size()
                                + " deferred "
                                + this.deferred.size()
                                + " liquids "
                                + this.scan.liquids.size()
                                + " late "
                                + this.scan.late
                                + " latePhase "
                                + this.latePhase
                                + " whole "
                                + this.scan.wholeSchematic
                                + " waterGivenUp "
                                + this.waterGivenUp
                                + " retriedDeferred "
                                + this.retriedDeferred
                                + " buckets "
                                + String.valueOf(
                                        this.mc.player == null
                                                ? "?"
                                                : Integer.valueOf(
                                                        SchematicBuilderModule.count(
                                                                this.mc.player,
                                                                Items.WATER_BUCKET))));
        int i = 0;
        for (BuildScan.Cell cell : this.scan.missing) {
            if (i++ >= 6) break;
            long l = cell.pos().asLong();
            sb.append(" [")
                    .append(cell.pos().toShortString())
                    .append(' ')
                    .append(cell.expected().getBlock().getName().getString())
                    .append(this.scan.waiting.contains(l) ? " waiting" : "")
                    .append(this.deferred.contains(l) ? " deferred" : "")
                    .append(this.lackingKeys.contains(l) ? " lacking" : "")
                    .append(']');
        }
        return sb.toString();
    }

    private void releaseMovement() {
        if (this.climber != null) {
            this.climber.release(this.mc);
        }
        if (this.hop != null) {
            this.hop.release(this.mc);
        }
        if (this.bridger != null) {
            this.bridger.release(this.mc);
        }
        if (this.breaker != null && this.mc.interactionManager != null) {
            this.mc.interactionManager.cancelBlockBreaking();
        }
        if (this.breaker != null) {
            this.breaker.release(this.mc);
        }
        if (this.eater != null) {
            this.eater.release(this.mc);
        }
        this.climber = null;
        this.hop = null;
        this.bridger = null;
        this.breaker = null;
        this.eater = null;
        this.filler = null;
        this.planner = null;
        this.route = null;
        this.routeTarget = null;
        this.walker.stop();
        this.placing = false;
    }

    private void restartAfterRejoin() {
        this.releaseMovement();
        if (this.user != null) {
            this.user.close(this.mc);
        }
        this.user = null;
        if (this.purchase != null) {
            this.purchase.cancel();
        }
        this.purchase = null;
        if (this.sell != null) {
            this.sell.cancel();
        }
        this.sell = null;
        this.placement.reset();
        this.refusedTracked = 0;
        this.scan = null;
        this.nextScan = 0L;
        this.noPlanUntil = 0L;
        SchematicBuilderModule.trace("back in the world, starting over from a fresh scan");
    }

    private void keepScaffoldList() {
        String worldKey = ScaffoldStore.worldKey(this.mc);
        if (!Objects.equals(worldKey, this.scaffoldWorld)) {
            this.scaffoldStore.save(this.scaffoldWorld, this.scaffold);
            this.scaffold.clear();
            this.scaffoldWorld = worldKey;
            if (worldKey == null) {
                return;
            }
            Set<Long> set = this.scaffoldStore.load(worldKey);
            this.scaffold.addAll(set);
            if (!set.isEmpty()) {
                this.feedback(
                        "scaffold-kept",
                        set.size()
                                + " scaffold blocks from an earlier session are still there, they"
                                + " get mined too",
                        1);
            }
            return;
        }
        if (this.tick % 100L == 0L) {
            this.scaffoldStore.save(this.scaffoldWorld, this.scaffold);
        }
    }

    private PlacementConfig config() {
        return new PlacementConfig(
                true,
                true,
                180.0,
                InventoryPickSourceMode.SERVER_RANGE,
                true,
                this.tickDelay.getInt(),
                true,
                4.5,
                true,
                false,
                false,
                false,
                360.0);
    }

    private void step() {
        ClientPlayerEntity player = this.mc.player;
        ClientWorld world = this.mc.world;
        if (player == null || world == null || this.mc.interactionManager == null) {
            this.placing = false;
            return;
        }
        ++this.tick;
        if (world != this.seenLevel) {
            if (this.seenLevel != null) {
                this.restartAfterRejoin();
            }
            this.seenLevel = world;
        }
        if (this.watchRefusals(player)) {
            return;
        }
        this.keepScaffoldList();
        if (this.watchStall(player)) {
            return;
        }
        if (this.mc.currentScreen instanceof AbstractSignEditScreen) {
            this.mc.currentScreen.close();
            return;
        }
        if (this.mc.currentScreen instanceof HandledScreen
                && this.purchase == null
                && this.sell == null
                && this.user == null) {
            if (SchematicBuilderModule.tracing()) {
                Object object;
                String string = this.mc.currentScreen.getTitle().getString();
                HitResult crosshairTarget = this.mc.crosshairTarget;
                if (crosshairTarget instanceof BlockHitResult) {
                    BlockHitResult blockHitResult = (BlockHitResult) crosshairTarget;
                    object =
                            blockHitResult.getBlockPos().toShortString()
                                    + " "
                                    + String.valueOf(
                                            world.getBlockState(blockHitResult.getBlockPos())
                                                    .getBlock());
                } else {
                    object = "-";
                }
                SchematicBuilderModule.trace(
                        "menu "
                                + string
                                + " opened; aiming at "
                                + (String) object
                                + " shift "
                                + player.isSneaking()
                                + " key "
                                + this.mc.options.sneakKey.isPressed()
                                + " hop "
                                + (this.hop != null)
                                + " climber "
                                + (this.climber != null)
                                + " bridger "
                                + (this.bridger != null)
                                + " filler "
                                + (this.filler != null)
                                + " user "
                                + (this.user != null)
                                + " breaker "
                                + (this.breaker != null)
                                + " "
                                + this.placement.debugState(player.getBlockPos()));
            }
            player.closeHandledScreen();
            this.feedback("stray-menu", "Closed a menu that was opened by mistake", 100);
            return;
        }
        if (this.purchase != null) {
            this.placing = false;
            if (this.purchaseItem != null
                    && this.purchase.status() == AhPurchase.Status.RUNNING
                    && SchematicBuilderModule.count(player, this.purchaseItem)
                            >= this.purchaseNeed) {
                this.purchase.cancel();
                this.spent += this.purchase.spent();
                this.bought.merge(this.purchaseItem, this.purchase.boughtItems(), Integer::sum);
                this.feedback(
                        "enough-" + this.purchaseName,
                        "Already have enough " + this.purchaseName + ", stopped buying",
                        1);
                this.purchase = null;
                this.rescanSoon();
                return;
            }
            if (this.keepTotem.get()
                    && this.totemPopped
                    && this.purchaseItem != Items.TOTEM_OF_UNDYING
                    && !player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)
                    && !Hands.has(player, TOTEM)) {
                this.purchase.cancel();
                this.spent += this.purchase.spent();
                if (this.purchaseItem != null) {
                    this.bought.merge(this.purchaseItem, this.purchase.boughtItems(), Integer::sum);
                } else {
                    this.leavesBought += this.purchase.boughtItems();
                }
                this.purchase = null;
                return;
            }
            this.followPurchase();
            return;
        }
        if (this.sell != null) {
            this.placing = false;
            if (!this.sell.tick()) {
                this.sell = null;
                this.nextSellTick = this.tick + 200L;
                this.rescanSoon();
            }
            return;
        }
        if (this.user != null) {
            this.placing = false;
            BlockUser.Result result = this.user.tick(this.mc);
            if (result != BlockUser.Result.RUNNING) {
                if (this.user.crafter()) {
                    this.craftersDone.add(this.user.pos().asLong());
                    if (result == BlockUser.Result.FAILED) {
                        this.feedback(
                                "crafter",
                                "Could not set the crafter at " + this.user.pos().toShortString(),
                                1);
                    }
                } else if (result == BlockUser.Result.FAILED) {
                    this.clickFailed.add(this.user.pos().asLong());
                }
                this.user.close(this.mc);
                this.user = null;
                this.rescanSoon();
            }
            return;
        }
        if (this.eater != null) {
            this.placing = false;
            Eater.Result result2 = this.eater.tick(this.mc);
            if (result2 != Eater.Result.RUNNING) {
                this.eater.release(this.mc);
                this.eater = null;
            }
            return;
        }
        if (this.climber != null) {
            this.placing = false;
            Climber.Result result3 = this.climber.tick(this.mc);
            if (result3 != Climber.Result.RUNNING) {
                this.climber.release(this.mc);
                this.climber = null;
                this.walkFails.clear();
                if (result3 == Climber.Result.FAILED) {
                    SchematicBuilderModule.trace(
                            "climb failed at "
                                    + player.getBlockPos().toShortString()
                                    + " in "
                                    + String.valueOf(world.getFluidState(player.getBlockPos())));
                    this.badClimbs.put(this.climber.column().asLong(), this.tick + 1200L);
                    this.route = null;
                    if (++this.climbFails >= 2) {
                        this.climbFails = 0;
                        this.noClimbUntil = this.tick + 600L;
                        this.feedback(
                                "climb-pause",
                                "Pillaring keeps failing here, trying other spots for 30s",
                                1);
                    } else {
                        this.feedback("climb-failed", "Could not climb here", 60);
                    }
                } else {
                    this.climbFails = 0;
                }
                this.rescanSoon();
            }
            return;
        }
        if (this.bridger != null) {
            this.placing = false;
            Bridger.Result result4 = this.bridger.tick(this.mc);
            if (result4 != Bridger.Result.RUNNING) {
                this.bridger.release(this.mc);
                if (result4 == Bridger.Result.FAILED) {
                    SchematicBuilderModule.trace(
                            "bridge failed to "
                                    + this.bridger.spot().toShortString()
                                    + " from "
                                    + player.getBlockPos().toShortString());
                    this.badStands.put(this.bridger.spot().asLong(), this.tick + 400L);
                    this.route = null;
                }
                this.bridger = null;
                this.rescanSoon();
            }
            return;
        }
        if (this.hop != null) {
            this.placing = false;
            Hop.Result result5 = this.hop.tick(this.mc);
            if (result5 != Hop.Result.RUNNING) {
                if (result5 == Hop.Result.FAILED) {
                    SchematicBuilderModule.trace(
                            "hop failed to "
                                    + this.hop.spot().toShortString()
                                    + " from "
                                    + player.getBlockPos().toShortString()
                                    + " y="
                                    + player.getY());
                    this.badStands.put(this.hop.spot().asLong(), this.tick + 400L);
                    this.route = null;
                }
                this.hop = null;
                if (result5 == Hop.Result.DONE) {
                    this.stuckWalks = 0;
                }
                this.rescanSoon();
            }
            return;
        }
        if (this.breaker != null) {
            this.placing = false;
            Breaker.Result result6 = this.breaker.tick(this.mc);
            if (result6 != Breaker.Result.RUNNING) {
                if (result6 == Breaker.Result.DONE) {
                    long l = this.breaker.pos().asLong();
                    this.scaffold.remove(l);
                    this.spentSupport.remove(l);
                    this.supportFor.remove(l);
                } else {
                    this.stuckScaffold.add(this.breaker.pos().asLong());
                }
                this.breaker = null;
                this.rescanSoon();
            }
            return;
        }
        if (this.filler != null) {
            this.placing = false;
            CellFiller.Result result7 = this.filler.tick(this.mc);
            if (result7 != CellFiller.Result.RUNNING) {
                this.fillerDone(result7 == CellFiller.Result.DONE);
                this.filler = null;
                this.rescanSoon();
            }
            return;
        }
        if (this.route != null) {
            this.placing = false;
            if (this.routeIndex < this.route.size()) {
                StandPlanner.Step step;
                BlockPos pos = this.cleanupLeaf(player, this.routeFloor());
                if (pos != null) {
                    this.mc.options.forwardKey.setPressed(false);
                    this.mc.options.sprintKey.setPressed(false);
                    this.mc.options.jumpKey.setPressed(false);
                    if (!player.isOnGround()
                            || player.getVelocity().horizontalLengthSquared() > 4.0E-4) {
                        return;
                    }
                    SchematicBuilderModule.trace(
                            "mining "
                                    + pos.toShortString()
                                    + " in passing, standing at "
                                    + player.getBlockPos().toShortString());
                    this.stopMoving();
                    this.breaker = new Breaker(pos, false);
                    return;
                }
                BlockPos pos2 =
                        this.routeIndex == 0
                                ? this.routeStart
                                : this.route.get(this.routeIndex - 1).pos();
                BlockPos pos3 = player.getBlockPos();
                if (!player.isOnGround()) {
                    return;
                }
                if (pos2 != null
                        && (pos3.getX() != pos2.getX()
                                || pos3.getZ() != pos2.getZ()
                                || Math.abs(pos3.getY() - pos2.getY()) > 1)) {
                    this.mc.options.forwardKey.setPressed(false);
                    this.mc.options.sprintKey.setPressed(false);
                    this.mc.options.jumpKey.setPressed(false);
                    if (player.getVelocity().horizontalLengthSquared() > 4.0E-4) {
                        this.mc.options.sneakKey.setPressed(true);
                        return;
                    }
                    this.mc.options.sneakKey.setPressed(false);
                    SchematicBuilderModule.trace(
                            "off the route at "
                                    + pos3.toShortString()
                                    + ", expected "
                                    + pos2.toShortString()
                                    + ", planning again");
                    this.route = null;
                    this.routeTarget = null;
                    this.noPlanUntil = this.tick;
                    return;
                }
                if ((step = this.route.get(this.routeIndex++)).move() == StandPlanner.Move.WALK
                        || step.move() == StandPlanner.Move.LEAP) {
                    boolean bl =
                            step.move() == StandPlanner.Move.WALK
                                    && this.routeIndex < this.route.size()
                                    && this.route.get(this.routeIndex).move()
                                            == StandPlanner.Move.WALK;
                    this.hop = new Hop(step.pos(), bl, step.move() == StandPlanner.Move.LEAP);
                    boolean bl2 =
                            step.pos().getManhattanDistance(player.getBlockPos())
                                            - Math.abs(
                                                    step.pos().getY() - player.getBlockPos().getY())
                                    >= 3;
                    boolean bl3 = bl2;
                    if (this.flatRunAhead(step.pos()) >= 3
                            || step.move() == StandPlanner.Move.LEAP && bl2) {
                        this.hop.sprinting();
                    }
                    if (step.move() == StandPlanner.Move.WALK
                            && (Walker.risky(world, player.getBlockPos())
                                    || Walker.risky(world, step.pos())
                                    || Walker.icy(world, player.getBlockPos())
                                    || Walker.icy(world, step.pos()))) {
                        if (step.pos().getY() >= player.getBlockPos().getY()) {
                            this.hop.careful();
                        } else {
                            this.hop.slow();
                        }
                    }
                    return;
                }
                if (step.move() == StandPlanner.Move.BRIDGE) {
                    BlockPos pos4 = step.pos().down();
                    WorldSchematic schematicWorld = this.schematics.getSchematicWorld();
                    Item item = null;
                    if (!(schematicWorld == null
                            || schematicWorld.getBlockState(pos4).isAir()
                            || (item =
                                                    BlockStateUtil.requiredItem(
                                                            schematicWorld.getBlockState(pos4)))
                                            != null
                                    && SchematicBuilderModule.count(player, item) > 0)) {
                        SchematicBuilderModule.trace(
                                "route bridge into " + pos4.toShortString() + " without its block");
                        this.route = null;
                        this.noPlanUntil = this.tick + 40L;
                        return;
                    }
                    this.bridger =
                            new Bridger(player.getBlockPos(), step.pos(), this.scaffold, item);
                    if (this.routeIndex < this.route.size()
                            && this.route.get(this.routeIndex).move() == StandPlanner.Move.BRIDGE
                            && this.route
                                    .get(this.routeIndex)
                                    .pos()
                                    .subtract(step.pos())
                                    .equals(step.pos().subtract(player.getBlockPos()))) {
                        this.bridger.chained();
                    }
                    return;
                }
                int i = step.pos().getY() - player.getBlockPos().getY();
                BlockPos pos5 = step.pos().down(Math.max(1, i));
                if (i < 1
                        || i > 2
                        || !this.pillarFits(world, pos5)
                        || Climber.leavesCount(player) < i) {
                    SchematicBuilderModule.trace(
                            "route pillar refused at "
                                    + player.getBlockPos().toShortString()
                                    + " column="
                                    + pos5.toShortString()
                                    + " y="
                                    + player.getY()
                                    + " ground="
                                    + player.isOnGround());
                    this.route = null;
                    this.noPlanUntil = this.tick + 40L;
                    return;
                }
                this.climber = new Climber(pos5, this.scaffold, i);
                return;
            }
            this.route = null;
            this.arrivedAt = this.tick;
            this.placedAtArrival = this.placement.placedCount();
            this.rescanSoon();
        }
        if (this.planner != null) {
            this.placing = false;
            this.followPlanner();
            return;
        }
        if (this.walker.active()) {
            this.placing = false;
            Walker.Result result8 = this.walker.tick(player, this.tick);
            if (result8 == Walker.Result.FAILED) {
                if (this.walkStand != null) {
                    this.badStands.put(this.walkStand.asLong(), this.tick + 1200L);
                }
                if (this.walkTarget != null
                        && this.walkFails.merge(this.walkTarget.asLong(), 1, Integer::sum) >= 2) {
                    this.deferred.add(this.walkTarget.asLong());
                }
                if (this.walkFrom != null
                        && player.getEntityPos().distanceTo(this.walkFrom) < 1.0) {
                    ++this.stuckWalks;
                }
            } else if (result8 == Walker.Result.ARRIVED) {
                this.stuckWalks = 0;
            }
            if (result8 != Walker.Result.RUNNING) {
                this.walkTarget = null;
                this.rescanSoon();
            }
            return;
        }
        if (this.mc.currentScreen != null) {
            this.placing = false;
            if (player.currentScreenHandler != player.playerScreenHandler
                    && ++this.strayMenuTicks > 100) {
                this.strayMenuTicks = 0;
                player.closeHandledScreen();
                this.feedback("stray-menu", "Closed a menu that was left open", 100);
            }
            return;
        }
        this.strayMenuTicks = 0;
        if (this.placement.busy()) {
            this.placing = true;
            if (++this.busyTicks > 100) {
                this.busyTicks = 0;
                this.placement.abort();
                this.feedback("busy-reset", "A placement got stuck, skipping it", 100);
            }
            return;
        }
        this.busyTicks = 0;
        if (this.keepTotem.get() && this.mindTotem(player)) {
            return;
        }
        if (this.eat.get() && Eater.hungry(player) && this.tick >= this.nextEatTick) {
            this.nextEatTick = this.tick + 60L;
            if (Eater.hasFood(player)) {
                this.stopMoving();
                this.eater = new Eater();
                return;
            }
            if (player.getHealth() <= 8.0f) {
                this.feedback(
                        "no-food", "Low on health and nothing to eat: health won't come back", 600);
            }
        }
        if (this.scan == null
                || this.tick >= this.nextScan
                || this.placement.placedCount() != this.placedAtScan
                        && this.tick - this.lastScan >= 4L) {
            this.rescan(world, player);
        }
        if (this.scan == null) {
            return;
        }
        this.decide(world, player);
    }

    private void fillerDone(boolean bl) {
        long l = this.filler.cell().asLong();
        switch (this.filler.kind()) {
            case SUPPORT_LEAF:
                {
                    if (bl) {
                        this.scaffold.add(l);
                        break;
                    }
                    this.supportFor.remove(l);
                    if (this.scaffoldFails.merge(l, 1, Integer::sum) < 2) break;
                    this.badScaffold.add(l);
                    break;
                }
            case WATER_SOURCE:
            case WATERLOG:
            case LAVA_SOURCE:
                {
                    if (bl) {
                        ++this.bucketsEmptied;
                        this.pouredAt.put(l, this.tick);
                    } else {
                        this.waterFails.merge(l, 1, Integer::sum);
                    }
                    SchematicBuilderModule.trace(
                            (bl ? "poured into " : "could not pour into ")
                                    + this.filler.cell().toShortString()
                                    + " ("
                                    + String.valueOf((Object) this.filler.kind())
                                    + ")");
                    break;
                }
            case REFILL:
                {
                    if (bl) {
                        this.bucketsEmptied = Math.max(0, this.bucketsEmptied - 1);
                        break;
                    }
                    this.nextRefillTick = this.tick + 100L;
                }
        }
    }

    private boolean mindTotem(ClientPlayerEntity player) {
        if (player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            if (this.totemSwapTick > 0L) {
                this.feedback("totem-ok", "Totem is in the offhand", 1);
            }
            this.totemPopped = false;
            this.totemSwapTick = 0L;
            return false;
        }
        if (Hands.has(player, TOTEM)) {
            if (this.totemSwapTick > 0L && this.tick - this.totemSwapTick < 5L) {
                return true;
            }
            if (!Hands.toOffhand(this.mc, player, TOTEM)) {
                return false;
            }
            this.totemSwapTick = this.tick;
            return true;
        }
        if (this.tick < this.nextTotemBuyTick) {
            return false;
        }
        this.nextTotemBuyTick = this.tick + 1200L;
        if (!this.buyFromAh.get()
                || SchematicBuilderModule.room(player, new ItemStack(Items.TOTEM_OF_UNDYING)) <= 0
                        && !this.startSelling(player, false)) {
            this.feedback(
                    "totem-none",
                    "No totem for the offhand"
                            + (this.buyFromAh.get()
                                    ? " and no room to buy one"
                                    : " (Buy From AH is off)"),
                    1200);
            this.totemPopped = false;
            return false;
        }
        if (this.sell != null) {
            return true;
        }
        this.feedback(
                "totem-buy",
                this.totemPopped
                        ? "Totem popped, buying a new one"
                        : "No totem in the offhand, buying one",
                1);
        this.totemPopped = false;
        return this.startPurchase(Items.TOTEM_OF_UNDYING, 1);
    }

    private void debugLookUp(ClientWorld world, WorldSchematic worldSchematic) {
        if (!SchematicBuilderModule.tracing()
                || this.tick - this.lastLookUpTrace < 400L
                || this.scan == null) {
            return;
        }
        for (BuildScan.Cell cell : this.scan.missing) {
            BlockPos pos = cell.pos();
            if (StateRules.lookFor(cell.expected()) != Direction.UP
                    || world.getBlockState(pos.up()).isAir()) continue;
            StringBuilder sb =
                    new StringBuilder(
                            "lookup "
                                    + pos.toShortString()
                                    + " waiting="
                                    + this.scan.waiting.contains(pos.asLong())
                                    + " deferred="
                                    + this.deferred.contains(pos.asLong()));
            for (Direction direction : Direction.Type.HORIZONTAL) {
                BlockPos pos2 = pos.down().offset(direction);
                double d = Walker.floor(world, pos2);
                Vec3d vec =
                        new Vec3d(
                                (double) pos2.getX() + 0.5,
                                (Double.isNaN(d) ? (double) pos2.getY() : d) + 1.62,
                                (double) pos2.getZ() + 0.5);
                sb.append(" ")
                        .append(direction)
                        .append("[feet=")
                        .append(world.getBlockState(pos2).getBlock().getName().getString())
                        .append(" head=")
                        .append(world.getBlockState(pos2.up()).getBlock().getName().getString())
                        .append(" floor=")
                        .append(d)
                        .append(" wait=")
                        .append(this.scan.waiting.contains(pos2.asLong()))
                        .append(" ok=")
                        .append(this.placement.placeableFrom(cell.expected(), pos, vec))
                        .append("]");
            }
            SchematicBuilderModule.trace(sb.toString());
            this.lastLookUpTrace = this.tick;
            return;
        }
    }

    private void rescan(ClientWorld world, ClientPlayerEntity player) {
        WorldSchematic schematicWorld = this.schematics.getSchematicWorld();
        List<SchematicManager.ActiveBox> list = this.schematics.activeBoxes();
        this.nextScan = this.tick + 20L;
        this.lastScan = this.tick;
        if (this.placement.refusedCount() > this.refusedSeen) {
            this.refusedSeen = this.placement.refusedCount();
            this.feedback(
                    "refused",
                    "The server refused "
                            + this.refusedSeen
                            + " blocks so far (ghost blocks); placing slower and trying them again",
                    200);
        }
        if (this.placement.placedCount() != this.placedAtScan) {
            this.stuckWalks = 0;
        }
        this.placedAtScan = this.placement.placedCount();
        if (schematicWorld == null || list.isEmpty()) {
            this.scan = null;
            this.placing = false;
            this.feedback("no-schematic", "No active Litematica placement", 100);
            return;
        }
        Box box = player.getBoundingBox().expand(0.05);
        this.scan =
                BuildScan.run(
                        world,
                        schematicWorld,
                        list,
                        blockPos ->
                                this.deferred.contains(blockPos.asLong())
                                        || box.intersects(new Box((BlockPos) blockPos))
                                        || SchematicBuilderModule.enclosed(world, blockPos),
                        blockPos -> this.deferred.contains(blockPos.asLong()),
                        this::liquidPending,
                        this.latePhase,
                        this.scaffold,
                        this.craftersDone,
                        this.crafterSlots.get(),
                        this.placeTnt.get(),
                        this.machineLast.get());
        this.debugLookUp(world, schematicWorld);
        this.scaffold.removeIf(
                long_ -> {
                    BlockPos pos = BlockPos.fromLong(long_);
                    if (!world.isChunkLoaded(pos)) {
                        return false;
                    }
                    BlockState blockState = world.getBlockState(pos);
                    return !Scaffold.block(blockState)
                            || !schematicWorld.getBlockState(pos).isAir()
                                    && StateRules.done(
                                            blockState, schematicWorld.getBlockState(pos));
                });
        for (long l : this.scaffold) {
            if (schematicWorld.getBlockState(BlockPos.fromLong(l)).isAir()) continue;
            this.buildScaffold.add(l);
        }
        this.missingKeys.clear();
        for (BuildScan.Cell cell2 : this.scan.missing) {
            this.missingKeys.add(cell2.pos().asLong());
        }
        this.lackingKeys.clear();
        if (!this.buyFromAh.get()) {
            HashMap map = new HashMap();
            for (BuildScan.Cell cell3 : this.scan.missing) {
                if (!SchematicBuilderModule.lacking(player, cell3.expected(), map)) continue;
                this.lackingKeys.add(cell3.pos().asLong());
            }
        }
        this.trackStubborn();
        if (this.scan.layer != Integer.MAX_VALUE && this.scan.layer != this.shownLayer) {
            this.shownLayer = this.scan.layer;
            long l2 =
                    this.scan.missing.stream()
                            .filter(cell -> cell.pos().getY() == this.scan.layer)
                            .count();
            this.feedback(
                    "layer",
                    "Layer Y="
                            + this.scan.layer
                            + ", "
                            + l2
                            + (l2 == 1L ? " block" : " blocks")
                            + " to place",
                    1);
        }
    }

    private void trackStubborn() {
        if (!this.scan.wholeSchematic) {
            int i = this.scan.layer + 1;
            this.tries
                    .keySet()
                    .removeIf(
                            long_ ->
                                    BlockPos.unpackLongY(long_) <= i
                                            && !this.missingKeys.contains(long_));
        } else {
            this.tries.keySet().retainAll(this.missingKeys);
        }
        this.stubborn.clear();
        for (Map.Entry<Long, Integer> entry : this.tries.entrySet()) {
            if (entry.getValue() >= 8) {
                if (!this.deferred.add(entry.getKey())) continue;
                this.feedback(
                        "gave-up-" + String.valueOf(entry.getKey()),
                        "The block at "
                                + BlockPos.fromLong(entry.getKey()).toShortString()
                                + " keeps disappearing, skipping it",
                        1);
                continue;
            }
            if (entry.getValue() < 3) continue;
            this.stubborn.add(entry.getKey());
        }
    }

    private void rescanSoon() {
        this.nextScan = Math.min(this.nextScan, this.tick + 1L);
    }

    private void decide(ClientWorld world, ClientPlayerEntity player) {
        BlockPos pos;
        Object object;
        if (this.armed) {
            this.endOfBuild(world, player);
            return;
        }
        int layer = this.scan.layer;
        WorldSchematic schematicWorld = this.schematics.getSchematicWorld();
        Item item =
                this.routeTarget == null || schematicWorld == null
                        ? null
                        : BlockStateUtil.requiredItem(
                                schematicWorld.getBlockState(this.routeTarget));
        this.routeItemTicks =
                item != null && SchematicBuilderModule.count(player, item) > 0
                        ? this.routeItemTicks + 1
                        : 0;
        int i = layer != Integer.MAX_VALUE ? layer : this.highestMissing();
        int j = i == Integer.MIN_VALUE ? 0 : this.waterWanted(i);
        int k = i == Integer.MIN_VALUE ? 0 : this.cappedLavaWanted(i);
        int m = k;
        if (j > 0 && this.refillWater(world, player, j)) {
            return;
        }
        if (this.buyFromAh.get() && i != Integer.MIN_VALUE) {
            object = this.scan.needs(i);
            if (j > 0) {
                ((java.util.Map) object).put((Item) Items.WATER_BUCKET, j);
            }
            if (k > 0) {
                ((java.util.Map) object).put((Item) Items.LAVA_BUCKET, Math.min(k, 8));
            }
            if (this.buyMissing(player, (Map<Item, Integer>) object)) {
                return;
            }
        }
        if ((pos = this.cleanupLeaf(player)) != null) {
            this.breaker = new Breaker(pos, false);
            this.placing = false;
            return;
        }
        if (this.sweepFinishedLayers(world, player)) {
            return;
        }
        if (this.crafterSlots.get()) {
            for (BlockPos crafter : this.scan.crafters) {
                if (!this.inReach(player, crafter) || Aim.at(this.mc, crafter) == null) continue;
                this.user = BlockUser.crafter(crafter, this.scan.crafterSlots.get(crafter));
                this.placing = false;
                return;
            }
        }
        if (this.clickStates.get()) {
            for (BuildScan.Cell cell : this.scan.adjust) {
                if (this.clickFailed.contains(cell.pos().asLong())
                        || !this.inReach(player, cell.pos())
                        || Aim.at(this.mc, cell.pos()) == null) continue;
                this.user = BlockUser.adjust(cell.pos(), cell.expected());
                this.placing = false;
                return;
            }
        }
        if (this.placeWater.get() && (this.waterNow(player) || this.cappedLavaNow(world, player))) {
            return;
        }
        if (i == Integer.MIN_VALUE) {
            this.endOfBuild(world, player);
            return;
        }
        object = player.getEyePos();
        final Vec3d vec = (Vec3d) object;
        Set<Long> waiting = this.scan.waiting;
        Set<Long> set = SchematicBuilderModule.lastExit(world, player);
        this.placement.setScope(
                new BuildScope(
                        i, 40.0, scopePos -> this.inBuildScope(waiting, set, vec, scopePos)));
        if (!this.placing) {
            this.placing = true;
            this.placingSince = this.tick;
        }
        if (this.tick - this.placingSince < 4L || this.placement.ticksWithoutTarget() < 4L) {
            return;
        }
        if (this.supportLeaves.get() && this.placeSupport(world, player, i, true)) {
            return;
        }
        if (this.supportLeaves.get()
                && this.routeTarget != null
                && this.tick - this.arrivedAt < 34L
                && this.placeSupportFor(world, player, this.routeTarget)) {
            return;
        }
        if (!this.walking && this.tick >= this.noPlanUntil && this.plan(world, player, i)) {
            return;
        }
        int n = player.getBlockPos().getY();
        if (this.walking
                && layer != Integer.MAX_VALUE
                && layer >= n + 2
                && this.goUp(world, player)) {
            return;
        }
        if (this.walking
                && layer != Integer.MAX_VALUE
                && layer >= n
                && this.stuckWalks >= 2
                && this.goUp(world, player)) {
            this.stuckWalks = 0;
            return;
        }
        if (this.walkTowardsWork(world, player, i)) {
            return;
        }
        if (this.supportLeaves.get() && this.placeSupport(world, player, i, false)) {
            return;
        }
        if (this.walking && layer != Integer.MAX_VALUE && layer >= n && this.goUp(world, player)) {
            return;
        }
        if (layer == Integer.MAX_VALUE) {
            this.endOfBuild(world, player);
            return;
        }
        if (this.tick - this.lastStuckTick < 60L) {
            return;
        }
        this.lastStuckTick = this.tick;
        BuildScan.Cell cell2 = this.nearestOpen(player, i);
        if (cell2 != null) {
            SchematicBuilderModule.trace(
                    "stuck, skipping "
                            + cell2.pos().toShortString()
                            + " at "
                            + player.getBlockPos().toShortString());
            this.deferred.add(cell2.pos().asLong());
        }
        this.feedback(
                "stuck",
                "Can't reach the rest of layer Y=" + layer + " from here, skipping it for now",
                100);
    }

    private static Set<Long> lastExit(ClientWorld world, ClientPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        List<BlockPos> list = Walker.steps(world, pos);
        if (list.size() != 1) {
            return Set.of();
        }
        BlockPos pos2 = list.get(0);
        HashSet<Long> set = new HashSet<Long>();
        for (int i = 0; i <= 2; ++i) {
            set.add(pos2.up(i).asLong());
        }
        if (pos2.getY() > pos.getY()) {
            set.add(pos.up(2).asLong());
        }
        set.add(
                pos.offset(
                                Direction.getFacing(
                                        pos2.getX() - pos.getX(), 0.0f, pos2.getZ() - pos.getZ()))
                        .up()
                        .asLong());
        return set;
    }

    private int highestMissing() {
        int mIN_VALUE = Integer.MIN_VALUE;
        for (BuildScan.Cell cell : this.scan.missing) {
            if (this.scan.waiting.contains(cell.pos().asLong())) continue;
            mIN_VALUE = Math.max(mIN_VALUE, cell.pos().getY());
        }
        return mIN_VALUE;
    }

    private boolean goUp(ClientWorld world, ClientPlayerEntity player) {
        BlockPos pos = Hop.spot(world, player, blockPos -> false);
        if (pos != null) {
            this.stopMoving();
            this.hop = new Hop(pos);
            return true;
        }
        return this.startClimb(player);
    }

    private boolean placeSupport(ClientWorld world, ClientPlayerEntity player, int i, boolean bl) {
        for (BuildScan.Cell cell : this.scan.missing) {
            BlockPos pos = cell.pos();
            if (pos.getY() > i
                    || this.deferred.contains(pos.asLong())
                    || this.scan.waiting.contains(pos.asLong())
                    || !this.inReach(player, pos)
                    || cell.expected().getBlock() instanceof FallingBlock
                    || StateRules.lookFor(cell.expected()) == Direction.UP
                            && !pos.equals(this.routeTarget)
                    || bl
                            && (this.missingNeighbour(pos)
                                    || SchematicBuilderModule.bridgeable(
                                            world, cell.expected(), pos))
                    || !this.placeSupportFor(world, player, pos)) continue;
            return true;
        }
        return false;
    }

    private static boolean bridgeable(ClientWorld world, BlockState state, BlockPos pos) {
        if (StateRules.lookFor(state) != null
                || StateRules.flatLookFor(state) != null
                || state.getBlock() instanceof FallingBlock
                || !state.isFullCube(world, pos)) {
            return false;
        }
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos pos2 = pos.offset(direction);
            if (!Block.isFaceFullSquare(
                            world.getBlockState(pos2).getOutlineShape(world, pos2),
                            direction.getOpposite())
                    || !Walker.roomAt(world, pos2.up())) continue;
            return true;
        }
        return false;
    }

    private boolean placeSupportFor(ClientWorld world, ClientPlayerEntity player, BlockPos pos) {
        if (this.latePhase && this.placeTnt.get()
                || !this.missingKeys.contains(pos.asLong())
                || this.scan.waiting.contains(pos.asLong())) {
            return false;
        }
        WorldSchematic schematicWorld = this.schematics.getSchematicWorld();
        if (schematicWorld == null) {
            return false;
        }
        BlockState blockState = schematicWorld.getBlockState(pos);
        if (blockState.getBlock() instanceof FallingBlock
                || this.placement.placeableFrom(blockState, pos, player.getEyePos())) {
            return false;
        }
        List<BlockPos> list = this.scaffoldFor(world, blockState, pos, player.getEyePos());
        if (list == null) {
            return false;
        }
        BlockPos pos2 = list.get(0);
        if (!CellFiller.reachable(this.mc, pos2, CellFiller.Kind.SUPPORT_LEAF)) {
            return false;
        }
        if (Scaffold.count(player) < 1) {
            if (this.buyFromAh.get()) {
                return this.buyLeaves();
            }
            this.feedback("no-scaffold", "Need glass (or leaves) to prop blocks up", 200);
            return false;
        }
        SchematicBuilderModule.trace(
                "scaffold "
                        + pos2.toShortString()
                        + " ("
                        + list.size()
                        + " to go) for "
                        + pos.toShortString());
        this.stopMoving();
        this.supportFor.put(pos2.asLong(), pos.asLong());
        if (pos.equals(this.routeTarget)) {
            this.proppedAt = this.tick;
        }
        if (!schematicWorld.getBlockState(pos2).isAir()) {
            this.buildScaffold.add(pos2.asLong());
        }
        this.filler = new CellFiller(pos2, CellFiller.Kind.SUPPORT_LEAF);
        return true;
    }

    private List<BlockPos> scaffoldFor(
            ClientWorld world, BlockState state, BlockPos pos, Vec3d vec) {
        WorldSchematic schematicWorld = this.schematics.getSchematicWorld();
        if (schematicWorld == null) {
            return null;
        }
        double d = (this.mc.player == null ? 4.5 : this.mc.player.getBlockInteractionRange()) - 0.6;
        Direction direction = StateRules.lookFor(state);
        Direction direction2 = StateRules.flatLookFor(state);
        Box box =
                new Box(
                        vec.x - 0.3,
                        vec.y - 1.62,
                        vec.z - 0.3,
                        vec.x + 0.3,
                        vec.y + 0.18,
                        vec.z + 0.3);
        for (int i = 0; i < 2; ++i) {
            for (BlockPos pos2 : BuildOrder.supportCells(state, pos)) {
                List<BlockPos> list;
                boolean bl =
                        i == 0
                                ? SchematicBuilderModule.scaffoldFits(world, schematicWorld, pos2)
                                : this.buildCellFits(world, schematicWorld, pos2);
                if (!bl
                        || this.badScaffold.contains(pos2.asLong())
                        || box.intersects(new Box(pos2))) continue;
                Direction facing =
                        Direction.getFacing(
                                pos.getX() - pos2.getX(),
                                pos.getY() - pos2.getY(),
                                pos.getZ() - pos2.getZ());
                Vec3d vec2 = pos2.toCenterPos().offset(facing, 0.5);
                Vec3d vec3 = vec2.subtract(vec);
                boolean bl2 = direction == Direction.UP && pos2.equals(pos.up());
                boolean bl3 = bl2;
                if (bl2
                        ? vec.y <= (double) pos2.getY() + 0.05
                                || pos2.toCenterPos().distanceTo(vec) > d + 0.5
                        : vec3.length() > d
                                || !PlacementUtil.isFaceVisibleToEye(pos2, facing, vec)
                                || direction != null && !Walker.dominant(vec3, direction)
                                || direction2 != null && !Walker.facingFlat(vec3, direction2))
                    continue;
                ArrayList<BlockPos> list2 = new ArrayList<BlockPos>();
                BlockPos pos3 = pos2;
                boolean bl4 = false;
                for (int j = 0;
                        j < 4
                                && (j <= 0
                                        || SchematicBuilderModule.scaffoldFits(
                                                        world, schematicWorld, pos3)
                                                && !box.intersects(new Box(pos3)))
                                && !(pos3.toCenterPos().distanceTo(vec) > d + 0.5);
                        ++j) {
                    list2.add(pos3);
                    if (j == 0 && SchematicBuilderModule.sideBase(world, pos3, pos, vec)
                            || SchematicBuilderModule.solidUnder(world, pos3)
                                    && vec.y > (double) pos3.getY() + 0.05) {
                        bl4 = true;
                        break;
                    }
                    pos3 = pos3.down();
                }
                if (!bl4
                        && list2.size() >= 1
                        && SchematicBuilderModule.scaffoldFits(world, schematicWorld, pos2)
                        && (list = this.besideChain(world, schematicWorld, pos2, pos, vec, box, d))
                                != null) {
                    return list;
                }
                if (!bl4 || list2.size() > 1 && vec.y <= (double) pos2.getY() + 0.05) continue;
                Collections.reverse(list2);
                if (Aim.against(this.mc, (BlockPos) list2.get(0), null, vec) == null) continue;
                return list2;
            }
        }
        return null;
    }

    private List<BlockPos> besideChain(
            ClientWorld world,
            WorldSchematic worldSchematic,
            BlockPos pos,
            BlockPos pos2,
            Vec3d vec,
            Box box,
            double d) {
        if (vec.y <= (double) pos.getY() + 0.05) {
            return null;
        }
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos pos3 = pos.offset(direction);
            if (pos3.equals(pos2)
                    || !SchematicBuilderModule.scaffoldFits(world, worldSchematic, pos3)
                    || !SchematicBuilderModule.solidUnder(world, pos3)
                    || box.intersects(new Box(pos3))
                    || pos3.toCenterPos().distanceTo(vec) > d + 0.5
                    || this.badScaffold.contains(pos3.asLong())
                    || Aim.against(this.mc, pos3, null, vec) == null) continue;
            return new ArrayList<BlockPos>(List.of(pos3, pos));
        }
        return null;
    }

    private boolean buildCellFits(ClientWorld world, WorldSchematic worldSchematic, BlockPos pos) {
        if (!this.missingKeys.contains(pos.asLong())
                || !world.getBlockState(pos).isAir()
                || SchematicBuilderModule.waterCell(world, worldSchematic, pos)) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            BlockPos pos2 = pos.offset(direction);
            BlockState blockState = worldSchematic.getBlockState(pos2);
            if (!(blockState.getBlock() instanceof ObserverBlock)
                    || !pos2.offset(blockState.get(Properties.FACING)).equals(pos)) continue;
            return false;
        }
        return true;
    }

    private static boolean sideBase(ClientWorld world, BlockPos pos, BlockPos pos2, Vec3d vec) {
        for (Direction direction : Direction.values()) {
            BlockState blockState;
            BlockPos pos3 = pos.offset(direction);
            if (pos3.equals(pos2)
                    || (blockState = world.getBlockState(pos3)).isReplaceable()
                    || blockState.getOutlineShape(world, pos3).isEmpty()
                    || !PlacementUtil.isFaceVisibleToEye(pos3, direction.getOpposite(), vec))
                continue;
            return true;
        }
        return false;
    }

    private static boolean solidUnder(ClientWorld world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos.down());
        return !blockState.isReplaceable()
                && !blockState.getOutlineShape(world, pos.down()).isEmpty();
    }

    private boolean neededNow(BlockPos pos) {
        int m =
                this.scan == null || this.scan.layer == Integer.MAX_VALUE
                        ? Integer.MAX_VALUE
                        : this.scan.layer;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int i = -1; i <= 1; ++i) {
            if (pos.getY() + i > m) continue;
            for (int j = -1; j <= 1; ++j) {
                for (int k = -1; k <= 1; ++k) {
                    long l = mutable.set(pos.getX() + j, pos.getY() + i, pos.getZ() + k).asLong();
                    if (!this.missingKeys.contains(l)
                            || this.deferred.contains(l)
                            || this.scan.waiting.contains(l)
                            || this.lackingKeys.contains(l)) continue;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean missingNeighbour(BlockPos pos) {
        for (Direction direction : Direction.values()) {
            long l = pos.offset(direction).asLong();
            if (!this.missingKeys.contains(l)
                    || this.deferred.contains(l)
                    || this.scan.waiting.contains(l)
                    || this.lackingKeys.contains(l)) continue;
            return true;
        }
        return false;
    }

    private boolean pillarFits(ClientWorld world, BlockPos pos) {
        Long badClimb = this.badClimbs.get(pos.asLong());
        if (badClimb != null && badClimb > this.tick) {
            return false;
        }
        if (world.getFluidState(pos).getHeight(world, pos) > 0.4f) {
            return false;
        }
        WorldSchematic schematicWorld = this.schematics.getSchematicWorld();
        for (int i = 0; i < 2; ++i) {
            BlockPos pos2 = pos.up(i);
            if (SchematicBuilderModule.scaffoldFits(world, schematicWorld, pos2)
                    || SchematicBuilderModule.runningWaterFits(world, schematicWorld, pos2))
                continue;
            return false;
        }
        return Climber.hasRoom(this.mc, pos);
    }

    private boolean digOut(ClientWorld world, ClientPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        BlockState blockState = world.getBlockState(pos);
        if (blockState.isAir()
                || blockState.isReplaceable()
                || blockState.isFullCube(world, pos)
                || world.getBlockEntity(pos) instanceof Inventory
                || blockState.getHardness(world, pos) < 0.0f) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            if (world.getFluidState(pos.offset(direction)).isEmpty()) continue;
            return false;
        }
        SchematicBuilderModule.trace(
                "shut in on "
                        + String.valueOf(blockState)
                        + " at "
                        + pos.toShortString()
                        + ", mining it to pillar out");
        this.feedback(
                "dig-out",
                "Shut in at "
                        + pos.toShortString()
                        + ", mining the block underfoot to climb out (it goes back in later)",
                100);
        this.stopMoving();
        this.breaker = new Breaker(pos, false);
        return true;
    }

    private boolean escapeClimb(ClientWorld world) {
        BlockPos pos;
        int i;
        BlockPos pos2 = Climber.column(this.mc);
        WorldSchematic schematicWorld = this.schematics.getSchematicWorld();
        if (pos2 == null || schematicWorld == null || !Climber.hasRoom(this.mc, pos2)) {
            return false;
        }
        for (i = 0; i < 2; ++i) {
            pos = pos2.up(i);
            if (world.getBlockState(pos).isReplaceable()
                    && (world.getFluidState(pos).isEmpty()
                            || SchematicBuilderModule.runningWaterFits(world, schematicWorld, pos))
                    && !(schematicWorld.getBlockState(pos).getBlock() instanceof FluidBlock))
                continue;
            return false;
        }
        for (i = 0; i < 2; ++i) {
            pos = pos2.up(i);
            if (schematicWorld.getBlockState(pos).isAir()) continue;
            this.buildScaffold.add(pos.asLong());
        }
        SchematicBuilderModule.trace("shut in at " + pos2.toShortString() + ", pillaring out");
        this.stopMoving();
        this.climber = new Climber(pos2, this.scaffold);
        return true;
    }

    private static boolean runningWaterFits(
            ClientWorld world, WorldSchematic worldSchematic, BlockPos pos) {
        FluidState fluidState = world.getFluidState(pos);
        if (fluidState.isEmpty()
                || fluidState.isStill()
                || !world.getBlockState(pos).isReplaceable()) {
            return false;
        }
        BlockState state =
                worldSchematic == null
                        ? Blocks.AIR.getDefaultState()
                        : worldSchematic.getBlockState(pos);
        return state.isAir()
                || state.getBlock() instanceof FluidBlock && !state.getFluidState().isStill();
    }

    static boolean scaffoldFits(ClientWorld world, WorldSchematic worldSchematic, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        if (!blockState.isReplaceable()
                || SchematicBuilderModule.waterCell(world, worldSchematic, pos)) {
            return false;
        }
        if (worldSchematic != null && !worldSchematic.getBlockState(pos).isAir()) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            BlockPos pos2 = pos.offset(direction);
            BlockState state =
                    worldSchematic != null
                            ? worldSchematic.getBlockState(pos2)
                            : world.getBlockState(pos2);
            BlockState state2 = state;
            if (!(state.getBlock() instanceof ObserverBlock)
                    || !pos2.offset(state.get(Properties.FACING)).equals(pos)) continue;
            return false;
        }
        return true;
    }

    static boolean waterCell(ClientWorld world, WorldSchematic worldSchematic, BlockPos pos) {
        return !world.getFluidState(pos).isEmpty()
                || worldSchematic != null
                        && !worldSchematic.getBlockState(pos).getFluidState().isEmpty();
    }

    private static boolean floating(ClientWorld world, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (world.getBlockState(pos.offset(direction)).isReplaceable()) continue;
            return false;
        }
        return true;
    }

    private boolean buyMissing(ClientPlayerEntity player, Map<Item, Integer> map) {
        if (this.tick - this.lastPlaceTick < 40L) {
            return false;
        }
        ArrayList<Map.Entry<Item, Integer>> list =
                new ArrayList<Map.Entry<Item, Integer>>(map.entrySet());
        list.sort(
                Comparator.comparing(
                        entry -> Registries.ITEM.getId((Item) entry.getKey()).toString()));
        for (Map.Entry<Item, Integer> entry2 : list) {
            Item key = entry2.getKey();
            int i = entry2.getValue() - SchematicBuilderModule.count(player, key);
            if (i <= 0) continue;
            int room = SchematicBuilderModule.room(player, new ItemStack(key));
            if (key == Items.WATER_BUCKET) {
                room = Math.min(room - 4, 8);
            }
            if (room <= 0) {
                if (!this.startSelling(player, false)) continue;
                return true;
            }
            return this.startPurchase(
                    key, Math.min(SchematicBuilderModule.buyAmount(key, i), room));
        }
        return false;
    }

    private static int buyAmount(Item item, int i) {
        int maxCount = new ItemStack(item).getMaxCount();
        if (i <= 16 || maxCount <= 16) {
            return i;
        }
        return (i + maxCount - 1) / maxCount * maxCount;
    }

    private boolean startPurchase(Item purchaseItem2, int i) {
        long remainingBudget = this.remainingBudget();
        if (remainingBudget < 0L) {
            this.say(ModuleChat.body().warn("Max Total Spend reached").text(", stopping."));
            this.requestDisable();
            return true;
        }
        this.stopMoving();
        this.purchaseItem = purchaseItem2;
        this.purchaseNeed =
                this.mc.player == null
                        ? i
                        : SchematicBuilderModule.count(this.mc.player, purchaseItem2) + i;
        this.purchaseName = new ItemStack(purchaseItem2).getName().getString();
        this.purchase =
                purchaseItem2 == Items.GLASS
                        ? AhPurchase.startAny(
                                this,
                                Items.GLASS,
                                "Glass",
                                GLASS_SEARCH,
                                itemStack -> itemStack.isOf(Items.GLASS),
                                i,
                                this.maxListing,
                                this.refresh.getInt(),
                                remainingBudget)
                        : AhPurchase.start(
                                this,
                                purchaseItem2,
                                i,
                                this.maxListing,
                                this.refresh.getInt(),
                                remainingBudget);
        return true;
    }

    private boolean buyLeaves() {
        ClientPlayerEntity player = this.mc.player;
        if (player == null
                || SchematicBuilderModule.room(player, new ItemStack(Items.GLASS)) <= 0
                        && !this.startSelling(player, false)) {
            this.feedback("leaves-room", "No free inventory slot for leaves", 200);
            return false;
        }
        if (this.sell != null) {
            return true;
        }
        long remainingBudget = this.remainingBudget();
        if (remainingBudget < 0L) {
            this.say(ModuleChat.body().warn("Max Total Spend reached").text(", stopping."));
            this.requestDisable();
            return true;
        }
        this.stopMoving();
        this.purchaseItem = null;
        this.purchaseName = "Leaves";
        this.purchase =
                AhPurchase.startAny(
                        this,
                        Items.GLASS,
                        "Glass",
                        GLASS_SEARCH,
                        Scaffold::glass,
                        16,
                        this.maxListing,
                        this.refresh.getInt(),
                        remainingBudget);
        return true;
    }

    private long remainingBudget() {
        if (this.spendLimit <= 0L) {
            return 0L;
        }
        long l = this.spendLimit - this.spent;
        return l > 0L ? l : -1L;
    }

    private void followPurchase() {
        AhPurchase.Status status = this.purchase.status();
        if (status == AhPurchase.Status.RUNNING) {
            return;
        }
        this.spent += this.purchase.spent();
        if (this.purchaseItem != null) {
            this.bought.merge(this.purchaseItem, this.purchase.boughtItems(), Integer::sum);
        } else {
            this.leavesBought += this.purchase.boughtItems();
        }
        this.purchase = null;
        this.rescanSoon();
        if (status == AhPurchase.Status.FAILED) {
            this.say(
                    ModuleChat.body()
                            .danger("Could not buy " + this.purchaseName)
                            .text(", Schematic Builder stopped."));
            this.requestDisable();
        }
    }

    private boolean startClimb(ClientPlayerEntity player) {
        if (!this.climb.get()) {
            this.feedback(
                    "climb-off",
                    "The next layer is out of reach; turn on Climb With Leaves to go up",
                    200);
            return false;
        }
        if (Climber.leavesCount(player) < 2) {
            if (this.buyFromAh.get()) {
                return this.buyLeaves();
            }
            this.feedback("no-leaves", "Need at least 2 leaves to climb", 200);
            return false;
        }
        if (this.tick < this.noClimbUntil) {
            return false;
        }
        BlockPos pos = Climber.column(this.mc);
        if (pos == null || !this.pillarFits(this.mc.world, pos)) {
            this.feedback(
                    "climb-room",
                    "No room to pillar here (solid ground, no water, no placed block in the way)",
                    200);
            return false;
        }
        this.stopMoving();
        this.placing = false;
        this.climber = new Climber(pos, this.scaffold);
        this.feedback("climbing", "Climbing 2 blocks with leaves", 20);
        return true;
    }

    private boolean walkTowardsWork(ClientWorld world, ClientPlayerEntity player, int i) {
        ArrayList<BuildScan.Cell> list = new ArrayList<BuildScan.Cell>();
        for (BuildScan.Cell cell2 : this.scan.missing) {
            long l = cell2.pos().asLong();
            if (cell2.pos().getY() > i
                    || this.deferred.contains(l)
                    || this.scan.waiting.contains(l)
                    || SchematicBuilderModule.enclosed(world, cell2.pos())) continue;
            list.add(cell2);
        }
        list.sort(
                Comparator.comparingDouble(
                        cell -> cell.pos().getSquaredDistance(player.getEntityPos())));
        BlockPos pos = player.getBlockPos();
        for (int j = 0; j < list.size() && j < 16; ++j) {
            BuildScan.Cell cell3 = (BuildScan.Cell) list.get(j);
            if (!this.walking) {
                return false;
            }
            boolean bl = this.stubborn.contains(cell3.pos().asLong());
            BlockPos pos2 =
                    bl
                            ? Walker.standFor(
                                    world,
                                    player,
                                    cell3.pos(),
                                    cell3.expected(),
                                    this::badStand,
                                    2.2)
                            : Walker.standFor(
                                    world, player, cell3.pos(), cell3.expected(), this::badStand);
            BlockPos pos3 = pos2;
            if (pos2 == null || pos2.equals(pos)) {
                if (this.walkFails.merge(cell3.pos().asLong(), 1, Integer::sum) < 2) continue;
                this.deferred.add(cell3.pos().asLong());
                continue;
            }
            this.walkTarget = cell3.pos();
            this.walkTo(player, pos2);
            return true;
        }
        return false;
    }

    private boolean badStand(BlockPos pos) {
        Long badStand = this.badStands.get(pos.asLong());
        if (badStand == null) {
            return false;
        }
        if (badStand > this.tick) {
            return true;
        }
        this.badStands.remove(pos.asLong());
        return false;
    }

    private boolean plan(ClientWorld world, ClientPlayerEntity player, int i) {
        // (decompiler reused one slot for several types; split into typed locals)
        HashMap<Object, Integer> map;
        ArrayList<StandPlanner.Target> list;
        this.missedRouteTarget(player);
        if (this.climbTo != Integer.MIN_VALUE) {
            var value =
                    player.getBlockPos().getY() < this.climbTo
                                    && Climber.leavesCount(player) >= 2
                                    && this.tick >= this.noClimbUntil
                            ? Climber.column(this.mc)
                            : null;
            if (value != null && this.pillarFits(world, value)) {
                this.stopMoving();
                this.climber = new Climber(value, this.scaffold);
                return true;
            }
            this.climbTo = Integer.MIN_VALUE;
        }
        list = new ArrayList<StandPlanner.Target>();
        map = new HashMap<Object, Integer>();
        for (BuildScan.Cell cell : this.scan.missing) {
            Object object;
            if (cell.pos().getY() > i
                    || this.deferred.contains(cell.pos().asLong())
                    || this.scan.waiting.contains(cell.pos().asLong())
                    || (object = BlockStateUtil.requiredItem(cell.expected())) == null) continue;
            if (SchematicBuilderModule.count(player, (Item) object) <= 0) {
                map.merge(object, 1, Integer::sum);
                continue;
            }
            list.add(
                    new StandPlanner.Target(
                            cell.pos(),
                            cell.expected(),
                            StandPlanner.Use.PLACE,
                            this.stubborn.contains(cell.pos().asLong())
                                    ? 2.5999999999999996
                                    : Double.MAX_VALUE));
        }
        if (Hands.has(player, itemStack -> itemStack.isOf(Items.WATER_BUCKET))) {
            for (BuildScan.Liquid liquid : this.scan.liquids) {
                if (liquid.kind() == CellFiller.Kind.LAVA_SOURCE
                        || !this.dueNow(liquid)
                        || !this.waterPending(liquid.pos())) continue;
                list.add(SchematicBuilderModule.liquidTarget(world, liquid));
            }
        }
        if (Hands.has(player, itemStack -> itemStack.isOf(Items.LAVA_BUCKET))
                && !this.lavaGivenUp) {
            var schematicWorld = this.schematics.getSchematicWorld();
            ArrayList<StandPlanner.Target> list2 = new ArrayList<StandPlanner.Target>();
            for (BuildScan.Liquid liquid2 : this.scan.liquids) {
                if (!liquid2.capped()
                        || this.scan.layer != Integer.MAX_VALUE
                                && liquid2.pos().getY() >= this.scan.layer
                        || !this.liquidPending(liquid2.pos())
                        || schematicWorld == null
                        || !SchematicBuilderModule.walledIn(world, schematicWorld, liquid2.pos()))
                    continue;
                list2.add(SchematicBuilderModule.liquidTarget(world, liquid2));
            }
            if (!list2.isEmpty() && this.tick >= this.lavaFirstUntil) {
                this.lavaFirstUntil = this.tick + 200L;
                this.lavaPlanning = true;
                this.startPlanner(world, player, list2);
                return true;
            }
            list.addAll(list2);
        }
        if (!map.isEmpty() && !this.buyFromAh.get()) {
            ArrayList<String> list3 = new ArrayList<>();
            map.forEach(
                    (item, integer) ->
                            list3.add(
                                    integer
                                            + "x "
                                            + new ItemStack((ItemConvertible) item)
                                                    .getName()
                                                    .getString()));
            this.feedback(
                    "lacking",
                    "Missing in inventory: "
                            + String.join(
                                    (CharSequence) ", ",
                                    list3.subList(0, Math.min(6, list3.size())))
                            + (list3.size() > 6 ? " and more" : "")
                            + " (Buy From AH is off)",
                    400);
        }
        if (list.isEmpty()) {
            return false;
        }
        this.startPlanner(world, player, list);
        return true;
    }

    private void missedRouteTarget(ClientPlayerEntity player) {
        if (this.routeTarget != null && this.tick - this.arrivedAt < 34L) {
            long l = this.routeTarget.asLong();
            if (this.missingKeys.contains(l)
                    && this.placement.placedCount() > this.placedAtArrival) {
                SchematicBuilderModule.trace(
                        "placed others instead of " + this.routeTarget.toShortString());
            } else if (this.missingKeys.contains(l) && this.proppedAt >= this.arrivedAt) {
                SchematicBuilderModule.trace(
                        "propped " + this.routeTarget.toShortString() + ", placing it comes next");
            } else if (this.missingKeys.contains(l) && this.routeItemTicks < 4) {
                SchematicBuilderModule.trace(
                        "arrived without the item for " + this.routeTarget.toShortString());
            } else if (this.missingKeys.contains(l)) {
                WorldSchematic schematicWorld = this.schematics.getSchematicWorld();
                boolean bl =
                        SchematicBuilderModule.tracing()
                                && schematicWorld != null
                                && this.placement.placeableFrom(
                                        schematicWorld.getBlockState(this.routeTarget),
                                        this.routeTarget,
                                        player.getEyePos());
                boolean bl2 = bl;
                if (SchematicBuilderModule.tracing()) {
                    SchematicBuilderModule.trace(
                            "arrived but did not place "
                                    + this.routeTarget.toShortString()
                                    + " standing at "
                                    + player.getBlockPos().toShortString()
                                    + " planned "
                                    + (this.routeStand == null
                                            ? "?"
                                            : this.routeStand.toShortString())
                                    + " pos="
                                    + String.valueOf(player.getEntityPos())
                                    + " exactNow="
                                    + bl
                                    + " waiting="
                                    + this.scan.waiting.contains(l)
                                    + " stubborn="
                                    + this.stubborn.contains(l)
                                    + " "
                                    + this.placement.debugState(this.routeTarget));
                }
                if (this.walkFails.merge(l, 1, Integer::sum) >= 2) {
                    this.deferred.add(l);
                }
            } else if (this.routeUse == StandPlanner.Use.BREAK && this.scaffold.contains(l)) {
                SchematicBuilderModule.trace(
                        "arrived but did not mine the leaf at " + this.routeTarget.toShortString());
                this.walkFails.merge(l, 1, Integer::sum);
            } else if ((this.routeUse == StandPlanner.Use.POUR
                            || this.routeUse == StandPlanner.Use.WATERLOG)
                    && this.scan.liquids.stream().anyMatch(liquid -> liquid.pos().asLong() == l)) {
                SchematicBuilderModule.trace(
                        "arrived but could not pour into " + this.routeTarget.toShortString());
                this.waterFails.merge(l, 1, Integer::sum);
            }
        }
        this.routeTarget = null;
    }

    private static StandPlanner.Target liquidTarget(ClientWorld world, BuildScan.Liquid liquid) {
        return new StandPlanner.Target(
                liquid.pos(),
                world.getBlockState(liquid.pos()),
                liquid.kind() == CellFiller.Kind.WATERLOG
                        ? StandPlanner.Use.WATERLOG
                        : StandPlanner.Use.POUR);
    }

    private void startPlanner(
            final ClientWorld world, ClientPlayerEntity player, List<StandPlanner.Target> list) {
        Vec3d entityPos = player.getEntityPos();
        list.sort(
                Comparator.comparingInt((StandPlanner.Target target2) -> target2.pos().getY())
                        .thenComparingDouble(target -> target.pos().getSquaredDistance(entityPos)));
        if (list.size() > 600) {
            list = new ArrayList<StandPlanner.Target>(list.subList(0, 600));
        }
        this.plannedKeys.clear();
        for (StandPlanner.Target target3 : list) {
            this.plannedKeys.add(target3.pos().asLong());
        }
        final WorldSchematic schematicWorld = this.schematics.getSchematicWorld();
        boolean bl = !this.latePhase || !this.placeTnt.get();
        boolean bl2 =
                this.climb.get()
                        && bl
                        && Climber.leavesCount(player) >= 2
                        && this.tick >= this.noClimbUntil;
        boolean bl3 =
                this.climb.get() && bl && !this.sweepPlanning && Climber.leavesCount(player) >= 1;
        SchematicBuilderModule.trace(
                "plan from "
                        + player.getBlockPos().toShortString()
                        + " targets="
                        + list.size()
                        + " first="
                        + list.get(0).pos().toShortString()
                        + " pillars="
                        + bl2
                        + " leaves="
                        + Climber.leavesCount(player));
        this.planner =
                new StandPlanner(
                                world,
                                player,
                                list,
                                blockPos ->
                                        this.badStand((BlockPos) blockPos)
                                                || this.scaffold.contains(blockPos.down().asLong())
                                                        && !SchematicBuilderModule.supports(
                                                                player, blockPos.down())
                                                        && !this.walkway(blockPos.down()),
                                bl2
                                        ? (blockPos, value) ->
                                                (value
                                                                || Walker.floor(world, blockPos)
                                                                        == (double) blockPos.getY())
                                                        && this.pillarFits(world, blockPos)
                                        : null,
                                bl3
                                        ? blockPos ->
                                                SchematicBuilderModule.scaffoldFits(
                                                                world, schematicWorld, blockPos)
                                                        && !this.scan.waiting.contains(
                                                                blockPos.asLong())
                                        : null,
                                this.placement::placeableFrom,
                                new StandPlanner.Support() {

                                    @Override
                                    public boolean possible(BlockPos pos, Vec3d vec2) {
                                        return schematicWorld != null
                                                && SchematicBuilderModule.this.scaffoldFor(
                                                                world,
                                                                schematicWorld.getBlockState(pos),
                                                                pos,
                                                                vec2)
                                                        != null;
                                    }

                                    @Override
                                    public boolean behind(
                                            BlockState state, BlockPos pos2, Vec3d vec3) {
                                        return SchematicBuilderModule.this.scaffoldFor(
                                                        world, state, pos2, vec3)
                                                != null;
                                    }
                                })
                        .falls(
                                !this.fallsNext
                                        ? 0.0
                                        : (player.getHealth() >= 18.0f
                                                ? 8.0
                                                : (player.getHealth() >= 15.0f ? 5.0 : 0.0)));
        this.planWithFalls = this.fallsNext;
        this.fallsNext = false;
        this.placing = false;
        this.followPlanner();
    }

    private int flatRunAhead(BlockPos pos) {
        StandPlanner.Step step;
        int j = 0;
        for (int i = this.routeIndex;
                i < this.route.size()
                        && (step = this.route.get(i)).move() == StandPlanner.Move.WALK
                        && step.pos().getY() == pos.getY();
                ++i) {
            ++j;
        }
        return j;
    }

    private void followPlanner() {
        BlockPos pos;
        StandPlanner.Status status = this.planner.step(System.nanoTime() + 3000000L);
        if (status == StandPlanner.Status.RUNNING) {
            return;
        }
        StandPlanner planner2 = this.planner;
        this.planner = null;
        if (status == StandPlanner.Status.NONE
                && planner2.visited() <= 2
                && this.mc.world != null
                && this.mc.player != null
                && SchematicBuilderModule.tracing()) {
            SchematicBuilderModule.trace(
                    "boxed in: "
                            + Walker.explain(this.mc.world, this.mc.player.getBlockPos())
                            + " pos "
                            + String.valueOf(this.mc.player.getEntityPos())
                            + " why "
                            + planner2.whyStuck());
        }
        if (status == StandPlanner.Status.NONE
                && planner2.visited() > 2
                && SchematicBuilderModule.tracing()) {
            SchematicBuilderModule.trace("plan NONE extent " + planner2.extent());
        }
        SchematicBuilderModule.trace(
                "plan "
                        + String.valueOf((Object) status)
                        + " visited="
                        + planner2.visited()
                        + (String)
                                (status == StandPlanner.Status.FOUND
                                        ? " target="
                                                + planner2.target().pos().toShortString()
                                                + " route="
                                                + String.valueOf(planner2.route())
                                        : ""));
        if (status == StandPlanner.Status.APPROACH && !planner2.route().isEmpty()) {
            SchematicBuilderModule.trace("approach " + String.valueOf(planner2.route()));
            if (this.lavaPlanning) {
                this.lavaFirstUntil = 0L;
            }
            this.lavaPlanning = false;
            this.sweepPlanning = false;
            this.route = planner2.route();
            this.routeIndex = 0;
            this.routeStart = this.mc.player == null ? null : this.mc.player.getBlockPos();
            this.routeTarget = null;
            this.routeUse = null;
            this.routeStand = this.route.get(this.route.size() - 1).pos();
            return;
        }
        if (this.lavaPlanning && status == StandPlanner.Status.FOUND) {
            this.lavaFirstUntil = 0L;
        }
        boolean lavaPlanning2 = this.lavaPlanning;
        this.lavaPlanning = false;
        if (lavaPlanning2 && status == StandPlanner.Status.NONE) {
            this.noPlanUntil = this.tick;
            return;
        }
        if (status == StandPlanner.Status.NONE
                && !this.planWithFalls
                && !this.sweepPlanning
                && this.mc.player != null
                && this.mc.player.getHealth() >= 15.0f) {
            this.fallsNext = true;
            this.noPlanUntil = this.tick;
            return;
        }
        if (this.sweepPlanning && status == StandPlanner.Status.NONE) {
            this.sweepDue = false;
        }
        if (!this.sweepPlanning && status == StandPlanner.Status.NONE) {
            this.deferUnplanned();
        }
        this.sweepPlanning = false;
        if (status == StandPlanner.Status.FOUND && !planner2.route().isEmpty()) {
            this.route = planner2.route();
            this.routeIndex = 0;
            this.routeStart = this.mc.player == null ? null : this.mc.player.getBlockPos();
            this.routeTarget = planner2.target().pos();
            this.routeUse = planner2.target().use();
            this.routeStand = this.route.get(this.route.size() - 1).pos();
            return;
        }
        this.noPlanUntil = this.tick + 40L;
        if (status == StandPlanner.Status.NONE
                && this.mc.player != null
                && this.mc.world != null
                && planner2.lowestTargetY() - this.mc.player.getBlockPos().getY() > 4
                && Climber.leavesCount(this.mc.player) >= 2
                && this.tick >= this.noClimbUntil
                && (pos = Climber.column(this.mc)) != null
                && this.pillarFits(this.mc.world, pos)) {
            SchematicBuilderModule.trace("far below the work, climbing at " + pos.toShortString());
            this.climbTo = planner2.lowestTargetY() - 4;
            this.stopMoving();
            this.climber = new Climber(pos, this.scaffold);
            return;
        }
        if (status == StandPlanner.Status.NONE
                && planner2.visited() <= 3
                && this.mc.player != null
                && this.mc.world != null
                && Climber.leavesCount(this.mc.player) >= 2
                && (this.escapeClimb(this.mc.world)
                        || this.digOut(this.mc.world, this.mc.player))) {
            return;
        }
        ClientPlayerEntity player = this.mc.player;
        BlockPos pos2 =
                player == null || planner2.visited() > 3 ? null : this.supportingLeaf(player);
        BlockPos pos3 = pos2;
        if (pos2 != null && this.dropBelow(pos2) <= 4) {
            SchematicBuilderModule.trace(
                    "stranded on the leaf at " + pos2.toShortString() + ", mining it to get down");
            this.stopMoving();
            this.breaker = new Breaker(pos2, true);
        }
    }

    private void deferUnplanned() {
        int i = 0;
        for (long plannedKey : this.plannedKeys) {
            if (i++ >= 4) break;
            if (this.walkFails.merge(plannedKey, 1, Integer::sum) < 2
                    || !this.deferred.add(plannedKey)) continue;
            SchematicBuilderModule.trace(
                    "no plan reaches "
                            + BlockPos.fromLong(plannedKey).toShortString()
                            + ", leaving it for later");
        }
    }

    private void walkTo(ClientPlayerEntity player, BlockPos walkStand2) {
        this.placing = false;
        this.walkStand = walkStand2;
        this.walkFrom = player.getEntityPos();
        this.walker.go(player, walkStand2, this.tick);
    }

    private boolean walkToFix(ClientWorld world, ClientPlayerEntity player) {
        ArrayList<BlockPos> list = new ArrayList<BlockPos>();
        if (this.crafterSlots.get()) {
            list.addAll(this.scan.crafters);
        }
        if (this.clickStates.get()) {
            for (BuildScan.Cell cell : this.scan.adjust) {
                if (this.clickFailed.contains(cell.pos().asLong())) continue;
                list.add(cell.pos());
            }
        }
        list.sort(
                Comparator.comparingDouble(
                        blockPos -> blockPos.getSquaredDistance(player.getEntityPos())));
        if (!this.walking) {
            return this.planToClick(
                    world,
                    player,
                    list,
                    blockPos -> {
                        if (this.scan.crafters.contains(blockPos)) {
                            this.craftersDone.add(blockPos.asLong());
                        } else {
                            this.clickFailed.add(blockPos.asLong());
                        }
                    });
        }
        BlockPos pos = player.getBlockPos();
        for (BlockPos pos2 : list) {
            BlockPos pos3 =
                    Walker.standFor(
                            world, player, pos2, Blocks.AIR.getDefaultState(), this::badStand);
            if (pos3 == null
                    || pos3.equals(pos)
                    || this.walkFails.merge(pos2.asLong(), 1, Integer::sum) > 2) {
                if (this.scan.crafters.contains(pos2)) {
                    this.craftersDone.add(pos2.asLong());
                    continue;
                }
                this.clickFailed.add(pos2.asLong());
                continue;
            }
            this.walkTarget = null;
            this.walkTo(player, pos3);
            return true;
        }
        return false;
    }

    private boolean planToClick(
            ClientWorld world,
            ClientPlayerEntity player,
            List<BlockPos> list,
            Consumer<BlockPos> consumer) {
        ArrayList<StandPlanner.Target> list2 = new ArrayList<StandPlanner.Target>();
        for (BlockPos pos : list) {
            if (this.walkFails.getOrDefault(pos.asLong(), 0) >= 2) {
                consumer.accept(pos);
                continue;
            }
            list2.add(
                    new StandPlanner.Target(pos, world.getBlockState(pos), StandPlanner.Use.CLICK));
        }
        if (list2.isEmpty()) {
            return false;
        }
        if (this.tick < this.noPlanUntil) {
            return true;
        }
        this.walkFails.merge(((StandPlanner.Target) list2.get(0)).pos().asLong(), 1, Integer::sum);
        this.startPlanner(world, player, list2);
        return true;
    }

    private boolean flipLevers(ClientWorld world, ClientPlayerEntity player) {
        WorldSchematic schematicWorld = this.schematics.getSchematicWorld();
        if (schematicWorld == null) {
            return false;
        }
        ArrayList<BlockPos> list = new ArrayList<BlockPos>();
        for (BlockPos lever : this.scan.levers) {
            if (this.clickFailed.contains(lever.asLong())) continue;
            if (this.inReach(player, lever) && Aim.at(this.mc, lever) != null) {
                if (!this.armed) {
                    this.feedback("levers", "Everything is in, flipping the levers on last", 1);
                }
                this.armed = true;
                ++this.leversFlipped;
                this.user = BlockUser.lever(lever, schematicWorld.getBlockState(lever));
                return true;
            }
            list.add(lever);
        }
        list.sort(
                Comparator.comparingDouble(
                        blockPos -> blockPos.getSquaredDistance(player.getEntityPos())));
        return this.planToClick(
                world, player, list, blockPos -> this.clickFailed.add(blockPos.asLong()));
    }

    private boolean relaxStuck(ClientWorld world, ClientPlayerEntity player) {
        WorldSchematic schematicWorld = this.schematics.getSchematicWorld();
        if (schematicWorld == null) {
            return false;
        }
        int i = 0;
        for (BuildScan.Cell cell : this.scan.missing) {
            if (i >= 32) break;
            BlockPos pos = cell.pos();
            if (!this.deferred.contains(pos.asLong()) || !BuildOrder.lookOnly(cell.expected()))
                continue;
            BlockState state = null;
            int j = 0;
            for (BlockState state2 : BuildOrder.turnedVariants(cell.expected())) {
                int placeSpots = this.placeSpots(world, player, pos, state2);
                if (placeSpots <= j) continue;
                j = placeSpots;
                state = state2;
            }
            if (state == null) continue;
            SchematicBuilderModule.trace(
                    "relaxing "
                            + pos.toShortString()
                            + " "
                            + String.valueOf(cell.expected())
                            + " -> "
                            + String.valueOf(state)
                            + " ("
                            + j
                            + " spots)");
            schematicWorld.setBlockState(pos, state, 18);
            this.relaxed.add(pos);
            this.deferred.remove(pos.asLong());
            this.tries.remove(pos.asLong());
            this.walkFails.remove(pos.asLong());
            this.stubborn.remove(pos.asLong());
            ++i;
        }
        if (i == 0) {
            return false;
        }
        this.feedback(
                "relax",
                i
                        + (i == 1 ? " block" : " blocks")
                        + " could not go in the schematic's way round, placing them another way",
                1);
        this.rescanSoon();
        return true;
    }

    private boolean releaseProps() {
        WorldSchematic schematicWorld = this.schematics.getSchematicWorld();
        if (schematicWorld == null) {
            return false;
        }
        boolean bl = false;
        Iterator<Map.Entry<Long, Long>> iterator = this.supportFor.entrySet().iterator();
        while (iterator.hasNext()) {
            long l = iterator.next().getValue();
            if (!this.deferred.contains(l)
                    || !this.missingKeys.contains(l)
                    || !BuildOrder.lookOnly(schematicWorld.getBlockState(BlockPos.fromLong(l))))
                continue;
            iterator.remove();
            bl = true;
        }
        if (bl) {
            this.rescanSoon();
        }
        return bl;
    }

    private int placeSpots(
            ClientWorld world, ClientPlayerEntity player, BlockPos pos, BlockState state) {
        double d = player.getBlockInteractionRange() - 0.65;
        int m = 0;
        for (int i = -2; i <= 2; ++i) {
            for (int j = -4; j <= 4; ++j) {
                for (int k = -4; k <= 4; ++k) {
                    Vec3d vec;
                    BlockPos pos2 = pos.add(j, i, k);
                    if (pos2.equals(pos)
                            || pos2.up().equals(pos)
                            || !Walker.standable(world, pos2)
                            || (vec =
                                                    new Vec3d(
                                                            (double) pos2.getX() + 0.5,
                                                            Walker.floor(world, pos2)
                                                                    + (double)
                                                                            player
                                                                                    .getStandingEyeHeight(),
                                                            (double) pos2.getZ() + 0.5))
                                            .distanceTo(pos.toCenterPos())
                                    > d
                            || !this.placement.placeableFrom(state, pos, vec)) continue;
                    ++m;
                }
            }
        }
        return m;
    }

    private BuildScan.Cell nearestOpen(ClientPlayerEntity player, int i) {
        BuildScan.Cell cell = null;
        double mAX_VALUE = Double.MAX_VALUE;
        for (BuildScan.Cell cell2 : this.scan.missing) {
            double squaredDistance;
            if (cell2.pos().getY() > i
                    || this.deferred.contains(cell2.pos().asLong())
                    || this.scan.waiting.contains(cell2.pos().asLong())
                    || !((squaredDistance = cell2.pos().getSquaredDistance(player.getEntityPos()))
                            < mAX_VALUE)) continue;
            mAX_VALUE = squaredDistance;
            cell = cell2;
        }
        return cell;
    }

    private void endOfBuild(ClientWorld world, ClientPlayerEntity player) {
        this.placing = false;
        if (this.armed) {
            this.finishBuild(world, player);
            return;
        }
        if (!this.scan.wholeSchematic) {
            return;
        }
        BlockPos pos = this.supportingLeaf(player);
        if (pos != null && this.walking) {
            BlockPos pos2 =
                    Walker.stepAside(
                            world,
                            player,
                            blockPos -> this.scaffold.contains(blockPos.down().asLong()));
            if (pos2 != null) {
                this.walkTarget = null;
                this.walkTo(player, pos2);
                return;
            }
        } else if (pos != null) {
            for (BlockPos pos3 : Walker.steps(world, player.getBlockPos())) {
                if (this.scaffold.contains(pos3.down().asLong()) || this.badStand(pos3)) continue;
                this.badStands.put(pos3.asLong(), this.tick + 200L);
                this.stopMoving();
                this.hop = new Hop(pos3);
                return;
            }
        }
        if (this.walkToFix(world, player)) {
            return;
        }
        if (!this.retriedDeferred && !this.deferred.isEmpty()) {
            this.retriedDeferred = true;
            this.deferred.clear();
            this.walkFails.clear();
            this.clickFailed.clear();
            this.tries.clear();
            this.rescanSoon();
            return;
        }
        if (this.relaxFacing.get() && !this.relaxTried && !this.deferred.isEmpty()) {
            if (this.releaseProps()
                    || this.tick < this.noPlanUntil
                    || this.planner != null
                    || this.sweepScaffold(world, player)) {
                return;
            }
            this.relaxTried = true;
            if (this.relaxStuck(world, player)) {
                return;
            }
        }
        if (this.placeWater.get() && !this.waterGivenUp && this.liquids(world, player, false)) {
            return;
        }
        if (!this.latePhase && this.scan.late > 0) {
            if (!this.leftoversAdopted) {
                this.leftoversAdopted = true;
                int adoptLeftoverScaffold = this.adoptLeftoverScaffold(world);
                if (adoptLeftoverScaffold > 0) {
                    this.feedback(
                            "leftover",
                            adoptLeftoverScaffold
                                    + " stray glass or leaf blocks are in empty cells of the build,"
                                    + " mining them first",
                            1);
                }
            }
            if (this.tick < this.noPlanUntil || this.planner != null) {
                return;
            }
            if (this.sweepScaffold(world, player)) {
                return;
            }
            if (!this.scaffold.isEmpty()) {
                ArrayList<String> list = new ArrayList<String>();
                for (long l : this.scaffold) {
                    if (list.size() >= 5) continue;
                    list.add(BlockPos.fromLong(l).toShortString());
                }
                this.say(
                        ModuleChat.body()
                                .warn(this.scaffold.size() + " scaffold blocks could not be mined")
                                .text(
                                        " ("
                                                + String.join((CharSequence) "; ", list)
                                                + (this.scaffold.size() > 5 ? "; ..." : "")
                                                + "). The redstone power and TNT are not placed:"
                                                + " mine these, then turn the builder on again."));
                this.requestDisable();
                return;
            }
            this.latePhase = true;
            this.feedback(
                    "late",
                    "Every other block is in and no scaffold is left, now the "
                            + this.scan.late
                            + (this.machineLast.get()
                                    ? " pistons, observers and redstone power sources, in that"
                                            + " order"
                                    : " redstone power sources")
                            + (this.placeTnt.get() ? " and TNT (TNT last)" : ""),
                    1);
            this.rescanSoon();
            return;
        }
        if (this.placeWater.get() && !this.lavaGivenUp && this.liquids(world, player, true)) {
            return;
        }
        if (this.sweepScaffold(world, player)) {
            return;
        }
        if (!this.verified) {
            this.verified = true;
            this.feedback("verify", "Scaffold is out, checking the whole build once more", 1);
            this.rescanSoon();
            return;
        }
        this.finishBuild(world, player);
    }

    private void finishBuild(ClientWorld world, ClientPlayerEntity player) {
        if (this.flipLevers.get() && this.flipLevers(world, player)) {
            return;
        }
        if (this.startSelling(player, true)) {
            return;
        }
        int i =
                (int)
                        this.scan.missing.stream()
                                .filter(cell -> this.scan.waiting.contains(cell.pos().asLong()))
                                .count();
        int j = this.scan.missing.size() - i;
        int liquidsLeft = this.liquidsLeft(false);
        int liquidsLeft2 = this.liquidsLeft(true);
        ModuleChat.Body body =
                ModuleChat.body()
                        .good("Build finished")
                        .text((String) (j == 0 ? "." : ", " + j + " blocks could not be reached."));
        if (this.spent > 0L) {
            body.text(" Spent $").value(AhPurchase.money(this.spent)).text(".");
        }
        if (this.scan.wrong > 0) {
            body.text(" ")
                    .warn(this.scan.wrong + " blocks differ from the schematic")
                    .text(" and were left alone.");
        }
        if (liquidsLeft > 0) {
            body.text(" " + liquidsLeft + " water cells are still dry.");
        }
        if (liquidsLeft2 > 0) {
            body.text(" " + liquidsLeft2 + " lava cells are still empty.");
        }
        if (!this.scan.poweredTnt.isEmpty()) {
            body.text(" ")
                    .warn(this.scan.poweredTnt.size() + " TNT left out")
                    .text(", power reaches those spots.");
        }
        if (!this.scan.tntForPlayer.isEmpty()) {
            body.text(" ")
                    .warn(this.scan.tntForPlayer.size() + " TNT left for you")
                    .text(" (" + SchematicBuilderModule.someOf(this.scan.tntForPlayer) + ")");
            if (!this.scan.afterTntForPlayer.isEmpty()) {
                body.text(
                        ", then "
                                + this.scan.afterTntForPlayer.size()
                                + " blocks in front of their pistons ("
                                + SchematicBuilderModule.someOf(this.scan.afterTntForPlayer)
                                + ")");
            }
            body.text(
                    ". Place them when you are ready for the machine to run (Litematica shows them"
                            + " as missing).");
        }
        if (i > this.scan.poweredTnt.size()) {
            body.text(
                    " "
                            + (i - this.scan.poweredTnt.size())
                            + " blocks were held back (sand over a gap, an observer before its"
                            + " block).");
        }
        if (this.scan.leversOff > 0 && !this.flipLevers.get()) {
            body.text(
                    " "
                            + this.scan.leversOff
                            + (this.scan.leversOff == 1 ? " lever is" : " levers are")
                            + " on in the schematic and left off, flip them when you are ready.");
        }
        if (this.scan.leversOff > 0 && this.flipLevers.get()) {
            body.text(" ")
                    .warn(
                            this.scan.leversOff
                                    + (this.scan.leversOff == 1 ? " lever" : " levers")
                                    + " could not be flipped on")
                    .text(" (" + SchematicBuilderModule.someOf(this.scan.levers) + ").");
        }
        if (this.leversFlipped > 0) {
            body.text(
                    " "
                            + this.leversFlipped
                            + (this.leversFlipped == 1 ? " lever was" : " levers were")
                            + " flipped on last.");
        }
        if (!this.relaxed.isEmpty()) {
            body.text(" ")
                    .warn(
                            this.relaxed.size()
                                    + (this.relaxed.size() == 1 ? " block" : " blocks")
                                    + " went in facing another way")
                    .text(
                            ", no spot could place them the schematic's way ("
                                    + SchematicBuilderModule.someOf(this.relaxed)
                                    + ").");
        }
        if (this.scan.railsOff > 0) {
            body.text(
                    " "
                            + this.scan.railsOff
                            + (this.scan.railsOff == 1 ? " rail has" : " rails have")
                            + " another shape than the schematic, vanilla picks it from the rails"
                            + " around.");
        }
        if (this.scan.noItem > 0) {
            body.text(" " + this.scan.noItem + " cells need blocks without an item.");
        }
        if (!this.scaffold.isEmpty()) {
            body.text(" " + this.scaffold.size() + " scaffold leaves are still there.");
        }
        this.say(body);
        this.requestDisable();
    }

    private static String someOf(List<BlockPos> list) {
        ArrayList<String> list2 = new ArrayList<String>();
        for (BlockPos pos : list) {
            if (list2.size() == 4) break;
            list2.add(pos.toShortString());
        }
        return String.join((CharSequence) "; ", list2) + (list.size() > 4 ? "; ..." : "");
    }

    private boolean waterNow(ClientPlayerEntity player) {
        if (!Hands.has(player, itemStack -> itemStack.isOf(Items.WATER_BUCKET))) {
            return false;
        }
        for (BuildScan.Liquid liquid : this.scan.liquids) {
            if (liquid.kind() == CellFiller.Kind.LAVA_SOURCE
                    || !this.dueNow(liquid)
                    || !this.waterPending(liquid.pos())
                    || !CellFiller.reachable(this.mc, liquid.pos(), liquid.kind())) continue;
            this.stopMoving();
            this.filler = new CellFiller(liquid.pos(), liquid.kind());
            return true;
        }
        return false;
    }

    private boolean dueNow(BuildScan.Liquid liquid) {
        return liquid.kind() == CellFiller.Kind.WATERLOG
                || this.scan.layer == Integer.MAX_VALUE
                || liquid.pos().getY() < this.scan.layer;
    }

    private boolean waterPending(BlockPos pos) {
        return this.placeWater.get()
                && !this.waterGivenUp
                && this.waterFails.getOrDefault(pos.asLong(), 0) < 2;
    }

    private boolean liquidPending(BlockPos pos) {
        WorldSchematic schematicWorld = this.schematics.getSchematicWorld();
        if (schematicWorld == null
                || !schematicWorld.getBlockState(pos).getFluidState().isIn(FluidTags.LAVA)) {
            return this.waterPending(pos);
        }
        return this.placeWater.get()
                && !this.lavaGivenUp
                && this.waterFails.getOrDefault(pos.asLong(), 0) < 2;
    }

    private boolean cappedLavaNow(ClientWorld world, ClientPlayerEntity player) {
        if (this.lavaGivenUp
                || !Hands.has(player, itemStack -> itemStack.isOf(Items.LAVA_BUCKET))) {
            return false;
        }
        WorldSchematic schematicWorld = this.schematics.getSchematicWorld();
        if (schematicWorld == null) {
            return false;
        }
        for (BuildScan.Liquid liquid : this.scan.liquids) {
            FluidState fluidState;
            BlockPos pos = liquid.pos();
            if (!liquid.capped()
                    || this.scan.layer != Integer.MAX_VALUE && pos.getY() >= this.scan.layer
                    || !this.liquidPending(pos)
                    || (fluidState = schematicWorld.getBlockState(pos.down()).getFluidState())
                                    .isStill()
                            && fluidState.isIn(FluidTags.LAVA)
                            && (!world.getFluidState(pos.down()).isStill()
                                    || !world.getFluidState(pos.down()).isIn(FluidTags.LAVA))
                    || !SchematicBuilderModule.walledIn(world, schematicWorld, pos)
                    || !CellFiller.reachable(this.mc, pos, liquid.kind())) continue;
            SchematicBuilderModule.trace("capped lava " + pos.toShortString());
            this.stopMoving();
            this.filler = new CellFiller(pos, liquid.kind());
            return true;
        }
        return false;
    }

    private int cappedLavaWanted(int i) {
        int j = 0;
        for (BuildScan.Liquid liquid : this.scan.liquids) {
            if (!liquid.capped() || liquid.pos().getY() >= i || !this.liquidPending(liquid.pos()))
                continue;
            ++j;
        }
        return j;
    }

    private int waterWanted(int i) {
        int j = 0;
        for (BuildScan.Liquid liquid : this.scan.liquids) {
            if (liquid.kind() == CellFiller.Kind.LAVA_SOURCE
                    || liquid.pos().getY() > i
                    || !this.waterPending(liquid.pos())) continue;
            ++j;
        }
        return j;
    }

    private boolean refillWater(ClientWorld world, ClientPlayerEntity player, int i) {
        if (i <= SchematicBuilderModule.count(player, Items.WATER_BUCKET)
                || SchematicBuilderModule.count(player, Items.BUCKET) <= 0
                || this.tick < this.nextRefillTick) {
            return false;
        }
        BlockPos pos = BlockPos.ofFloored(player.getEyePos());
        for (BlockPos pos2 : BlockPos.iterate(pos.add(-5, -5, -5), pos.add(5, 5, 5))) {
            Long long_Value = this.pouredAt.get(pos2.asLong());
            if (long_Value != null && this.tick - long_Value < 1200L
                    || !CellFiller.endless(world, pos2)
                    || Aim.water(this.mc, pos2) == null) continue;
            this.stopMoving();
            this.filler = new CellFiller(pos2.toImmutable(), CellFiller.Kind.REFILL);
            return true;
        }
        this.nextRefillTick = this.tick + 40L;
        return false;
    }

    private int liquidsLeft(boolean bl) {
        if (!this.placeWater.get()) {
            return 0;
        }
        return (int)
                this.scan.liquids.stream()
                        .filter(liquid -> liquid.kind() == CellFiller.Kind.LAVA_SOURCE == bl)
                        .count();
    }

    private boolean liquids(ClientWorld world, ClientPlayerEntity player, boolean bl) {
        ArrayList<StandPlanner.Target> list;
        WorldSchematic schematicWorld = this.schematics.getSchematicWorld();
        ArrayList<BuildScan.Liquid> list2 = new ArrayList<BuildScan.Liquid>();
        int i = 0;
        for (BuildScan.Liquid liquid2 : this.scan.liquids) {
            if (liquid2.kind() == CellFiller.Kind.LAVA_SOURCE != bl
                    || this.waterFails.getOrDefault(liquid2.pos().asLong(), 0) >= 2) continue;
            if (bl && !SchematicBuilderModule.walledIn(world, schematicWorld, liquid2.pos())) {
                ++i;
                continue;
            }
            list2.add(liquid2);
        }
        if (i > 0) {
            this.feedback(
                    "lava-open", i + " lava cells are not walled in, leaving them empty", 400);
        }
        if (list2.isEmpty()) {
            return false;
        }
        list2.sort(
                Comparator.comparingInt((BuildScan.Liquid liquid3) -> liquid3.pos().getY())
                        .thenComparingDouble(
                                liquid -> liquid.pos().getSquaredDistance(player.getEntityPos())));
        Item item = bl ? Items.LAVA_BUCKET : Items.WATER_BUCKET;
        String string = bl ? "lava" : "water";
        String string2 = string;
        if (!Hands.has(player, itemStack -> itemStack.isOf(item))) {
            if (!bl && this.refillWater(world, player, list2.size())) {
                return true;
            }
            if (!this.buyFromAh.get()) {
                this.feedback(
                        "no-" + string,
                        "Need " + string + " buckets for " + list2.size() + " " + string + " cells",
                        200);
                this.giveUpLiquid(bl);
                return false;
            }
            int freeSlots = SchematicBuilderModule.freeSlots(player);
            if (freeSlots <= 0) {
                if (this.startSelling(player, false)) {
                    return true;
                }
                this.feedback(
                        string + "-room", "No free inventory slot for " + string + " buckets", 200);
                this.giveUpLiquid(bl);
                return false;
            }
            this.feedback(
                    string + "-buy",
                    "Buying " + string + " buckets for " + list2.size() + " " + string + " cells",
                    1);
            return this.startPurchase(item, Math.min(list2.size(), Math.min(freeSlots, 8)));
        }
        for (Object object : list2) {
            if (!CellFiller.reachable(
                    this.mc, ((BuildScan.Liquid) object).pos(), ((BuildScan.Liquid) object).kind()))
                continue;
            this.stopMoving();
            this.filler =
                    new CellFiller(
                            ((BuildScan.Liquid) object).pos(), ((BuildScan.Liquid) object).kind());
            return true;
        }
        if (!this.walking) {
            this.missedRouteTarget(player);
            if (this.tick < this.noPlanUntil) {
                this.giveUpLiquid(bl);
                return false;
            }
            list = new ArrayList<StandPlanner.Target>();
            for (BuildScan.Liquid liquid4 : list2) {
                list.add(SchematicBuilderModule.liquidTarget(world, liquid4));
            }
            this.startPlanner(world, player, list);
            return true;
        }
        BlockPos pos = player.getBlockPos();
        for (BuildScan.Liquid liquid5 : list2) {
            BlockPos pos2 =
                    Walker.standFor(
                            world,
                            player,
                            liquid5.pos(),
                            Blocks.AIR.getDefaultState(),
                            this::badStand,
                            2.8);
            if (pos2 == null || pos2.equals(pos)) {
                this.waterFails.merge(liquid5.pos().asLong(), 1, Integer::sum);
                continue;
            }
            this.walkTarget = null;
            this.walkTo(player, pos2);
            return true;
        }
        return false;
    }

    private boolean sweepFinishedLayers(ClientWorld world, ClientPlayerEntity player) {
        if (this.scan.layer == Integer.MAX_VALUE || this.walking) {
            return false;
        }
        if (this.sweptLayer == Integer.MAX_VALUE || this.scan.layer > this.sweptLayer) {
            this.sweptLayer = this.scan.layer;
            this.sweepDue = true;
            this.sweepSince = this.tick;
        }
        if (this.sweepDue && this.tick - this.sweepSince > 1200L) {
            this.sweepDue = false;
        }
        if (!this.sweepDue || this.tick < this.noPlanUntil) {
            return false;
        }
        ArrayList<StandPlanner.Target> list = new ArrayList<StandPlanner.Target>();
        for (long l : this.scaffold) {
            BlockPos pos = BlockPos.fromLong(l);
            if (pos.getY() >= this.scan.layer - 1
                    || !world.isChunkLoaded(pos)
                    || this.walkFails.getOrDefault(l, 0) >= 2
                    || this.keepScaffold(player, l)) continue;
            list.add(
                    new StandPlanner.Target(pos, world.getBlockState(pos), StandPlanner.Use.BREAK));
        }
        if (list.isEmpty()) {
            this.sweepDue = false;
            return false;
        }
        this.feedback(
                "layer-sweep",
                "Mining " + list.size() + " scaffold blocks under the finished layers",
                200);
        this.missedRouteTarget(player);
        this.sweepPlanning = true;
        this.startPlanner(world, player, list);
        return true;
    }

    private int adoptLeftoverScaffold(ClientWorld world) {
        WorldSchematic schematicWorld = this.schematics.getSchematicWorld();
        if (schematicWorld == null) {
            return 0;
        }
        int m = 0;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (SchematicManager.ActiveBox activeBox : this.schematics.activeBoxes()) {
            for (int i = activeBox.minY(); i <= activeBox.maxY(); ++i) {
                for (int j = activeBox.minX(); j <= activeBox.maxX(); ++j) {
                    for (int k = activeBox.minZ(); k <= activeBox.maxZ(); ++k) {
                        mutable.set(j, i, k);
                        if (!world.isChunkLoaded(mutable)
                                || !schematicWorld.getBlockState(mutable).isAir()
                                || !Scaffold.block(world.getBlockState(mutable))
                                || !this.scaffold.add(mutable.asLong())) continue;
                        ++m;
                    }
                }
            }
        }
        return m;
    }

    private boolean sweepScaffold(ClientWorld world, ClientPlayerEntity player) {
        if (this.walking || this.scaffold.isEmpty()) {
            return false;
        }
        this.missedRouteTarget(player);
        if (this.tick < this.noPlanUntil) {
            return false;
        }
        ArrayList<StandPlanner.Target> list = new ArrayList<StandPlanner.Target>();
        for (long l : this.scaffold) {
            BlockPos pos = BlockPos.fromLong(l);
            if (!world.isChunkLoaded(pos)
                    || this.walkFails.getOrDefault(l, 0) >= 2
                    || this.keepScaffold(player, l)) continue;
            list.add(
                    new StandPlanner.Target(pos, world.getBlockState(pos), StandPlanner.Use.BREAK));
        }
        if (list.isEmpty()) {
            return false;
        }
        this.feedback("sweep", "Mining " + list.size() + " scaffold leaves left behind", 200);
        this.startPlanner(world, player, list);
        return true;
    }

    private void giveUpLiquid(boolean bl) {
        if (bl) {
            this.lavaGivenUp = true;
        } else {
            this.waterGivenUp = true;
        }
    }

    private static boolean walledIn(
            ClientWorld world, WorldSchematic worldSchematic, BlockPos pos) {
        return SchematicBuilderModule.walledIn(world, worldSchematic, pos, new HashSet<Long>());
    }

    private static boolean walledIn(
            ClientWorld world, WorldSchematic worldSchematic, BlockPos pos, Set<Long> set) {
        if (!set.add(pos.asLong()) || set.size() > 64) {
            return set.size() <= 64;
        }
        for (Direction direction : Direction.values()) {
            BlockPos pos2;
            BlockState blockState;
            if (direction == Direction.UP
                    || !(blockState = world.getBlockState(pos2 = pos.offset(direction)))
                            .isReplaceable()
                    || blockState.getFluidState().isIn(FluidTags.LAVA)
                            && blockState.getFluidState().isStill()
                    || worldSchematic != null
                            && worldSchematic
                                    .getBlockState(pos2)
                                    .getFluidState()
                                    .isIn(FluidTags.LAVA)
                            && SchematicBuilderModule.walledIn(world, worldSchematic, pos2, set))
                continue;
            return false;
        }
        return true;
    }

    private void snapshotInventory(ClientPlayerEntity player) {
        this.startCounts.clear();
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < 36; ++i) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) continue;
            this.startCounts.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
    }

    private boolean startSelling(ClientPlayerEntity player, boolean bl) {
        Map<Integer, Item> map;
        if (!this.sellLeftovers.get() || this.sell != null || this.tick < this.nextSellTick) {
            return false;
        }
        Map<Item, Integer> map2 = Map.of();
        if (!bl) {
            WorldSchematic schematicWorld = this.schematics.getSchematicWorld();
            if (schematicWorld == null || this.mc.world == null) {
                return false;
            }
            map2 =
                    BuildScan.remainingItems(
                            this.mc.world, schematicWorld, this.schematics.activeBoxes());
        }
        if ((map = this.leftoverSlots(player, map2)).isEmpty()) {
            this.nextSellTick = this.tick + 200L;
            return false;
        }
        this.stopMoving();
        this.feedback("selling", "Selling leftovers with /sell", 1);
        this.sell = DonutSell.start(this, map);
        return true;
    }

    private Map<Integer, Item> leftoverSlots(ClientPlayerEntity player, Map<Item, Integer> map) {
        PlayerInventory inventory = player.getInventory();
        LinkedHashMap<Item, List> map2 = new LinkedHashMap<Item, List>();
        HashMap<Item, Integer> map3 = new HashMap<Item, Integer>();
        for (int i = 0; i < 36; ++i) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()
                    || stack.isOf(Items.TOTEM_OF_UNDYING)
                    || stack.isOf(Items.WATER_BUCKET)
                    || stack.isOf(Items.LAVA_BUCKET)) continue;
            map2.computeIfAbsent(stack.getItem(), item -> new ArrayList()).add(i);
            map3.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        HashMap<Integer, Item> map4 = new HashMap<Integer, Item>();
        for (Map.Entry entry : map2.entrySet()) {
            Item key = (Item) entry.getKey();
            ItemStack stack2 = new ItemStack(key);
            int j =
                    this.bought.getOrDefault(key, 0)
                            + (key == Items.BUCKET ? this.bucketsEmptied : 0)
                            + (Scaffold.item(stack2) ? this.leavesBought : 0);
            int k =
                    Math.min(
                            j,
                            (Integer) map3.get(key)
                                    - this.startCounts.getOrDefault(key, 0)
                                    - map.getOrDefault(key, 0));
            if (k <= 0) continue;
            ArrayList<Integer> list = new ArrayList<Integer>((Collection) entry.getValue());
            list.sort(
                    Comparator.comparingInt(
                            integer -> -inventory.getStack((int) integer).getCount()));
            Iterator iterator = list.iterator();
            while (iterator.hasNext()) {
                int m = (Integer) iterator.next();
                int count = inventory.getStack(m).getCount();
                if (count > k) continue;
                map4.put(m, key);
                k -= count;
            }
        }
        return map4;
    }

    private BlockPos cleanupLeaf(ClientPlayerEntity player) {
        return this.cleanupLeaf(player, Set.of());
    }

    private Set<Long> routeFloor() {
        HashSet<Long> set = new HashSet<Long>();
        for (int i = Math.max(0, this.routeIndex - 1); i < this.route.size(); ++i) {
            set.add(this.route.get(i).pos().down().asLong());
        }
        return set;
    }

    private BlockPos cleanupLeaf(ClientPlayerEntity player, Set<Long> set) {
        double blockInteractionRange = player.getBlockInteractionRange();
        Iterator<Long> iterator = this.scaffold.iterator();
        while (iterator.hasNext()) {
            long l = iterator.next();
            BlockPos pos = BlockPos.fromLong(l);
            if (!this.mc.world.isChunkLoaded(pos)) continue;
            if (!Scaffold.block(this.mc.world.getBlockState(pos))) {
                iterator.remove();
                this.spentSupport.remove(l);
                this.supportFor.remove(l);
                this.buildScaffold.remove(l);
                continue;
            }
            Long long_Value = this.supportFor.get(l);
            if (long_Value != null) {
                if (this.missingKeys.contains(long_Value)) continue;
                this.supportFor.remove(l);
                this.spentSupport.add(l);
            }
            if (this.stuckScaffold.contains(l)
                    || SchematicBuilderModule.supports(player, pos)
                    || this.walkway(pos)
                    || set.contains(l)
                    || !this.buildScaffold.contains(l)
                            && !this.spentSupport.contains(l)
                            && this.neededNow(pos)
                    || pos.toCenterPos().distanceTo(player.getEyePos()) > blockInteractionRange
                    || Aim.at(this.mc, pos) == null) continue;
            return pos;
        }
        return null;
    }

    private boolean keepScaffold(ClientPlayerEntity player, long l) {
        BlockPos pos = BlockPos.fromLong(l);
        Long long_Value = this.supportFor.get(l);
        if (long_Value != null
                && this.missingKeys.contains(long_Value)
                && !this.deferred.contains(long_Value)) {
            return true;
        }
        return this.stuckScaffold.contains(l)
                || SchematicBuilderModule.supports(player, pos)
                || this.walkway(pos)
                || !this.buildScaffold.contains(l)
                        && !this.spentSupport.contains(l)
                        && long_Value == null
                        && this.neededNow(pos);
    }

    private boolean walkway(BlockPos pos) {
        return this.scan != null
                && this.scan.layer != Integer.MAX_VALUE
                && pos.getY() == this.scan.layer - 1
                && !this.buildScaffold.contains(pos.asLong());
    }

    private int dropBelow(BlockPos pos) {
        for (int i = 1; i <= 8; ++i) {
            BlockPos pos2 = pos.down(i - 1);
            if (Double.isNaN(Walker.floor(this.mc.world, pos2))) continue;
            return i;
        }
        return Integer.MAX_VALUE;
    }

    private BlockPos supportingLeaf(ClientPlayerEntity player) {
        for (long l : this.scaffold) {
            BlockPos pos = BlockPos.fromLong(l);
            if (!SchematicBuilderModule.supports(player, pos)) continue;
            return pos;
        }
        return null;
    }

    private static boolean supports(ClientPlayerEntity player, BlockPos pos) {
        Box box;
        Box boundingBox = player.getBoundingBox();
        return boundingBox.intersects(box = new Box(pos))
                || boundingBox.offset(0.0, -0.25, 0.0).intersects(box);
    }

    private boolean inReach(ClientPlayerEntity player, BlockPos pos) {
        return pos.toCenterPos().distanceTo(player.getEyePos())
                <= player.getBlockInteractionRange() - 0.3;
    }

    private static boolean enclosed(ClientWorld world, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos pos2 = pos.offset(direction);
            if (!world.getBlockState(pos2).isReplaceable()) continue;
            return false;
        }
        return true;
    }

    private void stopMoving() {
        this.walker.stop();
        this.walkTarget = null;
        this.placing = false;
    }

    private static boolean lacking(
            ClientPlayerEntity player, BlockState state, Map<Item, Boolean> map) {
        Item item2 = BlockStateUtil.requiredItem(state);
        if (item2 == null || item2 == Items.AIR) {
            return false;
        }
        return map.computeIfAbsent(item2, item -> SchematicBuilderModule.count(player, item) <= 0);
    }

    private static int count(ClientPlayerEntity player, Item item) {
        int j = 0;
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < 36; ++i) {
            if (!inventory.getStack(i).isOf(item)) continue;
            j += inventory.getStack(i).getCount();
        }
        if (player.getOffHandStack().isOf(item)) {
            j += player.getOffHandStack().getCount();
        }
        return j;
    }

    private static int room(ClientPlayerEntity player, ItemStack stack) {
        int j = 0;
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < 36; ++i) {
            ItemStack stack2 = inventory.getStack(i);
            if (stack2.isEmpty()) {
                j += stack.getMaxCount();
                continue;
            }
            if (!stack2.isOf(stack.getItem())) continue;
            j += Math.max(0, stack2.getMaxCount() - stack2.getCount());
        }
        return j;
    }

    private static int freeSlots(ClientPlayerEntity player) {
        int j = 0;
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < 36; ++i) {
            if (!inventory.getStack(i).isEmpty()) continue;
            ++j;
        }
        return j;
    }

    private void say(ModuleChat.Body body) {
        ModuleChat.send((ClientModule) this, body);
    }

    private void feedback(String string, String string2, int i) {
        if (!this.chatFeedback.get()) {
            return;
        }
        long l = this.lastFeedbackTick.getOrDefault(string, -4611686018427387904L);
        if (this.tick - l < (long) i) {
            return;
        }
        this.lastFeedbackTick.put(string, this.tick);
        ModuleChat.send((ClientModule) this, string2);
    }

    private boolean inBuildScope(Set set, Set set2, Vec3d vec, BlockPos pos) {
        return set.contains(pos.asLong())
                || set2.contains(pos.asLong())
                || this.stubborn.contains(pos.asLong()) && pos.toCenterPos().distanceTo(vec) > 2.8
                || this.deferred.contains(pos.asLong())
                        && this.tries.getOrDefault(pos.asLong(), 0) >= 8;
    }
}
