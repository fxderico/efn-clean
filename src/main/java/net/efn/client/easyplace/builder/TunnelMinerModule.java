package net.efn.client.easyplace.builder;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.efn.client.chat.ModuleChat;
import net.efn.client.easyplace.EasyPlace;
import net.efn.client.easyplace.inventory.InventoryCalm;
import net.efn.client.easyplace.util.PlacementUtil;
import net.efn.client.esp.ChunkScanner;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.donutmacros.AhPurchase;
import net.efn.client.module.impl.donutmacros.DonutSell;
import net.efn.client.module.impl.misc.AutoReconnectModule;
import net.efn.client.module.impl.render.BetterSusChunkModule;
import net.efn.client.module.impl.render.SusChunkFinderModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringSetting;
import net.efn.client.util.ChunkOverlay;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.InfestedBlock;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
public final class TunnelMinerModule extends ClientModule implements ChunkScanner.Consumer {

    private static final String SIZE_3X3 = "3x3 Shard Pickaxe";

    private static final String SIZE_1X2 = "1x2";

    private static final Predicate<ItemStack> TOTEM =
            itemStack -> itemStack.isOf(Items.TOTEM_OF_UNDYING);

    private static final Predicate<ItemStack> XP =
            itemStack -> itemStack.isOf(Items.EXPERIENCE_BOTTLE);

    private static final Predicate<ItemStack> APPLE =
            itemStack -> itemStack.isOf(Items.GOLDEN_APPLE);

    private static final Predicate<ItemStack> PICKAXE =
            itemStack -> itemStack.isIn(ItemTags.PICKAXES);

    private static final Predicate<ItemStack> SHARD =
            itemStack -> PICKAXE.test(itemStack) && shardName(itemStack);

    private static final Set<Block> STORAGE =
            Set.of(
                    Blocks.CHEST,
                    Blocks.TRAPPED_CHEST,
                    Blocks.BARREL,
                    Blocks.ENDER_CHEST,
                    Blocks.HOPPER,
                    Blocks.FURNACE,
                    Blocks.BLAST_FURNACE,
                    Blocks.SMOKER,
                    Blocks.DROPPER,
                    Blocks.DISPENSER);

    private static final Set<Item> JUNK =
            Set.of(
                    Items.COBBLESTONE,
                    Items.COBBLED_DEEPSLATE,
                    Items.DEEPSLATE,
                    Items.STONE,
                    Items.TUFF,
                    Items.GRANITE,
                    Items.DIORITE,
                    Items.ANDESITE,
                    Items.DIRT,
                    Items.GRAVEL,
                    Items.FLINT,
                    Items.NETHERRACK,
                    Items.CALCITE,
                    Items.SMOOTH_BASALT,
                    Items.BASALT,
                    Items.BLACKSTONE);

    private static final int PLAN_AHEAD = 40;

    private static final int PLAN_SIDE = 15;

    private static final int PLAN_NODES = 6000;

    private static final int REPLAN_STEPS = 9;

    private static final double STAND_MIN = 1.0;

    private static final double STAND_MAX = 2.6;

    private static final double STEP_TO_MAX = 1.6;

    private static final double STAND_CLOSE = 0.3;

    private static final float VARIED_STAND = 0.2F;

    private static final double QUICK_STEP_SPREAD = 0.3;

    private static final double STREAM_ASIDE = 0.45;

    private static final double STREAM_GAP = 1.0;

    private static final int FEET_WAIT = 4;

    private static final float OLD_WAY_CHANCE = 0.06F;

    private static final int OLD_WAY_MIN = 2;

    private static final int OLD_WAY_SPREAD = 4;

    private static final float LINGER_CHANCE = 0.01F;

    private static final double FALL_BEHIND = 3.0;

    private static final int PERCEIVE_MIN = 3;

    private static final int PERCEIVE_MAX = 5;

    private static final int BUY_RETRY_TICKS = 1200;

    private static final int SELL_STACKS = 4;

    private static final int SELL_RETRY_TICKS = 1200;

    private static final int VERIFY_TICKS = 60;

    private static final int XP_STACK = 64;

    private static final int APPLE_STACK = 64;

    private static final int START_ZONE = 48;

    private static final int CLOSE_SCREEN_TICKS = 100;

    private static final String FLAGS_OFF = "Off";

    private static final String FLAGS_SUS = "Sus Chunk";

    private static final String FLAGS_BETTER = "Better Sus Chunk";

    private static final String FLAGS_BOTH = "Both";

    private static final int ARRIVE_BLOCKS = 3;

    private static final int TOTEM_POP = 35;

    private static final int STALL_PLANS = 3;

    private static final double VISITED_COST = 4.0;

    private static final int UNFLAGGED_CHECKS = 10;

    private static final int VISITED_COLOR = 1431357695;

    private final ModeSetting size =
            this.addSetting(
                    "size",
                    new ModeSetting(
                            "Tunnel Size", "3x3 Shard Pickaxe", "3x3 Shard Pickaxe", "1x2"));

    private final BooleanSetting wander =
            this.addSetting("wander", new BooleanSetting("Wander", true));

    private final ModeSetting flags =
            this.addSetting(
                    "flags",
                    new ModeSetting(
                            "Dig To Flagged Chunks",
                            "Off",
                            "Off",
                            "Sus Chunk",
                            "Better Sus Chunk",
                            "Both"));

    private final BooleanSetting showVisited =
            this.addSetting("show-visited", new BooleanSetting("Show Visited Chunks", true));

    private final NumberSetting storageToStop =
            this.addSetting(
                    "storage-to-stop",
                    new NumberSetting("Storage To Stop", 30.0F, 3.0F, 300.0F).step(1.0F));

    private final NumberSetting baseRadius =
            this.addSetting(
                    "base-radius",
                    new NumberSetting("Base Radius", 80.0F, 16.0F, 192.0F).step(8.0F));

    private final BooleanSetting spawners =
            this.addSetting("spawners", new BooleanSetting("Stop At Spawners", true));

    private final BooleanSetting disconnect =
            this.addSetting("disconnect", new BooleanSetting("Disconnect On Find", true));

    private final BooleanSetting quitOnPop =
            this.addSetting("quit-on-pop", new BooleanSetting("Quit On Totem Pop", true));

    private final BooleanSetting keepTotem =
            this.addSetting("keep-totem", new BooleanSetting("Keep Totem In Offhand", true));

    private final BooleanSetting mend =
            this.addSetting("mend", new BooleanSetting("Mend Pickaxe", true));

    private final NumberSetting mendBelow =
            this.addSetting(
                    "mend-below",
                    new NumberSetting("Mend Below Durability", 150.0F, 20.0F, 1000.0F).step(10.0F));

    private final BooleanSetting eat =
            this.addSetting("eat", new BooleanSetting("Eat Golden Apples", true));

    private final BooleanSetting sellJunk =
            this.addSetting("sell-junk", new BooleanSetting("Sell Junk For Room", true));

    private final BooleanSetting buyFromAh =
            this.addSetting("buy-from-ah", new BooleanSetting("Buy From AH", true));

    private final StringSetting maxPerListing =
            this.addSetting(
                    "max-per-listing",
                    new StringSetting("Max Price Per Listing", "10m")
                            .hint("e.g. 10m, one whole listing"));

    private final NumberSetting refresh =
            this.addSetting(
                    "refresh",
                    new NumberSetting("AH Refresh (ms)", 1000.0F, 300.0F, 3000.0F).step(100.0F));

    private final BooleanSetting chatFeedback =
            this.addSetting("chat-feedback", new BooleanSetting("Chat Feedback", true));

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private final Random random = new Random();

    private final Map<Long, Boolean> finds = new HashMap<>();

    private final Map<Long, Integer> breakFails = new HashMap<>();

    private final Set<Long> blocked = new HashSet<>();

    private final Map<Item, Long> nextBuy = new HashMap<>();

    private final Map<String, Long> lastFeedback = new HashMap<>();

    private final List<BlockPos> steps = new ArrayList<>();

    private final LongOpenHashSet visitedChunks = new LongOpenHashSet();

    private final ChunkOverlay visitedOverlay = new ChunkOverlay();

    private boolean hasTarget;

    private long target;

    private int targetBest;

    private int targetNear;

    private int targetStalls;

    private int unflaggedChecks;

    private long tick;

    private int cellSize;

    private Direction heading;

    private BlockPos start;

    private BlockPos axis;

    private int drift;

    private long nextDrift;

    private int noiseSeed;

    private int stepIndex;

    private int stepsSincePlan;

    private int blockedPlans;

    private Breaker breaker;

    private Hop hop;

    private Shuffle shuffle;

    private Eater eater;

    private int originX;

    private int originZ;

    private BlockPos routeFrom;

    private BlockPos seenFrom;

    private int perceive;

    private BlockPos standTarget;

    private Vec3d standSpot;

    private boolean standVaried;

    private boolean streaming;

    private BlockPos streamTarget;

    private long headBrokeAt = -100L;

    private BlockPos decidedSlab;

    private int oldWayLeft;

    private float[] idleLook;

    private boolean lingering;

    private long leaveAt;

    private String foundWhat;

    private BlockPos foundAt;

    private int settle;

    private long restUntil;

    private long nextRest;

    private long screenSince;

    private DonutSell sell;

    private long nextSell;

    private boolean stopped;

    private boolean popped;

    private AhPurchase purchase;

    private Item purchaseItem;

    private String purchaseName;

    private int purchaseBefore;

    private long verifyUntil;

    private long maxListing;

    private boolean mending;

    private long mendStarted;

    public TunnelMinerModule() {
        super(
                "tunnel-miner",
                "Tunnel Base Finder",
                "Digs a tunnel the way you face (3x3 with a Shard Pickaxe) like a player would,"
                    + " around lava, water and gravel, or from one Sus Chunk flagged chunk to the"
                    + " next, and stops at a base or a spawner. Buys totems, XP bottles and golden"
                    + " apples from the AH.",
                ModuleCategory.BOTS);
        this.size.describe("3x3 needs a Shard Pickaxe; 1x2 works with any pickaxe.");
        this.wander.describe("Slowly changes direction instead of digging one straight line.");
        this.flags.describe("Digs toward chunks flagged by Sus Chunk / Better Sus Chunk.");
        this.showVisited.describe("Marks flagged chunks it has already reached.");
        this.section("Digging", this.size, this.wander, this.flags, this.showVisited);
        this.storageToStop.describe("How many storage blocks nearby count as a base.");
        this.baseRadius.describe("How far around you it counts storage blocks (blocks).");
        this.spawners.describe("Also stops when it finds a spawner.");
        this.disconnect.describe("Leaves the server when it finds something.");
        this.section(
                "Base detection",
                this.storageToStop,
                this.baseRadius,
                this.spawners,
                this.disconnect);
        this.quitOnPop.describe("Leaves the server if one of your totems pops.");
        this.keepTotem.describe("Keeps a totem in your offhand.");
        this.eat.describe("Eats golden apples when needed.");
        this.section("Safety", this.quitOnPop, this.keepTotem, this.eat);
        this.mend.describe("Repairs the pickaxe with XP bottles.");
        this.mendBelow.describe("Repairs when durability drops below this.");
        this.section("Pickaxe", this.mend, this.mendBelow);
        this.sellJunk.describe("Sells useless blocks when the inventory fills up.");
        this.buyFromAh.describe("Buys supplies from the AH when out.");
        this.maxPerListing.describe("Most it pays for one whole AH listing.");
        this.refresh.describe("How often it refreshes the AH (milliseconds).");
        this.section(
                "Inventory & AH", this.sellJunk, this.buyFromAh, this.maxPerListing, this.refresh);
        this.chatFeedback.describe("Says what it is doing in chat.");
        this.section("Display", this.chatFeedback);
        this.usesNotifications();
        this.showVisited.visibleWhen(() -> !this.flags.is("Off"));
        this.mendBelow.visibleWhen(this.mend::get);
        this.maxPerListing.visibleWhen(this.buyFromAh::get);
        this.refresh.visibleWhen(this.buyFromAh::get);
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    protected void onEnable() {
        ClientPlayerEntity player = this.mc.player;
        if (player != null && this.mc.world != null) {
            this.tick = 0L;
            this.finds.clear();
            this.breakFails.clear();
            this.blocked.clear();
            this.nextBuy.clear();
            this.steps.clear();
            this.settle = 0;
            this.blockedPlans = 0;
            this.stepsSincePlan = 0;
            this.stepIndex = 0;
            this.breaker = null;
            this.hop = null;
            this.eater = null;
            this.purchase = null;
            this.sell = null;
            this.nextSell = 0L;
            this.oldWayLeft = 0;
            this.decidedSlab = null;
            this.verifyUntil = 0L;
            this.stopped = false;
            this.mending = false;
            this.heading = player.getHorizontalFacing();
            this.axis = this.start = player.getBlockPos();
            this.drift = 0;
            this.nextDrift = 0L;
            this.noiseSeed = this.random.nextInt();
            this.restUntil = 0L;
            this.nextRest = this.nextGap();
            this.seenFrom = this.routeFrom = player.getBlockPos();
            this.shuffle = null;
            this.standTarget = null;
            this.standSpot = null;
            this.idleLook = null;
            this.lingering = false;
            this.leaveAt = 0L;
            this.maxListing = AhPurchase.parseMoney(this.maxPerListing.get());
            this.cellSize = 1;
            if (this.size.is("3x3 Shard Pickaxe")) {
                if (Hands.has(player, SHARD)) {
                    this.cellSize = 3;
                } else {
                    this.say(
                            ModuleChat.body()
                                    .warn("No Shard Pickaxe")
                                    .text(", digging a 1x2 tunnel instead."));
                }
            }
            this.originX = player.getBlockX() - this.cellSize / 2;
            this.originZ = player.getBlockZ() - this.cellSize / 2;
            this.visitedChunks.clear();
            this.hasTarget = false;
            if (!this.flags.is("Off")) {
                this.enableSources();
            }
            ChunkScanner.register(this);
            this.on(TickEvent.TYPE, Stage.PRE, tickEvent -> this.step());
            this.on(Render3DEvent.TYPE, Stage.POST, this::render);
            this.popped = false;
            this.on(
                    PacketAppliedEvent.TYPE,
                    packetAppliedEvent -> {
                        Packet packet = packetAppliedEvent.packet;
                        EntityStatusS2CPacket entityStatusPacket;
                        if (packet instanceof EntityStatusS2CPacket
                                && (entityStatusPacket = (EntityStatusS2CPacket) packet).getStatus()
                                        == 35
                                && this.mc.world != null
                                && this.mc.player != null
                                && entityStatusPacket.getEntity(this.mc.world) == this.mc.player) {
                            this.popped = true;
                        }
                    });
            String string = "Digging a " + (this.cellSize == 3 ? "3x3" : "1x2") + " tunnel";
            this.say(
                    ModuleChat.body()
                            .text(
                                    this.flags.is("Off")
                                            ? string
                                                    + " "
                                                    + this.heading.getId()
                                                    + " at Y "
                                                    + this.start.getY()
                                                    + "."
                                            : string
                                                    + " at Y "
                                                    + this.start.getY()
                                                    + " to the chunks "
                                                    + this.flags.get()
                                                    + " flags, "
                                                    + this.heading.getId()
                                                    + " while there are none."));
        } else {
            this.say(ModuleChat.body().warn("Join a world first."));
            this.requestDisable();
        }
    }

    @Override
    protected void onDisable() {
        ChunkScanner.unregister(this);
        if (this.purchase != null) {
            this.purchase.cancel();
        }
        this.purchase = null;
        if (this.sell != null) {
            this.sell.cancel();
        }
        this.sell = null;
        this.release();
        this.finds.clear();
    }

    private void step() {
        ClientPlayerEntity player = this.mc.player;
        ClientWorld world = this.mc.world;
        if (player != null && world != null && this.mc.interactionManager != null) {
            this.tick++;
            if (this.popped) {
                if (this.quitOnPop.get()) {
                    this.quitOnPop(player);
                    return;
                }
                this.popped = false;
            }
            if (this.stopped) {
                this.linger(player);
            } else {
                if (!this.flags.is("Off")) {
                    this.visitedChunks.add(
                            ChunkPos.toLong(player.getBlockX() >> 4, player.getBlockZ() >> 4));
                    if (this.hasTarget && this.tick % 20L == 0L) {
                        this.watchTarget(player, world);
                    }
                }
                if (player.isDead()) {
                    this.say(
                            ModuleChat.body()
                                    .danger("You died")
                                    .text(", Tunnel Base Finder stopped."));
                    this.requestDisable();
                } else if (this.purchase != null) {
                    this.followPurchase(player);
                } else if (this.verifyUntil > 0L) {
                    this.verifyPurchase(player);
                } else if (this.sell != null) {
                    if (!this.sell.tick()) {
                        this.sell = null;
                        if (freeSlots(player) == 0) {
                            this.nextSell = this.tick + 1200L;
                        }
                    }
                } else if (this.mending) {
                    this.mend(player);
                } else if (this.mc.currentScreen instanceof HandledScreen) {
                    this.release();
                    if (this.screenSince == 0L) {
                        this.screenSince = this.tick;
                    }
                    if (this.tick - this.screenSince > 100L) {
                        player.closeHandledScreen();
                    }
                } else {
                    this.screenSince = 0L;
                    if (this.eater != null) {
                        if (this.eater.tick(this.mc) != Eater.Result.RUNNING) {
                            this.eater = null;
                        }
                    } else if (this.breaker != null || this.hop != null || this.supplies(player)) {
                        if (this.tick % 20L != 0L || !this.checkFinds(player, world)) {
                            this.dig(player, world);
                        }
                    }
                }
            }
        }
    }

    private boolean supplies(ClientPlayerEntity player) {
        if (this.keepTotem.get() && !TOTEM.test(player.getOffHandStack())) {
            this.release();
            if (Hands.has(player, TOTEM)) {
                if (this.tick % 5L == 0L && this.calm(player)) {
                    Hands.toOffhand(this.mc, player, TOTEM);
                }
                return false;
            } else {
                if (!this.startBuy(player, Items.TOTEM_OF_UNDYING, "Totem", "totem", 1)) {
                    this.say(
                            ModuleChat.body()
                                    .danger("No totem")
                                    .text(" and Buy From AH is off, Tunnel Base Finder stopped."));
                    this.requestDisable();
                }
                return false;
            }
        } else {
            if (this.eat.get() && Eater.hungry(player)) {
                if (Hands.has(player, APPLE)) {
                    if (this.calm(player)) {
                        this.eater = new Eater(APPLE);
                    }
                    return false;
                }
                if (this.startBuy(
                        player, Items.GOLDEN_APPLE, "Golden Apples", "golden apple stack", 64)) {
                    return false;
                }
            }
            ItemStack stack;
            if ((stack = this.pickaxe(player)) == null) {
                this.say(
                        ModuleChat.body()
                                .danger("No pickaxe")
                                .text(this.cellSize == 3 ? " (Shard Pickaxe)" : "")
                                .text(", Tunnel Base Finder stopped."));
                this.requestDisable();
                return false;
            } else {
                int i = stack.getMaxDamage() - stack.getDamage();
                if (this.mend.get() && mendable(stack) && i < this.mendBelow.getInt()) {
                    if (Hands.has(player, XP)) {
                        this.release();
                        this.mending = true;
                        this.mendStarted = this.tick;
                        this.feedback("mend", "Mending the pickaxe with XP bottles", 200);
                        return false;
                    }
                    if (this.startBuy(
                            player, Items.EXPERIENCE_BOTTLE, "XP Bottles", "xp bottle stack", 64)) {
                        return false;
                    }
                }
                if (i >= 10 || this.mend.get() && mendable(stack)) {
                    return true;
                } else {
                    this.say(
                            ModuleChat.body()
                                    .danger("Pickaxe almost broken")
                                    .text(", Tunnel Base Finder stopped before it breaks."));
                    this.requestDisable();
                    return false;
                }
            }
        }
    }

    private void mend(ClientPlayerEntity player) {
        ItemStack mainHandStack = player.getMainHandStack();
        boolean bl = this.tool().test(mainHandStack) && mainHandStack.getDamage() == 0;
        if (bl
                || this.tick - this.mendStarted > 1200L
                || !XP.test(player.getOffHandStack()) && !Hands.has(player, XP)) {
            this.mc.options.useKey.setPressed(false);
            this.mending = false;
            if (bl) {
                this.feedback("mended", "Pickaxe mended", 200);
            }
        } else if (!XP.test(player.getOffHandStack())) {
            this.mc.options.useKey.setPressed(false);
            if (this.tick % 5L == 0L && this.calm(player)) {
                Hands.toOffhand(this.mc, player, XP);
            }
        } else if (Hands.hold(this.mc, player, this.tool())) {
            Aim.turn(this.mc, player.getYaw(), 80.0F, 22.0F);
            this.mc.options.useKey.setPressed(player.getPitch() > 60.0F);
        }
    }

    private boolean startBuy(
            ClientPlayerEntity player,
            Item purchaseItem2,
            String purchaseName2,
            String string,
            int i) {
        if (!this.buyFromAh.get()) {
            return false;
        } else if (this.tick < this.nextBuy.getOrDefault(purchaseItem2, 0L)) {
            this.feedback(
                    "wait-" + purchaseName2,
                    "Waiting to try buying " + purchaseName2 + " again",
                    600);
            return true;
        } else if (freeSlots(player) != 0) {
            if (!this.calm(player)) {
                return true;
            } else {
                this.purchaseItem = purchaseItem2;
                this.purchaseName = purchaseName2;
                this.purchaseBefore = count(player, purchaseItem2);
                this.feedback(
                        "buy-" + purchaseName2,
                        "Buying " + purchaseName2 + " (/ah " + string + ")",
                        1);
                this.purchase =
                        AhPurchase.startAny(
                                this,
                                purchaseItem2,
                                purchaseName2,
                                string,
                                itemStack -> itemStack.isOf(purchaseItem2),
                                i,
                                this.maxListing,
                                this.refresh.getInt(),
                                0L);
                return true;
            }
        } else {
            if (!this.sellJunk.get() || this.tick < this.nextSell || !this.startSelling(player)) {
                this.feedback(
                        "room-" + purchaseName2,
                        "No room in the inventory to buy " + purchaseName2,
                        600);
                this.nextBuy.put(purchaseItem2, this.tick + 200L);
            }
            return true;
        }
    }

    private void followPurchase(ClientPlayerEntity player) {
        AhPurchase.Status status = this.purchase.status();
        if (status != AhPurchase.Status.RUNNING) {
            this.purchase = null;
            if (status == AhPurchase.Status.FAILED) {
                this.say(
                        ModuleChat.body()
                                .warn("Could not buy " + this.purchaseName)
                                .text(", trying again in a minute."));
                this.nextBuy.put(this.purchaseItem, this.tick + 1200L);
                this.purchaseItem = null;
            } else {
                this.verifyUntil = this.tick + 60L;
            }
        }
    }

    private void verifyPurchase(ClientPlayerEntity player) {
        int i = count(player, this.purchaseItem) - this.purchaseBefore;
        if (i > 0) {
            this.say(
                    ModuleChat.body()
                            .good("Bought " + i + " " + this.purchaseName)
                            .text(", it is in the inventory."));
            EasyPlace.LOG.info(
                    "[Tunnel Base Finder] bought {} {} (verified)", i, this.purchaseName);
        } else {
            if (this.tick < this.verifyUntil) {
                return;
            }
            this.say(
                    ModuleChat.body()
                            .warn("The AH sold " + this.purchaseName + " but it never showed up")
                            .text(", trying again later."));
            this.nextBuy.put(this.purchaseItem, this.tick + 1200L);
        }
        this.verifyUntil = 0L;
        this.purchaseItem = null;
    }

    private boolean startSelling(ClientPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        HashMap map = new HashMap();
        for (int i = 35; i >= 0 && map.size() < 4; i--) {
            Item item = inventory.getStack(i).getItem();
            if (i != inventory.getSelectedSlot() && JUNK.contains(item)) {
                map.put(i, item);
            }
        }
        if (map.isEmpty()) {
            return false;
        } else if (!this.calm(player)) {
            return true;
        } else {
            this.feedback(
                    "selling",
                    "Selling " + map.size() + " stacks of junk stone with /sell to make room",
                    1);
            this.sell = DonutSell.start(this, map);
            return true;
        }
    }

    private void dig(ClientPlayerEntity player, ClientWorld world) {
        if (this.settle > 0) {
            this.settle--;
            if (this.lingering) {
                this.idle(player);
            }
        } else {
            this.lingering = false;
            if (this.breaker != null) {
                Breaker.Result result = this.breaker.tick(this.mc);
                if (result != Breaker.Result.RUNNING) {
                    BlockPos pos = this.breaker.pos();
                    this.breaker = null;
                    int i =
                            this.settle =
                                    this.cellSize == 3
                                            ? this.lognormal(2.2, 0.3, 2, 6)
                                            : this.lognormal(1.0, 0.5, 0, 4);
                    if (result == Breaker.Result.FAILED
                            && this.breakFails.merge(pos.asLong(), 1, Integer::sum) >= 3) {
                        this.blocked.add(this.cellKey(pos));
                        this.steps.clear();
                    }
                }
            } else if (this.shuffle != null) {
                Shuffle.Result result2 = this.shuffle.tick(this.mc);
                if (result2 != Shuffle.Result.RUNNING) {
                    boolean quick = this.shuffle.quick();
                    this.shuffle = null;
                    if (result2 == Shuffle.Result.FAILED) {
                        this.standSpot = null;
                    } else if (!quick) {
                        this.settle = this.lognormal(1.5, 0.5, 0, 6);
                    }
                }
            } else if (this.hop != null) {
                Hop.Result result3 = this.hop.tick(this.mc);
                if (result3 != Hop.Result.RUNNING) {
                    this.hop = null;
                    if (result3 == Hop.Result.FAILED) {
                        this.steps.clear();
                    }
                }
            } else if (!this.rest(player)) {
                if (Hands.hold(this.mc, player, this.tool())) {
                    if (this.hasTarget && this.arrived(player)) {
                        this.endTarget(player, null);
                    }
                    if (this.stepIndex < this.steps.size() && this.stepsSincePlan < 9
                            || this.plan(player, world)) {
                        BlockPos step = this.steps.get(this.stepIndex);
                        BlockPos pos2 =
                                this.stepIndex > 0
                                        ? this.steps.get(this.stepIndex - 1)
                                        : this.routeFrom;
                        Direction direction = beside(pos2, step);
                        boolean bl = this.cellKey(step) != this.cellKey(pos2);
                        this.seenFrom = player.getBlockPos();
                        if (direction != null
                                && (!bl
                                        || Boolean.TRUE.equals(
                                                this.safe(
                                                        world,
                                                        this.cellX(step.getX()),
                                                        this.cellZ(step.getZ()),
                                                        step.getY())))) {
                            BlockPos mine = this.toMine(world, step, direction);
                            if (!this.stream(player, world, step, direction, mine)) {
                                if (mine != null) {
                                    Vec3d vec = this.stand(world, player, mine, direction);
                                    if (vec == null) {
                                        if (flat(player, pos2) > 0.7) {
                                            this.hop = new Hop(pos2, false);
                                            return;
                                        }
                                    } else if (Math.hypot(
                                                    vec.x - player.getX(), vec.z - player.getZ())
                                            > 0.3) {
                                        this.shuffle =
                                                new Shuffle(
                                                        vec, mine.toCenterPos(), !this.standVaried);
                                        return;
                                    }
                                    if (this.random.nextFloat() < 0.01F) {
                                        this.settle = this.lognormal(20.0, 0.5, 8, 100);
                                        this.idleLook = null;
                                        this.lingering = true;
                                    } else {
                                        this.breaker =
                                                new Breaker(
                                                        mine.toImmutable(),
                                                        false,
                                                        this.face(step, mine, direction));
                                        this.standTarget = null;
                                    }
                                } else {
                                    if (flat(player, step) <= 3.0) {
                                        this.stepIndex++;
                                        this.stepsSincePlan++;
                                    } else if (flat(player, pos2) > 1.2) {
                                        this.hop = new Hop(pos2, true);
                                    } else {
                                        boolean bl2 =
                                                this.stepIndex + 1 < this.steps.size()
                                                        && beside(
                                                                        step,
                                                                        this.steps.get(
                                                                                this.stepIndex + 1))
                                                                == direction
                                                        && this.toMine(
                                                                        world,
                                                                        this.steps.get(
                                                                                this.stepIndex + 1),
                                                                        direction)
                                                                == null;
                                        this.hop = new Hop(step, bl2);
                                        this.stepIndex++;
                                        this.stepsSincePlan++;
                                    }
                                }
                            }
                        } else {
                            this.endStream();
                            this.steps.clear();
                        }
                    }
                }
            }
        }
    }

    private boolean stream(
            ClientPlayerEntity player,
            ClientWorld world,
            BlockPos pos,
            Direction direction,
            BlockPos pos2) {
        if (this.streamTarget != null && world.getBlockState(this.streamTarget).isAir()) {
            this.headBrokeAt = this.tick;
            this.streamTarget = null;
        }
        BlockPos pos3 = pos.up();
        boolean bl = pos2 != null && pos2.equals(pos) && this.tick - this.headBrokeAt < 4L;
        if (this.cellSize == 3
                && this.straight(direction)
                && !(Math.abs(aside(player, pos, direction)) > 0.45)
                && (pos2 == null || pos2.equals(pos3) || bl)) {
            if (pos2 != null && pos2.equals(pos3) && !pos3.equals(this.decidedSlab)) {
                this.decidedSlab = pos3.toImmutable();
                if (this.oldWayLeft > 0) {
                    this.oldWayLeft--;
                }
                if (this.oldWayLeft == 0 && this.random.nextFloat() < 0.06F) {
                    this.oldWayLeft = 2 + this.random.nextInt(4);
                }
            }
            if (this.oldWayLeft > 0) {
                this.endStream();
                return false;
            } else {
                this.streaming = true;
                Vec3d vec =
                        Vec3d.ofCenter(pos3)
                                .subtract(
                                        direction.getOffsetX() * 0.5,
                                        0.0,
                                        direction.getOffsetZ() * 0.5);
                float[] rotation = PlacementUtil.calcRotation(player.getEyePos(), vec);
                Aim.turn(this.mc, rotation[0], rotation[1], 22.0F);
                double d =
                        (vec.x - player.getX()) * direction.getOffsetX()
                                + (vec.z - player.getZ()) * direction.getOffsetZ();
                this.mc.options.forwardKey.setPressed(pos2 == null || d > 1.0);
                if (pos2 == null) {
                    this.stepIndex++;
                    this.stepsSincePlan++;
                    return true;
                } else if (bl) {
                    return true;
                } else {
                    BlockHitResult blockHitResult =
                            Aim.trace(
                                    this.mc,
                                    player,
                                    player.getYaw(),
                                    player.getPitch(),
                                    player.getBlockInteractionRange());
                    if (blockHitResult != null
                            && blockHitResult.getType() == Type.BLOCK
                            && blockHitResult.getBlockPos().equals(pos3)) {
                        this.streamTarget = pos3.toImmutable();
                        if (this.mc.interactionManager.updateBlockBreakingProgress(
                                pos3, blockHitResult.getSide())) {
                            player.swingHand(Hand.MAIN_HAND);
                        }
                    }
                    return true;
                }
            }
        } else {
            this.endStream();
            return false;
        }
    }

    private boolean straight(Direction direction) {
        return this.stepIndex + 1 < this.steps.size()
                && beside(this.steps.get(this.stepIndex), this.steps.get(this.stepIndex + 1))
                        == direction;
    }

    private static double aside(ClientPlayerEntity player, BlockPos pos, Direction direction) {
        Direction direction2 = direction.rotateYClockwise();
        return (player.getX() - pos.getX() - 0.5) * direction2.getOffsetX()
                + (player.getZ() - pos.getZ() - 0.5) * direction2.getOffsetZ();
    }

    private void endStream() {
        if (this.streaming) {
            this.streaming = false;
            this.streamTarget = null;
            this.mc.options.forwardKey.setPressed(false);
            if (this.mc.interactionManager != null
                    && this.mc.interactionManager.isBreakingBlock()) {
                this.mc.interactionManager.cancelBlockBreaking();
            }
        }
    }

    private int lognormal(double d, double e, int i, int j) {
        return (int)
                MathHelper.clamp(
                        Math.round(Math.exp(Math.log(d) + this.random.nextGaussian() * e)),
                        (long) i,
                        (long) j);
    }

    private static double flat(ClientPlayerEntity player, BlockPos pos) {
        return Math.hypot(pos.getX() + 0.5 - player.getX(), pos.getZ() + 0.5 - player.getZ());
    }

    private Vec3d stand(
            ClientWorld world, ClientPlayerEntity player, BlockPos pos, Direction direction) {
        if (pos.equals(this.standTarget)) {
            return this.standSpot;
        } else {
            this.standTarget = pos.toImmutable();
            this.standSpot = null;
            double d = pos.getX() + 0.5;
            double e = pos.getZ() + 0.5;
            double d2 = player.getY();
            Direction direction2 = direction.rotateYClockwise();
            double e2 = this.cellSize == 3 ? 0.7 : 0.2;
            double d3 =
                    (d - player.getX()) * direction.getOffsetX()
                            + (e - player.getZ()) * direction.getOffsetZ();
            double e3 =
                    (player.getX() - d) * direction2.getOffsetX()
                            + (player.getZ() - e) * direction2.getOffsetZ();
            double d4 = this.cellSize == 3 && this.oldWayLeft == 0 ? Math.min(e2, 0.45) : e2;
            if (d3 >= 1.0
                    && d3 <= 2.6
                    && Math.abs(e3) <= d4
                    && open(world, player, player.getEntityPos())) {
                this.standSpot = player.getEntityPos();
                return this.standSpot;
            } else {
                this.standVaried = this.random.nextFloat() < 0.2F;
                for (int i = 0; i < 16; i++) {
                    double e4 =
                            1.0
                                    + this.random.nextDouble()
                                            * (this.standVaried ? 0.6000000000000001 : 0.3);
                    double d5 =
                            this.standVaried ? (this.random.nextDouble() * 2.0 - 1.0) * e2 : 0.0;
                    Vec3d vec =
                            new Vec3d(
                                    d - direction.getOffsetX() * e4 + direction2.getOffsetX() * d5,
                                    d2,
                                    e - direction.getOffsetZ() * e4 + direction2.getOffsetZ() * d5);
                    if (open(world, player, vec)) {
                        this.standSpot = vec;
                        return vec;
                    }
                }
                return null;
            }
        }
    }

    private static boolean open(ClientWorld world, ClientPlayerEntity player, Vec3d vec) {
        Box box = player.getDimensions(EntityPose.STANDING).getBoxAt(vec).contract(0.02);
        if (!world.isSpaceEmpty(player, box)) {
            return false;
        } else {
            BlockPos pos = BlockPos.ofFloored(vec.x, vec.y - 0.5, vec.z);
            return !world.getBlockState(pos).getCollisionShape(world, pos).isEmpty();
        }
    }

    private BlockPos toMine(ClientWorld world, BlockPos pos, Direction direction) {
        BlockPos pos2 = pos.up();
        if (solid(world, pos2)) {
            return pos2;
        } else {
            return solid(world, pos) ? pos : null;
        }
    }

    private Direction face(BlockPos pos, BlockPos pos2, Direction direction) {
        return this.cellSize == 3 && pos2.equals(pos) ? Direction.UP : direction.getOpposite();
    }

    private static boolean solid(ClientWorld world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return !blockState.getCollisionShape(world, pos).isEmpty();
    }

    private boolean rest(ClientPlayerEntity player) {
        if (this.tick < this.restUntil) {
            this.idle(player);
            return true;
        } else if (this.tick >= this.nextRest) {
            this.release();
            this.restUntil = this.tick + this.lognormal(30.0, 0.6, 10, 200);
            this.nextRest = this.restUntil + this.nextGap();
            this.idleLook = null;
            return true;
        } else {
            return false;
        }
    }

    private long nextGap() {
        return (long)
                MathHelper.clamp(
                        -Math.log(1.0 - this.random.nextDouble()) * 2400.0, 600.0, 12000.0);
    }

    private void idle(ClientPlayerEntity player) {
        if (this.idleLook == null || this.random.nextFloat() < 0.025F) {
            this.idleLook =
                    new float[] {
                        player.getYaw() + (float) (this.random.nextGaussian() * 40.0),
                        MathHelper.clamp(
                                (float) (10.0 + this.random.nextGaussian() * 22.0), -45.0F, 70.0F)
                    };
        }
        Aim.turn(this.mc, this.idleLook[0], this.idleLook[1], 22.0F);
    }

    private boolean plan(ClientPlayerEntity player, ClientWorld world) {
        this.steps.clear();
        this.stepIndex = 0;
        this.stepsSincePlan = 0;
        BlockPos pos;
        this.routeFrom = pos = player.getBlockPos();
        this.seenFrom = pos;
        this.perceive = 3 + this.random.nextInt(3);
        int i = pos.getY();
        int cellX = this.cellX(pos.getX());
        int cellZ = this.cellZ(pos.getZ());
        this.pickTarget(player);
        boolean hasTarget2 = this.hasTarget;
        int j = hasTarget2 ? this.cellX(middle(ChunkPos.getPackedX(this.target))) : 0;
        int k = hasTarget2 ? this.cellZ(middle(ChunkPos.getPackedZ(this.target))) : 0;
        if (!hasTarget2 && this.wander.get() && this.tick >= this.nextDrift) {
            this.drift = MathHelper.clamp(this.drift + this.random.nextInt(3) - 1, -2, 2);
            this.nextDrift = this.tick + 20L * (30 + this.random.nextInt(60));
        }
        int offsetX = this.heading.getOffsetX();
        int offsetZ = this.heading.getOffsetZ();
        Direction direction = this.heading.rotateYClockwise();
        int offsetX2 = direction.getOffsetX();
        int offsetZ2 = direction.getOffsetZ();
        int cellX2 = this.cellX(this.axis.getX());
        int cellZ2 = this.cellZ(this.axis.getZ());
        int m = Math.max(4, 40 / this.cellSize);
        int n = Math.max(3, 15 / this.cellSize);
        int o = (cellX - cellX2) * offsetX2 + (cellZ - cellZ2) * offsetZ2;
        long cellChunk = this.cellChunk(cellX, cellZ);
        long l = hasTarget2 ? this.target : -9223372036854775808L;
        HashMap map = new HashMap();
        PriorityQueue priorityQueue =
                new PriorityQueue<>(
                        (TunnelMinerModule.Node node, TunnelMinerModule.Node node2) ->
                                Double.compare(node.rank, node2.rank));
        HashSet set = new HashSet();
        TunnelMinerModule.Node node3;
        TunnelMinerModule.Node node4 =
                node3 = new TunnelMinerModule.Node(cellX, cellZ, 0.0, 0.0, null);
        double d =
                hasTarget2
                        ? -apart(cellX, cellZ, j, k)
                        : this.score(
                                cellX, cellZ, cellX, cellZ, offsetX, offsetZ, offsetX2, offsetZ2,
                                cellX2, cellZ2);
        priorityQueue.add(node3);
        int p = 0;
        while (!priorityQueue.isEmpty() && p++ < 6000) {
            TunnelMinerModule.Node priority = (TunnelMinerModule.Node) priorityQueue.poll();
            if (set.add(key(priority.x, priority.z))) {
                double e =
                        hasTarget2
                                ? -apart(priority.x, priority.z, j, k)
                                : this.score(
                                        priority.x,
                                        priority.z,
                                        cellX,
                                        cellZ,
                                        offsetX,
                                        offsetZ,
                                        offsetX2,
                                        offsetZ2,
                                        cellX2,
                                        cellZ2);
                if (e > d || e == d && priority.cost < node4.cost) {
                    node4 = priority;
                    d = e;
                }
                for (Direction direction2 : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
                    int q = priority.x + direction2.getOffsetX();
                    int r;
                    long key = key(q, r = priority.z + direction2.getOffsetZ());
                    if (!set.contains(key) && !this.blocked.contains(key)) {
                        if (hasTarget2) {
                            if (Math.abs(q - cellX) > m || Math.abs(r - cellZ) > m) {
                                continue;
                            }
                        } else {
                            int t = (q - cellX) * offsetX + (r - cellZ) * offsetZ;
                            int u = (q - cellX2) * offsetX2 + (r - cellZ2) * offsetZ2;
                            if (t < -2 || t > m || Math.abs(u - o) > n) {
                                continue;
                            }
                        }
                        if (Boolean.TRUE.equals(
                                map.computeIfAbsent(key, long_ -> this.safe(world, q, r, i)))) {
                            double d2 =
                                    priority.cost
                                            + 1.0
                                            + this.noise(q, r)
                                            + this.visitedCost(q, r, cellChunk, l);
                            double e2 =
                                    hasTarget2
                                            ? apart(q, r, j, k)
                                            : -this.score(
                                                    q, r, cellX, cellZ, offsetX, offsetZ, offsetX2,
                                                    offsetZ2, cellX2, cellZ2);
                            priorityQueue.add(
                                    new TunnelMinerModule.Node(q, r, d2, d2 + e2, priority));
                        }
                    }
                }
            }
        }
        if (hasTarget2) {
            int apart = apart(node4.x, node4.z, j, k);
            int apart2 = apart(cellX, cellZ, j, k);
            if (node4 == node3) {
                this.endTarget(player, apart == 0 ? null : "could not get any closer");
                return false;
            }
            if (apart > 0) {
                if (apart >= this.targetBest && apart2 >= this.targetNear) {
                    if (++this.targetStalls >= 3) {
                        this.endTarget(player, "could not get any closer");
                        return false;
                    }
                } else {
                    this.targetBest = Math.min(this.targetBest, apart);
                    this.targetNear = Math.min(this.targetNear, apart2);
                    this.targetStalls = 0;
                }
            }
        } else if (node4 == node3) {
            if (++this.blockedPlans > 4) {
                this.say(
                        ModuleChat.body()
                                .danger("Nowhere safe to dig")
                                .text(", Tunnel Base Finder stopped."));
                this.requestDisable();
                return false;
            }
            this.heading =
                    this.blockedPlans % 2 == 1
                            ? this.heading.rotateYClockwise()
                            : this.heading.rotateYCounterclockwise().rotateYCounterclockwise();
            this.axis = pos;
            this.drift = 0;
            this.feedback(
                    "turn", "Blocked ahead, digging " + this.heading.getId() + " instead", 40);
            return false;
        }
        this.blockedPlans = 0;
        ArrayList list = new ArrayList();
        for (TunnelMinerModule.Node node5 = node4; node5 != null; node5 = node5.parent) {
            list.add(0, node5);
        }
        BlockPos pos2 = this.center((TunnelMinerModule.Node) list.get(0), i);
        BlockPos pos3 = this.walkTo(pos, pos2);
        for (int r2 = 1; r2 < list.size(); r2++) {
            pos3 = this.walkTo(pos3, this.center((TunnelMinerModule.Node) list.get(r2), i));
        }
        return !this.steps.isEmpty();
    }

    private BlockPos walkTo(BlockPos pos, BlockPos pos2) {
        BlockPos pos3 = pos;
        while (pos3.getX() != pos2.getX()) {
            pos3 = pos3.add(Integer.signum(pos2.getX() - pos3.getX()), 0, 0);
            this.steps.add(pos3);
        }
        while (pos3.getZ() != pos2.getZ()) {
            pos3 = pos3.add(0, 0, Integer.signum(pos2.getZ() - pos3.getZ()));
            this.steps.add(pos3);
        }
        return pos3;
    }

    private BlockPos center(TunnelMinerModule.Node node, int i) {
        int j = this.cellSize / 2;
        return new BlockPos(
                node.x * this.cellSize + this.originX + j,
                i,
                node.z * this.cellSize + this.originZ + j);
    }

    private int cellX(int i) {
        return Math.floorDiv(i - this.originX, this.cellSize);
    }

    private int cellZ(int i) {
        return Math.floorDiv(i - this.originZ, this.cellSize);
    }

    private double score(int i, int j, int k, int m, int n, int o, int p, int q, int r, int t) {
        int u = (i - k) * n + (j - m) * o;
        int v = (i - r) * p + (j - t) * q;
        return u - 2.0 * Math.abs(v - this.drift);
    }

    private LongSet flagged() {
        LongOpenHashSet longOpenHashSet = new LongOpenHashSet();
        SusChunkFinderModule module;
        if (!this.flags.is("Better Sus Chunk")
                && (module = ModuleRegistry.get(SusChunkFinderModule.class)) != null
                && module.isEnabled()) {
            longOpenHashSet.addAll(module.flaggedChunks());
        }
        BetterSusChunkModule module2;
        if (!this.flags.is("Sus Chunk")
                && (module2 = ModuleRegistry.get(BetterSusChunkModule.class)) != null
                && module2.isEnabled()) {
            longOpenHashSet.addAll(module2.flaggedChunks());
        }
        return longOpenHashSet;
    }

    private void enableSources() {
        if (!this.flags.is("Better Sus Chunk")) {
            this.turnOn(ModuleRegistry.get(SusChunkFinderModule.class), "Sus Chunk Finder");
        }
        if (!this.flags.is("Sus Chunk")) {
            this.turnOn(ModuleRegistry.get(BetterSusChunkModule.class), "Better Sus Chunk");
        }
    }

    private void turnOn(ClientModule module, String string) {
        if (module != null && !module.isEnabled()) {
            module.setEnabled(true);
            this.say(ModuleChat.body().text("Turned on " + string + " to find chunks to dig to."));
        }
    }

    private void pickTarget(ClientPlayerEntity player) {
        if (!this.flags.is("Off") && !this.hasTarget) {
            double d = 1.7976931348623157E308;
            long l = 0L;
            LongIterator longIterator = this.flagged().iterator();
            while (longIterator.hasNext()) {
                long l2 = longIterator.nextLong();
                if (!this.visitedChunks.contains(l2)) {
                    int middle = middle(ChunkPos.getPackedX(l2));
                    int middle2 = middle(ChunkPos.getPackedZ(l2));
                    double e;
                    if ((Math.abs(middle - this.start.getX()) > 48
                                    || Math.abs(middle2 - this.start.getZ()) > 48)
                            && (e = Math.hypot(middle - player.getX(), middle2 - player.getZ()))
                                    < d) {
                        d = e;
                        l = l2;
                    }
                }
            }
            if (d != 1.7976931348623157E308) {
                this.hasTarget = true;
                this.target = l;
                this.targetBest = 2147483647;
                this.targetNear = 2147483647;
                this.targetStalls = 0;
                this.unflaggedChecks = 0;
                int middle3 = middle(ChunkPos.getPackedX(l));
                int middle4 = middle(ChunkPos.getPackedZ(l));
                this.feedback(
                        "target",
                        "Digging to a flagged chunk at "
                                + middle3
                                + " "
                                + middle4
                                + " ("
                                + (int) d
                                + " blocks)",
                        1);
                EasyPlace.LOG.info(
                        "[Tunnel Base Finder] digging to flagged chunk at {} {} ({} blocks)",
                        middle3,
                        middle4,
                        (int) d);
            }
        }
    }

    private void watchTarget(ClientPlayerEntity player, ClientWorld world) {
        if (!this.flagged().contains(this.target)
                && world.isChunkLoaded(
                        ChunkPos.getPackedX(this.target), ChunkPos.getPackedZ(this.target))) {
            if (++this.unflaggedChecks >= 10) {
                this.endTarget(player, "it is not flagged any more");
            }
        } else {
            this.unflaggedChecks = 0;
        }
    }

    private boolean arrived(ClientPlayerEntity player) {
        return Math.abs(player.getBlockX() - middle(ChunkPos.getPackedX(this.target))) <= 3
                && Math.abs(player.getBlockZ() - middle(ChunkPos.getPackedZ(this.target))) <= 3;
    }

    private void endTarget(ClientPlayerEntity player, String string) {
        int middle = middle(ChunkPos.getPackedX(this.target));
        int middle2 = middle(ChunkPos.getPackedZ(this.target));
        this.visitedChunks.add(this.target);
        this.hasTarget = false;
        this.steps.clear();
        this.heading = player.getHorizontalFacing();
        this.axis = player.getBlockPos();
        this.drift = 0;
        String string2 =
                string == null
                        ? "Dug through the flagged chunk at " + middle + " " + middle2
                        : "Left the flagged chunk at "
                                + middle
                                + " "
                                + middle2
                                + " ("
                                + string
                                + ")";
        this.feedback("target-end", string2, 1);
        EasyPlace.LOG.info("[Tunnel Base Finder] {}", string2);
    }

    private double visitedCost(int i, int j, long l, long l2) {
        if (this.flags.is("Off")) {
            return 0.0;
        } else {
            long cellChunk = this.cellChunk(i, j);
            return cellChunk != l && cellChunk != l2 && this.visitedChunks.contains(cellChunk)
                    ? 4.0
                    : 0.0;
        }
    }

    private long cellChunk(int i, int j) {
        int k = this.cellSize / 2;
        return ChunkPos.toLong(
                i * this.cellSize + this.originX + k >> 4,
                j * this.cellSize + this.originZ + k >> 4);
    }

    private static int middle(int i) {
        return i * 16 + 8;
    }

    private static int apart(int i, int j, int k, int m) {
        return Math.abs(i - k) + Math.abs(j - m);
    }

    private void render(Render3DEvent event) {
        if (this.showVisited.get() && !this.flags.is("Off") && !this.visitedChunks.isEmpty()) {
            this.visitedOverlay.draw(this.visitedChunks, 1431357695, event.partialTick);
        }
    }

    private double noise(int i, int j) {
        if (!this.wander.get()) {
            return 0.0;
        } else {
            int k = i * 73856093 ^ j * 19349663 ^ this.noiseSeed;
            k ^= k >>> 13;
            k *= 1540483477;
            k ^= k >>> 15;
            return (k & 1023) / 1023.0 * 0.7;
        }
    }

    private Boolean safe(ClientWorld world, int i, int j, int k) {
        int cellSize2 = this.cellSize;
        int m = cellSize2 == 3 ? 3 : 2;
        int n = i * cellSize2 + this.originX;
        int o = j * cellSize2 + this.originZ;
        int p =
                Math.max(
                        0,
                        Math.max(
                                n - this.seenFrom.getX(),
                                this.seenFrom.getX() - (n + cellSize2 - 1)));
        if (Math.max(
                        p,
                        Math.max(
                                0,
                                Math.max(
                                        o - this.seenFrom.getZ(),
                                        this.seenFrom.getZ() - (o + cellSize2 - 1))))
                > this.perceive) {
            return world.isPosLoaded(new BlockPos(n, k, o)) ? Boolean.TRUE : null;
        } else {
            Mutable mutable = new Mutable();
            Mutable mutable2 = new Mutable();
            for (int r = n; r < n + cellSize2; r++) {
                for (int t = o; t < o + cellSize2; t++) {
                    mutable.set(r, k - 1, t);
                    if (!world.isPosLoaded(mutable)) {
                        return null;
                    }
                    BlockState blockState = world.getBlockState(mutable);
                    if (!blockState.getFluidState().isEmpty()
                            || blockState.getCollisionShape(world, mutable).isEmpty()) {
                        return false;
                    }
                    mutable.set(r, k + m, t);
                    if (world.getBlockState(mutable).getBlock() instanceof FallingBlock) {
                        return false;
                    }
                    for (int u = 0; u < m; u++) {
                        mutable.set(r, k + u, t);
                        BlockState blockState2 = world.getBlockState(mutable);
                        if (!blockState2.getFluidState().isEmpty()
                                || blockState2.getBlock() instanceof InfestedBlock
                                || blockState2.getHardness(world, mutable) < 0.0F) {
                            return false;
                        }
                        for (Direction direction : Direction.values()) {
                            mutable2.set(mutable, direction);
                            if (!world.getBlockState(mutable2).getFluidState().isEmpty()) {
                                return false;
                            }
                        }
                    }
                }
            }
            return true;
        }
    }

    private long cellKey(BlockPos pos) {
        return key(this.cellX(pos.getX()), this.cellZ(pos.getZ()));
    }

    private static long key(int i, int j) {
        return (long) i << 32 | j & 4294967295L;
    }

    private static Direction beside(BlockPos pos, BlockPos pos2) {
        if (pos.getY() != pos2.getY()) {
            return null;
        } else {
            int i = pos2.getX() - pos.getX();
            int j = pos2.getZ() - pos.getZ();
            if (Math.abs(i) + Math.abs(j) != 1) {
                return null;
            } else {
                return i > 0
                        ? Direction.EAST
                        : (i < 0 ? Direction.WEST : (j > 0 ? Direction.SOUTH : Direction.NORTH));
            }
        }
    }

    @Override
    public boolean wants(BlockState state) {
        return state.isOf(Blocks.SPAWNER)
                || state.isIn(BlockTags.SHULKER_BOXES)
                || STORAGE.contains(state.getBlock());
    }

    @Override
    public void found(long l, BlockState state) {
        this.finds.put(l, state.isOf(Blocks.SPAWNER));
    }

    @Override
    public void removed(long l) {
        this.finds.remove(l);
    }

    @Override
    public void reset() {
        this.finds.clear();
    }

    private boolean checkFinds(ClientPlayerEntity player, ClientWorld world) {
        int baseRadius2 = this.baseRadius.getInt();
        int i = 0;
        long l = 0L;
        long l2 = 0L;
        BlockPos pos = null;
        for (Entry entry : this.finds.entrySet()) {
            BlockPos pos2 = BlockPos.fromLong((Long) entry.getKey());
            if ((Math.abs(pos2.getX() - this.start.getX()) > 48
                            || Math.abs(pos2.getZ() - this.start.getZ()) > 48)
                    && Math.abs(pos2.getX() - player.getBlockX()) <= baseRadius2
                    && Math.abs(pos2.getZ() - player.getBlockZ()) <= baseRadius2) {
                if ((Boolean) entry.getValue()) {
                    if (pos == null && !spiderSpawner(world, pos2)) {
                        pos = pos2;
                    }
                } else {
                    i++;
                    l += pos2.getX();
                    l2 += pos2.getZ();
                }
            }
        }
        if (this.spawners.get() && pos != null) {
            this.found("a spawner", pos);
            return true;
        } else if (i >= this.storageToStop.getInt()) {
            this.found(
                    "a base (" + i + " storage blocks)",
                    new BlockPos((int) (l / i), player.getBlockY(), (int) (l2 / i)));
            return true;
        } else {
            return false;
        }
    }

    private static boolean spiderSpawner(ClientWorld world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof MobSpawnerBlockEntity mobSpawnerBlockEntity)) {
            return false;
        } else {
            Entity renderedEntity = mobSpawnerBlockEntity.getLogic().getRenderedEntity(world, pos);
            return renderedEntity != null
                    && (renderedEntity.getType() == EntityType.SPIDER
                            || renderedEntity.getType() == EntityType.CAVE_SPIDER);
        }
    }

    private void found(String foundWhat2, BlockPos pos) {
        this.stopped = true;
        this.release();
        this.foundWhat = foundWhat2;
        this.foundAt = pos.toImmutable();
        EasyPlace.LOG.info(
                "[Tunnel Base Finder] found {} at {} {} {}",
                foundWhat2,
                pos.getX(),
                pos.getY(),
                pos.getZ());
        this.say(
                ModuleChat.body()
                        .danger("Found " + foundWhat2)
                        .text(" at ")
                        .coords(pos.getX(), pos.getY(), pos.getZ()));
        if (this.notificationsOn()) {
            this.mc
                    .getSoundManager()
                    .play(PositionedSoundInstance.ui(SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.0F));
        }
        if (!this.disconnect.get()) {
            this.requestDisable();
        } else {
            this.leaveAt = this.tick + this.lognormal(260.0, 0.6, 70, 1400);
            this.idleLook = null;
        }
    }

    private void quitOnPop(ClientPlayerEntity player) {
        this.popped = false;
        this.release();
        this.stopped = true;
        BlockPos pos = player.getBlockPos();
        EasyPlace.LOG.info(
                "[Tunnel Base Finder] totem popped at {} {} {}, leaving the server",
                pos.getX(),
                pos.getY(),
                pos.getZ());
        if (this.mc.getNetworkHandler() != null) {
            AutoReconnectModule.holdOff();
            this.mc
                    .getNetworkHandler()
                    .getConnection()
                    .disconnect(
                            Text.literal(
                                    "[Tunnel Base Finder] Totem popped at "
                                            + pos.getX()
                                            + " "
                                            + pos.getY()
                                            + " "
                                            + pos.getZ()
                                            + ", left the server"));
        }
        this.requestDisable();
    }

    private void linger(ClientPlayerEntity player) {
        if (this.leaveAt != 0L) {
            this.idle(player);
            if (this.tick >= this.leaveAt) {
                this.leaveAt = 0L;
                if (this.mc.getNetworkHandler() != null) {
                    AutoReconnectModule.holdOff();
                    this.mc
                            .getNetworkHandler()
                            .getConnection()
                            .disconnect(
                                    Text.literal(
                                            "[Tunnel Base Finder] Found "
                                                    + this.foundWhat
                                                    + " at "
                                                    + this.foundAt.getX()
                                                    + " "
                                                    + this.foundAt.getY()
                                                    + " "
                                                    + this.foundAt.getZ()));
                }
                this.requestDisable();
            }
        }
    }

    private Predicate<ItemStack> tool() {
        return this.cellSize == 3 ? SHARD : PICKAXE;
    }

    private ItemStack pickaxe(ClientPlayerEntity player) {
        Predicate predicate = this.tool();
        if (predicate.test(player.getMainHandStack())) {
            return player.getMainHandStack();
        } else {
            PlayerInventory inventory = player.getInventory();
            for (int i = 0; i < 36; i++) {
                if (predicate.test(inventory.getStack(i))) {
                    return inventory.getStack(i);
                }
            }
            return null;
        }
    }

    static boolean shardName(ItemStack stack) {
        String string = stack.getName().getString().toLowerCase(Locale.ROOT);
        return string.contains("shard") || string.contains("amethyst");
    }

    private static boolean mendable(ItemStack stack) {
        return stack.getEnchantments().getEnchantments().stream()
                .anyMatch(registryEntry -> registryEntry.matchesKey(Enchantments.MENDING));
    }

    private static int count(ClientPlayerEntity player, Item item) {
        int i = player.getOffHandStack().isOf(item) ? player.getOffHandStack().getCount() : 0;
        PlayerInventory inventory = player.getInventory();
        for (int j = 0; j < 36; j++) {
            ItemStack stack = inventory.getStack(j);
            if (stack.isOf(item)) {
                i += stack.getCount();
            }
        }
        return i;
    }

    private static int freeSlots(ClientPlayerEntity player) {
        int i = 0;
        PlayerInventory inventory = player.getInventory();
        for (int j = 0; j < 36; j++) {
            if (inventory.getStack(j).isEmpty()) {
                i++;
            }
        }
        return i;
    }

    private boolean calm(ClientPlayerEntity player) {
        this.release();
        return InventoryCalm.still(player);
    }

    private void release() {
        if (this.breaker != null) {
            this.breaker.release(this.mc);
        }
        this.breaker = null;
        if (this.hop != null) {
            this.hop.release(this.mc);
        }
        this.hop = null;
        if (this.eater != null) {
            this.eater.release(this.mc);
        }
        this.eater = null;
        if (this.shuffle != null) {
            this.shuffle.release(this.mc);
        }
        this.shuffle = null;
        this.standTarget = null;
        this.streaming = false;
        this.streamTarget = null;
        this.mc.options.useKey.setPressed(false);
        this.mc.options.forwardKey.setPressed(false);
        this.mc.options.backKey.setPressed(false);
        this.mc.options.leftKey.setPressed(false);
        this.mc.options.rightKey.setPressed(false);
        this.mc.options.jumpKey.setPressed(false);
        this.mc.options.sprintKey.setPressed(false);
        this.mc.options.sneakKey.setPressed(false);
        if (this.mc.interactionManager != null) {
            this.mc.interactionManager.cancelBlockBreaking();
        }
    }

    private void say(ModuleChat.Body body) {
        ModuleChat.send(this, body);
    }

    private void feedback(String string, String string2, int i) {
        if (this.chatFeedback.get()) {
            long l = this.lastFeedback.getOrDefault(string, -4611686018427387904L);
            if (this.tick - l >= i) {
                this.lastFeedback.put(string, this.tick);
                ModuleChat.send(this, string2);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    private record Node(int x, int z, double cost, double rank, TunnelMinerModule.Node parent) {}
}
