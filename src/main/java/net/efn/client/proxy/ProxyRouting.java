package net.efn.client.proxy;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.net.InetAddress;
import java.net.InetSocketAddress;

@Environment(EnvType.CLIENT)
public final class ProxyRouting {

    private static final String HANDLER_NAME = "efn_proxy";

    private ProxyRouting() {}

    public static ChannelHandler route(
            InetSocketAddress inetSocketAddress, ChannelHandler channelHandler) {
        if (inetSocketAddress != null && !isLocal(inetSocketAddress)) {
            Proxy proxy = Proxies.get().enabledCopy();
            if (proxy != null && proxy.isValid()) {
                final ProxyHandler proxyHandler = handler(proxy);
                return new ChannelInitializer<Channel>() {

                    @Override
                    protected void initChannel(Channel channel) {
                        channel.pipeline().addFirst("efn_proxy", proxyHandler);
                        channel.pipeline().addLast(channelHandler);
                    }
                };
            } else {
                return channelHandler;
            }
        } else {
            return channelHandler;
        }
    }

    static ProxyHandler handler(Proxy proxy) {
        InetSocketAddress inetSocketAddress =
                new InetSocketAddress(proxy.address.trim(), proxy.port);
        String string = proxy.username != null && !proxy.username.isEmpty() ? proxy.username : null;
        String string2 =
                proxy.password != null && !proxy.password.isEmpty() ? proxy.password : null;
        return (ProxyHandler)
                (switch (proxy.type) {
                    case SOCKS4 -> new Socks4ProxyHandler(inetSocketAddress, string);
                    case SOCKS5 -> new Socks5ProxyHandler(inetSocketAddress, string, string2);
                });
    }

    private static boolean isLocal(InetSocketAddress inetSocketAddress) {
        InetAddress address = inetSocketAddress.getAddress();
        if (address == null) {
            return "localhost".equalsIgnoreCase(inetSocketAddress.getHostString());
        } else if (!address.isLoopbackAddress()
                && !address.isAnyLocalAddress()
                && !address.isLinkLocalAddress()
                && !address.isSiteLocalAddress()) {
            byte[] address2 = address.getAddress();
            return address2.length == 16 && (address2[0] & 254) == 252;
        } else {
            return true;
        }
    }
}
