package com.gmail.berndivader.mythicmobsext.volatilecode.v1_17_R1;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayInArmAnimation;
import net.minecraft.network.protocol.game.PacketPlayInBlockDig;
import net.minecraft.network.protocol.game.PacketPlayInFlying;
import net.minecraft.network.protocol.game.PacketPlayInResourcePackStatus;
import org.bukkit.entity.Player;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

public class PacketReader extends PacketReceivingHandler {

	Channel channel;

	private static String str_decoder = "BlablaPacketInjector";
	public static HashMap<UUID, PacketReader> readers;

	static {
		readers = new HashMap<>();
	}

	public PacketReader(Player player) {
		super(player);
	}

	public void inject() {
		channel = entity_player.b.a.k; // playerConnection.networkManager.channel
		channel.pipeline().addAfter("decoder", str_decoder, new MessageToMessageDecoder<Packet<?>>() {

			@Override
			protected void decode(ChannelHandlerContext context, Packet<?> packet, List<Object> packets)
					throws Exception {
				switch (packet.getClass().getSimpleName()) {
				case "PacketPlayInArmAnimation":
					packet = handle((PacketPlayInArmAnimation) packet);
					break;
				case "PacketPlayInPosition":
				case "PacketPlayInPositionLook":
				case "PacketPlayInLook":
					packet = handle((PacketPlayInFlying) packet);
					break;
				case "PacketPlayInBlockDig":
					packet = handle((PacketPlayInBlockDig) packet);
					break;
				case "PacketPlayInResourcePackStatus":
					packet = handle((PacketPlayInResourcePackStatus) packet);
					break;
				}
				packets.add(packet);
			}
		});
	}

	public void uninject() {
		if (channel.pipeline().get(str_decoder) != null) {
			channel.pipeline().remove(str_decoder);
		}
	}

}
