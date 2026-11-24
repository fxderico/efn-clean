package net.efn.client.module.impl.misc;

import com.mojang.authlib.GameProfile;

import net.efn.client.chat.ModuleChat;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.ModuleRegistry;
import net.efn.client.module.impl.misc.nameprotect.AliasPolicy;
import net.efn.client.module.impl.misc.nameprotect.NameMatcher;
import net.efn.client.module.impl.misc.nameprotect.ProtectedText;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.StringSetting;
import net.efn.client.util.NameFilter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.session.Session;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class NameProtectModule extends ClientModule {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final String OTHERS_KEEP = "Keep";

    private static final String OTHERS_SAME = "Same Label";

    private static final String OTHERS_NUMBERED = "Numbered";

    private static final String DEFAULT_SELF_ALIAS = "You";

    private static final int DEPARTED_MEMORY = 256;

    private static final int SEEN_MEMORY = 8192;

    private static final int MIN_OTHER_NAME_LENGTH = 3;

    private static final int NPC_UUID_VERSION = 2;

    private static final int CHAT_REFRESH_INTERVAL_TICKS = 20;

    private static final int REFUSAL_NOTICE_DELAY_TICKS = 30;

    private static final int RECENT_CHAT_LINES = 100;

    private static final int DISPLAY_CACHE_SLOTS = 64;

    private static volatile NameMatcher active;

    private static volatile NameProtectModule running;

    private static volatile int generation;

    private static volatile boolean failureLogged;

    private static final Object[] cacheInputs = new Object[64];

    private static final Object[] cacheOutputs = new Object[64];

    private static NameMatcher cacheMatcher;

    private static final String[] recentChat = new String[100];

    private static int recentChatNext;

    private final StringSetting selfAlias =
            this.addSetting("self-alias", new StringSetting("Your Alias", "You"));

    private final StringSetting extraNames =
            this.addSetting("extra-names", new StringSetting("Extra Names", ""));

    private final ModeSetting others =
            this.addSetting(
                    "others", new ModeSetting("Others", "Keep", "Keep", "Same Label", "Numbered"));

    private final ModeSetting othersLabel =
            this.addSetting(
                    "others-label",
                    new ModeSetting("Others Label", "Player", "Player", "Anonymous", "Unknown"));

    private final LinkedHashMap<String, String> knownOthers = new LinkedHashMap<>();

    private final Map<String, Integer> numbers = new HashMap<>();

    private int nextNumber = 1;

    private final LinkedHashSet<String> seenSkeletons = new LinkedHashSet<>();

    private WeakReference<ClientPlayNetworkHandler> trackedConnection = new WeakReference<>(null);

    private volatile boolean dirty = true;

    private volatile boolean settingsEdited;

    private int builtListSize = -1;

    private String builtProfileName;

    private NameMatcher matcher = NameMatcher.EMPTY;

    private String refusedAlias;

    private AliasPolicy.Refusal refusalReason;

    private String noticedAlias;

    private int refusalNoticeDelay;

    private boolean chatRefreshPending;

    private int chatRefreshCooldown;

    public NameProtectModule() {
        super(
                "name-protect",
                "Name Protect",
                "Shows aliases instead of player names on your screen.",
                ModuleCategory.MISC);
        this.usesNotifications();
        this.othersLabel.visibleWhen(() -> !this.others.is("Keep"));
        this.extraNames.privateValue();
        this.selfAlias.onChange(this::markEdited);
        this.extraNames.onChange(this::markEdited);
        this.others.onChange(this::markEdited);
        this.othersLabel.onChange(this::markEdited);
    }

    public static Text protect(Text text) {
        NameMatcher nameMatcher = active;
        if (nameMatcher != null && text != null) {
            boolean onRenderThread = onRenderThread();
            Object object;
            if (onRenderThread && (object = cached(nameMatcher, text)) instanceof Text) {
                Text text2 = (Text) object;
                return text2;
            } else {
                Text text3;
                try {
                    text3 = ProtectedText.rewrite(nameMatcher, text);
                } catch (RuntimeException exception) {
                    reportFailure(exception);
                    return text;
                }
                if (onRenderThread) {
                    remember(text, text3);
                }
                return text3;
            }
        } else {
            return text;
        }
    }

    public static boolean hidesName(String string) {
        NameMatcher nameMatcher = active;
        return nameMatcher != null
                && string != null
                && nameMatcher.aliasAt(string, 0, string.length()) != null;
    }

    public static SkinTextures hiddenHead(UUID uuid, SkinTextures skinTextures) {
        SkinChangeModule module = ModuleRegistry.get(SkinChangeModule.class);
        SkinTextures skinTextures2;
        return module != null
                        && module.isEnabled()
                        && (skinTextures2 = module.skinFor(uuid, skinTextures)) != skinTextures
                ? skinTextures2
                : DefaultSkinHelper.getSkinTextures(uuid);
    }

    public static StringVisitable protectText(StringVisitable stringVisitable) {
        NameMatcher nameMatcher = active;
        if (nameMatcher != null && stringVisitable != null) {
            boolean onRenderThread = onRenderThread();
            Object object;
            if (onRenderThread
                    && (object = cached(nameMatcher, stringVisitable)) instanceof StringVisitable) {
                StringVisitable stringVisitable2 = (StringVisitable) object;
                return stringVisitable2;
            } else {
                StringVisitable stringVisitable3;
                try {
                    stringVisitable3 = ProtectedText.rewrite(nameMatcher, stringVisitable);
                } catch (RuntimeException exception) {
                    reportFailure(exception);
                    return stringVisitable;
                }
                if (onRenderThread) {
                    remember(stringVisitable, stringVisitable3);
                }
                return stringVisitable3;
            }
        } else {
            return stringVisitable;
        }
    }

    public static StringVisitable protectChatLine(StringVisitable stringVisitable) {
        NameMatcher nameMatcher = active;
        if (nameMatcher != null && stringVisitable != null) {
            try {
                if (onRenderThread()) {
                    recentChat[recentChatNext] = stringVisitable.getString();
                    recentChatNext = (recentChatNext + 1) % 100;
                }
                return ProtectedText.rewrite(nameMatcher, stringVisitable);
            } catch (RuntimeException exception) {
                reportFailure(exception);
                return stringVisitable;
            }
        } else {
            return stringVisitable;
        }
    }

    public static int generation() {
        return generation;
    }

    public static void onPlayerInfoApplied(PlayerListS2CPacket playerListPacket) {
        NameProtectModule module = running;
        if (module != null && playerListPacket != null) {
            EnumSet actions = playerListPacket.getActions();
            if (actions.contains(Action.ADD_PLAYER) || actions.contains(Action.UPDATE_LISTED)) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.isOnThread()) {
                    module.syncConnection(client);
                    module.rebuild();
                }
            }
        }
    }

    private static String filterName(String string) {
        NameMatcher nameMatcher = active;
        return nameMatcher != null ? nameMatcher.replace(string) : string;
    }

    private static void reportFailure(RuntimeException exception) {
        if (!failureLogged) {
            failureLogged = true;
            LOGGER.error(
                    "Name Protect could not rewrite a text; showing it unchanged",
                    (Throwable) exception);
        }
    }

    private static boolean onRenderThread() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.isOnThread();
    }

    private static Object cached(NameMatcher nameMatcher, Object object) {
        if (cacheMatcher != nameMatcher) {
            clearDisplayCache();
            cacheMatcher = nameMatcher;
            return null;
        } else {
            int i = System.identityHashCode(object) & 63;
            return cacheInputs[i] == object ? cacheOutputs[i] : null;
        }
    }

    private static void remember(Object object, Object object2) {
        int i = System.identityHashCode(object) & 63;
        cacheInputs[i] = object;
        cacheOutputs[i] = object2;
    }

    private static void clearDisplayCache() {
        Arrays.fill(cacheInputs, null);
        Arrays.fill(cacheOutputs, null);
        cacheMatcher = null;
    }

    @Override
    protected void onEnable() {
        running = this;
        MinecraftClient client = MinecraftClient.getInstance();
        this.syncConnection(client);
        this.settingsEdited = false;
        this.rebuild();
        NameFilter.set(NameProtectModule::filterName);
        this.chatRefreshPending = true;
        this.chatRefreshCooldown = 0;
        this.on(TickEvent.TYPE, Stage.PRE, this::onTick);
    }

    @Override
    protected void onDisable() {
        running = null;
        active = null;
        generation++;
        this.matcher = NameMatcher.EMPTY;
        NameFilter.set(null);
        this.chatRefreshPending = false;
        this.dirty = true;
        this.refusedAlias = null;
        this.noticedAlias = null;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.execute(
                    () -> {
                        clearDisplayCache();
                        Arrays.fill(recentChat, null);
                        if (client.inGameHud != null) {
                            client.inGameHud.getChatHud().reset();
                        }
                    });
        }
    }

    private void markEdited() {
        this.settingsEdited = true;
        this.dirty = true;
    }

    private void onTick(TickEvent event) {
        MinecraftClient client = MinecraftClient.getInstance();
        this.syncConnection(client);
        if (listSize(client.getNetworkHandler()) != this.builtListSize) {
            this.dirty = true;
        }
        if (!Objects.equals(profileName(client), this.builtProfileName)) {
            this.dirty = true;
        }
        if (this.dirty) {
            this.rebuild();
        }
        this.announceRefusal(client);
        if (this.chatRefreshCooldown > 0) {
            this.chatRefreshCooldown--;
        }
        if (this.chatRefreshPending
                && this.chatRefreshCooldown == 0
                && !chatOpen(client)
                && client.inGameHud != null) {
            this.chatRefreshPending = false;
            this.chatRefreshCooldown = 20;
            client.inGameHud.getChatHud().reset();
        }
    }

    private void syncConnection(MinecraftClient client) {
        ClientPlayNetworkHandler clientPlayNetworkHandler =
                client != null ? client.getNetworkHandler() : null;
        if (clientPlayNetworkHandler != this.trackedConnection.get()) {
            this.trackedConnection = new WeakReference<>(clientPlayNetworkHandler);
            this.knownOthers.clear();
            this.numbers.clear();
            this.nextNumber = 1;
            this.builtListSize = -1;
            this.dirty = true;
        }
    }

    private void rebuild() {
        this.dirty = false;
        boolean settingsEdited2 = this.settingsEdited;
        this.settingsEdited = false;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayNetworkHandler clientPlayNetworkHandler =
                client != null ? client.getNetworkHandler() : null;
        Session session = client != null ? client.getSession() : null;
        String nameCore = NameMatcher.nameCore(session != null ? session.getUsername() : null);
        this.builtProfileName = profileName(client);
        String nameCore2 = NameMatcher.nameCore(this.builtProfileName);
        this.builtListSize = listSize(clientPlayNetworkHandler);
        ArrayList list = new ArrayList();
        for (String string : this.extraNames.get().split("[,;\\s]+")) {
            String nameCore3 = NameMatcher.nameCore(string);
            if (nameCore3 != null) {
                list.add(nameCore3);
            }
        }
        ArrayList list2 = new ArrayList(list.size() + 2);
        if (nameCore != null) {
            list2.add(nameCore);
        }
        if (nameCore2 != null) {
            list2.add(nameCore2);
        }
        list2.addAll(list);
        this.rememberOnline(clientPlayNetworkHandler, nameCore, nameCore2);
        String ownAlias = this.resolveOwnAlias(list);
        LinkedHashMap map = new LinkedHashMap();
        for (String string2 : (Iterable<String>) list2) {
            put(map, string2, ownAlias);
        }
        if (!this.others.is("Keep")) {
            this.trackOthers(clientPlayNetworkHandler, list2);
            String othersLabel2 = this.othersLabel.get();
            boolean is = this.others.is("Numbered");
            for (Entry entry : this.knownOthers.entrySet()) {
                put(
                        map,
                        (String) entry.getValue(),
                        is ? othersLabel2 + " " + this.numbers.get(entry.getKey()) : othersLabel2);
            }
        }
        this.publish(new NameMatcher(map), settingsEdited2);
    }

    private void publish(NameMatcher matcher2, boolean bl) {
        NameMatcher matcher3 = this.matcher;
        this.matcher = matcher2;
        active = matcher2;
        if (!matcher2.sameMapping(matcher3)) {
            generation++;
            if (bl || recentChatMentions(matcher2.newNamesSince(matcher3))) {
                this.chatRefreshPending = true;
            }
        }
    }

    private static boolean recentChatMentions(NameMatcher nameMatcher) {
        if (nameMatcher.isEmpty()) {
            return false;
        } else {
            for (String string : recentChat) {
                if (nameMatcher.mentionsAny(string)) {
                    return true;
                }
            }
            return false;
        }
    }

    private String resolveOwnAlias(List<String> list) {
        String sanitize = AliasPolicy.sanitize(this.selfAlias.get());
        if (!sanitize.isEmpty() && !sanitize.equalsIgnoreCase("You")) {
            HashSet set = new HashSet();
            for (String string : list) {
                set.add(AliasPolicy.skeleton(string));
            }
            AliasPolicy.Refusal refusal =
                    AliasPolicy.check(
                            sanitize,
                            value -> this.seenSkeletons.contains(value) || set.contains(value));
            if (refusal == null) {
                this.refusedAlias = null;
                return sanitize;
            } else {
                if (!sanitize.equals(this.refusedAlias)) {
                    this.refusedAlias = sanitize;
                    this.refusalNoticeDelay = 30;
                }
                this.refusalReason = refusal;
                return "You";
            }
        } else {
            this.refusedAlias = null;
            return sanitize.isEmpty() ? "You" : sanitize;
        }
    }

    private void announceRefusal(MinecraftClient client) {
        if (this.refusedAlias == null) {
            this.noticedAlias = null;
        } else if (!this.refusedAlias.equals(this.noticedAlias)) {
            if (this.refusalNoticeDelay > 0) {
                this.refusalNoticeDelay--;
            } else if (client.player != null) {
                this.noticedAlias = this.refusedAlias;
                ModuleChat.send(
                        this,
                        ModuleChat.body()
                                .text("Alias ")
                                .name(this.refusedAlias)
                                .text(" ")
                                .danger("not used")
                                .text(": " + this.refusalReason.reason() + ", showing ")
                                .name("You")
                                .text(" instead"));
            }
        }
    }

    private void rememberOnline(
            ClientPlayNetworkHandler clientPlayNetworkHandler, String string, String string2) {
        if (clientPlayNetworkHandler != null) {
            for (PlayerListEntry player : clientPlayNetworkHandler.getPlayerList()) {
                GameProfile profile = player.getProfile();
                String nameCore = NameMatcher.nameCore(profile != null ? profile.name() : null);
                if (nameCore != null
                        && !nameCore.equalsIgnoreCase(string)
                        && !nameCore.equalsIgnoreCase(string2)) {
                    String skeleton = AliasPolicy.skeleton(nameCore);
                    this.seenSkeletons.remove(skeleton);
                    this.seenSkeletons.add(skeleton);
                }
            }
            Iterator iterator = this.seenSkeletons.iterator();
            while (this.seenSkeletons.size() > 8192 && iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
    }

    private void trackOthers(ClientPlayNetworkHandler clientPlayNetworkHandler, List<String> list) {
        if (clientPlayNetworkHandler != null) {
            int i = 0;
            for (PlayerListEntry listedPlayerListEntry :
                    clientPlayNetworkHandler.getListedPlayerListEntries()) {
                GameProfile profile = listedPlayerListEntry.getProfile();
                String nameCore;
                if (profile != null
                        && (profile.id() == null || profile.id().version() != 2)
                        && isLikelyPlayer(nameCore = NameMatcher.nameCore(profile.name()))
                        && !isOwn(nameCore, list)) {
                    String string = nameCore.toLowerCase(Locale.ROOT);
                    this.knownOthers.remove(string);
                    this.knownOthers.put(string, nameCore);
                    this.numbers.computeIfAbsent(string, value -> this.nextNumber++);
                    i++;
                }
            }
            Iterator iterator = this.knownOthers.keySet().iterator();
            while (this.knownOthers.size() > i + 256 && iterator.hasNext()) {
                this.numbers.remove(iterator.next());
                iterator.remove();
            }
        }
    }

    private static void put(Map<String, String> map, String string, String string2) {
        String nameCore = NameMatcher.nameCore(string);
        if (nameCore != null && !string2.equalsIgnoreCase(nameCore)) {
            map.putIfAbsent(nameCore, string2);
        }
    }

    private static boolean isLikelyPlayer(String string) {
        if (string != null && string.length() >= 3) {
            for (int i = 0; i < string.length(); i++) {
                if (Character.isLetter(string.charAt(i))) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    private static boolean isOwn(String string, List<String> list) {
        for (String string2 : list) {
            if (string2.equalsIgnoreCase(string)) {
                return true;
            }
        }
        return false;
    }

    private static String profileName(MinecraftClient client) {
        return client != null && client.player != null
                ? client.player.getGameProfile().name()
                : null;
    }

    private static int listSize(ClientPlayNetworkHandler clientPlayNetworkHandler) {
        return clientPlayNetworkHandler != null
                ? clientPlayNetworkHandler.getListedPlayerListEntries().size()
                : -1;
    }

    private static boolean chatOpen(MinecraftClient client) {
        return client.inGameHud != null && client.inGameHud.getChatHud().isChatFocused();
    }
}
