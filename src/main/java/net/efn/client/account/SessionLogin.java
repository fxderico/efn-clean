package net.efn.client.account;

import com.mojang.util.UndashedUuid;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
final class SessionLogin {

    private SessionLogin() {}

    static Credentials signIn(String string) throws InterruptedException {
        AuthHttp.Reply reply =
                AuthHttp.get("https://api.minecraftservices.com/minecraft/profile", string);
        if (reply.status() == 401) {
            throw new AccountException("This session token is invalid or has expired");
        } else if (reply.status() == 404) {
            throw new AccountException("This token's account doesn't own Minecraft: Java Edition");
        } else {
            String text = reply.text("name");
            String text2 = reply.text("id");
            if (reply.ok() && text != null && text2 != null) {
                return new Credentials(text, UndashedUuid.fromStringLenient(text2), string, string);
            } else {
                throw new AccountException(
                        "Couldn't load the profile for this token (HTTP " + reply.status() + ")");
            }
        }
    }
}
