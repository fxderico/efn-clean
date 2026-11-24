package net.efn.client.proxy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Locale;

@Environment(EnvType.CLIENT)
public enum ProxyType {
    SOCKS4("Socks4"),
    SOCKS5("Socks5");

    private final String label;

    private ProxyType(String label2) {
        this.label = label2;
    }

    public String label() {
        return this.label;
    }

    static ProxyType parse(String string) {
        if (string == null) {
            return null;
        } else {
            String string2 = string.trim().toLowerCase(Locale.ROOT);
            return switch (string2) {
                case "socks4", "socks4a" -> SOCKS4;
                case "socks5", "socks5h", "socks" -> SOCKS5;
                default -> null;
            };
        }
    }
}
