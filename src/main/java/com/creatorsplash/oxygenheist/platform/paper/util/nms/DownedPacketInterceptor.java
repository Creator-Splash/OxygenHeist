package com.creatorsplash.oxygenheist.platform.paper.util.nms;

import com.creatorsplash.oxygenheist.platform.paper.world.DownedCrawlManager;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;

@ChannelHandler.Sharable
@RequiredArgsConstructor
public final class DownedPacketInterceptor extends ChannelOutboundHandlerAdapter {

    public static final String HANDLER_NAME = "oxygenheist_downed_pose";

    private final DownedCrawlManager crawlManager;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ClientboundSetEntityDataPacket packet
            && crawlManager.isTrackedEntity(packet.id())) {
            msg = PoseUtil.replacePoseWithSwimming(packet);
        }
        ctx.write(msg, promise);
    }
}
