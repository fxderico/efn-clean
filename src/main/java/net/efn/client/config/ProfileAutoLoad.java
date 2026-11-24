package net.efn.client.config;

import net.efn.client.event.impl.PacketAppliedEvent;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.gui.ChallengeToast;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.text.Text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class ProfileAutoLoad {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final Object OWNER = new Object();

    private static final long TOAST_WAIT_NANOS = 15000000000L;

    private static WeakReference<ClientConnection> handled = new WeakReference<>(null);

    private static String pendingTitle;

    private static String pendingMessage;

    private static long pendingSince;

    private ProfileAutoLoad() {}

    public static void install() {
        PacketAppliedEvent.TYPE.listen(
                OWNER,
                packetAppliedEvent -> {
                    if (packetAppliedEvent.packet instanceof GameJoinS2CPacket) {
                        onJoin();
                    }
                });
        TickEvent.TYPE.listen(OWNER, tickEvent -> showPendingToast());
    }

    private static void onJoin() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!client.isInSingleplayer()) {
            ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
            ServerInfo serverInfo = networkHandler == null ? null : networkHandler.getServerInfo();
            if (serverInfo != null && serverInfo.address != null) {
                ClientConnection connection = networkHandler.getConnection();
                if (connection != handled.get()) {
                    handled = new WeakReference<>(connection);
                    List forServer = ConfigProfiles.getInstance().loadForServer(serverInfo.address);
                    if (!forServer.isEmpty()) {
                        LOGGER.info(
                                "[EFN] Joined {} - loaded profile {}",
                                serverInfo.address,
                                String.join(", ", forServer));
                        pendingTitle = forServer.size() == 1 ? "Profile loaded" : "Profiles loaded";
                        pendingMessage = String.join(", ", forServer);
                        pendingSince = System.nanoTime();
                    }
                }
            }
        }
    }

    private static void showPendingToast() {
        if (pendingTitle != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            boolean bl = System.nanoTime() - pendingSince > 15000000000L;
            if (bl
                    || client.player != null
                            && !(client.currentScreen instanceof LevelLoadingScreen)) {
                String string = pendingTitle;
                String string2 = pendingMessage;
                pendingTitle = null;
                pendingMessage = null;
                if (client.world != null) {
                    ChallengeToast.addOrUpdate(
                            new ItemStack(Items.WRITABLE_BOOK),
                            string,
                            Text.literal(string2),
                            false);
                }
            }
        }
    }
}
