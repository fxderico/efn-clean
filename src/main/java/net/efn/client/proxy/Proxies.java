package net.efn.client.proxy;

import com.google.common.net.InetAddresses;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public final class Proxies {

    private static final Logger LOGGER = LoggerFactory.getLogger("EFN");

    private static final Proxies INSTANCE = new Proxies();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int CONFIG_VERSION = 1;

    private static final Pattern IPV4 = Pattern.compile("\\d{1,3}(?:\\.\\d{1,3}){3}");

    private static final Pattern HOST_NAME =
            Pattern.compile("[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)*");

    private final List<Proxy> proxies = new CopyOnWriteArrayList<>();

    private final List<Proxy> view = Collections.unmodifiableList(this.proxies);

    private volatile boolean loaded;

    private boolean readOnly;

    private Proxies() {}

    public static Proxies get() {
        return INSTANCE;
    }

    public List<Proxy> list() {
        this.ensureLoaded();
        return this.view;
    }

    public Proxy enabled() {
        this.ensureLoaded();
        for (Proxy proxy : this.proxies) {
            if (proxy.enabled) {
                return proxy;
            }
        }
        return null;
    }

    public synchronized void setEnabled(Proxy proxy, boolean bl) {
        this.ensureLoaded();
        if (this.proxies.contains(proxy)) {
            if (bl) {
                if (!proxy.isValid()) {
                    return;
                }
                for (Proxy proxy2 : this.proxies) {
                    proxy2.enabled = proxy2 == proxy;
                }
            } else {
                proxy.enabled = false;
            }
            this.save();
        }
    }

    public synchronized void remove(Proxy proxy) {
        this.ensureLoaded();
        if (this.proxies.remove(proxy)) {
            this.save();
        }
    }

    public synchronized void removeAll(Collection<Proxy> collection) {
        this.ensureLoaded();
        if (this.proxies.removeAll(Set.copyOf(collection))) {
            this.save();
        }
    }

    public synchronized void checked() {
        this.ensureLoaded();
        this.save();
    }

    public synchronized int importText(String string) {
        this.ensureLoaded();
        if (string != null && !string.isBlank()) {
            HashSet set = new HashSet();
            for (Proxy proxy : this.proxies) {
                set.add(key(proxy));
            }
            ArrayList list = new ArrayList();
            for (String string2 : string.split("\\R")) {
                Proxy line = parseLine(string2);
                if (line != null && set.add(key(line))) {
                    list.add(line);
                }
            }
            if (list.isEmpty()) {
                return 0;
            } else {
                this.proxies.addAll(list);
                this.save();
                return list.size();
            }
        } else {
            return 0;
        }
    }

    synchronized Proxy enabledCopy() {
        Proxy proxy = this.enabled();
        return proxy == null ? null : proxy.copy();
    }

    private void keepOneEnabled() {
        Proxy proxy = null;
        for (Proxy proxy2 : this.proxies) {
            if (proxy == null && proxy2.enabled && proxy2.isValid()) {
                proxy = proxy2;
            }
        }
        for (Proxy proxy3 : this.proxies) {
            boolean bl = proxy3 == proxy;
            proxy3.enabled = bl;
        }
    }

    private static String key(Proxy proxy) {
        return proxy.displayAddress().toLowerCase(Locale.ROOT);
    }

    static Proxy parseLine(String string) {
        String string2 = string == null ? "" : string.strip();
        if (!string2.isEmpty() && !string2.startsWith("#")) {
            ProxyType sOCKS5 = ProxyType.SOCKS5;
            int i = string2.indexOf("://");
            if (i >= 0) {
                sOCKS5 = ProxyType.parse(string2.substring(0, i));
                if (sOCKS5 == null) {
                    return null;
                }
                if ((string2 = string2.substring(i + 3)).endsWith("/")) {
                    string2 = string2.substring(0, string2.length() - 1);
                }
            }
            String string3 = "";
            String string4 = "";
            int j = string2.lastIndexOf(64);
            boolean bl = j >= 0;
            if (bl) {
                String string5 = string2.substring(0, j);
                string2 = string2.substring(j + 1);
                int k = string5.indexOf(58);
                string3 = k < 0 ? string5 : string5.substring(0, k);
                string4 = k < 0 ? "" : string5.substring(k + 1);
            }
            String string6;
            if (string2.startsWith("[")) {
                int m = string2.indexOf(93);
                if (m < 0 || m + 1 >= string2.length() || string2.charAt(m + 1) != ':') {
                    return null;
                }
                string6 = string2.substring(1, m);
                string2 = string2.substring(m + 2);
            } else {
                int n = string2.indexOf(58);
                if (n < 0) {
                    return null;
                }
                string6 = string2.substring(0, n);
                string2 = string2.substring(n + 1);
            }
            String[] strings = string2.split(":", 3);
            int port = parsePort(strings[0]);
            string6 = normalizeHost(string6);
            if (port >= 0 && string6 != null) {
                if (strings.length > 1) {
                    if (bl || strings[1].isEmpty()) {
                        return null;
                    }
                    string3 = strings[1];
                    string4 = strings.length > 2 ? strings[2] : "";
                }
                Proxy proxy = new Proxy();
                proxy.type = sOCKS5;
                proxy.address = string6;
                proxy.port = port;
                proxy.username = string3;
                proxy.password = string4;
                proxy.name = proxy.displayAddress();
                return proxy;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private static int parsePort(String string) {
        if (!string.isEmpty() && string.length() <= 5) {
            for (int i = 0; i < string.length(); i++) {
                if (string.charAt(i) < '0' || string.charAt(i) > '9') {
                    return -1;
                }
            }
            int j = Integer.parseInt(string);
            return j >= 1 && j <= 65535 ? j : -1;
        } else {
            return -1;
        }
    }

    private static String normalizeHost(String string) {
        if (IPV4.matcher(string).matches()) {
            StringBuilder sb = new StringBuilder();
            for (String string2 : string.split("\\.")) {
                int i = Integer.parseInt(string2);
                if (i > 255) {
                    return null;
                }
                if (!sb.isEmpty()) {
                    sb.append('.');
                }
                sb.append(i);
            }
            return sb.toString();
        } else if (string.indexOf(58) >= 0) {
            return InetAddresses.isInetAddress(string) ? string : null;
        } else {
            return HOST_NAME.matcher(string).matches() ? string : null;
        }
    }

    private void ensureLoaded() {
        if (!this.loaded) {
            this.load();
        }
    }

    private synchronized void load() {
        if (!this.loaded) {
            Path path = configPath();
            ArrayList list = new ArrayList();
            if (Files.isRegularFile(path)) {
                try (BufferedReader bufferedReader = Files.newBufferedReader(path)) {
                    JsonObject jsonObject =
                            JsonParser.parseReader(bufferedReader).getAsJsonObject();
                    int integer = integer(jsonObject, "version");
                    if (integer > 1) {
                        this.readOnly = true;
                        LOGGER.warn(
                                "[EFN] Proxy list version {} is newer than supported {}; skipping"
                                        + " load",
                                integer,
                                1);
                    } else {
                        JsonElement json = jsonObject.get("proxies");
                        if (json instanceof JsonArray) {
                            for (JsonElement json2 : (JsonArray) json) {
                                if (json2 instanceof JsonObject json3) {
                                    list.add(readEntry(json3));
                                }
                            }
                        }
                    }
                } catch (RuntimeException | IOException exception) {
                    LOGGER.warn(
                            "[EFN] Failed to load the proxy list; starting empty",
                            (Throwable) exception);
                    list.clear();
                    this.setAside(path);
                }
            }
            this.proxies.addAll(list);
            this.keepOneEnabled();
            this.loaded = true;
        }
    }

    private void setAside(Path path) {
        try {
            Files.move(
                    path,
                    path.resolveSibling(path.getFileName() + ".corrupt"),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            LOGGER.warn(
                    "[EFN] Couldn't move the broken proxy list aside; not saving proxies this"
                            + " session",
                    (Throwable) exception);
            this.readOnly = true;
        }
    }

    private static Proxy readEntry(JsonObject json) {
        Proxy proxy = new Proxy();
        proxy.name = string(json, "name");
        ProxyType proxyType = ProxyType.parse(string(json, "type"));
        proxy.type = proxyType == null ? ProxyType.SOCKS5 : proxyType;
        proxy.address = string(json, "address");
        proxy.port = integer(json, "port");
        proxy.username = string(json, "username");
        proxy.password = string(json, "password");
        JsonElement json2 = json.get("enabled");
        JsonPrimitive jsonPrimitive;
        proxy.enabled =
                json2 instanceof JsonPrimitive
                        && (jsonPrimitive = (JsonPrimitive) json2).isBoolean()
                        && jsonPrimitive.getAsBoolean();
        json2 = json.get("ping");
        JsonPrimitive jsonPrimitive2;
        if (json2 instanceof JsonPrimitive && (jsonPrimitive2 = (JsonPrimitive) json2).isNumber()) {
            proxy.ping = Math.max(-1, jsonPrimitive2.getAsInt());
        }
        return proxy;
    }

    private static String string(JsonObject json, String string) {
        String string2;
        if (json.get(string) instanceof JsonPrimitive jsonPrimitive) {
            string2 = jsonPrimitive.getAsString();
        } else {
            string2 = "";
        }
        return string2;
    }

    private static int integer(JsonObject json, String string) {
        JsonElement json2 = json.get(string);
        JsonPrimitive jsonPrimitive;
        return json2 instanceof JsonPrimitive && (jsonPrimitive = (JsonPrimitive) json2).isNumber()
                ? jsonPrimitive.getAsInt()
                : 0;
    }

    private void save() {
        if (!this.readOnly) {
            JsonObject json = new JsonObject();
            json.addProperty("version", 1);
            JsonArray array = new JsonArray();
            for (Proxy proxy : this.proxies) {
                JsonObject json2 = new JsonObject();
                json2.addProperty("name", proxy.name == null ? "" : proxy.name);
                json2.addProperty(
                        "type", (proxy.type == null ? ProxyType.SOCKS5 : proxy.type).name());
                json2.addProperty("address", proxy.address == null ? "" : proxy.address.trim());
                json2.addProperty("port", proxy.port);
                if (proxy.username != null && !proxy.username.isEmpty()) {
                    json2.addProperty("username", proxy.username);
                }
                if (proxy.password != null && !proxy.password.isEmpty()) {
                    json2.addProperty("password", proxy.password);
                }
                json2.addProperty("enabled", proxy.enabled);
                if (proxy.ping != -2) {
                    json2.addProperty("ping", proxy.ping);
                }
                array.add(json2);
            }
            json.add("proxies", array);
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
                LOGGER.warn("[EFN] Failed to save the proxy list", (Throwable) exception2);
            }
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("efn").resolve("proxies.json");
    }
}
