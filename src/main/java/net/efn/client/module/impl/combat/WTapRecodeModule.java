package net.efn.client.module.impl.combat;

import net.efn.client.event.EventHandler;
import net.efn.client.event.EventPriority;
import net.efn.client.event.impl.TickEvent;
import net.efn.client.module.ClientModule;
import net.efn.client.module.ModuleCategory;
import net.efn.client.module.impl.combat.melee.MeleeAttacks;
import net.efn.client.module.setting.ModeSetting;
import net.efn.client.module.setting.NumberSetting;
import net.efn.client.util.pvp.InputControl;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.type.PiercingWeaponComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

@Environment(EnvType.CLIENT)
public final class WTapRecodeModule extends ClientModule {

    private static final String W_TAP = "W Tap";

    private static final String S_TAP = "S Tap";

    private final ModeSetting mode =
            this.addSetting("mode", new ModeSetting("Mode", "W Tap", "W Tap", "S Tap"));

    private final NumberSetting hold =
            this.addSetting("hold", new NumberSetting("Hold", 1.0F, 1.0F, 3.0F).step(1.0F));

    private boolean sprintHit;

    private int taps;

    private int tapTick;

    public WTapRecodeModule() {
        super(
                "w-tap-recode",
                "W Tap",
                "Resets your sprint right after a sprint hit so the next hit knocks back too.",
                ModuleCategory.COMBAT);
    }

    @Override
    protected void onDisable() {
        this.taps = 0;
    }

    public void beforeAttack(PlayerEntity player, Entity entity) {
        ClientPlayerEntity player2 = MinecraftClient.getInstance().player;
        this.sprintHit =
                player == player2
                        && player2.isSprinting()
                        && MeleeAttacks.charge(player2) > 0.9F
                        && entity instanceof LivingEntity
                        && player2.input.playerInput.forward();
    }

    public void afterAttack() {
        if (this.sprintHit) {
            this.start(MinecraftClient.getInstance().player);
        }
    }

    public void afterStab() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player.isSprinting() && player.input.playerInput.forward()) {
            if (MeleeAttacks.stabTarget(
                            player, entity -> PiercingWeaponComponent.canHit(player, entity))
                    != null) {
                this.start(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTick(TickEvent event) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            this.taps = 0;
        } else {
            if (this.taps > 0 && player.age != this.tapTick) {
                this.apply();
            }
        }
    }

    private void start(ClientPlayerEntity player) {
        this.taps = this.hold.getInt();
        this.tapTick = player.age;
        this.apply();
    }

    private void apply() {
        InputControl.releaseForward(this.mode.is("S Tap"));
        this.taps--;
    }
}
