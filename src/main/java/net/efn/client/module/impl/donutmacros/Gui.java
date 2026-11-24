package net.efn.client.module.impl.donutmacros;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
final class Gui {

    private static final int COMMAND_GAP_MS = 200;

    final MacroWorker worker;

    Gui(MacroWorker worker2) {
        this.worker = worker2;
    }

    static MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }

    static ClientPlayerEntity player() {
        ClientPlayerEntity player = mc().player;
        if (player == null) {
            throw new IllegalStateException("Not in a world");
        } else {
            return player;
        }
    }

    String title() {
        return this.worker.call(
                () ->
                        mc().currentScreen == null
                                ? ""
                                : ServerText.normalize(mc().currentScreen.getTitle()));
    }

    static boolean containerOpenNow() {
        ClientPlayerEntity player = mc().player;
        return player != null
                && player.currentScreenHandler != player.playerScreenHandler
                && mc().currentScreen instanceof HandledScreen;
    }

    static List<ItemStack> containerNow() {
        ArrayList list = new ArrayList();
        ClientPlayerEntity player = mc().player;
        if (player != null && player.currentScreenHandler != player.playerScreenHandler) {
            for (Slot slot : player.currentScreenHandler.slots) {
                if (slot.inventory instanceof PlayerInventory) {
                    break;
                }
                list.add(slot.getStack().copy());
            }
            return list;
        } else {
            return list;
        }
    }

    List<ItemStack> container() {
        return this.worker.call(Gui::containerNow);
    }

    static String fingerprintNow() {
        StringBuilder sb = new StringBuilder();
        ClientPlayerEntity player = mc().player;
        if (player != null) {
            sb.append(player.currentScreenHandler.syncId).append('#');
        }
        for (ItemStack stack : containerNow()) {
            sb.append(Registries.ITEM.getId(stack.getItem()))
                    .append(':')
                    .append(stack.getCount())
                    .append(':')
                    .append(stack.getName().getString())
                    .append('|');
        }
        return sb.toString();
    }

    String fingerprint() {
        return this.worker.call(Gui::fingerprintNow);
    }

    static ItemStack slotNow(int i) {
        List list = containerNow();
        return i >= 0 && i < list.size() ? (ItemStack) list.get(i) : ItemStack.EMPTY;
    }

    static int findNow(Predicate<ItemStack> predicate, int i) {
        List list = containerNow();
        for (int j = 0; j < list.size() && j < i; j++) {
            if (!((ItemStack) list.get(j)).isEmpty() && predicate.test((ItemStack) list.get(j))) {
                return j;
            }
        }
        return -1;
    }

    static String nameOf(ItemStack stack) {
        return ServerText.normalize(stack.getName());
    }

    void click(int i, int j, SlotActionType slotActionType) {
        this.worker.run(
                () -> {
                    ClientPlayerEntity player = player();
                    ScreenHandler currentScreenHandler = player.currentScreenHandler;
                    mc().interactionManager
                            .clickSlot(currentScreenHandler.syncId, i, j, slotActionType, player);
                });
    }

    void clickInventory(int i, int j, SlotActionType slotActionType) {
        this.worker.run(
                () -> {
                    ClientPlayerEntity player = player();
                    mc().interactionManager
                            .clickSlot(
                                    player.playerScreenHandler.syncId,
                                    i,
                                    j,
                                    slotActionType,
                                    player);
                });
    }

    void closeInventory() {
        this.worker.run(
                () -> {
                    ClientPlayerEntity player = player();
                    if (player.currentScreenHandler == player.playerScreenHandler) {
                        player.networkHandler.sendPacket(
                                new CloseHandledScreenC2SPacket(player.playerScreenHandler.syncId));
                    }
                });
    }

    void command(String string) {
        this.worker.run(() -> player().networkHandler.sendChatCommand(string));
        this.worker.sleep(200L);
    }

    void closeAll() {
        for (int i = 0; i < 4; i++) {
            boolean bl =
                    this.worker.call(
                            () -> {
                                ClientPlayerEntity player = mc().player;
                                if (player == null) {
                                    return false;
                                } else if (player.currentScreenHandler
                                        != player.playerScreenHandler) {
                                    player.closeHandledScreen();
                                    return true;
                                } else if (mc().currentScreen != null
                                        && !(mc().currentScreen instanceof DeathScreen)) {
                                    mc().currentScreen.close();
                                    return true;
                                } else {
                                    return false;
                                }
                            });
            if (!bl) {
                return;
            }
            this.worker.ticks(2);
        }
    }

    boolean waitChange(String string, long l) {
        return this.worker.waitFor(() -> !fingerprintNow().equals(string), l);
    }
}
