package net.efn.client.easyplace.builder;

import net.efn.client.chat.ModuleChat;
import net.efn.client.easyplace.inventory.InventoryCalm;
import net.efn.client.event.EventPriority;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.MouseInputEvent;
import net.efn.client.event.impl.Render3DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.FreecamModule;
import net.efn.client.module.setting.ActionSetting;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
public final class AreaMinerModule extends ClientModule {

    private static final long PLAN_BUDGET_NANOS = 3000000L;

    private static final int PLAN_TARGETS = 400;

    private static final int BREAK_TRIES = 3;

    private static final int PLAN_FAILS = 2;

    private static final int SCAN_TICKS = 10;

    private static final int MEND_TICKS = 1200;

    private static final int MAX_CELLS = 2000000;

    private static final int AREA_COLOR = -11155201;

    private static final int TARGET_COLOR = -20416;

    private static final Predicate<ItemStack> XP =
            itemStack -> itemStack.isOf(Items.EXPERIENCE_BOTTLE);

    private final StringSetting corner1 =
            this.addSetting("corner-1", new StringSetting("Corner 1", "").hint("x y z"));

    private final StringSetting corner2 =
            this.addSetting("corner-2", new StringSetting("Corner 2", "").hint("x y z"));

    private final ActionSetting select =
            this.addSetting(
                    "select-area", new ActionSetting("Select Area With Clicks", this::selectArea));

    private final ActionSetting feet1 =
            this.addSetting(
                    "corner-1-feet",
                    new ActionSetting(
                            "Corner 1 At Feet", () -> this.setCorner(this.corner1, false)));

    private final ActionSetting look1 =
            this.addSetting(
                    "corner-1-look",
                    new ActionSetting(
                            "Corner 1 At Crosshair", () -> this.setCorner(this.corner1, true)));

    private final ActionSetting feet2 =
            this.addSetting(
                    "corner-2-feet",
                    new ActionSetting(
                            "Corner 2 At Feet", () -> this.setCorner(this.corner2, false)));

    private final ActionSetting look2 =
            this.addSetting(
                    "corner-2-look",
                    new ActionSetting(
                            "Corner 2 At Crosshair", () -> this.setCorner(this.corner2, true)));

    private final BooleanSetting useShard =
            this.addSetting("use-shard", new BooleanSetting("Use Shard Pickaxe", true));

    private final BooleanSetting eat =
            this.addSetting("eat", new BooleanSetting("Eat When Hungry", true));

    private final BooleanSetting mend =
            this.addSetting("mend", new BooleanSetting("Mend With XP Bottles", true));

    private final NumberSetting mendBelow =
            this.addSetting(
                    "mend-below",
                    new NumberSetting("Mend Below Durability", 150.0F, 20.0F, 1000.0F).step(10.0F));

    private final BooleanSetting pauseInMenus =
            this.addSetting("pause-in-menus", new BooleanSetting("Pause In Menus", true));

    private final BooleanSetting showArea =
            this.addSetting("show-area", new BooleanSetting("Show Area", true));

    private final BooleanSetting chatFeedback =
            this.addSetting("chat-feedback", new BooleanSetting("Chat Feedback", true));

    private final MinecraftClient mc = MinecraftClient.getInstance();

    private BlockPos min;

    private BlockPos max;

    private ClientWorld seenLevel;

    private long tick;

    private long nextScan;

    private long noPlanUntil;

    private Breaker breaker;

    private boolean breakerShard;

    private Hop hop;

    private Eater eater;

    private StandPlanner planner;

    private List<StandPlanner.Step> route;

    private int routeIndex;

    private BlockPos routeStart;

    private boolean mending;

    private long mendStarted;

    private int mined;

    private int startCount;

    private int layer = -2147483648;

    private final List<BlockPos> layerCells = new ArrayList<>();

    private final Map<Long, Integer> breakTries = new HashMap<>();

    private final Map<Long, Integer> planFails = new HashMap<>();

    private final Set<Long> unreachable = new HashSet<>();

    private final Set<Long> holding = new HashSet<>();

    private final Map<String, Long> lastFeedback = new HashMap<>();

    private int picking;

    private BlockPos firstPick;

    public AreaMinerModule() {
        super(
                "area-miner",
                "Area Miner",
                "Mines out everything between two corners, top layer first, walking and turning by"
                    + " hand like a player. A Shard Pickaxe cuts 3x3 wherever the whole cut stays"
                    + " inside the area; blocks holding back lava or water stay.",
                ModuleCategory.MISC);
        this.usesNotifications();
        this.mendBelow.visibleWhen(this.mend::get);
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    protected void onEnable() {
        ClientPlayerEntity player = this.mc.player;
        if (player != null && this.mc.world != null) {
            this.on(TickEvent.TYPE, Stage.PRE, tickEvent -> this.step());
            this.on(Render3DEvent.TYPE, Stage.POST, this::render);
            this.on(MouseInputEvent.TYPE, Stage.PRE, EventPriority.HIGH, this::onMouse);
            BlockPos pos = parse(this.corner1.get());
            BlockPos pos2 = parse(this.corner2.get());
            if (pos != null && pos2 != null) {
                if (!this.begin(pos, pos2)) {
                    this.requestDisable();
                }
            } else {
                this.startPicking();
            }
        } else {
            this.say(ModuleChat.body().warn("Join a world first."));
            this.requestDisable();
        }
    }

    private boolean begin(BlockPos pos, BlockPos pos2) {
        BlockPos pos3 =
                new BlockPos(
                        Math.min(pos.getX(), pos2.getX()),
                        Math.min(pos.getY(), pos2.getY()),
                        Math.min(pos.getZ(), pos2.getZ()));
        BlockPos pos4 =
                new BlockPos(
                        Math.max(pos.getX(), pos2.getX()),
                        Math.max(pos.getY(), pos2.getY()),
                        Math.max(pos.getZ(), pos2.getZ()));
        long l =
                (long) (pos4.getX() - pos3.getX() + 1)
                        * (pos4.getY() - pos3.getY() + 1)
                        * (pos4.getZ() - pos3.getZ() + 1);
        if (l > 2000000L) {
            this.say(
                    ModuleChat.body()
                            .warn("The area is too big")
                            .text(" (" + l + " blocks, at most 2000000)."));
            return false;
        } else {
            this.reset();
            this.min = pos3;
            this.max = pos4;
            this.seenLevel = this.mc.world;
            this.startCount = this.countLeft(this.mc.world);
            this.say(
                    ModuleChat.body()
                            .text(
                                    "Mining "
                                            + this.startCount
                                            + " blocks between "
                                            + this.min.toShortString()
                                            + " and "
                                            + this.max.toShortString()
                                            + "."));
            return true;
        }
    }

    private void startPicking() {
        this.release();
        this.min = null;
        this.max = null;
        this.firstPick = null;
        this.picking = 1;
        this.say(
                ModuleChat.body()
                        .text("Hit the first corner block")
                        .text(" (or type both corners in the settings)."));
    }

    private void selectArea() {
        this.corner1.set("");
        this.corner2.set("");
        if (this.isEnabled()) {
            this.startPicking();
        } else {
            this.setEnabled(true);
        }
    }

    private void onMouse(MouseInputEvent event) {
        if (this.picking != 0 && event.button == 0 && event.action == 1) {
            if (this.mc.currentScreen == null && this.mc.player != null && this.mc.world != null) {
                event.setCancelled(true);
                BlockPos pos = this.aimedBlock();
                if (pos == null) {
                    this.say(ModuleChat.body().warn("No block there").text(", look at a block."));
                } else {
                    String string = pos.getX() + " " + pos.getY() + " " + pos.getZ();
                    if (this.picking == 1) {
                        this.firstPick = pos;
                        this.corner1.set(string);
                        this.picking = 2;
                        this.say(
                                ModuleChat.body()
                                        .text(
                                                "Corner 1 set to "
                                                        + pos.toShortString()
                                                        + ". Hit the second corner block."));
                    } else {
                        this.corner2.set(string);
                        this.say(
                                ModuleChat.body()
                                        .text("Corner 2 set to " + pos.toShortString() + "."));
                        if (this.begin(this.firstPick, pos)) {
                            this.picking = 0;
                        } else {
                            this.say(ModuleChat.body().text("Hit the second corner block again."));
                        }
                    }
                }
            }
        }
    }

    private BlockPos aimedBlock() {
        ClientPlayerEntity player = this.mc.player;
        if (player != null && this.mc.world != null) {
            FreecamModule module = ModuleRegistry.get(FreecamModule.class);
            Vec3d vec;
            Vec3d cameraPos;
            if (module != null && module.isEnabled()) {
                Camera camera = this.mc.gameRenderer.getCamera();
                cameraPos = camera.getCameraPos();
                vec = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());
            } else {
                cameraPos = player.getCameraPosVec(1.0F);
                vec = player.getRotationVec(1.0F);
            }
            double d = this.mc.options.getClampedViewDistance() * 16.0;
            BlockHitResult blockHitResult =
                    this.mc.world.raycast(
                            new RaycastContext(
                                    cameraPos,
                                    cameraPos.add(vec.multiply(d)),
                                    ShapeType.OUTLINE,
                                    FluidHandling.NONE,
                                    player));
            return blockHitResult.getType() == Type.BLOCK ? blockHitResult.getBlockPos() : null;
        } else {
            return null;
        }
    }

    @Override
    protected void onDisable() {
        this.release();
        this.planner = null;
        this.route = null;
        this.picking = 0;
        this.firstPick = null;
    }

    private void reset() {
        this.tick = 0L;
        this.nextScan = 0L;
        this.noPlanUntil = 0L;
        this.breaker = null;
        this.hop = null;
        this.eater = null;
        this.planner = null;
        this.route = null;
        this.mending = false;
        this.mined = 0;
        this.layer = -2147483648;
        this.layerCells.clear();
        this.breakTries.clear();
        this.planFails.clear();
        this.unreachable.clear();
        this.holding.clear();
        this.lastFeedback.clear();
    }

    private void step() {
        ClientPlayerEntity player = this.mc.player;
        ClientWorld world = this.mc.world;
        if (player != null && world != null && this.mc.interactionManager != null) {
            if (this.picking == 0 && this.min != null) {
                this.tick++;
                if (world != this.seenLevel || !player.isAlive()) {
                    this.say(
                            ModuleChat.body()
                                    .warn("Stopped")
                                    .text(", the world changed or you died."));
                    this.requestDisable();
                } else if (this.mc.currentScreen != null && this.pauseInMenus.get()) {
                    this.release();
                } else if (this.eater != null) {
                    if (this.eater.tick(this.mc) != Eater.Result.RUNNING) {
                        this.eater = null;
                    }
                } else if (this.mending) {
                    this.mend(player);
                } else if (this.breaker != null) {
                    BlockState blockState = world.getBlockState(this.breaker.pos());
                    if (blockState.isAir()
                            || Hands.hold(
                                    this.mc,
                                    player,
                                    toolFor(player, blockState, this.breakerShard))) {
                        Breaker.Result result = this.breaker.tick(this.mc);
                        if (result != Breaker.Result.RUNNING) {
                            BlockPos pos = this.breaker.pos();
                            this.breaker = null;
                            if (minable(world, pos)
                                    && this.breakTries.merge(pos.asLong(), 1, Integer::sum) >= 3) {
                                this.unreachable.add(pos.asLong());
                                this.feedback(
                                        "gave-up",
                                        "Could not mine " + pos.toShortString() + ", leaving it",
                                        40);
                            }
                            this.nextScan = 0L;
                        }
                    }
                } else if (this.hop != null) {
                    Hop.Result result2 = this.hop.tick(this.mc);
                    if (result2 != Hop.Result.RUNNING) {
                        this.hop = null;
                        if (result2 == Hop.Result.FAILED) {
                            this.route = null;
                            this.noPlanUntil = this.tick + 20L;
                        }
                    }
                } else if (this.eat.get()
                        && Eater.hungry(player)
                        && Eater.hasFood(player)
                        && InventoryCalm.still(player)) {
                    this.release();
                    this.eater = new Eater();
                } else if (this.mendDue(player)) {
                    this.mending = true;
                    this.mendStarted = this.tick;
                    this.feedback("mend", "Tool is worn, mending it with XP bottles", 200);
                } else if (this.route != null) {
                    this.followRoute(world, player);
                } else if (this.planner != null) {
                    this.followPlanner();
                } else {
                    if (this.tick >= this.nextScan) {
                        this.nextScan = this.tick + 10L;
                        if (!this.pickLayer(world)) {
                            this.finish();
                            return;
                        }
                    }
                    this.dig(world, player);
                }
            }
        }
    }

    private boolean pickLayer(ClientWorld world) {
        this.layerCells.clear();
        Mutable mutable = new Mutable();
        for (int i = this.max.getY(); i >= this.min.getY(); i--) {
            for (int j = this.min.getX(); j <= this.max.getX(); j++) {
                for (int k = this.min.getZ(); k <= this.max.getZ(); k++) {
                    mutable.set(j, i, k);
                    long l = mutable.asLong();
                    if (!this.unreachable.contains(l)
                            && world.isChunkLoaded(mutable)
                            && minable(world, mutable)) {
                        if (this.holdsFluid(world, mutable)) {
                            if (this.holding.add(l)) {
                                this.feedback(
                                        "fluid",
                                        "Leaving "
                                                + mutable.toShortString()
                                                + ", it holds back lava or water",
                                        100);
                            }
                        } else {
                            this.holding.remove(l);
                            this.layerCells.add(mutable.toImmutable());
                        }
                    }
                }
            }
            if (!this.layerCells.isEmpty()) {
                if (i != this.layer) {
                    this.layer = i;
                    this.planFails.clear();
                    this.feedback(
                            "layer",
                            "Layer Y="
                                    + i
                                    + ", "
                                    + this.layerCells.size()
                                    + (this.layerCells.size() == 1 ? " block" : " blocks"),
                            1);
                }
                return true;
            }
        }
        return false;
    }

    private void dig(ClientWorld world, ClientPlayerEntity player) {
        if (!this.layerCells.isEmpty()) {
            Vec3d eyePos = player.getEyePos();
            double d = player.getBlockInteractionRange() - 0.3;
            ArrayList list = new ArrayList();
            for (BlockPos layerCell : this.layerCells) {
                if (layerCell.toCenterPos().distanceTo(eyePos) <= d && minable(world, layerCell)) {
                    list.add(layerCell);
                }
            }
            BlockPos pos = player.getBlockPos().down();
            list.sort(
                    Comparator.<BlockPos>comparingInt(blockPos2 -> blockPos2.equals(pos) ? 1 : 0)
                            .thenComparingDouble(
                                    blockPos -> blockPos.toCenterPos().squaredDistanceTo(eyePos)));
            for (BlockPos pos2 : (Iterable<BlockPos>) list) {
                if (Aim.at(this.mc, pos2) != null) {
                    BlockState blockState = world.getBlockState(pos2);
                    boolean bl =
                            this.breakerShard = this.useShard.get() && this.cutInside(world, pos2);
                    if (!Hands.hold(
                            this.mc, player, toolFor(player, blockState, this.breakerShard))) {
                        return;
                    }
                    this.breaker = new Breaker(pos2, true);
                    return;
                }
            }
            if (this.tick >= this.noPlanUntil) {
                ArrayList list2 = new ArrayList();
                Vec3d entityPos = player.getEntityPos();
                ArrayList list3 = new ArrayList<>(this.layerCells);
                list3.sort(
                        Comparator.comparingDouble(
                                (BlockPos pos3) -> pos3.getSquaredDistance(entityPos)));
                for (BlockPos pos4 : (Iterable<BlockPos>) list3) {
                    if (list2.size() >= 400) {
                        break;
                    }
                    list2.add(
                            new StandPlanner.Target(
                                    pos4, world.getBlockState(pos4), StandPlanner.Use.BREAK));
                }
                this.planner =
                        new StandPlanner(
                                world, player, list2, blockPos -> false, null, null, null, null);
                this.followPlanner();
            }
        }
    }

    private void followPlanner() {
        StandPlanner.Status status = this.planner.step(System.nanoTime() + 3000000L);
        if (status != StandPlanner.Status.RUNNING) {
            StandPlanner planner2 = this.planner;
            this.planner = null;
            if ((status == StandPlanner.Status.FOUND || status == StandPlanner.Status.APPROACH)
                    && !planner2.route().isEmpty()) {
                this.route = planner2.route();
                this.routeIndex = 0;
                this.routeStart = this.mc.player == null ? null : this.mc.player.getBlockPos();
            } else {
                this.noPlanUntil = this.tick + 40L;
                boolean bl = false;
                for (BlockPos layerCell : this.layerCells) {
                    if (this.planFails.merge(layerCell.asLong(), 1, Integer::sum) >= 2) {
                        this.unreachable.add(layerCell.asLong());
                        bl = true;
                    }
                }
                if (bl) {
                    this.feedback(
                            "unreachable",
                            "No spot gets at the rest of layer Y="
                                    + this.layer
                                    + ", leaving those blocks",
                            40);
                }
                this.nextScan = 0L;
            }
        }
    }

    private void followRoute(ClientWorld world, ClientPlayerEntity player) {
        if (this.routeIndex >= this.route.size()) {
            this.route = null;
            this.nextScan = 0L;
        } else if (player.isOnGround()) {
            BlockPos pos =
                    this.routeIndex == 0
                            ? this.routeStart
                            : this.route.get(this.routeIndex - 1).pos();
            BlockPos pos2 = player.getBlockPos();
            if (pos == null
                    || pos2.getX() == pos.getX()
                            && pos2.getZ() == pos.getZ()
                            && Math.abs(pos2.getY() - pos.getY()) <= 1) {
                StandPlanner.Step step;
                if ((step = this.route.get(this.routeIndex++)).move() != StandPlanner.Move.WALK
                        && step.move() != StandPlanner.Move.LEAP) {
                    this.route = null;
                    this.noPlanUntil = this.tick + 40L;
                } else {
                    boolean bl =
                            step.move() == StandPlanner.Move.WALK
                                    && this.routeIndex < this.route.size()
                                    && this.route.get(this.routeIndex).move()
                                            == StandPlanner.Move.WALK;
                    this.hop = new Hop(step.pos(), bl, step.move() == StandPlanner.Move.LEAP);
                    if (step.move() == StandPlanner.Move.WALK
                            && (Walker.risky(world, pos2) || Walker.risky(world, step.pos()))) {
                        if (step.pos().getY() >= pos2.getY()) {
                            this.hop.careful();
                        } else {
                            this.hop.slow();
                        }
                    }
                }
            } else {
                this.release();
                this.route = null;
                this.noPlanUntil = this.tick + 10L;
            }
        }
    }

    private static boolean minable(ClientWorld world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        return !blockState.isAir() && !(blockState.getBlock() instanceof FluidBlock)
                ? blockState.getHardness(world, pos) >= 0.0F
                : false;
    }

    private boolean holdsFluid(ClientWorld world, BlockPos pos) {
        if (!world.getBlockState(pos).getFluidState().isEmpty()) {
            return true;
        } else {
            for (Direction direction : Direction.values()) {
                if (direction != Direction.DOWN
                        && !world.getFluidState(pos.offset(direction)).isEmpty()) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean cutInside(ClientWorld world, BlockPos pos) {
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    BlockPos pos2 = pos.add(i, j, k);
                    if (!this.inside(pos2)) {
                        return false;
                    }
                    if (minable(world, pos2) && this.holdsFluid(world, pos2)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean inside(BlockPos pos) {
        return this.min != null
                && pos.getX() >= this.min.getX()
                && pos.getX() <= this.max.getX()
                && pos.getY() >= this.min.getY()
                && pos.getY() <= this.max.getY()
                && pos.getZ() >= this.min.getZ()
                && pos.getZ() <= this.max.getZ();
    }

    private static Predicate<ItemStack> toolFor(
            ClientPlayerEntity player, BlockState state, boolean bl) {
        PlayerInventory inventory = player.getInventory();
        ItemStack eMPTY = ItemStack.EMPTY;
        float f = 1.0F;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            float miningSpeedMultiplier;
            if (!stack.isEmpty()
                    && isShard(stack) == bl
                    && ((miningSpeedMultiplier = stack.getMiningSpeedMultiplier(state)) > f
                            || miningSpeedMultiplier == f
                                    && !eMPTY.isEmpty()
                                    && stack.isSuitableFor(state)
                                    && !eMPTY.isSuitableFor(state))) {
                eMPTY = stack;
                f = miningSpeedMultiplier;
            }
        }
        if (eMPTY.isEmpty()) {
            return itemStack -> !isShard(itemStack);
        } else {
            ItemStack stack2 = eMPTY;
            return itemStack2 -> ItemStack.areItemsAndComponentsEqual(itemStack2, stack2);
        }
    }

    static boolean isShard(ItemStack stack) {
        return stack.isIn(ItemTags.PICKAXES) && TunnelMinerModule.shardName(stack);
    }

    private boolean mendDue(ClientPlayerEntity player) {
        if (this.mend.get() && (Hands.has(player, XP) || XP.test(player.getOffHandStack()))) {
            ItemStack mainHandStack = player.getMainHandStack();
            return mainHandStack.isDamageable() && mendable(mainHandStack)
                    ? mainHandStack.getMaxDamage() - mainHandStack.getDamage()
                            < this.mendBelow.get()
                    : false;
        } else {
            return false;
        }
    }

    private void mend(ClientPlayerEntity player) {
        ItemStack mainHandStack = player.getMainHandStack();
        boolean bl = mainHandStack.isDamageable() && mainHandStack.getDamage() == 0;
        boolean bl2 = !XP.test(player.getOffHandStack()) && !Hands.has(player, XP);
        if (bl || bl2 || this.tick - this.mendStarted > 1200L || !mainHandStack.isDamageable()) {
            this.mc.options.useKey.setPressed(false);
            this.mending = false;
            if (bl) {
                this.feedback("mended", "Tool mended", 200);
            } else if (bl2) {
                this.feedback("no-xp", "Out of XP bottles, mining on", 200);
            }
        } else if (!XP.test(player.getOffHandStack())) {
            this.mc.options.useKey.setPressed(false);
            if (this.tick % 5L == 0L && InventoryCalm.still(player)) {
                Hands.toOffhand(this.mc, player, XP);
            }
        } else {
            Aim.turn(this.mc, player.getYaw(), 80.0F, 22.0F);
            this.mc.options.useKey.setPressed(player.getPitch() > 60.0F);
        }
    }

    private static boolean mendable(ItemStack stack) {
        return stack.getEnchantments().getEnchantments().stream()
                .anyMatch(registryEntry -> registryEntry.matchesKey(Enchantments.MENDING));
    }

    private int countLeft(ClientWorld world) {
        int i = 0;
        Mutable mutable = new Mutable();
        for (int j = this.min.getY(); j <= this.max.getY(); j++) {
            for (int k = this.min.getX(); k <= this.max.getX(); k++) {
                for (int m = this.min.getZ(); m <= this.max.getZ(); m++) {
                    mutable.set(k, j, m);
                    if (world.isChunkLoaded(mutable) && minable(world, mutable)) {
                        i++;
                    }
                }
            }
        }
        return i;
    }

    private void finish() {
        this.release();
        ClientWorld world = this.mc.world;
        int i = world == null ? 0 : this.countLeft(world);
        ModuleChat.Body body =
                ModuleChat.body()
                        .good("Area cleared")
                        .text(", " + Math.max(0, this.startCount - i) + " blocks mined.");
        if (!this.unreachable.isEmpty()) {
            body.text(" ")
                    .warn(
                            this.unreachable.size()
                                    + (this.unreachable.size() == 1 ? " block" : " blocks")
                                    + " could not be reached")
                    .text(" (" + some(this.unreachable) + ").");
        }
        if (!this.holding.isEmpty()) {
            body.text(
                    " "
                            + this.holding.size()
                            + (this.holding.size() == 1 ? " block was" : " blocks were")
                            + " left to hold back lava or water ("
                            + some(this.holding)
                            + ").");
        }
        this.say(body);
        this.requestDisable();
    }

    private static String some(Set<Long> set) {
        ArrayList list = new ArrayList();
        for (long l : set) {
            if (list.size() == 4) {
                break;
            }
            list.add(BlockPos.fromLong(l).toShortString());
        }
        return String.join("; ", list) + (set.size() > 4 ? "; ..." : "");
    }

    private void render(Render3DEvent event) {
        if (this.picking != 0) {
            BlockPos pos = this.aimedBlock();
            BlockPos pos2 = this.picking == 2 ? this.firstPick : pos;
            BlockPos pos3 = pos != null ? pos : pos2;
            if (pos2 != null) {
                GizmoDrawing.box(
                        new Box(
                                Math.min(pos2.getX(), pos3.getX()),
                                Math.min(pos2.getY(), pos3.getY()),
                                Math.min(pos2.getZ(), pos3.getZ()),
                                Math.max(pos2.getX(), pos3.getX()) + 1,
                                Math.max(pos2.getY(), pos3.getY()) + 1,
                                Math.max(pos2.getZ(), pos3.getZ()) + 1),
                        DrawStyle.stroked(-11155201, 2.0F));
                if (this.picking == 2) {
                    GizmoDrawing.box(new Box(this.firstPick), DrawStyle.stroked(-20416, 2.0F));
                }
            }
        } else if (this.showArea.get() && this.min != null) {
            GizmoDrawing.box(
                    new Box(
                            this.min.getX(),
                            this.min.getY(),
                            this.min.getZ(),
                            this.max.getX() + 1,
                            this.max.getY() + 1,
                            this.max.getZ() + 1),
                    DrawStyle.stroked(-11155201, 2.0F));
            if (this.breaker != null) {
                GizmoDrawing.box(this.breaker.pos(), DrawStyle.stroked(-20416, 2.0F));
            }
        }
    }

    private void setCorner(StringSetting setting, boolean bl) {
        ClientPlayerEntity player = this.mc.player;
        if (player != null) {
            BlockPos pos = player.getBlockPos();
            if (bl) {
                HitResult crosshairTarget = this.mc.crosshairTarget;
                BlockHitResult blockHitResult;
                if (!(crosshairTarget instanceof BlockHitResult)
                        || (blockHitResult = (BlockHitResult) crosshairTarget).getType()
                                != Type.BLOCK) {
                    this.say(ModuleChat.body().warn("Look at a block first."));
                    return;
                }
                pos = blockHitResult.getBlockPos();
            }
            setting.set(pos.getX() + " " + pos.getY() + " " + pos.getZ());
            this.say(
                    ModuleChat.body()
                            .text(setting.name() + " set to " + pos.toShortString() + "."));
        }
    }

    private static BlockPos parse(String string) {
        String[] strings = string.trim().replace(",", " ").split("\\s+");
        if (strings.length != 3) {
            return null;
        } else {
            try {
                return new BlockPos(
                        Integer.parseInt(strings[0]),
                        Integer.parseInt(strings[1]),
                        Integer.parseInt(strings[2]));
            } catch (NumberFormatException exception) {
                return null;
            }
        }
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
        this.mending = false;
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
}
