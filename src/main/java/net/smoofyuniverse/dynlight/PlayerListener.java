/*
 * Copyright (c) 2018 Hugo Dupanloup (Yeregorix)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.smoofyuniverse.dynlight;

import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerListener {
	private final Map<UUID, Vector3i> fakeBlocks = new HashMap<>();

	@Listener(order = Order.LATE)
	public void onPlayerChangeHeld(ChangeInventoryEvent.Held e, @Root Player p) {
		update(p, p.getLocation());
	}

	@Listener(order = Order.LATE)
	public void onPlayerSwapHand(ChangeInventoryEvent.SwapHand e, @Root Player p) {
		update(p, p.getLocation());
	}

	private void update(Player p, Location<World> loc) {
		Vector3i lastPos = this.fakeBlocks.get(p.getUniqueId()), newPos = null;
		Config.Immutable cfg = DynLight.get().getConfig();

		ItemType hand1 = p.getItemInHand(HandTypes.MAIN_HAND).map(ItemStack::getType).orElse(ItemTypes.NONE);
		ItemType hand2 = p.getItemInHand(HandTypes.OFF_HAND).map(ItemStack::getType).orElse(ItemTypes.NONE);

		BlockType fakeBlock = cfg.getApplicableType(hand1).orElse(null);
		if (fakeBlock == null || !p.hasPermission("dynlight.item." + hand1.getId())) {
			fakeBlock = cfg.getApplicableType(hand2).orElse(null);
			if (fakeBlock != null && !p.hasPermission("dynlight.item." + hand2.getId()))
				fakeBlock = null;
		}

		if (fakeBlock != null) {
			World w = loc.getExtent();
			newPos = loc.getPosition().toInt();

			BlockType type = w.getBlockType(newPos);
			if (!cfg.canBeReplaced(type)) {
				newPos = newPos.add(0, 1, 0);
				type = w.getBlockType(newPos);
				if (!cfg.canBeReplaced(type))
					newPos = null;
			}
		}

		if (newPos == null)
			this.fakeBlocks.remove(p.getUniqueId());
		else {
			this.fakeBlocks.put(p.getUniqueId(), newPos);
			p.sendBlockChange(newPos, fakeBlock.getDefaultState());
		}

		if (lastPos != null && !lastPos.equals(newPos))
			p.resetBlockChange(lastPos);
	}

	@Listener(order = Order.LATE)
	public void onPlayerMove(MoveEntityEvent e, @Getter("getTargetEntity") Player p) {
		Transform<World> to = e.getToTransform();

		if (!e.getFromTransform().getPosition().toInt().equals(to.getPosition().toInt()))
			update(p, to.getLocation());
	}
}
