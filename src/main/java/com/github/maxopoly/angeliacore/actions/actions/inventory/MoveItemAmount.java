package com.github.maxopoly.angeliacore.actions.actions.inventory;

import com.github.maxopoly.angeliacore.connection.ServerConnection;
import com.github.maxopoly.angeliacore.model.ItemStack;
import com.github.maxopoly.angeliacore.model.Material;
import com.github.maxopoly.angeliacore.model.inventory.Inventory;

/**
 * Takes a given ItemStack and moves a partial amount of it into an empty slot
 *
 */
public class MoveItemAmount extends InventoryAction {

	private int originSlot;
	private int targetSlot;
	private byte windowID;
	private boolean done;
	private ClickInventory pickUp;
	private ClickInventory layDown;
	private ClickInventory putBack;
	private ItemStack toMove;
	private ItemStack targetStack;
	private int amount;
	private int leftToPlaceDown;
	private int fails;

	public MoveItemAmount(ServerConnection connection, byte windowID, int originSlot, int targetSlot, int amount) {
		super(connection);
		this.windowID = windowID;
		this.originSlot = originSlot;
		this.targetSlot = targetSlot;
		this.amount = amount;
		this.fails = 0;
	}

	@Override
	public void execute() {
		Inventory inv = connection.getPlayerStatus().getInventory(windowID);
		if (inv == null) {
			this.done = true;
			this.successfull = false;
			return;
		}
		if (pickUp == null) {
			// nothing was done so far, so we create the actions and begin the pickUp;
			toMove = inv.getSlot(originSlot);
			if (toMove.isEmpty() || toMove.getAmount() < amount) {
				// we could just move as many here as we can, but I like this behavior better as it means the execution either
				// completly works or it doesnt
				this.done = true;
				this.successfull = false;
				return;
			}
			this.pickUp = new ClickInventory(connection, windowID, (short) originSlot, (byte) 0, 0, toMove);
			targetStack = connection.getPlayerStatus().getInventory(windowID).getSlot(targetSlot);
			if (!targetStack.isEmpty()) {
				done = true;
				successfull = false;
				return;
			}
			this.layDown = new ClickInventory(connection, windowID, (short) targetSlot, (byte) 1, 0, new ItemStack(
					Material.EMPTY_SLOT));
			this.leftToPlaceDown = amount;
			this.pickUp.execute();
		}
		if (!pickUp.isDone()) {
			// still working on pickup
			pickUp.execute();
			return;
		}
		if (!pickUp.wasSuccessfull()) {
			// pickup failed
			done = true;
			successfull = false;
			return;
		}
		if (!layDown.isDone()) {
			// still working on laydown
			layDown.execute();
			return;
		}
		if (!layDown.wasSuccessfull()) {
			// we count up how often the laydown failed. After 32 laydown fails, we cancel the whole thing
			fails++;
			if (fails > 32) {
				done = true;
				successfull = false;
				return;
			}
			this.layDown = new ClickInventory(connection, windowID, (short) targetSlot, (byte) 1, 0, new ItemStack(
					Material.EMPTY_SLOT));
			return;
		} else {
			// succesfully laydown of a single item
			if (targetStack == null) {
				targetStack = toMove.clone();
				targetStack.setAmount(1);
			} else {
				targetStack.setAmount(targetStack.getAmount() + 1);
			}
			toMove.setAmount(toMove.getAmount() - 1);
			leftToPlaceDown--;
		}
		if (leftToPlaceDown > 0) {
			this.layDown = new ClickInventory(connection, windowID, (short) targetSlot, (byte) 1, 0, new ItemStack(
					Material.EMPTY_SLOT));
			return;
		}
		if (toMove.getAmount() > 0) {
			if (putBack == null) {
				putBack = new ClickInventory(connection, windowID, (short) originSlot, (byte) 0, 0, new ItemStack(
						Material.EMPTY_SLOT));
				putBack.execute();
				return;
			} else {
				if (!putBack.isDone()) {
					putBack.execute();
					return;
				}
				// putback done, everything worked
			}
		} else {
			toMove = new ItemStack(Material.EMPTY_SLOT);
		}
		done = true;
		successfull = true;
		// update clientside model
		inv.updateSlot(originSlot, toMove);
		inv.updateSlot(targetSlot, targetStack);
	}

	/**
	 * @return How many items are moved if the action works completly
	 */
	public int getAmountMoved() {
		return amount;
	}

	@Override
	public boolean isDone() {
		return done;
	}

}
