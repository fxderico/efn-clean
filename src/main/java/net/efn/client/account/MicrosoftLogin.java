package net.efn.client.account;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.util.UndashedUuid;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Environment(EnvType.CLIENT)
final class MicrosoftLogin {

    private static final String CLIENT_ID = "00000000402b5328";

    private static final String SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";

    private static final String DEVICE_CODE_URL = "https://login.live.com/oauth20_connect.srf";

    private static final String TOKEN_URL = "https://login.live.com/oauth20_token.srf";

    private static final String DEVICE_AUTH_URL =
            "https://device.auth.xboxlive.com/device/authenticate";

    private static final String SISU_URL = "https://sisu.xboxlive.com/authorize";

    private static final String LAUNCHER_LOGIN_URL =
            "https://api.minecraftservices.com/launcher/login";

    static final String PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";

    private static final String MINECRAFT_RELYING_PARTY = "rp://api.minecraftservices.com/";

    private static final long FILETIME_EPOCH_OFFSET = 11644473600L;

    private static final long SLOW_DOWN_MS = 5000L;

    private MicrosoftLogin() {}

    static MicrosoftLogin.DeviceCode requestDeviceCode() throws InterruptedException {
        AuthHttp.Reply reply =
                AuthHttp.postForm(
                        "https://login.live.com/oauth20_connect.srf",
                        Map.of(
                                "client_id",
                                "00000000402b5328",
                                "scope",
                                "service::user.auth.xboxlive.com::MBI_SSL",
                                "response_type",
                                "device_code"));
        String text = reply.text("device_code");
        String text2 = reply.text("user_code");
        String text3 = reply.text("verification_uri");
        String text4 = reply.text("interval");
        String text5 = reply.text("expires_in");
        if (reply.ok()
                && text != null
                && text2 != null
                && text3 != null
                && text4 != null
                && text5 != null) {
            return new MicrosoftLogin.DeviceCode(
                    text,
                    text2,
                    text3,
                    Long.parseLong(text4) * 1000L,
                    System.currentTimeMillis() + Long.parseLong(text5) * 1000L);
        } else {
            throw new AccountException("Microsoft sign-in couldn't start: " + liveError(reply));
        }
    }

    static MicrosoftLogin.MsaToken awaitApproval(MicrosoftLogin.DeviceCode deviceCode2)
            throws InterruptedException {
        long intervalMs2 = deviceCode2.intervalMs();
        while (System.currentTimeMillis() < deviceCode2.expiresAtMs()) {
            Thread.sleep(intervalMs2);
            AuthHttp.Reply reply =
                    AuthHttp.postForm(
                            "https://login.live.com/oauth20_token.srf",
                            Map.of(
                                    "client_id",
                                    "00000000402b5328",
                                    "grant_type",
                                    "device_code",
                                    "device_code",
                                    deviceCode2.deviceCode()));
            if (reply.ok()) {
                return token(reply);
            }
            String text = reply.text("error");
            if (!"authorization_pending".equals(text)) {
                if (!"slow_down".equals(text)) {
                    if ("authorization_declined".equals(text) || "access_denied".equals(text)) {
                        throw new AccountException("Microsoft sign-in was declined in the browser");
                    }
                    if (!"expired_token".equals(text)) {
                        throw new AccountException("Microsoft sign-in failed: " + liveError(reply));
                    }
                    break;
                }
                intervalMs2 += 5000L;
            }
        }
        throw new AccountException("The Microsoft sign-in code expired - try again");
    }

    static MicrosoftLogin.MsaToken refresh(String string) throws InterruptedException {
        AuthHttp.Reply reply =
                AuthHttp.postForm(
                        "https://login.live.com/oauth20_token.srf",
                        Map.of(
                                "client_id",
                                "00000000402b5328",
                                "scope",
                                "service::user.auth.xboxlive.com::MBI_SSL",
                                "grant_type",
                                "refresh_token",
                                "refresh_token",
                                string));
        if (reply.ok()) {
            return token(reply);
        } else if ("invalid_grant".equals(reply.text("error"))) {
            throw new AccountException(
                    "This Microsoft login has expired - remove the account and add it again");
        } else {
            throw new AccountException("Microsoft login failed: " + liveError(reply));
        }
    }

    static Credentials minecraft(MicrosoftLogin.MsaToken msaToken) throws InterruptedException {
        KeyPair proofKey = newProofKey();
        JsonObject json = new JsonObject();
        json.addProperty("DeviceType", "Win32");
        json.addProperty("Id", "{" + UUID.randomUUID() + "}");
        json.addProperty("AuthMethod", "ProofOfPossession");
        json.add("ProofKey", jwk((ECPublicKey) proofKey.getPublic()));
        JsonObject json2 = new JsonObject();
        json2.add("Properties", json);
        json2.addProperty("RelyingParty", "http://auth.xboxlive.com");
        json2.addProperty("TokenType", "JWT");
        AuthHttp.Reply reply =
                signedPost(
                        "https://device.auth.xboxlive.com/device/authenticate",
                        json2,
                        proofKey,
                        Map.of("x-xbl-contract-version", "1"));
        String text = reply.text("Token");
        if (reply.ok() && text != null) {
            JsonObject json3 = new JsonObject();
            json3.addProperty("Sandbox", "RETAIL");
            json3.addProperty("UseModernGamertag", true);
            json3.addProperty("AppId", "00000000402b5328");
            json3.addProperty("AccessToken", "t=" + msaToken.accessToken());
            json3.addProperty("DeviceToken", text);
            json3.add("ProofKey", jwk((ECPublicKey) proofKey.getPublic()));
            json3.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
            AuthHttp.Reply reply2 =
                    signedPost("https://sisu.xboxlive.com/authorize", json3, proofKey, Map.of());
            JsonObject json4 = AuthHttp.object(reply2.json(), "AuthorizationToken");
            String text2 = AuthHttp.text(json4, "Token");
            String userHash = userHash(json4);
            if (reply2.ok() && text2 != null && userHash != null) {
                JsonObject json5 = new JsonObject();
                json5.addProperty("platform", "PC_LAUNCHER");
                json5.addProperty("xtoken", "XBL3.0 x=" + userHash + ";" + text2);
                AuthHttp.Reply reply3 =
                        AuthHttp.postJson(
                                "https://api.minecraftservices.com/launcher/login",
                                json5.toString(),
                                Map.of());
                String text3 = reply3.text("access_token");
                if (reply3.ok() && text3 != null) {
                    AuthHttp.Reply reply4 =
                            AuthHttp.get(
                                    "https://api.minecraftservices.com/minecraft/profile", text3);
                    if (reply4.status() == 404) {
                        throw new AccountException(
                                "This Microsoft account doesn't own Minecraft: Java Edition");
                    } else {
                        String text4 = reply4.text("name");
                        String text5 = reply4.text("id");
                        if (reply4.ok() && text4 != null && text5 != null) {
                            return new Credentials(
                                    text4,
                                    UndashedUuid.fromStringLenient(text5),
                                    text3,
                                    msaToken.refreshToken());
                        } else {
                            throw new AccountException(
                                    "Couldn't load the Minecraft profile: "
                                            + servicesError(reply4));
                        }
                    }
                } else {
                    throw new AccountException("Minecraft login failed: " + servicesError(reply3));
                }
            } else {
                throw xboxError(reply2);
            }
        } else {
            throw xboxError(reply);
        }
    }

    private static MicrosoftLogin.MsaToken token(AuthHttp.Reply reply) {
        String text = reply.text("access_token");
        String text2 = reply.text("refresh_token");
        if (text != null && text2 != null) {
            return new MicrosoftLogin.MsaToken(text, text2);
        } else {
            throw new AccountException(
                    "Microsoft returned no refresh token, so the account couldn't be saved");
        }
    }

    private static String userHash(JsonObject json) {
        JsonObject json2 = AuthHttp.object(json, "DisplayClaims");
        JsonElement json3 = json2 == null ? null : json2.get("xui");
        JsonArray array;
        return json3 instanceof JsonArray
                        && !(array = (JsonArray) json3).isEmpty()
                        && array.get(0).isJsonObject()
                ? AuthHttp.text(array.get(0).getAsJsonObject(), "uhs")
                : null;
    }

    private static KeyPair newProofKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
            keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
            return keyPairGenerator.generateKeyPair();
        } catch (GeneralSecurityException exception) {
            throw new AccountException(
                    "This Java runtime can't create the key Xbox Live requires", exception);
        }
    }

    private static AuthHttp.Reply signedPost(
            String string, JsonObject json, KeyPair keyPair, Map<String, String> map)
            throws InterruptedException {
        String string2 = json.toString();
        HashMap map2 = new HashMap(map);
        map2.put(
                "Signature",
                signature(URI.create(string).getPath(), string2, keyPair.getPrivate()));
        return AuthHttp.postJson(string, string2, map2);
    }

    private static String signature(String string, String string2, PrivateKey privateKey) {
        long l = (Instant.now().getEpochSecond() + 11644473600L) * 10000000L;
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeInt(1);
            dataOutputStream.writeByte(0);
            dataOutputStream.writeLong(l);
            dataOutputStream.writeByte(0);
            dataOutputStream.write("POST".getBytes(StandardCharsets.UTF_8));
            dataOutputStream.writeByte(0);
            dataOutputStream.write(string.getBytes(StandardCharsets.UTF_8));
            dataOutputStream.writeByte(0);
            dataOutputStream.writeByte(0);
            dataOutputStream.write(string2.getBytes(StandardCharsets.UTF_8));
            dataOutputStream.writeByte(0);
            Signature signature = Signature.getInstance("SHA256withECDSAinP1363Format");
            signature.initSign(privateKey);
            signature.update(byteArrayOutputStream.toByteArray());
            ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream2 = new DataOutputStream(byteArrayOutputStream2);
            dataOutputStream2.writeInt(1);
            dataOutputStream2.writeLong(l);
            dataOutputStream2.write(signature.sign());
            return Base64.getEncoder().encodeToString(byteArrayOutputStream2.toByteArray());
        } catch (GeneralSecurityException | IOException exception) {
            throw new AccountException("Couldn't sign the Xbox Live request", exception);
        }
    }

    private static JsonObject jwk(ECPublicKey eCPublicKey) {
        JsonObject json = new JsonObject();
        json.addProperty("kty", "EC");
        json.addProperty("alg", "ES256");
        json.addProperty("crv", "P-256");
        json.addProperty("use", "sig");
        json.addProperty("x", coordinate(eCPublicKey.getW().getAffineX()));
        json.addProperty("y", coordinate(eCPublicKey.getW().getAffineY()));
        return json;
    }

    private static String coordinate(BigInteger bigInteger) {
        byte[] byteArray = bigInteger.toByteArray();
        byte[] bytes = new byte[32];
        int i = Math.min(byteArray.length, bytes.length);
        System.arraycopy(byteArray, byteArray.length - i, bytes, bytes.length - i, i);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String liveError(AuthHttp.Reply reply) {
        String text = reply.text("error_description");
        if (text != null) {
            return text;
        } else {
            String text2 = reply.text("error");
            return text2 != null ? text2 : "HTTP " + reply.status();
        }
    }

    private static String servicesError(AuthHttp.Reply reply) {
        String text = reply.text("errorMessage");
        return text != null ? text : "HTTP " + reply.status();
    }

    private static AccountException xboxError(AuthHttp.Reply reply) {
        String text = reply.text("XErr");
        if (text == null) {
            text = reply.headers().firstValue("X-Err").orElse(null);
        }
        if (text == null) {
            return new AccountException("Xbox Live sign-in failed (HTTP " + reply.status() + ")");
        } else {
            long l;
            try {
                l = Long.parseLong(text);
            } catch (NumberFormatException exception) {
                return new AccountException("Xbox Live sign-in failed (" + text + ")");
            }
            return new AccountException(
                    switch ((int) l) {
                        case -2146051069 -> "This account is banned from Xbox Live";
                        default ->
                                "Xbox Live refused the sign-in (error 0x"
                                        + Long.toHexString(l).toUpperCase(Locale.ROOT)
                                        + ")";
                        case -2146051063 ->
                                "This Microsoft account has no Xbox profile yet - create one at"
                                        + " xbox.com, then try again";
                        case -2146051061 -> "Xbox Live isn't available in this account's country";
                        case -2146051060 ->
                                "This account needs age verification at account.microsoft.com"
                                        + " first";
                        case -2146051059 -> "This account has used up its allowed playtime for now";
                        case -2146051058 ->
                                "This is a child account - an adult has to add it to a Microsoft"
                                        + " family first";
                    });
        }
    }

    @Environment(EnvType.CLIENT)
    record DeviceCode(
            String deviceCode,
            String userCode,
            String verificationUri,
            long intervalMs,
            long expiresAtMs) {

        String directUri() {
            return this.verificationUri + "?otc=" + this.userCode;
        }
    }

    @Environment(EnvType.CLIENT)
    record MsaToken(String accessToken, String refreshToken) {}
}
