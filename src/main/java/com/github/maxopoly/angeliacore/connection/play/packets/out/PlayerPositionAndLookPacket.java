package com.github.maxopoly.angeliacore.connection.play.packets.out;

import com.github.maxopoly.angeliacore.binary.WriteOnlyPacket;

import com.github.maxopoly.angeliacore.model.location.Location;
import java.io.IOException;

public class PlayerPositionAndLookPacket extends WriteOnlyPacket {

	public PlayerPositionAndLookPacket(double x, double y, double z, float yaw, float pitch, boolean onGround)
			throws IOException {
		super(0x0E);
		writeDouble(x);
		writeDouble(y);
		writeDouble(z);
		writeFloat(yaw);
		writeFloat(pitch);
		// this bool specifies whether the client is on the ground or not
		writeBoolean(onGround);
	}

	public PlayerPositionAndLookPacket(Location loc, boolean onGround) throws IOException {
		this(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), onGround);
	}

}
