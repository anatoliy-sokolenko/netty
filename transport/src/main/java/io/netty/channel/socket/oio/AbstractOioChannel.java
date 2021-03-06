package io.netty.channel.socket.oio;

import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelBufferHolder;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoop;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

abstract class AbstractOioChannel extends AbstractChannel {

    protected AbstractOioChannel(Channel parent, Integer id, ChannelBufferHolder<?> outboundBuffer) {
        super(parent, id, outboundBuffer);
    }

    @Override
    public InetSocketAddress localAddress() {
        return (InetSocketAddress) super.localAddress();
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return (InetSocketAddress) super.remoteAddress();
    }

    @Override
    public OioUnsafe unsafe() {
        return (OioUnsafe) super.unsafe();
    }

    public interface OioUnsafe extends Unsafe {
        void read();
    }

    protected abstract class AbstractOioUnsafe extends AbstractUnsafe implements OioUnsafe {

        @Override
        public void connect(
                final SocketAddress remoteAddress,
                final SocketAddress localAddress, final ChannelFuture future) {
            if (eventLoop().inEventLoop()) {
                if (!ensureOpen(future)) {
                    return;
                }

                try {
                    boolean wasActive = isActive();
                    doConnect(remoteAddress, localAddress);
                    future.setSuccess();
                    if (!wasActive && isActive()) {
                        pipeline().fireChannelActive();
                    }
                } catch (Throwable t) {
                    future.setFailure(t);
                    pipeline().fireExceptionCaught(t);
                    closeIfClosed();
                }
            } else {
                eventLoop().execute(new Runnable() {
                    @Override
                    public void run() {
                        connect(remoteAddress, localAddress, future);
                    }
                });
            }
        }
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return loop instanceof OioChildEventLoop;
    }

    @Override
    protected Runnable doRegister() throws Exception {
        // NOOP
        return null;
    }

    @Override
    protected void doDeregister() throws Exception {
        // NOOP
    }

    @Override
    protected boolean isFlushPending() {
        return false;
    }

    protected abstract void doConnect(
            SocketAddress remoteAddress, SocketAddress localAddress) throws Exception;
}
