package com.gmail.berndivader.mythicmobsext.volatilecode.v1_18_R2;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;

public interface IPacketReceivingHandler {
	Packet<?> handle(ServerboundSwingPacket packet);

	Packet<?> handle(ServerboundResourcePackPacket packet);

	Packet<?> handle(ServerboundMovePlayerPacket.Pos packet);

	Packet<?> handle(ServerboundMovePlayerPacket packet);

	Packet<?> handle(ServerboundPlayerInputPacket packet);

	Packet<?> handle(ServerboundPlayerActionPacket packet);
}
