package net.efn.client.account;

import com.example.client.mixin.AccessorAccountsMinecraft;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.UserApiService.UserProperties;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.ReporterEnvironment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
public final class GameSession {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private YggdrasilAuthenticationService mojangAuth;

    GameSession() {}

    GameSession.Prepared prepare(AccountType accountType, Credentials credentials) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean bl = accountType == AccountType.CRACKED;
        Session session =
                new Session(
                        credentials.name(),
                        credentials.uuid(),
                        credentials.accessToken(),
                        Optional.empty(),
                        Optional.empty());
        UserApiService userApiService =
                bl
                        ? UserApiService.OFFLINE
                        : this.mojangAuth(client).createUserApiService(credentials.accessToken());
        UserProperties properties2;
        try {
            properties2 = userApiService.fetchProperties();
        } catch (AuthenticationException exception) {
            LOGGER.warn(
                    "[EFN] Couldn't fetch user properties for {}", credentials.name(), exception);
            properties2 = UserApiService.OFFLINE_PROPERTIES;
        }
        ProfileResult profileResult =
                bl
                        ? null
                        : client.getApiServices()
                                .sessionService()
                                .fetchProfile(credentials.uuid(), true);
        return new GameSession.Prepared(session, userApiService, properties2, profileResult, bl);
    }

    void apply(GameSession.Prepared prepared) {
        MinecraftClient client = MinecraftClient.getInstance();
        AccessorAccountsMinecraft accessorAccountsMinecraft = (AccessorAccountsMinecraft) client;
        accessorAccountsMinecraft.efn$setUser(prepared.user());
        accessorAccountsMinecraft.efn$setUserApiService(prepared.api());
        accessorAccountsMinecraft.efn$setUserPropertiesFuture(
                CompletableFuture.completedFuture(prepared.properties()));
        accessorAccountsMinecraft.efn$setProfileFuture(
                CompletableFuture.completedFuture(prepared.profile()));
        accessorAccountsMinecraft.efn$setPlayerSocialManager(
                new SocialInteractionsManager(client, prepared.api()));
        accessorAccountsMinecraft.efn$setProfileKeyPairManager(
                prepared.offline()
                        ? ProfileKeys.MISSING
                        : ProfileKeys.create(
                                prepared.api(), prepared.user(), client.runDirectory.toPath()));
        accessorAccountsMinecraft.efn$setReportingContext(
                AbuseReportContext.create(
                        ReporterEnvironment.ofIntegratedServer(), prepared.api()));
    }

    private YggdrasilAuthenticationService mojangAuth(MinecraftClient client) {
        if (this.mojangAuth == null) {
            this.mojangAuth = new YggdrasilAuthenticationService(client.getNetworkProxy());
        }
        return this.mojangAuth;
    }

    @Environment(EnvType.CLIENT)
    record Prepared(
            Session user,
            UserApiService api,
            UserProperties properties,
            ProfileResult profile,
            boolean offline) {}
}
