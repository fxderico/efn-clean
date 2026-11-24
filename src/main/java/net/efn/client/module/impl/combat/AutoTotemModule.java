package net.efn.client.module.impl.combat;

import net.efn.client.event.EventHandler;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.PacketReceiveNettyEvent;
import net.efn.client.event.impl.Render2DEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.combat.totem.OffhandRefill;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.NumberSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.SetPlayerInventoryS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;

@Environment(EnvType.CLIENT)
public final class AutoTotemModule extends ClientModule {

    private static final int OFFHAND_BUTTON = 40;

    private static final int OFFHAND_MENU_SLOT = 45;

    private static final int TOTEM_POP_EVENT = 35;

    private static final int NEAREST_CHOICES = 3;

    private static AutoTotemModule instance;

    private final NumberSetting refillDelay =
            this.addSetting(
                    "refill-delay",
                    new NumberSetting("Refill Delay", 3.0F, 1.0F, 10.0F).step(1.0F));

    private final BooleanSetting useHotbar =
            this.addSetting("use-hotbar-totems", new BooleanSetting("Use Hotbar Totems", false));

    private final OffhandRefill refill =
            new OffhandRefill(
                    new OffhandRefill.Driver() {

                        @Override
                        public boolean needsTotem() {
                            return AutoTotemModule.player().getOffHandStack().isEmpty();
                        }

                        @Override
                        public boolean canInteract() {
                            MinecraftClient client = MinecraftClient.getInstance();
                            ClientPlayerEntity player = client.player;
                            return client.interactionManager.getCurrentGameMode()
                                            == GameMode.SURVIVAL
                                    && player.currentScreenHandler == player.playerScreenHandler
                                    && player.playerScreenHandler.getCursorStack().isEmpty();
                        }

                        @Override
                        public int findSource() {
                            return AutoTotemModule.this.findSource(
                                    AutoTotemModule.player().getInventory());
                        }

                        @Override
                        public boolean validSource(int i) {
                            ClientPlayerEntity player2 = AutoTotemModule.player();
                            if (!AutoTotemModule.isSource(
                                    player2.getInventory(),
                                    i,
                                    AutoTotemModule.this.useHotbar.get())) {
                                return false;
                            } else {
                                Slot slot =
                                        player2.playerScreenHandler.getSlot(
                                                AutoTotemModule.menuSlot(i));
                                return slot.inventory == player2.getInventory()
                                        && slot.getIndex() == i
                                        && slot.canTakeItems(player2);
                            }
                        }

                        @Override
                        public void swap(int i) {
                            MinecraftClient client2 = MinecraftClient.getInstance();
                            ClientPlayerEntity player3 = client2.player;
                            client2.interactionManager.clickSlot(
                                    player3.playerScreenHandler.syncId,
                                    AutoTotemModule.menuSlot(i),
                                    40,
                                    SlotActionType.SWAP,
                                    player3);
                            AutoTotemModule.this.previousSource = i;
                        }

                        @Override
                        public int containerId() {
                            return AutoTotemModule.player().playerScreenHandler.syncId;
                        }

                        @Override
                        public int stateId() {
                            return AutoTotemModule.player().playerScreenHandler.getRevision();
                        }

                        @Override
                        public int latencyMillis() {
                            ClientPlayNetworkHandler networkHandler =
                                    MinecraftClient.getInstance().getNetworkHandler();
                            PlayerListEntry listEntry =
                                    networkHandler == null
                                            ? null
                                            : networkHandler.getPlayerListEntry(
                                                    AutoTotemModule.player().getUuid());
                            return listEntry == null ? 0 : listEntry.getLatency();
                        }
                    },
                    ThreadLocalRandom.current());

    private int previousSource = -1;

    public AutoTotemModule() {
        super(
                "autototem",
                "Auto Totem",
                "Refills an empty offhand with a totem from your inventory, at a human pace you"
                        + " choose with Refill Delay.",
                ModuleCategory.COMBAT);
        instance = this;
    }

    public static boolean wantsHotbar() {
        return false;
    }

    public static boolean busy() {
        AutoTotemModule module = instance;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return module != null
                        && module.isEnabled()
                        && player != null
                        && !player.isSpectator()
                        && !player.isInCreativeMode()
                ? module.refill.busy()
                        || player.getOffHandStack().isEmpty()
                                && module.findSource(player.getInventory()) >= 0
                : false;
    }

    @Override
    protected void onEnable() {
        AutoInvTotemModule module = ModuleRegistry.get(AutoInvTotemModule.class);
        if (module != null) {
            module.setEnabled(false);
        }
        this.reset();
    }

    @Override
    protected void onDisable() {
        this.reset();
    }

    @EventHandler(stage = Stage.POST)
    public void onTick(TickEvent event) {
        this.update();
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        this.update();
    }

    @EventHandler
    public void onPacketApplied(PacketAppliedEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.player != null) {
            Packet packet = event.packet;
            EntityStatusS2CPacket entityStatusPacket;
            if (packet instanceof EntityStatusS2CPacket
                    && (entityStatusPacket = (EntityStatusS2CPacket) packet).getStatus() == 35
                    && entityStatusPacket.getEntity(client.world) == client.player) {
                this.reset();
            }
        }
    }

    @EventHandler
    public void onPacketReceiveNetty(PacketReceiveNettyEvent event) {
        long l = System.nanoTime();
        if (event.packet instanceof SetPlayerInventoryS2CPacket setPlayerInventoryPacket) {
            if (setPlayerInventoryPacket.slot() == 40) {
                this.observe(-1, -1, offhand(setPlayerInventoryPacket.contents()), l);
            }
        } else if (event.packet
                instanceof ScreenHandlerSlotUpdateS2CPacket screenHandlerSlotUpdatePacket) {
            if (screenHandlerSlotUpdatePacket.getSlot() == 45) {
                this.observe(
                        screenHandlerSlotUpdatePacket.getSyncId(),
                        screenHandlerSlotUpdatePacket.getRevision(),
                        offhand(screenHandlerSlotUpdatePacket.getStack()),
                        l);
            }
        } else {
            Packet packet = event.packet;
            InventoryS2CPacket inventoryPacket;
            if (packet instanceof InventoryS2CPacket
                    && (inventoryPacket = (InventoryS2CPacket) packet).contents().size() > 45) {
                this.observe(
                        inventoryPacket.syncId(),
                        inventoryPacket.revision(),
                        offhand(inventoryPacket.contents().get(45)),
                        l);
            }
        }
    }

    private void observe(int i, int j, OffhandRefill.Offhand offhand, long l) {
        MinecraftClient.getInstance()
                .execute(
                        () -> {
                            if (this.isEnabled()) {
                                this.refill.observe(i, j, offhand, l);
                            }
                        });
    }

    private void update() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (client.world != null
                && player != null
                && client.interactionManager != null
                && player.isAlive()
                && !player.isRemoved()
                && !(player.getHealth() <= 0.0F)) {
            this.refill.step(System.nanoTime(), this.refillDelay.getInt());
        } else {
            this.reset();
        }
    }

    private void reset() {
        this.refill.reset();
        this.previousSource = -1;
    }

    private int findSource(PlayerInventory playerInventory) {
        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        if (this.useHotbar.get()) {
            ArrayList list = new ArrayList();
            for (int i = 0; i < 9; i++) {
                if (isSource(playerInventory, i, true)) {
                    list.add(i);
                }
            }
            if (!list.isEmpty()) {
                return (Integer) list.get(threadLocalRandom.nextInt(list.size()));
            }
        }
        ArrayList list2 = new ArrayList();
        for (int j = 9; j < 36; j++) {
            if (isSource(playerInventory, j, false)) {
                list2.add(j);
            }
        }
        if (list2.isEmpty()) {
            return -1;
        } else {
            int k =
                    this.previousSource >= 9 && this.previousSource < 36
                            ? this.previousSource - 9
                            : threadLocalRandom.nextInt(27);
            list2.sort(
                    Comparator.comparingInt(
                            (Integer integer) -> {
                                int m = integer - 9;
                                return Math.abs(m % 9 - k % 9) + Math.abs(m / 9 - k / 9);
                            }));
            return (Integer) list2.get(threadLocalRandom.nextInt(Math.min(3, list2.size())));
        }
    }

    private static boolean isSource(PlayerInventory playerInventory, int i, boolean bl) {
        return i >= (bl ? 0 : 9)
                && i < 36
                && playerInventory.getStack(i).isOf(Items.TOTEM_OF_UNDYING);
    }

    private static int menuSlot(int i) {
        return i < 9 ? i + 36 : i;
    }

    private static OffhandRefill.Offhand offhand(ItemStack stack) {
        if (stack.isEmpty()) {
            return OffhandRefill.Offhand.EMPTY;
        } else {
            return stack.isOf(Items.TOTEM_OF_UNDYING)
                    ? OffhandRefill.Offhand.TOTEM
                    : OffhandRefill.Offhand.OTHER;
        }
    }

    private static ClientPlayerEntity player() {
        return MinecraftClient.getInstance().player;
    }
}
