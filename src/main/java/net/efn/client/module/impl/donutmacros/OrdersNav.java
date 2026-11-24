package net.efn.client.module.impl.donutmacros;

import net.efn.client.chat.ModuleChat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
final class OrdersNav {

    static final int YOUR_ORDERS_BUTTON = 51;

    static final int ORDER_SLOTS = 45;

    private static final long OPEN_TIMEOUT_MS = 10000L;

    private OrdersNav() {}

    static boolean openYourOrders(
            MacroWorker macroWorker, Gui gui, Consumer<ModuleChat.Body> consumer) {
        gui.closeAll();
        macroWorker.sleep(150L);
        gui.command("orders");
        if (!macroWorker.waitFor(OrdersNav::isOrdersRootNow, 10000L)) {
            consumer.accept(ModuleChat.body().warn("Could not open /orders."));
            gui.closeAll();
            return false;
        } else {
            String fingerprint = gui.fingerprint();
            gui.click(51, 0, SlotActionType.PICKUP);
            if (macroWorker.waitFor(OrdersNav::isYourOrdersNow, 2500L)) {
                return true;
            } else {
                if (gui.waitChange(fingerprint, 1000L)) {
                    macroWorker.sleep(150L);
                    if (macroWorker.call(Gui::containerOpenNow)
                            && !macroWorker.call(OrdersNav::isOrdersRootNow)) {
                        return true;
                    }
                }
                consumer.accept(ModuleChat.body().warn("Could not open Your Orders."));
                gui.closeAll();
                return false;
            }
        }
    }

    static boolean isOrdersRootNow() {
        if (!Gui.containerOpenNow()) {
            return false;
        } else {
            String string = ServerText.normalize(Gui.mc().currentScreen.getTitle());
            if (string.startsWith("orders (page") || string.equals("orders")) {
                return true;
            } else if (string.contains("your orders") || string.contains("auction")) {
                return false;
            } else {
                return string.contains("orders")
                        ? true
                        : Gui.nameOf(Gui.slotNow(51)).contains("your order");
            }
        }
    }

    static boolean isYourOrdersNow() {
        if (!Gui.containerOpenNow()) {
            return false;
        } else {
            String string = ServerText.normalize(Gui.mc().currentScreen.getTitle());
            return string.contains("your orders")
                    || Gui.findNow(OrdersNav::isNewOrderPane, 45) >= 0;
        }
    }

    static boolean isNewOrderPane(ItemStack stack) {
        return stack.isOf(Items.GRAY_STAINED_GLASS_PANE) && Gui.nameOf(stack).contains("new order");
    }

    static boolean isPane(ItemStack stack) {
        return stack.isEmpty()
                || Registries.ITEM.getId(stack.getItem()).getPath().contains("stained_glass_pane");
    }
}
