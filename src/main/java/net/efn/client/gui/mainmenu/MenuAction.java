package net.efn.client.gui.mainmenu;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.List;

@Environment(EnvType.CLIENT)
public enum MenuAction {
    SINGLEPLAYER("Singleplayer"),
    MULTIPLAYER("Multiplayer"),
    REALMS("Realms"),
    OPTIONS("Options"),
    QUIT("Quit"),
    LANGUAGE("Language"),
    ACCESSIBILITY("Accessibility"),
    EFN("EFN Client"),
    MODS("Mods Folder"),
    ACCOUNTS("Accounts"),
    CREDITS("Credits");

    public static final List<MenuAction> MAIN =
            List.of(SINGLEPLAYER, MULTIPLAYER, REALMS, OPTIONS, QUIT);

    private final String label;

    private MenuAction(String label2) {
        this.label = label2;
    }

    public String label() {
        return this.label;
    }
}
