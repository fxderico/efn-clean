package net.efn.client.addon;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public final class MenuPanels {

    private static volatile Supplier<? extends MenuPanels.Panel> accounts;

    private MenuPanels() {}

    public static void registerAccounts(Supplier<? extends MenuPanels.Panel> supplier) {
        accounts = supplier;
    }

    public static MenuPanels.Panel newAccountsPanel() {
        Supplier supplier = accounts;
        return supplier == null ? null : (MenuPanels.Panel) supplier.get();
    }

    @Environment(EnvType.CLIENT)
    public interface Panel {

        void open();

        boolean isOpen();

        boolean keyPressed(int i);

        void render(float f, float g, float h, float f2);
    }
}
