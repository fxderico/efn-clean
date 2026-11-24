package net.efn.client.proxy;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class Proxy {

    public static final int UNCHECKED = -2;

    public static final int DEAD = -1;

    public String name = "";

    public ProxyType type = ProxyType.SOCKS5;

    public String address = "";

    public int port;

    public String username = "";

    public String password = "";

    public boolean enabled;

    public volatile int ping = -2;

    public Proxy() {}

    public boolean isValid() {
        return this.name != null
                && !this.name.isBlank()
                && this.address != null
                && !this.address.isBlank()
                && this.type != null
                && this.port >= 1
                && this.port <= 65535;
    }

    public String displayAddress() {
        String string = this.address == null ? "" : this.address.trim();
        return (string.indexOf(58) >= 0 ? "[" + string + "]" : string) + ":" + this.port;
    }

    Proxy copy() {
        Proxy proxy = new Proxy();
        proxy.name = this.name;
        proxy.type = this.type;
        proxy.address = this.address;
        proxy.port = this.port;
        proxy.username = this.username;
        proxy.password = this.password;
        proxy.enabled = this.enabled;
        return proxy;
    }

    @Override
    public String toString() {
        return (this.type == null ? "?" : this.type.label()) + " proxy " + this.displayAddress();
    }
}
