package net.efn.launcher;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

public final class InstallNotice {

    private static final String TITLE = "EFN Client";

    private static final String MESSAGE =
            "EFN Client is a Fabric mod for Minecraft 1.21.11.\n"
                    + "Put this file in your mods folder and start the game with Fabric Loader.";

    private InstallNotice() {}

    public static void main(String[] strings) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println(
                    "EFN Client is a Fabric mod for Minecraft 1.21.11.\n"
                            + "Put this file in your mods folder and start the game with Fabric"
                            + " Loader.");
        } else {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception exception) {
            }
            String[] strings2 = new String[] {"Open Mods Folder", "Close"};
            int showOptionDialog =
                    JOptionPane.showOptionDialog(
                            null,
                            "EFN Client is a Fabric mod for Minecraft 1.21.11.\n"
                                + "Put this file in your mods folder and start the game with Fabric"
                                + " Loader.",
                            "EFN Client",
                            -1,
                            1,
                            null,
                            strings2,
                            strings2[1]);
            if (showOptionDialog == 0) {
                openModsFolder();
            }
            System.exit(0);
        }
    }

    private static void openModsFolder() {
        File file = modsFolder();
        if (!file.isDirectory()) {
            file.mkdirs();
        }
        if (Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported(Action.OPEN)
                && file.isDirectory()) {
            try {
                Desktop.getDesktop().open(file);
                return;
            } catch (IOException exception) {
            }
        }
        JOptionPane.showMessageDialog(
                null, "Your mods folder is:\n" + file.getAbsolutePath(), "EFN Client", 1);
    }

    private static File modsFolder() {
        String string = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String property = System.getProperty("user.home", ".");
        if (string.contains("win")) {
            String getenv = System.getenv("APPDATA");
            return new File(new File(getenv != null ? getenv : property, ".minecraft"), "mods");
        } else {
            return string.contains("mac")
                    ? new File(property, "Library/Application Support/minecraft/mods")
                    : new File(property, ".minecraft/mods");
        }
    }
}
