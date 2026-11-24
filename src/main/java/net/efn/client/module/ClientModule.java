package net.efn.client.module;

import net.efn.client.event.Event;
import net.efn.client.event.EventBus;
import net.efn.client.event.EventPriority;
import net.efn.client.event.EventType;
import net.efn.client.event.ListenerHandle;
import net.efn.client.event.Stage;
import net.efn.client.event.impl.ModuleToggleEvent;
import net.efn.client.module.setting.BooleanSetting;
import net.efn.client.module.setting.Setting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class ClientModule {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    public static final String SHOW_IN_HUD_SETTING = "Show in HUD";

    private final String id;

    private final String name;

    private final String description;

    private final ModuleCategory category;

    private boolean enabled;

    private boolean enabling;

    private boolean disabling;

    private boolean disableRequested;

    private final List<Setting> settings = new ArrayList<>();

    private final List<Setting> settingsView = Collections.unmodifiableList(this.settings);

    private final BooleanSetting showInHud;

    private final BooleanSetting toggleMessage;

    private BooleanSetting notifications;

    private final List<ListenerHandle<?>> ownedListeners = new ArrayList<>();

    private boolean busRegistered;

    volatile boolean registered;

    private int keybind = -1;

    private boolean holdMode;

    protected ClientModule(String string, String string2, ModuleCategory moduleCategory) {
        this(StableId.legacy(string), string, string2, moduleCategory);
    }

    protected ClientModule(
            String string, String name2, String description2, ModuleCategory moduleCategory) {
        this.id = StableId.validate(string);
        this.name = name2;
        this.description = description2;
        this.category = Objects.requireNonNull(moduleCategory, "category");
        this.showInHud =
                moduleCategory != ModuleCategory.CLIENT
                        ? this.addSetting("show-in-hud", new BooleanSetting("Show in HUD", true))
                        : null;
        BooleanSetting setting =
                this.toggleMessage =
                        moduleCategory != ModuleCategory.CLIENT
                                ? this.addSetting(
                                        "toggle-message",
                                        new BooleanSetting("Toggle Message", true))
                                : null;
        if (this.toggleMessage != null) {
            this.toggleMessage.visibleWhen(() -> !this.isAction());
        }
    }

    /** Puts the given settings under one titled group in the module page. */
    protected final void section(String title, Setting... settings) {
        for (Setting setting : settings) {
            setting.inSection(title);
        }
    }

    protected final <S extends Setting> S addSetting(S s) {
        return this.addSetting(s.id(), (S) s);
    }

    protected final <S extends Setting> S addSetting(String string, S s) {
        return this.insertSetting(this.settings.size(), string, (S) s);
    }

    private <S extends Setting> S insertSetting(int i, String string, S s) {
        for (Setting setting : this.settings) {
            if (setting.id().equals(string)) {
                throw new IllegalArgumentException(
                        "Duplicate setting id: " + this.id + "/" + string);
            }
        }
        s.bindId(string);
        s.onChange(ModuleRevision.CHANGED);
        this.settings.add(i, s);
        ModuleRevision.changed();
        return (S) s;
    }

    protected final BooleanSetting usesNotifications() {
        if (this.notifications == null) {
            int i = (this.showInHud != null ? 1 : 0) + (this.toggleMessage != null ? 1 : 0);
            this.notifications =
                    this.insertSetting(
                            i, "notifications", new BooleanSetting("Notifications", true));
        }
        return this.notifications;
    }

    public final boolean notificationsOn() {
        return this.notifications == null || this.notifications.get();
    }

    public final boolean toggleMessageOn() {
        return this.toggleMessage != null && this.toggleMessage.get() && !this.isAction();
    }

    public final String id() {
        return this.id;
    }

    public final String name() {
        return this.name;
    }

    public final String description() {
        return this.description;
    }

    public final ModuleCategory category() {
        return this.category;
    }

    public final List<Setting> settings() {
        return this.settingsView;
    }

    public final void captureDefaults() {
        for (Setting setting : this.settings) {
            setting.captureDefault();
        }
    }

    public final boolean hasModifiedSettings() {
        for (Setting setting : this.settings) {
            if (!setting.isDefault()) {
                return true;
            }
        }
        return false;
    }

    public final void resetSettings() {
        for (Setting setting : this.settings) {
            setting.resetToDefault();
        }
    }

    public final BooleanSetting showInHud() {
        return this.showInHud;
    }

    public boolean persistEnabled() {
        return true;
    }

    public boolean enabledByDefault() {
        return false;
    }

    public boolean inConfigs() {
        return true;
    }

    public int settingsVersion() {
        return 0;
    }

    public void migrateSettings(int i) {}

    public boolean isAction() {
        return false;
    }

    public void onClick() {}

    public final boolean isEnabled() {
        return this.enabled;
    }

    public final void setEnabled(boolean bl) {
        boolean enabled2 = this.enabled;
        this.switchTo(bl);
        if (this.enabled != enabled2) {
            ModuleToggleEvent.dispatch(this);
        }
    }

    private void switchTo(boolean enabled2) {
        if (this.enabled != enabled2 && !this.disabling) {
            this.enabled = enabled2;
            ModuleRevision.changed();
            if (this.registered) {
                ModuleRegistry.switched(this);
            }
            if (enabled2) {
                this.enabling = true;
                this.disableRequested = false;
                try {
                    this.busRegistered = EventBus.INSTANCE.register(this) > 0;
                    this.onEnable();
                    this.enabling = false;
                    if (this.disableRequested) {
                        this.disableRequested = false;
                        this.switchTo(false);
                    }
                } catch (Throwable exception) {
                    this.enabling = false;
                    this.disableRequested = false;
                    try {
                        this.switchTo(false);
                    } catch (Throwable exception2) {
                        if (exception2 != exception) {
                            exception.addSuppressed(exception2);
                        }
                        if (exception2 instanceof Error error) {
                            throw error;
                        }
                    }
                    LOGGER.error(
                            "Failed to enable module '{}'; activation rolled back",
                            this.name,
                            exception);
                    if (exception instanceof Error error2) {
                        throw error2;
                    }
                }
            } else {
                this.disabling = true;
                try {
                    this.onDisable();
                    return;
                } catch (RuntimeException exception3) {
                    LOGGER.error("Failed to deactivate module '{}'", this.name, exception3);
                } finally {
                    try {
                        this.ownedListeners.forEach(ListenerHandle::unregister);
                        this.ownedListeners.clear();
                        if (this.busRegistered) {
                            this.busRegistered = false;
                            EventBus.INSTANCE.unregister(this);
                        }
                    } finally {
                        this.disabling = false;
                    }
                }
            }
        }
    }

    public final void toggle() {
        this.setEnabled(!this.enabled);
    }

    protected final void requestDisable() {
        if (this.enabling) {
            this.disableRequested = true;
        } else {
            this.setEnabled(false);
        }
    }

    public final int keybind() {
        return this.keybind;
    }

    public final void setKeybind(int keybind2) {
        if (this.keybind != keybind2) {
            this.keybind = keybind2;
            ModuleRevision.changed();
        }
    }

    public final boolean isHoldMode() {
        return this.holdMode;
    }

    public final void setHoldMode(boolean holdMode2) {
        if (this.holdMode != holdMode2) {
            this.holdMode = holdMode2;
            ModuleRevision.changed();
        }
    }

    protected void onEnable() {}

    protected void onDisable() {}

    protected <E extends Event> void on(EventType<E> eventType, Consumer<E> consumer) {
        this.on(eventType, Stage.PRE, EventPriority.NORMAL, consumer);
    }

    protected <E extends Event> void on(EventType<E> eventType, Stage stage, Consumer<E> consumer) {
        this.on(eventType, stage, EventPriority.NORMAL, consumer);
    }

    protected <E extends Event> void on(
            EventType<E> eventType,
            Stage stage,
            EventPriority eventPriority,
            Consumer<E> consumer) {
        if (!this.enabled) {
            throw new IllegalStateException(
                    "Cannot register a listener on disabled module: " + this.name);
        } else {
            this.ownedListeners.add(eventType.listen(this, stage, eventPriority, consumer));
        }
    }
}
