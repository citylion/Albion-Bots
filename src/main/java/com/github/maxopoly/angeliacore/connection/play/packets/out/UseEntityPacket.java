package com.github.maxopoly.angeliacore.connection.play.packets.out;

import com.github.maxopoly.angeliacore.binary.WriteOnlyPacket;

import java.io.IOException;

public class UseEntityPacket extends WriteOnlyPacket {

	public UseEntityPacket(int entityID, byte type) throws IOException {
		super(0x0A);
		writeVarInt(entityID);
		writeByte(type);
	}

	public UseEntityPacket(int entityID, byte type, float targetX, float targetY, float targetZ) throws IOException {
		// why the fuck is this using floats for coords instead of doubles like everything else? wtf mojang
		super(0x0A);
		writeVarInt(entityID);
		writeByte(type);
		writeFloat(targetX);
		writeFloat(targetY);
		writeFloat(targetZ);
	}

}
