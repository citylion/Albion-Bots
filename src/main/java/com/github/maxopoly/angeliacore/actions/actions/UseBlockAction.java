package com.github.maxopoly.angeliacore.actions.actions;

import com.github.maxopoly.angeliacore.actions.AbstractAction;
import com.github.maxopoly.angeliacore.connection.ServerConnection;
import com.github.maxopoly.angeliacore.connection.play.packets.out.BreakAnimationPacket;
import com.github.maxopoly.angeliacore.model.Location;
import java.io.IOException;

public class UseBlockAction extends AbstractAction {

	private Location location;

	public UseBlockAction(ServerConnection connection, Location loc) {
		super(connection);
		this.location = loc;
	}

	@Override
	public void execute() {
		try {
			new ChangeViewingDirection(connection, location).execute();
			connection.sendPacket(new BreakAnimationPacket());
		} catch (IOException e) {
			connection.getLogger().error("Failed to send block usage", e);
		}
	}

	@Override
	public boolean isDone() {
		return true; // always 1 tick
	}

}
