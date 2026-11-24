package net.efn.client.proxy;

import com.google.common.net.InetAddresses;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.util.concurrent.DefaultThreadFactory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Environment(EnvType.CLIENT)
public final class ProxyChecker {

    private static final InetSocketAddress TARGET =
            new InetSocketAddress(InetAddresses.forString("1.1.1.1"), 443);

    private static final int TIMEOUT_MS = 8000;

    private static final int IN_FLIGHT = 32;

    private static final ProxyChecker INSTANCE = new ProxyChecker();

    private final Set<Proxy> checking = ConcurrentHashMap.newKeySet();

    private final Deque<Proxy> queue = new ArrayDeque<>();

    private int running;

    private EventLoopGroup group;

    private ProxyChecker() {}

    public static ProxyChecker get() {
        return INSTANCE;
    }

    public synchronized void check(Collection<Proxy> collection) {
        for (Proxy proxy : collection) {
            if (this.checking.add(proxy)) {
                this.queue.add(proxy);
            }
        }
        this.pump();
    }

    public boolean isChecking(Proxy proxy) {
        return this.checking.contains(proxy);
    }

    public int pending() {
        return this.checking.size();
    }

    private synchronized void pump() {
        if (this.group == null) {
            this.group =
                    new MultiThreadIoEventLoopGroup(
                            2,
                            new DefaultThreadFactory("EFN proxy check", true),
                            NioIoHandler.newFactory());
        }
        while (this.running < 32 && !this.queue.isEmpty()) {
            Proxy proxy = this.queue.poll();
            this.running++;
            this.group.execute(() -> this.start(proxy));
        }
    }

    private void start(Proxy proxy) {
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        if (!proxy.isValid()) {
            this.finish(proxy, -1, atomicBoolean);
        } else {
            final ProxyHandler proxyHandler;
            try {
                proxyHandler = ProxyRouting.handler(proxy);
            } catch (RuntimeException exception) {
                this.finish(proxy, -1, atomicBoolean);
                return;
            }
            proxyHandler.setConnectTimeoutMillis(8000L);
            long l = System.nanoTime();
            ChannelFuture channelFuture =
                    new Bootstrap()
                            .group(this.group)
                            .channel(NioSocketChannel.class)
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 8000)
                            .handler(
                                    new ChannelInitializer<Channel>() {

                                        @Override
                                        protected void initChannel(Channel channel) {
                                            channel.pipeline()
                                                    .addLast(
                                                            proxyHandler,
                                                            new ChannelInboundHandlerAdapter() {

                                                                @Override
                                                                public void exceptionCaught(
                                                                        ChannelHandlerContext
                                                                                channelHandlerContext,
                                                                        Throwable throwable) {
                                                                    channelHandlerContext.close();
                                                                }
                                                            });
                                        }
                                    })
                            .connect(TARGET);
            channelFuture.addListener(
                    future -> {
                        if (!future.isSuccess()) {
                            this.finish(proxy, -1, atomicBoolean);
                        }
                    });
            proxyHandler
                    .connectFuture()
                    .addListener(
                            future -> {
                                int i = (int) ((System.nanoTime() - l) / 1000000L);
                                this.finish(proxy, future.isSuccess() ? i : -1, atomicBoolean);
                                channelFuture.channel().close();
                            });
        }
    }

    private void finish(Proxy proxy, int i, AtomicBoolean atomicBoolean) {
        if (atomicBoolean.compareAndSet(false, true)) {
            proxy.ping = i;
            this.checking.remove(proxy);
            boolean bl;
            synchronized (this) {
                this.running--;
                bl = this.running == 0 && this.queue.isEmpty();
                this.pump();
            }
            if (bl) {
                Proxies.get().checked();
            }
        }
    }
}
