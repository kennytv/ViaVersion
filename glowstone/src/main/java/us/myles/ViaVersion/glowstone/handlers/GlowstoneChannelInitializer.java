package us.myles.ViaVersion.glowstone.handlers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import net.glowstone.net.pipeline.CodecsHandler;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.protocol.ProtocolPipeline;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GlowstoneChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final ChannelInitializer<SocketChannel> original;
    private Method method;

    public GlowstoneChannelInitializer(ChannelInitializer<SocketChannel> oldInit) {
        this.original = oldInit;
        try {
            this.method = ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class);
            this.method.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public ChannelInitializer<SocketChannel> getOriginal() {
        return original;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) {
        UserConnection info = new UserConnection(socketChannel);
        // init protocol
        new ProtocolPipeline(info);
        // Add originals
        try {
            this.method.invoke(this.original, socketChannel);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        CodecsHandler oldCodecs = (CodecsHandler) socketChannel.pipeline().get("codecs");
        socketChannel.pipeline().replace("codecs", "codecs", new GlowstoneCodecHandler(info, oldCodecs));
    }
}
