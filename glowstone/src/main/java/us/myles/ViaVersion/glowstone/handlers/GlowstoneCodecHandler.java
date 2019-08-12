package us.myles.ViaVersion.glowstone.handlers;

import com.flowpowered.network.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.EncoderException;
import net.glowstone.net.pipeline.CodecsHandler;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.exception.CancelException;
import us.myles.ViaVersion.handlers.ViaHandler;
import us.myles.ViaVersion.packets.Direction;
import us.myles.ViaVersion.protocols.base.ProtocolInfo;
import us.myles.ViaVersion.util.PipelineUtil;

import java.util.List;

public class GlowstoneCodecHandler extends CodecsHandler implements ViaHandler {

    private final UserConnection info;

    public GlowstoneCodecHandler(UserConnection info, CodecsHandler minecraftEncoder) {
        super(minecraftEncoder.getProtocol());
        this.info = info;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        try {
            super.write(ctx, msg, promise);
        } catch (EncoderException ignored) {
            // Without this cancelling packets will throw errors :(
        }
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, Message message, List<Object> out) throws Exception {
        try {
            super.encode(ctx, message, out);
        } catch (Exception e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
        }

        for (Object o : out) {
            if (o instanceof ByteBuf)
                transform((ByteBuf) o);
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception {
        // use transformers
        if (byteBuf.readableBytes() > 0) {
            // Ignore if pending disconnect
            if (info.isPendingDisconnect()) {
                return;
            }
            // Increment received
            boolean second = info.incrementReceived();
            // Check PPS
            if (second) {
                if (info.handlePPS())
                    return;
            }

            if (info.isActive()) {
                // Handle ID
                int id = Type.VAR_INT.read(byteBuf);
                // Transform
                ByteBuf newPacket = ctx.alloc().buffer();
                try {
                    if (id == PacketWrapper.PASSTHROUGH_ID) {
                        newPacket.writeBytes(byteBuf);
                    } else {
                        PacketWrapper wrapper = new PacketWrapper(id, byteBuf, info);
                        ProtocolInfo protInfo = info.get(ProtocolInfo.class);
                        protInfo.getPipeline().transform(Direction.INCOMING, protInfo.getState(), wrapper);
                        wrapper.writeToBuffer(newPacket);
                    }

                    byteBuf.clear();
                    byteBuf = newPacket;
                } catch (Exception e) {
                    // Clear Buffer
                    byteBuf.clear();
                    // Release Packet, be free!
                    newPacket.release();
                    throw e;
                }
            }

            // call minecraft decoder
            try {
                super.decode(ctx, byteBuf, out);
            } catch (Exception e) {
                if (e.getCause() instanceof Exception) {
                    throw (Exception) e.getCause();
                }
            } finally {
                if (info.isActive()) {
                    byteBuf.release();
                }
            }
        }
    }

    public void transform(ByteBuf byteBuf) throws Exception {
        if (byteBuf.readableBytes() == 0) {
            return;
        }
        // Increment sent
        info.incrementSent();
        if (info.isActive()) {
            // Handle ID
            int id = Type.VAR_INT.read(byteBuf);
            // Transform
            ByteBuf oldPacket = byteBuf.copy();
            byteBuf.clear();

            try {
                PacketWrapper wrapper = new PacketWrapper(id, oldPacket, info);
                ProtocolInfo protInfo = info.get(ProtocolInfo.class);
                protInfo.getPipeline().transform(Direction.OUTGOING, protInfo.getState(), wrapper);
                wrapper.writeToBuffer(byteBuf);
            } catch (Exception e) {
                byteBuf.clear();
                throw e;
            } finally {
                oldPacket.release();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (PipelineUtil.containsCause(cause, CancelException.class)) return;
        super.exceptionCaught(ctx, cause);
    }
}
