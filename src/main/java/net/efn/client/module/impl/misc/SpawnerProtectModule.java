package net.efn.client.module.impl.misc;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.impl.BlockUpdateEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.misc.spawnerprotect.SpawnerAlerts;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.module.setting.StringSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class SpawnerProtectModule extends ClientModule {

    private static final int SHOP_END_CATEGORY_SLOT = 11;

    private static final int SHOP_ENDER_CHEST_SLOT = 9;

    private static final int SHOP_CONFIRM_SLOT = 25;

    private final NumberSetting targetStackCount =
            this.addSetting(
                    "stack-to-deposit",
                    new NumberSetting("Stack To Deposit", 35.0F, 1.0F, 35.0F).step(1.0F));

    private final NumberSetting scanRange =
            this.addSetting(
                    "trigger-range",
                    new NumberSetting("Trigger Range", 64.0F, 16.0F, 128.0F).step(0.1F));

    private final NumberSetting rotationSpeed =
            this.addSetting(
                    "rotation-speed",
                    new NumberSetting("Rotation Speed", 15.0F, 1.0F, 30.0F).step(0.1F));

    private final BooleanSetting detectBlockUpdates =
            this.addSetting(
                    "detect-block-updates", new BooleanSetting("Detect Block Updates", true));

    private final StringSetting whitelist =
            this.addSetting("whitelist", new StringSetting("Whitelist", ""));

    private final NumberSetting breakRange =
            this.addSetting(
                    "break-range", new NumberSetting("Break Range", 5.5F, 1.0F, 8.0F).step(0.1F));

    private final NumberSetting blockUpdateThreshold =
            this.addSetting(
                    "block-update-threshold",
                    new NumberSetting("Block Update Threshold", 5.0F, 1.0F, 20.0F).step(1.0F));

    private final NumberSetting blockUpdateWindow =
            this.addSetting(
                    "block-update-window",
                    new NumberSetting("Block Update Window", 60.0F, 1.0F, 600.0F).step(1.0F));

    private final StringSetting webhookUrl =
            this.addSetting("webhook-url", new StringSetting("Webhook URL", ""));

    private SpawnerProtectModule.State currentState =
            SpawnerProtectModule.State.WAITING_FOR_STRANGER;

    private BlockPos targetBlock;

    private BlockPos targetChest;

    private int chestTick;

    private int breakCooldown;

    private int lagWaitTicks;

    private boolean strangerDetected;

    private final Set<UUID> nearbyPlayers = new HashSet<>();

    private int blockBreakCounter;

    private long lastBreakTimestamp;

    private int shopSequence;

    private int shopTick;

    private int miningTicks;

    public SpawnerProtectModule() {
        super(
                "spawner-protect",
                "Spawner Protect",
                "Protects spawners on block updates; player arrivals only send webhook"
                        + " notifications.",
                ModuleCategory.DONUT);
        this.whitelist.privateValue();
        this.webhookUrl.privateValue();
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    public int settingsVersion() {
        return 1;
    }

    @Override
    public void migrateSettings(int i) {
        if (i < 1 && this.targetStackCount.get() == 3.0F) {
            this.targetStackCount.resetToDefault();
        }
    }

    @Override
    protected void onEnable() {
        this.on(TickEvent.TYPE, this::onTick);
        this.on(BlockUpdateEvent.TYPE, this::onBlockUpdate);
        this.resetModule();
    }

    @Override
    protected void onDisable() {
        this.stopMovement();
        this.updateSneak(false);
    }

    private void resetModule() {
        this.currentState = SpawnerProtectModule.State.WAITING_FOR_STRANGER;
        this.strangerDetected = false;
        this.nearbyPlayers.clear();
        this.lagWaitTicks = 0;
        this.blockBreakCounter = 0;
        this.lastBreakTimestamp = 0L;
        this.stopMovement();
        this.updateSneak(false);
        this.shopSequence = 0;
        this.shopTick = 0;
        this.resetState();
    }

    private void resetState() {
        this.targetChest = null;
        this.targetBlock = null;
        this.chestTick = 0;
        this.breakCooldown = 0;
        this.miningTicks = 0;
    }

    private void onBlockUpdate(BlockUpdateEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!this.strangerDetected
                && client.player != null
                && client.world != null
                && !this.isNearSpawn()
                && this.detectBlockUpdates.get()) {
            if (!event.oldState.isAir()
                    && !(event.oldState.getBlock() instanceof ShulkerBoxBlock)) {
                this.checkBlockUpdate(event.pos, event.newState);
            }
        }
    }

    private void checkBlockUpdate(BlockPos pos, BlockState state) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!this.strangerDetected && client.world != null) {
            for (PlayerEntity player : client.world.getPlayers()) {
                if (player.getEyePos().distanceTo(Vec3d.ofCenter(pos)) < 8.0
                        && this.isWhitelisted(player.getName().getString())) {
                    return;
                }
            }
            if (pos.getY() > -64 && state.isAir()) {
                if (!(client.player.getEyePos().distanceTo(Vec3d.ofCenter(pos)) < 6.0)) {
                    double d = client.player.getX() - pos.getX();
                    double e;
                    double d2 = Math.sqrt(d * d + (e = client.player.getZ() - pos.getZ()) * e);
                    if (!(d2 > this.scanRange.get())) {
                        long l = System.currentTimeMillis();
                        if (l - this.lastBreakTimestamp > this.blockUpdateWindow.getInt() * 1000L) {
                            this.blockBreakCounter = 0;
                        }
                        this.blockBreakCounter++;
                        this.lastBreakTimestamp = l;
                        if (this.blockBreakCounter >= this.blockUpdateThreshold.getInt()) {
                            this.strangerDetected = true;
                            this.currentState = SpawnerProtectModule.State.WORKING;
                            ModuleChat.send(
                                    this,
                                    "\ud83d\udea8 UZAK TESP\u0130T: Yak\u0131nda bir blok"
                                            + " k\u0131r\u0131ld\u0131! (Yatay Mesafe: "
                                            + (int) d2
                                            + "m, Y: "
                                            + pos.getY()
                                            + ")");
                            this.sendWebhook(
                                    "\ud83d\udea8 Protection activated",
                                    "The block-break threshold was reached. Spawner collection has"
                                            + " started.",
                                    15548997,
                                    SpawnerAlerts.field(
                                            "\ud83d\udce1 Trigger", "Block breaking", true),
                                    SpawnerAlerts.field(
                                            "\ud83e\uddf1 Block counter",
                                            this.blockBreakCounter
                                                    + " / "
                                                    + this.blockUpdateThreshold.getInt(),
                                            true),
                                    SpawnerAlerts.field(
                                            "\ud83d\udccf Horizontal distance",
                                            (int) d2 + " blocks",
                                            true),
                                    SpawnerAlerts.field(
                                            "\ud83d\udccd Block location",
                                            coordinates(pos),
                                            false));
                        }
                    }
                }
            }
        }
    }

    private void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null
                || client.world == null
                || client.interactionManager == null
                || client.currentScreen instanceof DisconnectedScreen) {
            this.resetModule();
        } else if (this.isNearSpawn()) {
            if (this.strangerDetected) {
                this.resetModule();
            }
        } else {
            this.notifyNearbyPlayers();
            if (this.currentState != SpawnerProtectModule.State.WAITING_FOR_STRANGER) {
                if (this.currentState != SpawnerProtectModule.State.OPENING_CHEST
                        && this.currentState != SpawnerProtectModule.State.DEPOSITING_ITEMS
                        && this.currentState != SpawnerProtectModule.State.BUYING_ECHEST) {
                    this.updateSneak(true);
                }
                this.handleRotation();
                switch (this.currentState) {
                    case WORKING:
                        this.handleWorking();
                        break;
                    case GOING_TO_CHEST:
                        this.handleGoingToChest();
                        break;
                    case OPENING_CHEST:
                        this.handleOpeningChest();
                        break;
                    case DEPOSITING_ITEMS:
                        this.handleDepositing();
                        break;
                    case FINAL_EXIT:
                        this.handleFinalExit();
                        break;
                    case BUYING_ECHEST:
                        this.handleBuyingEChest();
                        break;
                    case PLACING_ECHEST:
                        this.handlePlacingEChest();
                }
            }
        }
    }

    private void notifyNearbyPlayers() {
        MinecraftClient client = MinecraftClient.getInstance();
        HashSet set = new HashSet();
        for (PlayerEntity player : client.world.getPlayers()) {
            if (player != client.player
                    && !player.isSpectator()
                    && !this.isWhitelisted(player.getName().getString())
                    && !(client.player.distanceTo(player) > this.scanRange.get())) {
                set.add(player.getUuid());
                if (!this.nearbyPlayers.contains(player.getUuid())) {
                    this.sendWebhook(
                            "\ud83d\udc64 A player entered your range",
                            "A new player is nearby. This event only sends a notification.",
                            5793266,
                            SpawnerAlerts.field(
                                    "Player", "`" + player.getName().getString() + "`", true),
                            SpawnerAlerts.field(
                                    "\ud83d\udccf Distance",
                                    String.format(
                                            Locale.ROOT,
                                            "%.1f blocks",
                                            client.player.distanceTo(player)),
                                    true),
                            SpawnerAlerts.field(
                                    "\ud83d\udee1\ufe0f Protection",
                                    this.strangerDetected ? "In progress" : "Standby",
                                    true),
                            SpawnerAlerts.field(
                                    "\ud83d\udccd Player location",
                                    coordinates(player.getBlockPos()),
                                    false));
                }
            }
        }
        this.nearbyPlayers.clear();
        this.nearbyPlayers.addAll(set);
    }

    private void handleRotation() {
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d vec = null;
        if (this.currentState == SpawnerProtectModule.State.WORKING) {
            ItemEntity droppedSpawner = this.findDroppedSpawner();
            if (droppedSpawner != null) {
                vec = droppedSpawner.getEntityPos();
            } else {
                this.retargetSpawner(client);
                if (this.targetBlock != null) {
                    vec = Vec3d.ofCenter(this.targetBlock);
                }
            }
        } else if ((this.currentState == SpawnerProtectModule.State.GOING_TO_CHEST
                        || this.currentState == SpawnerProtectModule.State.OPENING_CHEST)
                && this.targetChest != null) {
            vec = Vec3d.ofCenter(this.targetChest);
        }
        if (vec != null) {
            this.smoothLook(client.player, vec);
        }
    }

    private void retargetSpawner(MinecraftClient client) {
        if (this.targetBlock == null
                || client.world.getBlockState(this.targetBlock).getBlock() != Blocks.SPAWNER
                || client.player.getBlockPos().getSquaredDistance(this.targetBlock) > 256.0) {
            this.targetBlock = this.findRandomBlock(Blocks.SPAWNER, 16);
            this.miningTicks = 0;
        }
    }

    private void smoothLook(ClientPlayerEntity player, Vec3d vec) {
        Vec3d eyePos = player.getEyePos();
        double d = vec.x - eyePos.x;
        double e = vec.y - eyePos.y;
        double d2 = vec.z - eyePos.z;
        double e2 = Math.sqrt(d * d + d2 * d2);
        float f = (float) Math.toDegrees(Math.atan2(-d, d2));
        float g = (float) (-Math.toDegrees(Math.atan2(e, e2)));
        float rotationSpeed2 = this.rotationSpeed.get();
        player.setYaw(
                player.getYaw()
                        + MathHelper.clamp(
                                MathHelper.wrapDegrees(f - player.getYaw()),
                                -rotationSpeed2,
                                rotationSpeed2));
        player.setPitch(
                player.getPitch()
                        + MathHelper.clamp(
                                MathHelper.wrapDegrees(g - player.getPitch()),
                                -rotationSpeed2,
                                rotationSpeed2));
    }

    private void handleWorking() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player.currentScreenHandler != player.playerScreenHandler) {
            player.closeHandledScreen();
        }
        if (this.findDroppedSpawner() != null) {
            this.lagWaitTicks = 0;
            this.stopBreaking();
            client.options.forwardKey.setPressed(true);
        } else {
            int spawnerCount = this.getSpawnerCount();
            if (spawnerCount < this.targetStackCount.getInt() * 64
                    && (spawnerCount <= 0 || player.getInventory().getEmptySlot() != -1)) {
                this.retargetSpawner(client);
                if (this.targetBlock == null) {
                    this.stopMovement();
                    if (this.lagWaitTicks < 40) {
                        this.lagWaitTicks++;
                    } else if (this.getSpawnerCount() > 0) {
                        this.goToChest();
                    } else {
                        this.currentState = SpawnerProtectModule.State.FINAL_EXIT;
                        this.lagWaitTicks = 0;
                    }
                } else {
                    this.lagWaitTicks = 0;
                    if (player.getEyePos().distanceTo(Vec3d.ofCenter(this.targetBlock))
                            > this.breakRange.get()) {
                        this.stopBreaking();
                        client.options.forwardKey.setPressed(true);
                    } else {
                        client.options.forwardKey.setPressed(false);
                        this.miningTicks++;
                        if (this.miningTicks > 60) {
                            client.interactionManager.interactBlock(
                                    player,
                                    Hand.MAIN_HAND,
                                    new BlockHitResult(
                                            Vec3d.ofCenter(this.targetBlock),
                                            Direction.UP,
                                            this.targetBlock,
                                            false));
                            player.swingHand(Hand.MAIN_HAND);
                            this.miningTicks = 0;
                        }
                        if (this.breakCooldown <= 0) {
                            client.interactionManager.updateBlockBreakingProgress(
                                    this.targetBlock, Direction.UP);
                            player.swingHand(Hand.MAIN_HAND);
                            client.options.attackKey.setPressed(true);
                            this.breakCooldown = 6;
                        } else {
                            this.breakCooldown--;
                        }
                    }
                }
            } else {
                this.goToChest();
            }
        }
    }

    private void goToChest() {
        this.stopMovement();
        this.targetChest = this.findNearestBlock(Blocks.ENDER_CHEST, 4);
        if (this.targetChest != null) {
            this.currentState = SpawnerProtectModule.State.GOING_TO_CHEST;
        } else if (this.getEnderChestCount() > 0) {
            this.currentState = SpawnerProtectModule.State.PLACING_ECHEST;
            this.shopTick = 0;
        } else {
            this.currentState = SpawnerProtectModule.State.BUYING_ECHEST;
            this.shopSequence = 0;
            this.shopTick = 0;
        }
    }

    private void handleGoingToChest() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (this.targetChest == null) {
            this.targetChest = this.findNearestBlock(Blocks.ENDER_CHEST, 4);
        }
        if (this.targetChest == null) {
            this.currentState = SpawnerProtectModule.State.WORKING;
        } else {
            client.options.forwardKey.setPressed(true);
            if (client.player.getBlockPos().isWithinDistance(this.targetChest, 4.0)) {
                this.stopMovement();
                this.currentState = SpawnerProtectModule.State.OPENING_CHEST;
            }
        }
    }

    private void handleOpeningChest() {
        MinecraftClient client = MinecraftClient.getInstance();
        this.updateSneak(false);
        if (this.chestTick > 200) {
            ModuleChat.send(
                    this,
                    "Ender chest a\u00e7\u0131lamad\u0131; e\u015fyalar envanterde tutuluyor.");
            this.currentState = SpawnerProtectModule.State.FINAL_EXIT;
            this.lagWaitTicks = 0;
        } else {
            if (this.chestTick % 12 == 0 && this.targetChest != null) {
                client.interactionManager.interactBlock(
                        client.player,
                        Hand.MAIN_HAND,
                        new BlockHitResult(
                                Vec3d.ofCenter(this.targetChest),
                                Direction.UP,
                                this.targetChest,
                                false));
            }
            this.chestTick++;
            if (client.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
                this.chestTick = 0;
                this.currentState = SpawnerProtectModule.State.DEPOSITING_ITEMS;
                this.lagWaitTicks = 0;
            }
        }
    }

    private void handleBuyingEChest() {
        MinecraftClient client = MinecraftClient.getInstance();
        this.stopMovement();
        this.updateSneak(false);
        this.shopTick++;
        if (this.shopTick > 200) {
            ModuleChat.send(
                    this, "Market zaman a\u015f\u0131m\u0131; e\u015fyalar envanterde tutuluyor.");
            this.currentState = SpawnerProtectModule.State.FINAL_EXIT;
            this.lagWaitTicks = 0;
        } else if (this.shopSequence > 0 && client.currentScreen == null && this.shopTick > 60) {
            ModuleChat.send(this, "Market ekran\u0131 kapand\u0131, ba\u015ftan deneniyor...");
            this.shopSequence = 0;
            this.shopTick = 0;
        } else if (this.shopTick >= 30) {
            switch (this.shopSequence) {
                case 0:
                    ModuleChat.send(this, "Market a\u00e7\u0131l\u0131yor: /shop");
                    client.getNetworkHandler().sendChatCommand("shop");
                    this.shopSequence = 1;
                    this.shopTick = 0;
                    break;
                case 1:
                    this.shopStep(client, "SHOP", "Market: End kategorisi se\u00e7iliyor...", 11);
                    break;
                case 2:
                    this.shopStep(client, "END", "Market: Ender Chest se\u00e7iliyor...", 9);
                    break;
                case 3:
                    this.shopStep(
                            client,
                            "ENDER CHEST",
                            "Market: Sat\u0131n alma onaylan\u0131yor...",
                            25);
                    break;
                case 4:
                    if (this.getEnderChestCount() > 0) {
                        ModuleChat.send(this, "Ender Chest sat\u0131n al\u0131nd\u0131.");
                        client.player.closeHandledScreen();
                        this.currentState = SpawnerProtectModule.State.PLACING_ECHEST;
                        this.shopTick = 0;
                    } else if (this.shopTick > 100) {
                        ModuleChat.send(
                                this,
                                "Sat\u0131n alma ba\u015far\u0131s\u0131z oldu (Zaman"
                                        + " a\u015f\u0131m\u0131).");
                        client.player.closeHandledScreen();
                        this.shopSequence = 0;
                        this.shopTick = 0;
                    }
            }
        }
    }

    private void shopStep(MinecraftClient client, String string, String string2, int i) {
        if (client.currentScreen != null
                && client.currentScreen
                        .getTitle()
                        .getString()
                        .toUpperCase(Locale.ROOT)
                        .contains(string)) {
            ModuleChat.send(this, string2);
            client.interactionManager.clickSlot(
                    client.player.currentScreenHandler.syncId,
                    i,
                    0,
                    SlotActionType.PICKUP,
                    client.player);
            this.shopSequence++;
            this.shopTick = 0;
        }
    }

    private void handlePlacingEChest() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        this.shopTick++;
        if (this.shopTick >= 10) {
            int i = -1;
            for (int j = 0; j < 9; j++) {
                if (player.getInventory().getStack(j).getItem() == Blocks.ENDER_CHEST.asItem()) {
                    i = j;
                    break;
                }
            }
            if (i == -1) {
                for (int k = 9; k < 36; k++) {
                    if (player.getInventory().getStack(k).getItem()
                            == Blocks.ENDER_CHEST.asItem()) {
                        player.closeHandledScreen();
                        client.interactionManager.clickSlot(
                                player.playerScreenHandler.syncId,
                                k,
                                0,
                                SlotActionType.SWAP,
                                player);
                        this.shopTick = 0;
                        return;
                    }
                }
                this.currentState = SpawnerProtectModule.State.BUYING_ECHEST;
                this.shopSequence = 0;
            } else {
                player.getInventory().setSelectedSlot(i);
                BlockPos pos = player.getBlockPos();
                BlockPos pos2 = null;
                for (Direction direction : Direction.values()) {
                    BlockPos pos3;
                    if (direction != Direction.UP
                            && direction != Direction.DOWN
                            && client.world
                                    .getBlockState(pos3 = pos.offset(direction))
                                    .isReplaceable()
                            && !client.world.getBlockState(pos3.down()).isAir()) {
                        pos2 = pos3;
                        break;
                    }
                }
                if (pos2 == null) {
                    ModuleChat.send(this, "E-Chest koyacak yer bulunamad\u0131!");
                    this.currentState = SpawnerProtectModule.State.FINAL_EXIT;
                    this.lagWaitTicks = 0;
                } else {
                    BlockPos pos4 = pos2.down();
                    client.interactionManager.interactBlock(
                            player,
                            Hand.MAIN_HAND,
                            new BlockHitResult(
                                    Vec3d.ofCenter(pos4).add(0.0, 0.5, 0.0),
                                    Direction.UP,
                                    pos4,
                                    false));
                    player.swingHand(Hand.MAIN_HAND);
                    this.targetChest = pos2;
                    this.currentState = SpawnerProtectModule.State.GOING_TO_CHEST;
                }
            }
        }
    }

    private int getEnderChestCount() {
        return countInInventory(Blocks.ENDER_CHEST);
    }

    private void handleDepositing() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (this.lagWaitTicks < 15) {
            this.lagWaitTicks++;
        } else if (!(client.player.currentScreenHandler
                instanceof GenericContainerScreenHandler genericContainerScreenHandler)) {
            this.currentState = SpawnerProtectModule.State.OPENING_CHEST;
            this.chestTick = 0;
        } else {
            int i = genericContainerScreenHandler.slots.size() - 36;
            boolean bl = false;
            for (int j = 0; j < i; j++) {
                ItemStack stack = genericContainerScreenHandler.getSlot(j).getStack();
                if (stack.isEmpty()
                        || stack.getItem() == Blocks.SPAWNER.asItem()
                                && stack.getCount() < stack.getMaxCount()) {
                    bl = true;
                    break;
                }
            }
            boolean bl2 = false;
            for (int k = 0; k < 36; k++) {
                int m = i + k;
                if (genericContainerScreenHandler.getSlot(m).getStack().getItem()
                        == Blocks.SPAWNER.asItem()) {
                    if (!bl) {
                        this.currentState = SpawnerProtectModule.State.FINAL_EXIT;
                        this.lagWaitTicks = 0;
                        return;
                    }
                    client.interactionManager.clickSlot(
                            genericContainerScreenHandler.syncId,
                            m,
                            0,
                            SlotActionType.QUICK_MOVE,
                            client.player);
                    bl2 = true;
                    break;
                }
            }
            if (!bl2) {
                client.player.closeHandledScreen();
                if (this.findNearestBlock(Blocks.SPAWNER, 16) != null) {
                    this.currentState = SpawnerProtectModule.State.WORKING;
                } else {
                    this.currentState = SpawnerProtectModule.State.FINAL_EXIT;
                    this.lagWaitTicks = 0;
                }
            }
        }
    }

    private void handleFinalExit() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (this.lagWaitTicks == 0) {
            int spawnerCount = this.getSpawnerCount();
            boolean bl = spawnerCount > 0;
            this.sendWebhook(
                    bl ? "\u26a0\ufe0f Spawners remain in inventory" : "\u2705 Task completed",
                    bl
                            ? "Some spawners are still in your inventory. Disconnecting in two"
                                    + " seconds."
                            : "The collection sequence has ended. Disconnecting in two seconds.",
                    bl ? 16705372 : 5763719,
                    SpawnerAlerts.field(
                            "\ud83c\udf92 Remaining in inventory",
                            spawnerCount + " spawners",
                            true),
                    SpawnerAlerts.field("\ud83d\udeaa Next step", "Disconnect from server", true));
            ModuleChat.send(
                    this,
                    "G\u00f6rev bitti, e\u015fyalar\u0131n sunucuya kaydedilmesi i\u00e7in 2 saniye"
                            + " beklenip \u00e7\u0131k\u0131lacak...");
        }
        this.stopMovement();
        this.updateSneak(false);
        this.lagWaitTicks++;
        if (this.lagWaitTicks > 40) {
            if (client.getNetworkHandler() != null
                    && client.getNetworkHandler().getConnection().isOpen()) {
                AutoReconnectModule.holdOff();
                client.getNetworkHandler()
                        .getConnection()
                        .disconnect(
                                Text.literal(
                                        "[SpawnerProtect] G\u00f6rev tamamland\u0131, g\u00fcvenli"
                                                + " \u00e7\u0131k\u0131\u015f yap\u0131ld\u0131."));
            }
            this.resetModule();
            this.requestDisable();
        }
    }

    @SafeVarargs
    private void sendWebhook(String string, String string2, int i, Map<String, Object>... map) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!this.webhookUrl.get().isBlank() && client.player != null && client.world != null) {
            ArrayList list = new ArrayList<>(Arrays.asList(map));
            list.add(
                    SpawnerAlerts.field(
                            "\ud83d\udc64 Account",
                            "`" + client.player.getName().getString() + "`",
                            true));
            list.add(
                    SpawnerAlerts.field(
                            "\ud83c\udf0d Dimension",
                            "`" + client.world.getRegistryKey().getValue() + "`",
                            true));
            list.add(
                    SpawnerAlerts.field(
                            "\ud83d\udccd Your location",
                            coordinates(client.player.getBlockPos()),
                            false));
            SpawnerAlerts.send(
                    this.webhookUrl.get(),
                    SpawnerAlerts.payload(string, string2, i, list, Instant.now()));
        }
    }

    private static String coordinates(BlockPos pos) {
        return "`X: " + pos.getX() + "  Y: " + pos.getY() + "  Z: " + pos.getZ() + "`";
    }

    private void updateSneak(boolean pressed) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.setSneaking(pressed);
        }
        client.options.sneakKey.setPressed(pressed);
    }

    private boolean isNearSpawn() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return player != null && Math.abs(player.getX()) < 100.0 && Math.abs(player.getZ()) < 100.0;
    }

    private boolean isWhitelisted(String string) {
        return Arrays.stream(this.whitelist.get().split("[,;\\s]+"))
                .anyMatch(value -> value.equalsIgnoreCase(string));
    }

    private BlockPos findNearestBlock(Block block, int i) {
        MinecraftClient client = MinecraftClient.getInstance();
        BlockPos pos = client.player.getBlockPos();
        BlockPos pos2 = null;
        double d = 1.7976931348623157E308;
        for (int j = -i; j <= i; j++) {
            for (int k = -i; k <= i; k++) {
                for (int m = -i; m <= i; m++) {
                    BlockPos pos3 = pos.add(j, k, m);
                    double squaredDistance;
                    if (client.world.getBlockState(pos3).getBlock() == block
                            && (squaredDistance = pos.getSquaredDistance(pos3)) < d) {
                        d = squaredDistance;
                        pos2 = pos3;
                    }
                }
            }
        }
        return pos2;
    }

    private BlockPos findRandomBlock(Block block, int i) {
        MinecraftClient client = MinecraftClient.getInstance();
        BlockPos pos = client.player.getBlockPos();
        ArrayList list = new ArrayList();
        double d = i * i;
        for (int j = -i; j <= i; j++) {
            for (int k = -i; k <= i; k++) {
                for (int m = -i; m <= i; m++) {
                    BlockPos pos2 = pos.add(j, k, m);
                    if (pos.getSquaredDistance(pos2) <= d
                            && client.world.getBlockState(pos2).getBlock() == block) {
                        list.add(pos2);
                    }
                }
            }
        }
        return list.isEmpty() ? null : (BlockPos) list.get(new Random().nextInt(list.size()));
    }

    private int getSpawnerCount() {
        return countInInventory(Blocks.SPAWNER);
    }

    private static int countInInventory(Block block) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        int i = 0;
        for (int j = 0; j < 36; j++) {
            ItemStack stack = player.getInventory().getStack(j);
            if (stack.getItem() == block.asItem()) {
                i += stack.getCount();
            }
        }
        return i;
    }

    private ItemEntity findDroppedSpawner() {
        MinecraftClient client = MinecraftClient.getInstance();
        ItemEntity itemEntity = null;
        float f = 3.4028235E38F;
        for (Entity entity : client.world.getEntities()) {
            float distance;
            ItemEntity itemEntity2;
            if (entity instanceof ItemEntity
                    && (itemEntity2 = (ItemEntity) entity).getStack().getItem()
                            == Blocks.SPAWNER.asItem()
                    && !((distance = client.player.distanceTo(itemEntity2)) >= 16.0F)
                    && !itemEntity2.cannotPickup()
                    && distance < f) {
                f = distance;
                itemEntity = itemEntity2;
            }
        }
        return itemEntity;
    }

    private void stopBreaking() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.options.attackKey.setPressed(false);
        if (client.interactionManager != null) {
            client.interactionManager.cancelBlockBreaking();
        }
        this.breakCooldown = 0;
    }

    private void stopMovement() {
        this.stopBreaking();
        MinecraftClient.getInstance().options.forwardKey.setPressed(false);
    }

    @Environment(EnvType.CLIENT)
    private static enum State {
        WAITING_FOR_STRANGER,
        WORKING,
        GOING_TO_CHEST,
        OPENING_CHEST,
        DEPOSITING_ITEMS,
        FINAL_EXIT,
        BUYING_ECHEST,
        PLACING_ECHEST;

        private State() {}
    }
}
