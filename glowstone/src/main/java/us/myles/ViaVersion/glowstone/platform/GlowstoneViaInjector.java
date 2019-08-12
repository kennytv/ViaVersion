package us.myles.ViaVersion.glowstone.platform;

import com.google.gson.JsonObject;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import net.glowstone.GlowServer;
import net.glowstone.net.GlowSocketServer;
import org.bukkit.Bukkit;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.platform.ViaInjector;
import us.myles.ViaVersion.glowstone.handlers.GlowstoneChannelInitializer;
import us.myles.ViaVersion.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.util.List;

public class GlowstoneViaInjector implements ViaInjector {
    private ChannelInitializer original;

    @Override
    public void inject() throws Exception {
        try {
            Field bootstrapField = GlowSocketServer.class.getDeclaredField("bootstrap");
            bootstrapField.setAccessible(true);

            GlowSocketServer server = ((GlowServer) Bukkit.getServer()).getNetworkServer();
            ServerBootstrap bootstrap = (ServerBootstrap) bootstrapField.get(server);
            Field childHandlerField = ServerBootstrap.class.getDeclaredField("childHandler");
            childHandlerField.setAccessible(true);

            original = (ChannelInitializer<SocketChannel>) childHandlerField.get(bootstrap);
            childHandlerField.set(bootstrap, new GlowstoneChannelInitializer(original));
        } catch (IllegalAccessException | NoSuchFieldException e) {
            Via.getPlatform().getLogger().severe("Unable to inject ViaVersion, please post these details on our GitHub and ensure you're using a compatible server version.");
            throw e;
        }

        //TODO only this needed? else reformat maybe
        GlowSocketServer server = ((GlowServer) Bukkit.getServer()).getNetworkServer();
        List<String> names = server.getChannel().pipeline().names();
        ChannelHandler bootstrapAcceptor = null;
        // Pick best
        for (String name : names) {
            ChannelHandler handler = server.getChannel().pipeline().get(name);
            try {
                ReflectionUtil.get(handler, "childHandler", ChannelInitializer.class);
                bootstrapAcceptor = handler;
            } catch (Exception e) {
                // Not this one
            }
        }
        // Default to first (Also allows blame to work)
        if (bootstrapAcceptor == null) {
            bootstrapAcceptor = server.getChannel().pipeline().first();
        }
        try {
            ChannelInitializer<SocketChannel> oldInit = ReflectionUtil.get(bootstrapAcceptor, "childHandler", ChannelInitializer.class);
            ChannelInitializer newInit = new GlowstoneChannelInitializer(oldInit);

            ReflectionUtil.set(bootstrapAcceptor, "childHandler", newInit);
        } catch (NoSuchFieldException ignored) {
            throw new Exception("Unable to find core component 'childHandler', please check your plugins. issue: " + bootstrapAcceptor.getClass().getName());
        }
    }

    @Override
    public void uninject() {
        //TODO uninject channel
        try {
            Field bootstrapField = GlowSocketServer.class.getDeclaredField("bootstrap");
            bootstrapField.setAccessible(true);

            GlowSocketServer server = ((GlowServer) Bukkit.getServer()).getNetworkServer();
            ServerBootstrap bootstrap = (ServerBootstrap) bootstrapField.get(server);
            Field childHandlerField = ServerBootstrap.class.getDeclaredField("childHandler");
            childHandlerField.setAccessible(true);
            childHandlerField.set(bootstrap, original);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getServerProtocolVersion() throws Exception {
        return GlowServer.PROTOCOL_VERSION;
    }

    @Override
    public String getEncoderName() {
        return "codecs";
    }

    @Override
    public String getDecoderName() {
        return "codecs";
    }

    @Override
    public JsonObject getDump() {
        JsonObject data = new JsonObject();
        //TODO
        return data;
    }
}
