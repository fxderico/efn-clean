/*
 * Decompiled with CFR 0.152.
 */
package net.efn.client.easyplace.builder;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import net.efn.client.chat.ModuleChat;
import net.efn.client.easyplace.EasyPlace;
import net.efn.client.event.EventHandler;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.donutmacros.AhPurchase;
import net.efn.client.module.impl.misc.AutoReconnectModule;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.Heightmap;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

@Environment(value = EnvType.CLIENT)
public final class ElytraAutopilotModule extends ClientModule {

    private static final Predicate<ItemStack> ROCKET = ElytraAutopilotModule::plainRocket;

    private static final Predicate<ItemStack> XP =
            itemStack -> itemStack.isOf(Items.EXPERIENCE_BOTTLE);

    private static final float URGENT_TURN = 27.0f;

    private static final int ROCKET_GAP = 10;

    private static final double ROCKET_RAY = 10.0;

    private static final double LOADED_AHEAD = 110.0;

    private static final double ARRIVE_RANGE = 30.0;

    private static final double TOUCHDOWN_RANGE = 72.0;

    private static final double GLIDE_SLOPE = 0.095;

    private static final double MIN_CLEAR = 14.0;

    private static final int SETBACK_WINDOW = 200;

    private static final int SETBACK_HOLD = 60;

    private static final int STALL_TICKS = 900;

    private static final double STALL_GAIN = 60.0;

    private static final int MAX_STALLS = 6;

    private static final int BUY_RETRY = 6000;

    private static final int VERIFY_TICKS = 60;

    private static final int MEND_STALL = 200;

    private static final int WATER_LIMIT = 1200;

    private final StringSetting target =
            this.addSetting(
                    "target",
                    new StringSetting("Target", "").hint("x z, e.g. 12000 -3500 or 12k -3.5k"));

    private final NumberSetting altitude =
            this.addSetting(
                    "altitude",
                    new NumberSetting("Cruise Altitude", 290.0f, 180.0f, 320.0f).step(10.0f));

    private final BooleanSetting mend =
            this.addSetting("mend", new BooleanSetting("Repair With XP", true));

    private final BooleanSetting buyRockets =
            this.addSetting("buy-rockets", new BooleanSetting("Buy Rockets", true));

    private final NumberSetting rocketStacks =
            this.addSetting(
                    "rocket-stacks",
                    new NumberSetting("Rocket Stacks", 3.0f, 1.0f, 3.0f).step(1.0f));

    private final StringSetting rocketPrice =
            this.addSetting(
                    "rocket-price",
                    new StringSetting("Max Rocket Stack Price", "60k").hint("per 64 rockets"));

    private final BooleanSetting buyXp =
            this.addSetting("buy-xp", new BooleanSetting("Buy XP Bottles", true));

    private final NumberSetting xpStacks =
            this.addSetting(
                    "xp-stacks", new NumberSetting("XP Stacks", 2.0f, 1.0f, 3.0f).step(1.0f));

    private final StringSetting xpPrice =
            this.addSetting(
                    "xp-price",
                    new StringSetting("Max XP Stack Price", "450k").hint("per 64 bottles"));

    private final StringSetting tripLimit =
            this.addSetting(
                    "trip-limit", new StringSetting("Trip Spend Limit", "2m").hint("0 = no limit"));

    private final BooleanSetting disconnect =
            this.addSetting("disconnect", new BooleanSetting("Disconnect On Arrival", true));

    private final BooleanSetting status =
            this.addSetting("status", new BooleanSetting("Status Line", true));

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private volatile int setbacksReceived;

    private State state = State.PREPARE;

    private Job job = Job.ARRIVE;

    private Task task = Task.NONE;

    private int tick;

    private int stateTicks;

    private int taskTicks;

    private double destX;

    private double destZ;

    private RegistryKey<World> dimension;

    private ClientPlayNetworkHandler connection;

    private boolean pauseWas;

    private boolean pauseSaved;

    private String stopReason;

    private int boostUntil;

    private int lastRocket = -1000;

    private int rocketHoldUntil;

    private int seenSetbacks;

    private final IntArrayList recentSetbacks = new IntArrayList();

    private int eatLockUntil;

    private boolean eatingInFlight;

    private boolean climbing;

    private ElytraFlight.Plan plan;

    private int planTick = -1000;

    private float bandJitter;

    private float glideJitter;

    private double bestDistance;

    private int progressTick;

    private int stalls;

    private double altitudeBump;

    private BlockPos spot;

    private int nextSpotSearch;

    private int spotSearches;

    private final List<BlockPos> banned = new ArrayList<BlockPos>();

    private int goArounds;

    private boolean spiralling;

    private double bestDrop;

    private int dropTick;

    private int takeoffTries;

    private int jumps;

    private int jumpTick = -1000;

    private float escapeYaw = Float.NaN;

    private int swapTick = -1000;

    private int waterTicks;

    private Eater eater;

    private int mendBest;

    private int mendGainTick;

    private int mendFailures;

    private final EnumSet<Task> doneTasks = EnumSet.noneOf(Task.class);

    private AhPurchase purchase;

    private Item purchaseItem;

    private int purchaseBefore;

    private int verifyUntil;

    private final Map<Item, Integer> nextBuy = new HashMap<Item, Integer>();

    private final Map<Item, Integer> buyFails = new HashMap<Item, Integer>();

    private long spent;

    private int rocketsUsed;

    private int pitStops;

    private double flown;

    private Vec3d lastPos;

    private long startedAt;

    public ElytraAutopilotModule() {
        super(
                "elytra-autopilot",
                "Elytra Autopilot",
                "Flies an elytra with rockets to the target coordinates and lands there; repairs,"
                        + " eats and restocks on the way.",
                ModuleCategory.BOTS);
        this.target.describe("Where to fly: x z, e.g. 12000 -3500 or 12k -3.5k.");
        this.altitude.describe("Height it cruises at.");
        this.disconnect.describe("Leaves the server once it has landed.");
        this.status.describe("Shows flight progress on screen.");
        this.section("Route", this.target, this.altitude, this.disconnect, this.status);
        this.mend.describe("Repairs the elytra with XP bottles on the way.");
        this.section("Repairs", this.mend);
        this.buyRockets.describe("Buys firework rockets from the AH when running low.");
        this.rocketStacks.describe("How many stacks of rockets to keep.");
        this.rocketPrice.describe("Most it pays for 64 rockets.");
        this.buyXp.describe("Buys XP bottles from the AH when running low.");
        this.xpStacks.describe("How many stacks of XP bottles to keep.");
        this.xpPrice.describe("Most it pays for 64 XP bottles.");
        this.tripLimit.describe("Most money it spends on one trip, 0 = no limit.");
        this.section(
                "Shopping",
                this.buyRockets,
                this.rocketStacks,
                this.rocketPrice,
                this.buyXp,
                this.xpStacks,
                this.xpPrice,
                this.tripLimit);
        this.rocketStacks.visibleWhen(this.buyRockets::get);
        this.rocketPrice.visibleWhen(this.buyRockets::get);
        this.xpStacks.visibleWhen(this.buyXp::get);
        this.xpPrice.visibleWhen(this.buyXp::get);
    }

    @Override
    protected void onEnable() {
        ClientPlayerEntity player = this.mc.player;
        if (player == null || this.mc.world == null || this.mc.getNetworkHandler() == null) {
            this.say(ModuleChat.body().warn("Join a world first."));
            this.requestDisable();
            return;
        }
        double[] target2 = ElytraAutopilotModule.parseTarget(this.target.get());
        if (target2 == null) {
            this.say(
                    ModuleChat.body()
                            .warn("Set the Target first")
                            .text(" (\"x z\", e.g. 12000 -3500)."));
            this.requestDisable();
            return;
        }
        if (this.mc.world.getRegistryKey() == World.NETHER) {
            this.say(
                    ModuleChat.body()
                            .warn("The nether roof is no place for this")
                            .text("; the overworld and the end only."));
            this.requestDisable();
            return;
        }
        if (!ElytraAutopilotModule.usable(ElytraAutopilotModule.chest(player))
                && !Hands.has(player, ElytraAutopilotModule::usable)) {
            this.say(ModuleChat.body().warn("No usable elytra").text(" worn or in the inventory."));
            this.requestDisable();
            return;
        }
        if (ElytraAutopilotModule.count(player, ROCKET) == 0 && !this.buyRockets.get()) {
            this.say(
                    ModuleChat.body()
                            .warn("No rockets")
                            .text(" (plain ones, without stars), and buying them is off."));
            this.requestDisable();
            return;
        }
        this.destX = Math.floor(target2[0]) + 0.5;
        this.destZ = Math.floor(target2[1]) + 0.5;
        this.dimension = this.mc.world.getRegistryKey();
        this.connection = this.mc.getNetworkHandler();
        this.reset();
        this.pauseWas = this.mc.options.pauseOnLostFocus;
        this.pauseSaved = true;
        this.mc.options.pauseOnLostFocus = false;
        this.lastPos = player.getEntityPos();
        this.startedAt = System.currentTimeMillis();
        this.say(
                ModuleChat.body()
                        .text("Flying to ")
                        .coords((int) Math.floor(this.destX), 0, (int) Math.floor(this.destZ))
                        .text(", " + Math.round(this.distance(player)) + " blocks away."));
        EasyPlace.LOG.info(
                "[Elytra Autopilot] start at {} to {} {} ({} blocks)",
                player.getBlockPos().toShortString(),
                Math.floor(this.destX),
                Math.floor(this.destZ),
                Math.round(this.distance(player)));
        this.on(TickEvent.TYPE, Stage.PRE, tickEvent -> this.tick());
    }

    @Override
    protected void onDisable() {
        this.releaseKeys();
        if (this.purchase != null) {
            this.purchase.cancel();
        }
        this.purchase = null;
        if (this.eater != null) {
            this.eater.release(this.mc);
        }
        this.eater = null;
        if (this.pauseSaved) {
            this.mc.options.pauseOnLostFocus = this.pauseWas;
        }
        this.pauseSaved = false;
    }

    private void reset() {
        this.state = State.PREPARE;
        this.job = Job.ARRIVE;
        this.task = Task.NONE;
        this.taskTicks = 0;
        this.stateTicks = 0;
        this.tick = 0;
        this.stopReason = null;
        this.boostUntil = 0;
        this.lastRocket = -1000;
        this.rocketHoldUntil = 0;
        this.seenSetbacks = this.setbacksReceived;
        this.recentSetbacks.clear();
        this.eatLockUntil = 0;
        this.eatingInFlight = false;
        this.climbing = false;
        this.plan = null;
        this.planTick = -1000;
        this.bestDistance = Double.MAX_VALUE;
        this.progressTick = 0;
        this.stalls = 0;
        this.altitudeBump = 0.0;
        this.spot = null;
        this.nextSpotSearch = 0;
        this.spotSearches = 0;
        this.banned.clear();
        this.goArounds = 0;
        this.spiralling = false;
        this.jumps = 0;
        this.takeoffTries = 0;
        this.jumpTick = -1000;
        this.escapeYaw = Float.NaN;
        this.swapTick = -1000;
        this.waterTicks = 0;
        this.eater = null;
        this.mendFailures = 0;
        this.doneTasks.clear();
        this.purchase = null;
        this.purchaseItem = null;
        this.verifyUntil = 0;
        this.nextBuy.clear();
        this.buyFails.clear();
        this.spent = 0L;
        this.rocketsUsed = 0;
        this.pitStops = 0;
        this.flown = 0.0;
        this.newJitter();
    }

    @EventHandler
    public void onPacketReceive(PacketReceiveNettyEvent event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket) {
            ++this.setbacksReceived;
        }
    }

    private void tick() {
        ++this.tick;
        ++this.stateTicks;
        ClientPlayerEntity player = this.mc.player;
        ClientWorld world = this.mc.world;
        if (player == null
                || world == null
                || this.mc.interactionManager == null
                || this.mc.getNetworkHandler() != this.connection) {
            this.requestDisable();
            return;
        }
        if (world.getRegistryKey() != this.dimension) {
            this.say(ModuleChat.body().warn("Left the dimension").text(", the flight is off."));
            this.requestDisable();
            return;
        }
        if (player.isDead()) {
            this.say(
                    ModuleChat.body()
                            .danger("Died on the way")
                            .text(" at ")
                            .coords(player.getBlockX(), player.getBlockY(), player.getBlockZ())
                            .text("."));
            this.requestDisable();
            return;
        }
        this.releaseKeys();
        this.bookkeeping(player);
        if (this.status.get() && this.tick % 20 == 0) {
            this.showStatus(player);
        }
        if (this.state != State.TAKEOFF
                && this.state != State.SERVICE
                && this.emergencyDeploy(player)) {
            return;
        }
        if (this.midAirSwap(player)) {
            return;
        }
        switch (this.state.ordinal()) {
            case 0:
                {
                    this.prepare(player);
                    break;
                }
            case 1:
                {
                    this.takeoff(player, world);
                    break;
                }
            case 2:
                {
                    this.climb(player, world);
                    break;
                }
            case 3:
                {
                    this.cruise(player, world);
                    break;
                }
            case 4:
                {
                    this.descend(player, world);
                    break;
                }
            case 5:
                {
                    this.land(player, world);
                    break;
                }
            case 6:
                {
                    this.service(player, world);
                }
        }
    }

    private void go(State state2) {
        if (state2 == State.TAKEOFF) {
            ++this.takeoffTries;
            this.jumps = 0;
            this.escapeYaw = Float.NaN;
        }
        if (state2 == State.CRUISE) {
            this.takeoffTries = 0;
        }
        if (state2 == State.DESCEND || state2 == State.LAND) {
            this.plan = null;
        }
        this.state = state2;
        this.stateTicks = 0;
    }

    private void releaseKeys() {
        this.mc.options.jumpKey.setPressed(false);
        this.mc.options.forwardKey.setPressed(false);
        this.mc.options.useKey.setPressed(false);
    }

    private void bookkeeping(ClientPlayerEntity player) {
        Vec3d entityPos = player.getEntityPos();
        if (this.lastPos != null
                && player.isGliding()
                && entityPos.distanceTo(this.lastPos) < 20.0) {
            this.flown += entityPos.distanceTo(this.lastPos);
        }
        this.lastPos = entityPos;
        if (!player.isTouchingWater()) {
            this.waterTicks = 0;
        }
        int setbacksReceived2 = this.setbacksReceived;
        while (this.seenSetbacks != setbacksReceived2) {
            ++this.seenSetbacks;
            if (!player.isGliding()) continue;
            this.recentSetbacks.add(this.tick);
        }
        this.recentSetbacks.removeIf(i -> this.tick - i > 200);
        if (this.recentSetbacks.size() >= 3) {
            this.rocketHoldUntil = this.tick + 60;
            this.boostUntil = 0;
            this.recentSetbacks.clear();
            EasyPlace.LOG.info(
                    "[Elytra Autopilot] setbacks, rockets held for {} ticks", (Object) 60);
        }
        if (this.eatingInFlight
                && (!player.isGliding() || !Eater.hungry(player) && !player.isUsingItem())) {
            this.eatingInFlight = false;
        }
    }

    private boolean emergencyDeploy(ClientPlayerEntity player) {
        if (player.isGliding()
                || player.isOnGround()
                || player.isTouchingWater()
                || player.isClimbing()
                || !ElytraAutopilotModule.usable(ElytraAutopilotModule.chest(player))) {
            return false;
        }
        if (this.state == State.PREPARE
                || player.getVelocity().y > -0.5
                || player.fallDistance < 3.0) {
            return false;
        }
        this.mc.options.jumpKey.setPressed(this.tick % 2 == 0);
        Aim.turn(this.mc, player.getYaw(), 8.0f, 27.0f);
        return true;
    }

    private boolean midAirSwap(ClientPlayerEntity player) {
        if (!player.isGliding() || this.tick - this.swapTick < 6) {
            return false;
        }
        int durability = ElytraAutopilotModule.durability(ElytraAutopilotModule.chest(player));
        if (durability < 0 || durability > 6) {
            return false;
        }
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < 9; ++i) {
            if (ElytraAutopilotModule.durability(inventory.getStack(i)) < 20
                    || !inventory.getStack(i).isOf(Items.ELYTRA)) continue;
            this.swapTick = this.tick;
            if (inventory.getSelectedSlot() != i) {
                inventory.setSelectedSlot(i);
            } else {
                this.mc.interactionManager.interactItem(player, Hand.MAIN_HAND);
            }
            return true;
        }
        return false;
    }

    private void prepare(ClientPlayerEntity player) {
        if (player.isTouchingWater()) {
            this.swim(player);
            return;
        }
        if (player.isGliding()) {
            this.go(State.CLIMB);
            return;
        }
        if (!player.isOnGround()) {
            return;
        }
        if (this.mc.currentScreen instanceof HandledScreen && this.purchase == null) {
            player.closeHandledScreen();
        }
        if (!ElytraAutopilotModule.usable(ElytraAutopilotModule.chest(player))) {
            if (!Hands.has(player, ElytraAutopilotModule::usable)) {
                this.finish(false, "no usable elytra left");
                return;
            }
            if (Hands.hold(this.mc, player, ElytraAutopilotModule::usable)
                    && this.stateTicks % 6 == 3) {
                this.mc.interactionManager.interactItem(player, Hand.MAIN_HAND);
            }
            return;
        }
        if (this.distance(player) <= 30.0) {
            this.finish(true, null);
            return;
        }
        if (this.stateTicks < 4) {
            return;
        }
        Task task2 = this.nextTask(player);
        if (task2 != Task.NONE) {
            this.job = Job.SERVICE;
            this.go(State.SERVICE);
            return;
        }
        if (ElytraAutopilotModule.count(player, ROCKET) == 0) {
            this.finish(false, "out of rockets");
            return;
        }
        if (ElytraAutopilotModule.durability(ElytraAutopilotModule.chest(player)) < 30) {
            this.finish(false, "the elytra is nearly broken");
            return;
        }
        if (!ElytraAutopilotModule.inHotbar(player, ROCKET)) {
            Hands.hold(this.mc, player, ROCKET);
            return;
        }
        this.job = Job.ARRIVE;
        this.go(State.TAKEOFF);
    }

    private void takeoff(ClientPlayerEntity player, ClientWorld world) {
        if (this.takeoffTries > 6) {
            this.finish(false, "could not take off here");
            return;
        }
        if (player.isTouchingWater()) {
            this.go(State.PREPARE);
            return;
        }
        if (player.isGliding()) {
            this.go(State.CLIMB);
            return;
        }
        if (!ElytraAutopilotModule.inHotbar(player, ROCKET)) {
            this.go(State.PREPARE);
            return;
        }
        Hands.hold(this.mc, player, ROCKET);
        if (player.isOnGround()) {
            if (Float.isNaN(this.escapeYaw)) {
                this.escapeYaw =
                        ElytraAutopilotModule.openSky(player, world)
                                ? player.getYaw()
                                : ElytraAutopilotModule.escapeHeading(player, world);
                float escapeYaw2 = this.escapeYaw;
            }
            if (Float.isNaN(this.escapeYaw) || this.escapeYaw == Float.NEGATIVE_INFINITY) {
                this.finish(false, "no open sky to take off into");
                return;
            }
            boolean openSky = ElytraAutopilotModule.openSky(player, world);
            float f = openSky ? player.getYaw() : this.escapeYaw;
            float g = openSky ? -88.0f : -35.0f;
            Aim.turn(this.mc, f, g, 22.0f);
            boolean bl =
                    Math.abs(MathHelper.wrapDegrees(player.getYaw() - f)) < 10.0f
                            && Math.abs(player.getPitch() - g) < 10.0f;
            boolean bl2 = bl;
            if (bl && this.tick - this.jumpTick >= 14) {
                if (++this.jumps > 5) {
                    this.finish(false, "could not get the elytra open");
                    return;
                }
                this.mc.options.jumpKey.setPressed(true);
                this.jumpTick = this.tick;
            }
            return;
        }
        Aim.turn(
                this.mc,
                player.getYaw(),
                player.getPitch() < -60.0f ? player.getPitch() : -85.0f,
                22.0f);
        if (player.getVelocity().y < 0.12 && this.tick - this.jumpTick >= 2) {
            this.mc.options.jumpKey.setPressed(this.tick % 2 == 0);
        }
    }

    private static boolean openSky(ClientPlayerEntity player, ClientWorld world) {
        Box box = player.getBoundingBox().contract(0.02);
        for (int i = 1; i <= 16; ++i) {
            if (world.isSpaceEmpty(player, box.offset(0.0, i, 0.0))) continue;
            return false;
        }
        return true;
    }

    private static float escapeHeading(ClientPlayerEntity player, ClientWorld world) {
        if (!world.isSpaceEmpty(
                player, player.getBoundingBox().contract(0.02).offset(0.0, 2.2, 0.0))) {
            return Float.NEGATIVE_INFINITY;
        }
        float[] floats = new float[] {0.0f, 45.0f, -45.0f, 90.0f, -90.0f, 135.0f, -135.0f, 180.0f};
        for (float f : floats) {
            float g = player.getYaw() + f;
            Vec3d vec = Vec3d.fromPolar(-32.0f, g);
            Vec3d vec2 = Vec3d.fromPolar(0.0f, g + 90.0f).multiply(0.35);
            Vec3d entityPos = player.getEntityPos();
            Vec3d[] vecs =
                    new Vec3d[] {
                        entityPos.add(0.0, 1.2, 0.0),
                        entityPos.add(0.0, 1.8, 0.0),
                        entityPos.add(0.0, 1.2, 0.0).add(vec2),
                        entityPos.add(0.0, 1.2, 0.0).subtract(vec2)
                    };
            boolean bl = true;
            for (Vec3d vec3 : vecs) {
                BlockHitResult blockHitResult =
                        world.raycast(
                                new RaycastContext(
                                        vec3,
                                        vec3.add(vec.multiply(22.0)),
                                        RaycastContext.ShapeType.COLLIDER,
                                        RaycastContext.FluidHandling.NONE,
                                        player));
                if (blockHitResult.getType() == HitResult.Type.MISS) continue;
                bl = false;
                break;
            }
            if (!bl) continue;
            return g;
        }
        return Float.NEGATIVE_INFINITY;
    }

    private void swim(ClientPlayerEntity player) {
        if (++this.waterTicks > 1200) {
            this.finish(false, "stuck in water");
            return;
        }
        this.mc.options.jumpKey.setPressed(true);
        BlockPos pos = this.shore(player);
        if (pos == null) {
            return;
        }
        float bearing =
                ElytraFlight.bearing(
                        player.getX(),
                        player.getZ(),
                        (double) pos.getX() + 0.5,
                        (double) pos.getZ() + 0.5);
        Aim.turn(this.mc, bearing, 0.0f, 22.0f);
        this.mc.options.forwardKey.setPressed(
                Math.abs(MathHelper.wrapDegrees(player.getYaw() - bearing)) < 30.0f);
    }

    private BlockPos shore(ClientPlayerEntity player) {
        ClientWorld world = this.mc.world;
        int blockX = player.getBlockX();
        int blockZ = player.getBlockZ();
        for (int i = 1; i <= 32; ++i) {
            for (int j = -i; j <= i; ++j) {
                for (int k = -i; k <= i; ++k) {
                    int topY;
                    if (Math.max(Math.abs(j), Math.abs(k)) != i
                            || !ElytraFlight.goodFloor(
                                    world,
                                    new BlockPos(
                                            blockX + j,
                                            (topY =
                                                            world.getTopY(
                                                                    Heightmap.Type.MOTION_BLOCKING,
                                                                    blockX + j,
                                                                    blockZ + k))
                                                    - 1,
                                            blockZ + k))) continue;
                    return new BlockPos(blockX + j, topY, blockZ + k);
                }
            }
        }
        return null;
    }

    private boolean flying(ClientPlayerEntity player) {
        if (player.isGliding()) {
            return true;
        }
        if (player.isTouchingWater()) {
            this.go(State.PREPARE);
        } else if (player.isOnGround()) {
            if (this.state == State.DESCEND || this.state == State.LAND) {
                this.touchdown(player);
            } else {
                this.go(State.PREPARE);
            }
        } else {
            this.mc.options.jumpKey.setPressed(this.tick % 2 == 0);
        }
        return false;
    }

    private void climb(ClientPlayerEntity player, ClientWorld world) {
        if (!this.flying(player)) {
            return;
        }
        double distance = this.distance(player);
        double cruiseAltitude = this.cruiseAltitude(world, distance);
        if (this.supplyCheck(player, distance)) {
            return;
        }
        if (player.getY() >= cruiseAltitude - 15.0 || this.stateTicks > 600) {
            this.go(distance <= this.descentStart(world, player) ? State.DESCEND : State.CRUISE);
            return;
        }
        ElytraFlight.Terrain terrain = this.terrain(world);
        if (ElytraAutopilotModule.clearAround(player, terrain)) {
            Aim.turn(this.mc, player.getYaw(), -85.0f, 22.0f);
            if (player.getPitch() < -60.0f && !this.boosting() && this.rocketReady(player)) {
                this.fire(player);
            }
            return;
        }
        if (terrain.surface(player.getX(), player.getZ()) > player.getY() + 1.5
                && this.stateTicks < 200) {
            float f =
                    Float.isNaN(this.escapeYaw) || this.escapeYaw == Float.NEGATIVE_INFINITY
                            ? player.getYaw()
                            : this.escapeYaw;
            Aim.turn(this.mc, f, -32.0f, 27.0f);
            if (Math.abs(player.getPitch() + 32.0f) < 12.0f
                    && !this.boosting()
                    && this.rocketReady(player)) {
                this.fire(player);
            }
            return;
        }
        this.steer(
                player,
                terrain,
                new ElytraFlight.Goal(
                        this.destX,
                        this.destZ,
                        cruiseAltitude,
                        14.0,
                        false,
                        this.rocketsAllowed(player, world, cruiseAltitude),
                        ElytraAutopilotModule.rocketCost(player),
                        9.0,
                        ElytraAutopilotModule.floor(world)),
                false);
    }

    private void cruise(ClientPlayerEntity player, ClientWorld world) {
        if (!this.flying(player)) {
            return;
        }
        double distance = this.distance(player);
        if (this.supplyCheck(player, distance)) {
            return;
        }
        if (this.progressStalled(distance)) {
            return;
        }
        if (distance <= this.descentStart(world, player)) {
            this.job = Job.ARRIVE;
            this.go(State.DESCEND);
            return;
        }
        double cruiseAltitude = this.cruiseAltitude(world, distance);
        ElytraFlight.Terrain terrain = this.terrain(world);
        if (this.eatInFlight(player)) {
            this.glideOn(player, terrain, cruiseAltitude, false);
            return;
        }
        boolean rocketsAllowed = this.rocketsAllowed(player, world, cruiseAltitude);
        double d = this.terrainAhead(player, terrain, distance) + 14.0 + 4.0;
        double e = Math.max(d, cruiseAltitude - 45.0 + (double) this.bandJitter);
        double d2 = Math.max(cruiseAltitude + 15.0 + (double) this.bandJitter, e + 10.0);
        if (!this.climbing && player.getY() < e && (rocketsAllowed || this.boosting())) {
            this.climbing = true;
        }
        if (this.climbing && player.getY() >= d2 - 8.0) {
            this.climbing = false;
            this.newJitter();
        }
        if (this.climbing
                && !rocketsAllowed
                && !this.boosting()
                && player.getVelocity().y < 0.05
                && player.getPitch() < -37.0f) {
            this.climbing = false;
        }
        this.glideOn(player, terrain, cruiseAltitude, rocketsAllowed);
    }

    private void glideOn(
            ClientPlayerEntity player, ElytraFlight.Terrain terrain, double d, boolean bl) {
        float f;
        float bearing = ElytraFlight.bearing(player.getX(), player.getZ(), this.destX, this.destZ);
        double horizontalLength = player.getVelocity().horizontalLength();
        boolean bl2 = false;
        if (this.climbing) {
            f = -47.0f;
            bl2 = bl && !this.boosting() && player.getVelocity().y < 0.45;
        } else {
            f =
                    (horizontalLength < 1.0 ? 14.0f : (horizontalLength < 1.4 ? 7.0f : 2.0f))
                            + this.glideJitter;
        }
        ElytraFlight.Goal goal =
                new ElytraFlight.Goal(
                        this.destX,
                        this.destZ,
                        d,
                        14.0,
                        false,
                        bl,
                        ElytraAutopilotModule.rocketCost(player),
                        9.0,
                        ElytraAutopilotModule.floor(this.mc.world));
        ElytraFlight.Glide glide = ElytraFlight.Glide.of(player, this.boostLeft());
        if (ElytraFlight.clear(glide, goal, terrain, bearing, f, bl2, 22.0f)) {
            this.plan = null;
            Aim.turn(this.mc, bearing, f, 22.0f);
            if (bl2
                    && this.rocketReady(player)
                    && ElytraAutopilotModule.aimedAt(player, bearing, f)) {
                this.fire(player);
            }
            return;
        }
        this.steer(player, terrain, goal, false);
    }

    private void steer(
            ClientPlayerEntity player,
            ElytraFlight.Terrain terrain,
            ElytraFlight.Goal goal,
            boolean bl) {
        if (this.plan == null || this.tick - this.planTick >= 2) {
            this.plan =
                    ElytraFlight.search(
                            ElytraFlight.Glide.of(player, this.boostLeft()),
                            goal,
                            terrain,
                            this.boosting(),
                            22.0f);
            this.planTick = this.tick;
        }
        float f = bl || !this.plan.safe() ? 27.0f : 22.0f;
        Aim.turn(this.mc, this.plan.yaw(), this.plan.pitch(), f);
        if (this.plan.rocket()
                && !this.boosting()
                && this.rocketReady(player)
                && ElytraAutopilotModule.aimedAt(player, this.plan.yaw(), this.plan.pitch())) {
            this.fire(player);
        }
    }

    private static boolean aimedAt(ClientPlayerEntity player, float f, float g) {
        return Math.abs(player.getPitch() - g) < 20.0f
                && Math.abs(MathHelper.wrapDegrees(player.getYaw() - f)) < 30.0f;
    }

    private void descend(ClientPlayerEntity player, ClientWorld world) {
        if (!this.flying(player)) {
            return;
        }
        double distance = this.distance(player);
        if (this.job == Job.ARRIVE && this.supplyCheck(player, distance)) {
            return;
        }
        boolean bl = this.job != Job.ARRIVE && (this.critical(player) || this.job == Job.STOP);
        ElytraFlight.Terrain terrain = this.terrain(world);
        if (this.spot == null && this.tick >= this.nextSpotSearch) {
            this.findSpot(player, terrain, bl);
        }
        if (this.spot == null && this.job != Job.ARRIVE) {
            this.glideOn(
                    player,
                    terrain,
                    this.cruiseAltitude(world, distance),
                    this.rocketsAllowed(player, world, this.cruiseAltitude(world, distance)));
            return;
        }
        double d = this.spot != null ? (double) this.spot.getX() + 0.5 : this.destX;
        double e = this.spot != null ? (double) this.spot.getZ() + 0.5 : this.destZ;
        double d2 =
                this.spot != null
                        ? (double) this.spot.getY()
                        : ElytraAutopilotModule.surfaceAt(world, this.destX, this.destZ);
        double e2 = Math.hypot(player.getX() - d, player.getZ() - e);
        double d3 = player.getY() - d2;
        if (this.spot != null) {
            if (d3 < this.bestDrop - 3.0) {
                this.bestDrop = d3;
                this.dropTick = this.tick;
            } else if (this.tick - this.dropTick > 700) {
                this.ban(this.spot);
                return;
            }
            if (e2 <= 50.0 && d3 >= -1.0 && d3 <= 28.0) {
                this.go(State.LAND);
                return;
            }
        }
        double e3 = d2 + 14.0 + Math.max(0.0, e2 - 45.0) * 0.095;
        double d4 = player.getY() - e3;
        if (!this.spiralling && e2 < 170.0 && d3 > 32.0 && (d4 > 22.0 || e2 < 60.0)) {
            this.spiralling = true;
        }
        if (this.spiralling && (d3 <= 26.0 || d4 < 8.0 && e2 >= 60.0 || e2 > 260.0)) {
            this.spiralling = false;
        }
        double e4 = Math.max(3.0, Math.min(14.0, (e2 - 45.0) * 0.25 + 4.0));
        if (this.spiralling) {
            float f = ElytraFlight.bearing(d, e, player.getX(), player.getZ()) + 90.0f + 55.0f;
            Vec3d vec = Vec3d.fromPolar(0.0f, f).multiply(42.0);
            this.steer(
                    player,
                    terrain,
                    new ElytraFlight.Goal(
                            d + vec.x,
                            e + vec.z,
                            d2 + 14.0,
                            Math.max(10.0, e4),
                            true,
                            false,
                            ElytraAutopilotModule.rocketCost(player),
                            d4 > 40.0 ? 3.0 : 1.9,
                            ElytraAutopilotModule.floor(world)),
                    false);
            return;
        }
        boolean bl2 = this.rocketsAllowed(player, world, e3) && d3 < 10.0;
        this.steer(
                player,
                terrain,
                new ElytraFlight.Goal(
                        d,
                        e,
                        e3,
                        e4,
                        true,
                        bl2,
                        ElytraAutopilotModule.rocketCost(player),
                        1.45,
                        ElytraAutopilotModule.floor(world)),
                bl);
    }

    private void findSpot(ClientPlayerEntity player, ElytraFlight.Terrain terrain, boolean bl) {
        Predicate<BlockPos> predicate;
        BlockPos pos;
        int i;
        int j;
        int k;
        this.nextSpotSearch = this.tick + (bl ? 10 : 20);
        if (this.job == Job.ARRIVE) {
            if (!terrain.known(MathHelper.floor(this.destX), MathHelper.floor(this.destZ))) {
                return;
            }
            k = MathHelper.floor(this.destX);
            j = MathHelper.floor(this.destZ);
            ++this.spotSearches;
            i = this.spotSearches > 12 ? 300 : (this.spotSearches > 4 ? 160 : 96);
            if (this.spotSearches > 40) {
                this.finish(false, "no safe landing near the target");
                return;
            }
        } else {
            Vec3d vec = Vec3d.fromPolar(0.0f, player.getYaw()).multiply(bl ? 30.0 : 110.0);
            k = MathHelper.floor(player.getX() + vec.x);
            j = MathHelper.floor(player.getZ() + vec.z);
            i = bl ? 120 : 150;
        }
        if ((pos =
                        ElytraFlight.landingSpot(
                                terrain,
                                k,
                                j,
                                i,
                                true,
                                predicate =
                                        blockPos -> {
                                            for (BlockPos pos2 : this.banned) {
                                                if (pos2.getManhattanDistance((Vec3i) blockPos)
                                                        >= 30
                                                                + Math.abs(
                                                                        pos2.getY()
                                                                                - blockPos.getY()))
                                                    continue;
                                                return true;
                                            }
                                            return false;
                                        }))
                == null) {
            pos = ElytraFlight.landingSpot(terrain, k, j, i, false, predicate);
        }
        if (pos != null) {
            this.spot = pos;
            this.bestDrop = Double.MAX_VALUE;
            this.dropTick = this.tick;
            EasyPlace.LOG.info("[Elytra Autopilot] landing spot {}", (Object) pos.toShortString());
        }
    }

    private void ban(BlockPos pos) {
        this.banned.add(pos);
        this.spot = null;
        this.goArounds = 0;
        this.nextSpotSearch = this.tick;
    }

    private void land(ClientPlayerEntity player, ClientWorld world) {
        if (!player.isGliding()) {
            if (player.isOnGround() || player.isTouchingWater()) {
                this.touchdown(player);
            } else if (player.getVelocity().y < -0.3) {
                this.mc.options.jumpKey.setPressed(this.tick % 2 == 0);
            }
            return;
        }
        if (this.spot == null) {
            this.go(State.DESCEND);
            return;
        }
        double d =
                Math.hypot(
                        player.getX() - ((double) this.spot.getX() + 0.5),
                        player.getZ() - ((double) this.spot.getZ() + 0.5));
        double e = player.getY() - (double) this.spot.getY();
        if (e < -4.0 || this.stateTicks > 700 || d > 90.0) {
            if (++this.goArounds >= 4) {
                this.ban(this.spot);
            }
            EasyPlace.LOG.info(
                    "[Elytra Autopilot] going round again ({})", (Object) this.goArounds);
            this.go(State.DESCEND);
            return;
        }
        if (this.plan == null || this.tick - this.planTick >= 2) {
            this.plan =
                    ElytraFlight.touchdown(
                            ElytraFlight.Glide.of(player, this.boostLeft()),
                            this.spot,
                            this.terrain(world),
                            27.0f);
            this.planTick = this.tick;
        }
        Aim.turn(this.mc, this.plan.yaw(), this.plan.pitch(), 27.0f);
    }

    private void touchdown(ClientPlayerEntity player) {
        this.plan = null;
        this.spiralling = false;
        switch (this.job.ordinal()) {
            case 0:
                {
                    if (this.distance(player) <= 72.0) {
                        this.finish(true, null);
                        break;
                    }
                    if (this.spot != null) {
                        this.ban(this.spot);
                    }
                    this.go(State.PREPARE);
                    break;
                }
            case 1:
                {
                    this.spot = null;
                    ++this.pitStops;
                    this.doneTasks.clear();
                    this.go(State.SERVICE);
                    break;
                }
            case 2:
                {
                    this.finish(false, this.stopReason != null ? this.stopReason : "had to stop");
                }
        }
    }

    private boolean supplyCheck(ClientPlayerEntity player, double d) {
        if (this.tick % 20 != 0 || this.job != Job.ARRIVE) {
            return false;
        }
        ItemStack stack = ElytraAutopilotModule.chest(player);
        int durability = ElytraAutopilotModule.durability(stack);
        int level = ElytraAutopilotModule.level(stack, Enchantments.UNBREAKING);
        int i = ElytraAutopilotModule.count(player, ROCKET);
        double e = d / 27.0 + 50.0;
        double d2 = e / (double) (1 + level) * 1.35 + 25.0;
        double e2 = d / 1000.0 * 2.6 + 5.0;
        boolean bl = (durability < 0 || (double) durability > d2) && (double) i > e2;
        boolean bl2 = bl;
        if (bl && !this.critical(player)) {
            return false;
        }
        boolean canMend = this.canMend(player);
        boolean bl3 = ElytraAutopilotModule.spare(player) >= 0;
        boolean bl4 = durability >= 0 && durability <= ElytraAutopilotModule.mendThreshold(stack);
        Job job2 = null;
        String string = null;
        if (bl4 && (canMend || bl3)) {
            job2 = Job.SERVICE;
            string = canMend ? "to repair the elytra" : "to put on the spare elytra";
        } else if (bl4 && durability <= 40) {
            job2 = Job.STOP;
            string = "the elytra is worn out";
        } else if (i <= 20 && this.canBuy(Items.FIREWORK_ROCKET)) {
            job2 = Job.SERVICE;
            string = "to buy rockets";
        } else if (i <= 6) {
            job2 = Job.STOP;
            string = "out of rockets";
        }
        if (job2 == null) {
            return false;
        }
        this.job = job2;
        this.stopReason = job2 == Job.STOP ? string : null;
        this.spot = null;
        this.goArounds = 0;
        this.say(
                ModuleChat.body()
                        .warn(job2 == Job.SERVICE ? "Landing for a pit stop" : "Landing to stop")
                        .text(" (" + string + ")."));
        this.go(State.DESCEND);
        return true;
    }

    private boolean critical(ClientPlayerEntity player) {
        ItemStack stack = ElytraAutopilotModule.chest(player);
        int durability = ElytraAutopilotModule.durability(stack);
        int i = ElytraAutopilotModule.level(stack, Enchantments.UNBREAKING) >= 1 ? 40 : 60;
        return durability >= 0 && durability <= i
                || ElytraAutopilotModule.count(player, ROCKET) <= 3;
    }

    private boolean progressStalled(double bestDistance2) {
        if (bestDistance2 < this.bestDistance - 60.0) {
            this.bestDistance = bestDistance2;
            this.progressTick = this.tick;
            return false;
        }
        if (this.tick - this.progressTick < 900) {
            return false;
        }
        this.progressTick = this.tick;
        this.bestDistance = bestDistance2;
        if (++this.stalls >= 6) {
            this.job = Job.STOP;
            this.stopReason = "no progress";
            this.go(State.DESCEND);
            return true;
        }
        this.altitudeBump =
                Math.min(this.altitudeBump + 20.0, 316.0 - (double) this.altitude.get());
        this.plan = null;
        this.say(ModuleChat.body().warn("Not getting closer").text(", flying higher."));
        return false;
    }

    private boolean eatInFlight(ClientPlayerEntity player) {
        if (!this.eatingInFlight
                && (!Eater.hungry(player)
                        || this.climbing
                        || this.boosting()
                        || player.getY() < 70.0)) {
            return false;
        }
        int hotbarSlot = ElytraAutopilotModule.hotbarSlot(player, Eater::edible);
        if (hotbarSlot < 0) {
            this.eatingInFlight = false;
            return false;
        }
        player.getInventory().setSelectedSlot(hotbarSlot);
        this.mc.options.useKey.setPressed(true);
        if (!this.eatingInFlight) {
            this.eatLockUntil = this.tick + 45;
        }
        this.eatingInFlight = true;
        return true;
    }

    private void service(ClientPlayerEntity player, ClientWorld world) {
        if (player.isTouchingWater()) {
            this.swim(player);
            return;
        }
        if (player.isGliding()) {
            this.go(State.LAND);
            return;
        }
        if (!player.isOnGround()) {
            return;
        }
        if (this.danger(player, world)) {
            this.stopTask();
            if (ElytraAutopilotModule.count(player, ROCKET) >= 2
                    && ElytraAutopilotModule.durability(ElytraAutopilotModule.chest(player))
                            >= 30) {
                this.banned.add(player.getBlockPos());
                this.say(
                        ModuleChat.body()
                                .warn("Trouble at the pit stop")
                                .text(", taking off again."));
                this.job = Job.ARRIVE;
                this.go(State.TAKEOFF);
            } else {
                this.finish(false, "attacked at the pit stop");
            }
            return;
        }
        if (this.task == Task.NONE) {
            this.task = this.nextTask(player);
            this.taskTicks = 0;
            if (this.task == Task.NONE) {
                if (ElytraAutopilotModule.count(player, ROCKET) == 0) {
                    this.finish(false, "out of rockets");
                    return;
                }
                this.say(
                        ModuleChat.body()
                                .text(
                                        "Pit stop done: "
                                                + ElytraAutopilotModule.count(player, ROCKET)
                                                + " rockets, elytra "
                                                + ElytraAutopilotModule.durability(
                                                        ElytraAutopilotModule.chest(player))
                                                + "."));
                this.job = Job.ARRIVE;
                this.go(State.PREPARE);
                return;
            }
            this.doneTasks.add(this.task);
        }
        ++this.taskTicks;
        switch (this.task.ordinal()) {
            case 1:
                {
                    this.eat(player);
                    break;
                }
            case 2:
                {
                    this.swapElytra(player);
                    break;
                }
            case 3:
                {
                    this.mendElytra(player);
                    break;
                }
            case 4:
            case 5:
                {
                    this.buy(player);
                    break;
                }
            default:
                {
                    this.task = Task.NONE;
                }
        }
    }

    private Task nextTask(ClientPlayerEntity player) {
        ItemStack stack = ElytraAutopilotModule.chest(player);
        int durability = ElytraAutopilotModule.durability(stack);
        boolean bl = durability >= 0 && durability <= ElytraAutopilotModule.mendThreshold(stack);
        boolean bl2 =
                this.mend.get()
                        && ElytraAutopilotModule.mends(stack)
                        && this.mendFailures < 3
                        && durability >= 0
                        && (double) durability < (double) stack.getMaxDamage() * 0.97;
        boolean bl3 = bl2;
        if (!this.doneTasks.contains((Object) Task.EAT)
                && Eater.hungry(player)
                && Eater.hasFood(player)) {
            return Task.EAT;
        }
        if (!(this.doneTasks.contains((Object) Task.SWAP)
                || !bl
                || bl2 && ElytraAutopilotModule.count(player, XP) != 0
                || ElytraAutopilotModule.spare(player) < 0)) {
            return Task.SWAP;
        }
        int i = ElytraAutopilotModule.count(player, ROCKET);
        if (this.distance(player) < 1500.0 && i >= 4) {
            return !this.doneTasks.contains((Object) Task.MEND)
                            && bl
                            && bl2
                            && ElytraAutopilotModule.count(player, XP) > 0
                    ? Task.MEND
                    : Task.NONE;
        }
        if (!this.doneTasks.contains((Object) Task.BUY_XP)
                && bl2
                && ElytraAutopilotModule.count(player, XP) == 0
                && this.canBuy(Items.EXPERIENCE_BOTTLE)) {
            return Task.BUY_XP;
        }
        if (!this.doneTasks.contains((Object) Task.MEND)
                && bl2
                && ElytraAutopilotModule.count(player, XP) > 0) {
            return Task.MEND;
        }
        if (!this.doneTasks.contains((Object) Task.BUY_ROCKETS)
                && i < this.rocketStacks.getInt() * 32
                && this.canBuy(Items.FIREWORK_ROCKET)) {
            return Task.BUY_ROCKETS;
        }
        if (!this.doneTasks.contains((Object) Task.BUY_XP)
                && this.mend.get()
                && ElytraAutopilotModule.mends(stack)
                && ElytraAutopilotModule.count(player, XP) < this.xpStacks.getInt() * 32
                && this.canBuy(Items.EXPERIENCE_BOTTLE)) {
            return Task.BUY_XP;
        }
        return Task.NONE;
    }

    private void stopTask() {
        if (this.purchase != null) {
            this.purchase.cancel();
        }
        this.purchase = null;
        this.purchaseItem = null;
        this.verifyUntil = 0;
        if (this.eater != null) {
            this.eater.release(this.mc);
        }
        this.eater = null;
        this.task = Task.NONE;
        if (this.mc.currentScreen instanceof HandledScreen && this.mc.player != null) {
            this.mc.player.closeHandledScreen();
        }
    }

    private void eat(ClientPlayerEntity player) {
        Eater.Result result;
        if (this.eater == null) {
            this.eater = new Eater();
        }
        if ((result = this.eater.tick(this.mc)) != Eater.Result.RUNNING || this.taskTicks > 200) {
            this.eater.release(this.mc);
            this.eater = null;
            this.task = Task.NONE;
        }
    }

    private void swapElytra(ClientPlayerEntity player) {
        int durability = ElytraAutopilotModule.durability(ElytraAutopilotModule.chest(player));
        int spare = ElytraAutopilotModule.spare(player);
        if (spare < 0 || this.taskTicks > 120) {
            this.task = Task.NONE;
            return;
        }
        int durability2 = ElytraAutopilotModule.durability(player.getInventory().getStack(spare));
        Predicate<ItemStack> predicate =
                itemStack ->
                        itemStack.isOf(Items.ELYTRA)
                                && ElytraAutopilotModule.durability(itemStack) == durability2;
        if (Hands.hold(this.mc, player, predicate) && this.taskTicks % 6 == 3) {
            this.mc.interactionManager.interactItem(player, Hand.MAIN_HAND);
        }
        if (ElytraAutopilotModule.durability(ElytraAutopilotModule.chest(player)) > durability) {
            this.say(
                    ModuleChat.body()
                            .good("Put on the spare elytra")
                            .text(
                                    " ("
                                            + ElytraAutopilotModule.durability(
                                                    ElytraAutopilotModule.chest(player))
                                            + " left)."));
            this.task = Task.NONE;
        }
    }

    private void mendElytra(ClientPlayerEntity player) {
        ItemStack stack = ElytraAutopilotModule.chest(player);
        int durability = ElytraAutopilotModule.durability(stack);
        if (this.taskTicks == 1) {
            this.mendBest = durability;
            this.mendGainTick = this.tick;
        }
        if (durability >= stack.getMaxDamage() || durability < 0) {
            if (durability >= 0) {
                this.say(ModuleChat.body().good("Elytra repaired").text("."));
            }
            this.mendFailures = 0;
            this.task = Task.NONE;
            return;
        }
        if (durability > this.mendBest) {
            this.mendBest = durability;
            this.mendGainTick = this.tick;
        }
        if (this.tick - this.mendGainTick > 200) {
            ++this.mendFailures;
            this.say(
                    ModuleChat.body()
                            .warn("The XP is not reaching the elytra")
                            .text(" (other Mending gear may be taking it)."));
            this.task = Task.NONE;
            return;
        }
        if (ElytraAutopilotModule.count(player, XP) == 0) {
            this.task =
                    this.canBuy(Items.EXPERIENCE_BOTTLE)
                                    && !this.doneTasks.contains((Object) Task.BUY_XP)
                            ? Task.BUY_XP
                            : Task.NONE;
            Task task2 = this.task;
            if (this.task == Task.BUY_XP) {
                this.doneTasks.add(Task.BUY_XP);
                this.taskTicks = 0;
            }
            return;
        }
        if (!Hands.hold(this.mc, player, XP)) {
            return;
        }
        Aim.turn(this.mc, player.getYaw(), 86.0f, 22.0f);
        this.mc.options.useKey.setPressed(player.getPitch() > 70.0f);
    }

    private void buy(ClientPlayerEntity player) {
        if (this.verifyUntil > 0) {
            this.verifyPurchase(player);
            return;
        }
        if (this.purchase != null) {
            Aim.turn(this.mc, player.getYaw(), 40.0f, 22.0f);
            this.followPurchase();
            return;
        }
        boolean bl = this.task == Task.BUY_ROCKETS;
        Item item = bl ? Items.FIREWORK_ROCKET : Items.EXPERIENCE_BOTTLE;
        Predicate<ItemStack> predicate = bl ? ElytraAutopilotModule::buyableRocket : XP;
        int i =
                (bl ? this.rocketStacks.getInt() : this.xpStacks.getInt()) * 64
                        - ElytraAutopilotModule.count(player, predicate);
        long money = AhPurchase.parseMoney(bl ? this.rocketPrice.get() : this.xpPrice.get());
        if (i <= 0
                || money <= 0L
                || !this.canBuy(item)
                || ElytraAutopilotModule.freeSlots(player) == 0) {
            this.task = Task.NONE;
            return;
        }
        double d = (double) money / 64.0;
        Predicate<ItemStack> predicate2 =
                itemStack ->
                        predicate.test((ItemStack) itemStack)
                                && AhPurchase.unitPrice(itemStack) <= d;
        this.purchaseItem = item;
        this.purchaseBefore = ElytraAutopilotModule.count(player, predicate);
        long money2 = AhPurchase.parseMoney(this.tripLimit.get());
        long l = money2 <= 0L ? 0L : Math.max(1L, money2 - this.spent);
        this.say(
                ModuleChat.body()
                        .text("Buying " + i + (bl ? " rockets" : " XP bottles") + " on the AH."));
        this.purchase =
                AhPurchase.startAny(
                        this,
                        item,
                        bl ? "Rockets" : "XP Bottles",
                        bl ? "firework rocket" : "xp bottle stack",
                        predicate2,
                        i,
                        money,
                        1000L,
                        l);
    }

    private void followPurchase() {
        AhPurchase.Status status2 = this.purchase.status();
        if (status2 == AhPurchase.Status.RUNNING && this.taskTicks < 4800) {
            return;
        }
        this.spent += this.purchase.spent();
        if (status2 == AhPurchase.Status.RUNNING) {
            this.purchase.cancel();
        }
        this.purchase = null;
        if (status2 != AhPurchase.Status.DONE) {
            this.buyFails.merge(this.purchaseItem, 1, Integer::sum);
            this.nextBuy.put(this.purchaseItem, this.tick + 6000);
            this.say(
                    ModuleChat.body()
                            .warn(
                                    "Could not buy "
                                            + (this.purchaseItem == Items.FIREWORK_ROCKET
                                                    ? "rockets"
                                                    : "XP bottles"))
                            .text(", flying on."));
            this.purchaseItem = null;
            this.task = Task.NONE;
            return;
        }
        this.verifyUntil = this.tick + 60;
    }

    private void verifyPurchase(ClientPlayerEntity player) {
        Predicate<ItemStack> predicate =
                this.purchaseItem == Items.FIREWORK_ROCKET
                        ? ElytraAutopilotModule::buyableRocket
                        : XP;
        int i = ElytraAutopilotModule.count(player, predicate) - this.purchaseBefore;
        if (i <= 0 && this.tick < this.verifyUntil) {
            return;
        }
        if (i > 0) {
            this.say(
                    ModuleChat.body()
                            .good(
                                    "Bought "
                                            + i
                                            + (this.purchaseItem == Items.FIREWORK_ROCKET
                                                    ? " rockets"
                                                    : " XP bottles"))
                            .text("."));
            EasyPlace.LOG.info(
                    "[Elytra Autopilot] bought {} {} (verified), {} spent this trip",
                    i,
                    this.purchaseItem,
                    this.spent);
        } else {
            this.say(
                    ModuleChat.body()
                            .warn("The AH sold it but nothing showed up")
                            .text(", trying again later."));
            this.nextBuy.put(this.purchaseItem, this.tick + 6000);
        }
        this.verifyUntil = 0;
        this.purchaseItem = null;
        this.task = Task.NONE;
    }

    private boolean canBuy(Item item) {
        boolean bl = item == Items.FIREWORK_ROCKET ? this.buyRockets.get() : this.buyXp.get();
        boolean bl2 = bl;
        if (!bl
                || this.tick < this.nextBuy.getOrDefault(item, 0)
                || this.buyFails.getOrDefault(item, 0) >= 6) {
            return false;
        }
        long money = AhPurchase.parseMoney(this.tripLimit.get());
        return money <= 0L || this.spent < money;
    }

    private boolean danger(ClientPlayerEntity player, ClientWorld world) {
        if (player.hurtTime > 0 && player.getHealth() < 14.0f) {
            return true;
        }
        Box box = player.getBoundingBox().expand(4.5, 2.0, 4.5);
        for (LivingEntity entity : world.getNonSpectatingEntities(LivingEntity.class, box)) {
            if (!(entity instanceof Monster) || !entity.isAlive() || !player.canSee(entity))
                continue;
            return true;
        }
        return false;
    }

    private boolean rocketReady(ClientPlayerEntity player) {
        if (this.tick - this.lastRocket < 10
                || this.tick < this.eatLockUntil
                || this.tick < this.rocketHoldUntil
                || player.isUsingItem()) {
            return false;
        }
        if (this.mc.currentScreen != null) {
            return false;
        }
        return ElytraAutopilotModule.hotbarSlot(player, ROCKET) >= 0
                || ROCKET.test(player.getOffHandStack());
    }

    private void fire(ClientPlayerEntity player) {
        Hand mAIN_HAND;
        if (!player.isGliding()) {
            return;
        }
        Vec3d eyePos = player.getEyePos();
        BlockHitResult blockHitResult =
                this.mc.world.raycast(
                        new RaycastContext(
                                eyePos,
                                eyePos.add(player.getRotationVector().multiply(10.0)),
                                RaycastContext.ShapeType.COLLIDER,
                                RaycastContext.FluidHandling.ANY,
                                player));
        if (blockHitResult.getType() != HitResult.Type.MISS) {
            return;
        }
        if (ROCKET.test(player.getMainHandStack())) {
            mAIN_HAND = Hand.MAIN_HAND;
        } else if (ROCKET.test(player.getOffHandStack())) {
            mAIN_HAND = Hand.OFF_HAND;
        } else {
            int hotbarSlot = ElytraAutopilotModule.hotbarSlot(player, ROCKET);
            if (hotbarSlot >= 0) {
                player.getInventory().setSelectedSlot(hotbarSlot);
            }
            return;
        }
        if (!this.mc.interactionManager.interactItem(player, mAIN_HAND).isAccepted()) {
            return;
        }
        player.swingHand(mAIN_HAND);
        this.lastRocket = this.tick;
        this.boostUntil = this.tick + 45;
        ++this.rocketsUsed;
    }

    private boolean boosting() {
        return this.tick < this.boostUntil;
    }

    private int boostLeft() {
        return Math.max(0, this.boostUntil - this.tick);
    }

    private boolean rocketsAllowed(ClientPlayerEntity player, ClientWorld world, double d) {
        if (this.tick < this.rocketHoldUntil
                || this.tick < this.eatLockUntil
                || ElytraAutopilotModule.count(player, ROCKET) == 0) {
            return false;
        }
        Vec3d velocity = player.getVelocity();
        Vec3d vec =
                velocity.horizontalLength() > 0.2
                        ? new Vec3d(velocity.x, 0.0, velocity.z).normalize()
                        : Vec3d.fromPolar(0.0f, player.getYaw());
        Vec3d vec2 = player.getEntityPos().add(vec.multiply(110.0));
        return world.getChunkManager()
                        .isChunkLoaded(MathHelper.floor(vec2.x) >> 4, MathHelper.floor(vec2.z) >> 4)
                || player.getY() < d - 50.0;
    }

    private static double rocketCost(ClientPlayerEntity player) {
        int i = ElytraAutopilotModule.count(player, ROCKET);
        return i < 12 ? 140.0 : (i < 32 ? 95.0 : 70.0);
    }

    private ElytraFlight.Terrain terrain(ClientWorld world) {
        return new ElytraFlight.Terrain(
                world, (world.getRegistryKey() == World.END ? 185.0 : 262.0) - 14.0);
    }

    private static double floor(ClientWorld world) {
        return world.getRegistryKey() == World.END ? 35.0 : (double) world.getBottomY();
    }

    private static double surfaceAt(ClientWorld world, double d, double e) {
        int i = MathHelper.floor(d);
        int j = MathHelper.floor(e);
        if (!world.getChunkManager().isChunkLoaded(i >> 4, j >> 4)) {
            return world.getRegistryKey() == World.END ? 60.0 : 68.0;
        }
        return world.getTopY(Heightmap.Type.MOTION_BLOCKING, i, j);
    }

    private double cruiseAltitude(ClientWorld world, double d) {
        double surface = ElytraAutopilotModule.surfaceAt(world, this.destX, this.destZ);
        double e = world.getRegistryKey() == World.END ? 150.0 : 140.0;
        double d2 = surface + 14.0 + Math.max(0.0, d - 45.0) * 0.095 + 30.0;
        return Math.max(e, Math.min((double) this.altitude.get() + this.altitudeBump, d2));
    }

    private double descentStart(ClientWorld world, ClientPlayerEntity player) {
        double surface = ElytraAutopilotModule.surfaceAt(world, this.destX, this.destZ);
        return 45.0 + Math.max(0.0, player.getY() - surface - 14.0) / 0.095 + 60.0;
    }

    private double terrainAhead(ClientPlayerEntity player, ElytraFlight.Terrain terrain, double d) {
        float bearing = ElytraFlight.bearing(player.getX(), player.getZ(), this.destX, this.destZ);
        Vec3d vec = Vec3d.fromPolar(0.0f, bearing);
        Vec3d vec2 = Vec3d.fromPolar(0.0f, bearing + 90.0f);
        double e = Math.min(320.0, d + 24.0);
        double d2 = 0.0;
        for (double e2 = 0.0; e2 <= e; e2 += 4.0) {
            for (int i = -8; i <= 8; i += 8) {
                double d3 = player.getX() + vec.x * e2 + vec2.x * (double) i;
                double e3 = player.getZ() + vec.z * e2 + vec2.z * (double) i;
                if (!terrain.known(MathHelper.floor(d3), MathHelper.floor(e3)) && e2 > 200.0)
                    continue;
                d2 = Math.max(d2, terrain.surface(d3, e3));
            }
        }
        return d2;
    }

    private static boolean clearAround(ClientPlayerEntity player, ElytraFlight.Terrain terrain) {
        for (int i = -1; i <= 1; ++i) {
            for (int j = -1; j <= 1; ++j) {
                if (!(terrain.surface(player.getX() + (double) i, player.getZ() + (double) j)
                        > player.getY())) continue;
                return false;
            }
        }
        return true;
    }

    private void newJitter() {
        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        this.bandJitter = (float) threadLocalRandom.nextDouble(-8.0, 8.0);
        this.glideJitter = (float) threadLocalRandom.nextDouble(-1.0, 1.0);
    }

    private static boolean plainRocket(ItemStack stack) {
        if (!stack.isOf(Items.FIREWORK_ROCKET)) {
            return false;
        }
        FireworksComponent fireworksComponent = stack.get(DataComponentTypes.FIREWORKS);
        return fireworksComponent == null || fireworksComponent.explosions().isEmpty();
    }

    private static boolean buyableRocket(ItemStack stack) {
        FireworksComponent fireworksComponent = stack.get(DataComponentTypes.FIREWORKS);
        return ElytraAutopilotModule.plainRocket(stack)
                && fireworksComponent != null
                && fireworksComponent.flightDuration() == 3;
    }

    private static ItemStack chest(ClientPlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.CHEST);
    }

    private static int durability(ItemStack stack) {
        return stack.isOf(Items.ELYTRA) ? stack.getMaxDamage() - stack.getDamage() : -1;
    }

    private static boolean usable(ItemStack stack) {
        return ElytraAutopilotModule.durability(stack) > 1;
    }

    private static boolean mends(ItemStack stack) {
        return ElytraAutopilotModule.level(stack, Enchantments.MENDING) > 0;
    }

    private static int level(ItemStack stack, RegistryKey<Enchantment> key) {
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> enchantmentEntry :
                stack.getEnchantments().getEnchantmentEntries()) {
            if (!((RegistryEntry) enchantmentEntry.getKey()).matchesKey(key)) continue;
            return enchantmentEntry.getIntValue();
        }
        return 0;
    }

    private static int mendThreshold(ItemStack stack) {
        int level = ElytraAutopilotModule.level(stack, Enchantments.UNBREAKING);
        int maxDamage = stack.getMaxDamage();
        if (level >= 3) {
            return Math.max(50, (int) ((double) maxDamage * 0.15));
        }
        if (level >= 1) {
            return Math.max(80, (int) ((double) maxDamage * 0.2));
        }
        return Math.max(110, (int) ((double) maxDamage * 0.27));
    }

    private boolean canMend(ClientPlayerEntity player) {
        ItemStack stack = ElytraAutopilotModule.chest(player);
        return this.mend.get()
                && ElytraAutopilotModule.mends(stack)
                && this.mendFailures < 3
                && (ElytraAutopilotModule.count(player, XP) > 0
                        || this.canBuy(Items.EXPERIENCE_BOTTLE));
    }

    private static int spare(ClientPlayerEntity player) {
        int j = Math.max(0, ElytraAutopilotModule.durability(ElytraAutopilotModule.chest(player)));
        PlayerInventory inventory = player.getInventory();
        int k = -1;
        int m = j + 40;
        for (int i = 0; i < 36; ++i) {
            int durability = ElytraAutopilotModule.durability(inventory.getStack(i));
            if (durability <= m) continue;
            k = i;
            m = durability;
        }
        return k;
    }

    private static int count(ClientPlayerEntity player, Predicate<ItemStack> predicate) {
        int j = predicate.test(player.getOffHandStack()) ? player.getOffHandStack().getCount() : 0;
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < 36; ++i) {
            ItemStack stack = inventory.getStack(i);
            if (!predicate.test(stack)) continue;
            j += stack.getCount();
        }
        return j;
    }

    private static int hotbarSlot(ClientPlayerEntity player, Predicate<ItemStack> predicate) {
        PlayerInventory inventory = player.getInventory();
        if (predicate.test(inventory.getStack(inventory.getSelectedSlot()))) {
            return inventory.getSelectedSlot();
        }
        for (int i = 0; i < 9; ++i) {
            if (!predicate.test(inventory.getStack(i))) continue;
            return i;
        }
        return -1;
    }

    private static boolean inHotbar(ClientPlayerEntity player, Predicate<ItemStack> predicate) {
        return ElytraAutopilotModule.hotbarSlot(player, predicate) >= 0
                || predicate.test(player.getOffHandStack());
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

    private void finish(boolean bl, String stopReason2) {
        ClientPlayerEntity player = this.mc.player;
        if (!bl && player != null && player.isGliding() && this.job != Job.STOP) {
            this.job = Job.STOP;
            this.stopReason = stopReason2;
            this.spot = null;
            this.spotSearches = 0;
            this.say(ModuleChat.body().warn("Landing to stop").text(" (" + stopReason2 + ")."));
            this.go(State.DESCEND);
            return;
        }
        long l = (System.currentTimeMillis() - this.startedAt) / 60000L;
        String string =
                (String) (bl ? "Arrived" : "Stopped: " + stopReason2)
                        + " after "
                        + Math.round(this.flown)
                        + " blocks in "
                        + l
                        + " min, "
                        + this.rocketsUsed
                        + " rockets"
                        + (String) (this.pitStops > 0 ? ", " + this.pitStops + " pit stops" : "")
                        + (String)
                                (this.spent > 0L
                                        ? ", $" + AhPurchase.money(this.spent) + " spent"
                                        : "")
                        + ".";
        if (bl) {
            this.say(ModuleChat.body().good(string));
        } else {
            this.say(ModuleChat.body().warn(string));
        }
        EasyPlace.LOG.info("[Elytra Autopilot] {}", (Object) string);
        this.requestDisable();
        if (this.disconnect.get()
                && this.mc.getNetworkHandler() != null
                && !this.mc.isConnectedToLocalServer()) {
            AutoReconnectModule.holdOff();
            this.mc
                    .getNetworkHandler()
                    .getConnection()
                    .disconnect(Text.literal("[Elytra Autopilot] " + string));
        }
    }

    private void showStatus(ClientPlayerEntity player) {
        double distance = this.distance(player);
        long l = Math.round(distance / 27.0);
        String string =
                switch (this.state.ordinal()) {
                    default -> throw new MatchException(null, null);
                    case 0 -> "getting ready";
                    case 1 -> "taking off";
                    case 2 -> "climbing";
                    case 3 -> {
                        if (this.climbing) {
                            yield "climbing";
                        }
                        yield "gliding";
                    }
                    case 4 -> {
                        if (this.spiralling) {
                            yield "circling down";
                        }
                        yield "gliding down";
                    }
                    case 5 -> "landing";
                    case 6 -> "pit stop";
                };
        ModuleChat.actionBar(
                this,
                ModuleChat.body()
                        .text(
                                String.format(
                                        Locale.ROOT,
                                        "%,d blocks, ~%d:%02d, %d rockets, elytra %d, %s",
                                        Math.round(distance),
                                        l / 60L,
                                        l % 60L,
                                        ElytraAutopilotModule.count(player, ROCKET),
                                        Math.max(
                                                0,
                                                ElytraAutopilotModule.durability(
                                                        ElytraAutopilotModule.chest(player))),
                                        string)));
    }

    private double distance(ClientPlayerEntity player) {
        return Math.hypot(player.getX() - this.destX, player.getZ() - this.destZ);
    }

    private void say(ModuleChat.Body body) {
        ModuleChat.send((ClientModule) this, body);
    }

    static double[] parseTarget(String string) {
        double[] doubles;
        if (string == null) {
            return null;
        }
        String string2 =
                string.toLowerCase(Locale.ROOT)
                        .replaceAll("[xyz]\\s*[:=]", " ")
                        .replaceAll("[,;/|()\\[\\]]", " ")
                        .trim();
        if (string2.isEmpty()) {
            return null;
        }
        String[] strings = string2.split("\\s+");
        if (strings.length < 2 || strings.length > 3) {
            return null;
        }
        double[] doubles2 = new double[strings.length];
        for (int i = 0; i < strings.length; ++i) {
            String string3 = strings[i];
            double d = 1.0;
            if (string3.endsWith("k")) {
                d = 1000.0;
                string3 = string3.substring(0, string3.length() - 1);
            }
            try {
                doubles2[i] = Double.parseDouble(string3) * d;
            } catch (NumberFormatException exception) {
                return null;
            }
            if (Double.isFinite(doubles2[i]) && !(Math.abs(doubles2[i]) > 3.0E7)) continue;
            return null;
        }
        if (strings.length == 2) {
            double[] doubles3 = new double[2];
            doubles3[0] = doubles2[0];
            doubles = doubles3;
            doubles3[1] = doubles2[1];
        } else {
            double[] doubles4 = new double[2];
            doubles4[0] = doubles2[0];
            doubles = doubles4;
            doubles4[1] = doubles2[2];
        }
        return doubles;
    }

    @Environment(value = EnvType.CLIENT)
    private static enum State {
        PREPARE,
        TAKEOFF,
        CLIMB,
        CRUISE,
        DESCEND,
        LAND,
        SERVICE
    }

    @Environment(value = EnvType.CLIENT)
    private static enum Job {
        ARRIVE,
        SERVICE,
        STOP
    }

    @Environment(value = EnvType.CLIENT)
    private static enum Task {
        NONE,
        EAT,
        SWAP,
        MEND,
        BUY_ROCKETS,
        BUY_XP
    }
}
