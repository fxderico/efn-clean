package net.efn.client.config;

import com.google.common.net.HostAndPort;
import com.google.common.net.InetAddresses;
import com.google.common.net.InternetDomainName;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.net.IDN;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public final class ServerAddresses {

    public static final int DEFAULT_PORT = 25565;

    private static final int MAX_HOST = 253;

    private static final Pattern HOST = Pattern.compile("[a-z0-9_-]+(\\.[a-z0-9_-]+)*");

    private static final Set<String> FRONT_DOORS =
            Set.of("play", "mc", "www", "join", "hub", "lobby", "server", "connect", "go");

    private ServerAddresses() {}

    public static ServerAddresses.Address parse(String string) {
        if (string == null) {
            return null;
        } else {
            String string2 = string.trim();
            if (!string2.isEmpty() && string2.length() <= 261) {
                try {
                    HostAndPort hostAndPort =
                            HostAndPort.fromString(string2).withDefaultPort(25565);
                    String string3 = hostAndPort.getHost().toLowerCase(Locale.ROOT);
                    while (string3.endsWith(".")) {
                        string3 = string3.substring(0, string3.length() - 1);
                    }
                    if (string3.isEmpty()) {
                        return null;
                    } else {
                        return InetAddresses.isInetAddress(string3)
                                        || (string3 = IDN.toASCII(string3).toLowerCase(Locale.ROOT))
                                                                .length()
                                                        <= 253
                                                && HOST.matcher(string3).matches()
                                ? new ServerAddresses.Address(string3, hostAndPort.getPort())
                                : null;
                    }
                } catch (IllegalStateException | IllegalArgumentException exception) {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    public static int score(ServerAddresses.Address address, ServerAddresses.Address address2) {
        if (address != null && address2 != null && address.port() == address2.port()) {
            String host2 = address.host();
            String host3;
            if (host2.equals(host3 = address2.host())) {
                return 3;
            } else if (!InetAddresses.isInetAddress(host2) && !InetAddresses.isInetAddress(host3)) {
                String site = site(host2);
                if (site.equals(site(host3))) {
                    return 2;
                } else {
                    return host3.endsWith("." + site) ? 1 : 0;
                }
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    private static String site(String string) {
        int i = string.indexOf(46);
        if (i > 0 && FRONT_DOORS.contains(string.substring(0, i))) {
            String string2 = string.substring(i + 1);
            try {
                return InternetDomainName.from(string2).isUnderPublicSuffix() ? string2 : string;
            } catch (IllegalArgumentException exception) {
                return string2.indexOf(46) > 0 ? string2 : string;
            }
        } else {
            return string;
        }
    }

    @Environment(EnvType.CLIENT)
    public record Address(String host, int port) {

        @Override
        public String toString() {
            String string = this.host.indexOf(58) >= 0 ? "[" + this.host + "]" : this.host;
            return this.port == 25565 ? string : string + ":" + this.port;
        }
    }
}
