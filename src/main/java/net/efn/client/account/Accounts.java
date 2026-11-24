package net.efn.client.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import net.minecraft.util.Uuids;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public final class Accounts {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final Accounts INSTANCE = new Accounts();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int CONFIG_VERSION = 1;

    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

    private final ExecutorService worker =
            Executors.newSingleThreadExecutor(
                    runnable -> {
                        Thread thread = new Thread(runnable, "EFN Accounts");
                        thread.setDaemon(true);
                        return thread;
                    });

    private final GameSession session = new GameSession();

    private final List<Account> accounts = new ArrayList<>();

    private volatile List<Account> snapshot = List.of();

    private volatile String activeId;

    private volatile boolean busy;

    private volatile String status = "";

    private volatile Thread microsoftSignIn;

    private volatile boolean loaded;

    private boolean readOnly;

    private Accounts() {}

    public static Accounts get() {
        return INSTANCE;
    }

    public List<Account> list() {
        this.ensureLoaded();
        return this.snapshot;
    }

    public Account active() {
        String string = MinecraftClient.getInstance().getSession().getUuidOrNull().toString();
        List list = this.list();
        String activeId2 = this.activeId;
        for (Account account : (Iterable<Account>) list) {
            if (account.id().equals(activeId2) && account.uuid().equals(string)) {
                return account;
            }
        }
        for (Account account2 : (Iterable<Account>) list) {
            if (account2.uuid().equals(string)) {
                return account2;
            }
        }
        return null;
    }

    public CompletableFuture<Account> addCracked(String string) {
        String string2 = string == null ? "" : string.trim();
        return !VALID_NAME.matcher(string2).matches()
                ? this.failed("A cracked name is 1-16 letters, digits or underscores")
                : this.signIn(
                        AccountType.CRACKED,
                        null,
                        "Logging in as " + string2 + "...",
                        () -> offline(string2));
    }

    public CompletableFuture<Account> addSession(String string) {
        String string2 = string == null ? "" : string.trim();
        return string2.isEmpty()
                ? this.failed("Paste an access token first")
                : this.signIn(
                        AccountType.SESSION,
                        null,
                        "Checking the session token...",
                        () -> SessionLogin.signIn(string2));
    }

    public CompletableFuture<Account> addMicrosoft() {
        return this.signIn(
                AccountType.MICROSOFT,
                null,
                "Opening Microsoft sign-in...",
                this::microsoftBrowserSignIn);
    }

    public void cancelMicrosoft() {
        Thread microsoftSignIn2 = this.microsoftSignIn;
        if (microsoftSignIn2 != null) {
            microsoftSignIn2.interrupt();
        }
    }

    public CompletableFuture<Void> login(Account account) {
        Account account2 = this.find(account);
        if (account2 == null) {
            return this.failed("That account is no longer saved");
        } else {
            String string =
                    account2.name().isEmpty()
                            ? account2.type().label() + " account"
                            : account2.name();
            return this.signIn(
                            account2.type(),
                            account2.id(),
                            "Logging in as " + string + "...",
                            () -> {
                                return switch (account2.type()) {
                                    case CRACKED -> offline(account2.name());
                                    case SESSION -> SessionLogin.signIn(account2.secret());
                                    case MICROSOFT ->
                                            MicrosoftLogin.minecraft(
                                                    MicrosoftLogin.refresh(account2.secret()));
                                };
                            })
                    .thenAccept(accountx -> {});
        }
    }

    public synchronized void remove(Account account) {
        int i = this.indexOf(account);
        if (i >= 0) {
            this.accounts.remove(i);
            this.publish();
            this.save();
        }
    }

    public boolean busy() {
        return this.busy;
    }

    public String status() {
        return this.status;
    }

    private static Credentials offline(String string) {
        return new Credentials(string, Uuids.getOfflinePlayerUuid(string), "", "");
    }

    private Credentials microsoftBrowserSignIn() throws InterruptedException {
        this.microsoftSignIn = Thread.currentThread();
        Credentials credentials;
        try {
            MicrosoftLogin.DeviceCode deviceCode = MicrosoftLogin.requestDeviceCode();
            this.status =
                    "Waiting for Microsoft sign-in in your browser... (code "
                            + deviceCode.userCode()
                            + " at "
                            + deviceCode.verificationUri().replaceFirst("^https?://", "")
                            + ")";
            Util.getOperatingSystem().open(deviceCode.directUri());
            MicrosoftLogin.MsaToken msaToken = MicrosoftLogin.awaitApproval(deviceCode);
            this.status = "Signing in to Xbox Live...";
            Credentials credentials2 = MicrosoftLogin.minecraft(msaToken);
            credentials = credentials2;
        } finally {
            this.microsoftSignIn = null;
        }
        return credentials;
    }

    private CompletableFuture<Account> signIn(
            AccountType accountType, String string, String status2, Accounts.SignIn signIn) {
        synchronized (this) {
            if (this.busy) {
                return CompletableFuture.failedFuture(
                        new AccountException("Another account action is still running"));
            }
            if (inGame()) {
                return this.failed("Leave the world before switching accounts");
            }
            this.busy = true;
        }
        this.ensureLoaded();
        this.status = status2;
        CompletableFuture future = new CompletableFuture();
        this.worker.execute(
                () -> {
                    Thread.interrupted();
                    try {
                        Credentials credentials = signIn.run();
                        GameSession.Prepared prepared =
                                this.session.prepare(accountType, credentials);
                        MinecraftClient.getInstance()
                                .execute(
                                        () ->
                                                this.finish(
                                                        accountType,
                                                        string,
                                                        credentials,
                                                        prepared,
                                                        future));
                    } catch (InterruptedException exception) {
                        this.settleLater(
                                future,
                                null,
                                new AccountException(
                                        accountType == AccountType.MICROSOFT
                                                ? "Microsoft sign-in cancelled"
                                                : "Login cancelled"));
                    } catch (AccountException exception2) {
                        LOGGER.warn(
                                "[EFN] {} login failed: {}",
                                accountType.label(),
                                exception2.getMessage());
                        this.settleLater(future, null, exception2);
                    } catch (RuntimeException exception3) {
                        LOGGER.warn("[EFN] {} login failed", accountType.label(), exception3);
                        this.settleLater(
                                future,
                                null,
                                new AccountException(
                                        "Login failed: " + exception3.getMessage(), exception3));
                    }
                });
        return future;
    }

    private void finish(
            AccountType accountType,
            String string,
            Credentials credentials,
            GameSession.Prepared prepared,
            CompletableFuture<Account> future) {
        Account account;
        synchronized (this) {
            account = this.remember(accountType, string, credentials);
        }
        if (inGame()) {
            this.settle(
                    future,
                    null,
                    new AccountException(
                            "Joined a world before the switch finished - leave it and log in"
                                    + " again"));
        } else {
            try {
                this.session.apply(prepared);
            } catch (RuntimeException exception) {
                LOGGER.error("[EFN] Switching the session to {} failed", account, exception);
                this.settle(
                        future,
                        null,
                        new AccountException(
                                "Couldn't switch the game to "
                                        + credentials.name()
                                        + ": "
                                        + exception.getMessage(),
                                exception));
                return;
            }
            this.activeId = account.id();
            LOGGER.info("[EFN] Switched session to {}", account);
            this.status = "Logged in as " + credentials.name();
            this.settle(future, account, null);
        }
    }

    private void settleLater(
            CompletableFuture<Account> future, Account account, AccountException accountException) {
        MinecraftClient.getInstance().execute(() -> this.settle(future, account, accountException));
    }

    private void settle(
            CompletableFuture<Account> future, Account account, AccountException accountException) {
        if (accountException != null) {
            this.status = accountException.getMessage();
        }
        this.busy = false;
        if (accountException != null) {
            future.completeExceptionally(accountException);
        } else {
            future.complete(account);
        }
    }

    private Account remember(AccountType accountType, String string, Credentials credentials) {
        String string2 = credentials.uuid().toString();
        int i = -1;
        for (int j = 0; j < this.accounts.size(); j++) {
            Account account = this.accounts.get(j);
            boolean bl =
                    string != null
                            ? account.id().equals(string)
                            : account.type() == accountType
                                    && (accountType == AccountType.CRACKED
                                            ? account.name().equalsIgnoreCase(credentials.name())
                                            : account.uuid().equals(string2));
            if (bl) {
                i = j;
                break;
            }
        }
        if (i < 0 && string != null) {
            return new Account(
                    string, accountType, credentials.name(), string2, credentials.secret());
        } else {
            Account account2 =
                    i >= 0
                            ? this.accounts
                                    .get(i)
                                    .withLogin(credentials.name(), string2, credentials.secret())
                            : new Account(
                                    UUID.randomUUID().toString(),
                                    accountType,
                                    credentials.name(),
                                    string2,
                                    credentials.secret());
            if (i >= 0) {
                this.accounts.set(i, account2);
            } else {
                this.accounts.add(account2);
            }
            this.publish();
            this.save();
            return account2;
        }
    }

    private static boolean inGame() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.world != null || client.getNetworkHandler() != null;
    }

    private <T> CompletableFuture<T> failed(String status2) {
        this.status = status2;
        return CompletableFuture.failedFuture(new AccountException(status2));
    }

    private synchronized Account find(Account account) {
        int i = this.indexOf(account);
        return i < 0 ? null : this.accounts.get(i);
    }

    private int indexOf(Account account) {
        this.ensureLoaded();
        if (account == null) {
            return -1;
        } else {
            for (int i = 0; i < this.accounts.size(); i++) {
                if (this.accounts.get(i).id().equals(account.id())) {
                    return i;
                }
            }
            return -1;
        }
    }

    private void publish() {
        this.snapshot = List.copyOf(this.accounts);
    }

    private void ensureLoaded() {
        if (!this.loaded) {
            this.load();
        }
    }

    private synchronized void load() {
        if (!this.loaded) {
            this.loaded = true;
            Path path = configPath();
            if (Files.isRegularFile(path)) {
                try {
                    label101:
                    {
                        try (BufferedReader bufferedReader = Files.newBufferedReader(path)) {
                            JsonObject jsonObject =
                                    JsonParser.parseReader(bufferedReader).getAsJsonObject();
                            int i =
                                    jsonObject.has("version")
                                            ? jsonObject.get("version").getAsInt()
                                            : 0;
                            if (i > 1) {
                                this.readOnly = true;
                                LOGGER.warn(
                                        "[EFN] Account list version {} is newer than supported {};"
                                                + " skipping load",
                                        i,
                                        1);
                                return;
                            }
                            if (jsonObject.has("accounts")
                                    && jsonObject.get("accounts").isJsonArray()) {
                                Iterator iterator =
                                        jsonObject.getAsJsonArray("accounts").iterator();
                                while (true) {
                                    if (!iterator.hasNext()) {
                                        break label101;
                                    }
                                    JsonElement json = (JsonElement) iterator.next();
                                    Account account;
                                    if (json.isJsonObject()
                                            && (account = read(json.getAsJsonObject())) != null) {
                                        this.accounts.add(account);
                                    }
                                }
                            }
                        }
                        return;
                    }
                } catch (RuntimeException | IOException exception) {
                    LOGGER.warn(
                            "[EFN] Failed to load the account list; preserving the file",
                            (Throwable) exception);
                    this.accounts.clear();
                    this.readOnly = true;
                }
                this.publish();
            }
        }
    }

    private static Account read(JsonObject json) {
        String text = AuthHttp.text(json, "type");
        AccountType accountType = null;
        for (AccountType accountType2 : AccountType.values()) {
            if (accountType2.name().equalsIgnoreCase(text)) {
                accountType = accountType2;
            }
        }
        if (accountType == null) {
            return null;
        } else {
            String text2 = AuthHttp.text(json, "id");
            String text3 = AuthHttp.text(json, "name");
            String text4 = AuthHttp.text(json, "uuid");
            String text5 = AuthHttp.text(json, "token");
            if (text3 == null) {
                text3 = "";
            }
            if (accountType == AccountType.CRACKED
                    ? VALID_NAME.matcher(text3).matches()
                    : text5 != null && !text5.isEmpty()) {
                if (accountType == AccountType.CRACKED) {
                    text4 = Uuids.getOfflinePlayerUuid(text3).toString();
                }
                return new Account(
                        text2 != null && !text2.isEmpty() ? text2 : UUID.randomUUID().toString(),
                        accountType,
                        text3,
                        text4 == null ? "" : text4,
                        text5 == null ? "" : text5);
            } else {
                return null;
            }
        }
    }

    private void save() {
        if (!this.readOnly) {
            JsonObject json = new JsonObject();
            json.addProperty("version", 1);
            JsonArray array = new JsonArray();
            for (Account account : this.accounts) {
                JsonObject json2 = new JsonObject();
                json2.addProperty("id", account.id());
                json2.addProperty("type", account.type().name());
                json2.addProperty("name", account.name());
                json2.addProperty("uuid", account.uuid());
                if (!account.secret().isEmpty()) {
                    json2.addProperty("token", account.secret());
                }
                array.add(json2);
            }
            json.add("accounts", array);
            Path path = configPath();
            try {
                Files.createDirectories(path.getParent());
                Path sibling = path.resolveSibling(path.getFileName().toString() + ".tmp");
                try (BufferedWriter bufferedWriter = Files.newBufferedWriter(sibling)) {
                    GSON.toJson((JsonElement) json, bufferedWriter);
                }
                try {
                    Files.move(
                            sibling,
                            path,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException exception) {
                    Files.move(sibling, path, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException exception2) {
                LOGGER.warn("[EFN] Failed to save the account list", (Throwable) exception2);
            }
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("efn").resolve("accounts.json");
    }

    @Environment(EnvType.CLIENT)
    private interface SignIn {

        Credentials run() throws InterruptedException;
    }
}
