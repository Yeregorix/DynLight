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
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerListener {
	private final Map<UUID, Vector3i> fakeBlocks = new HashMap<>();

	@Listener(order = Order.LATE)
	public void onPlayerMove(MoveEntityEvent e, @Getter("getTargetEntity") Player p) {
		Transform<World> from = e.getFromTransform();
		Transform<World> to = e.getToTransform();

		if (!from.getPosition().toInt().equals(to.getPosition().toInt())) {
			Vector3i lastPos = this.fakeBlocks.get(p.getUniqueId());
			if (lastPos != null)
				p.resetBlockChange(lastPos);

			Vector3i newPos = null;
			ItemType hand = p.getItemInHand(HandTypes.MAIN_HAND).map(ItemStack::getType).orElse(ItemTypes.NONE);

			Config.Immutable cfg = DynLight.get().getConfig();
			BlockType fakeBlock = cfg.getApplicableType(hand).orElse(null);

			if (fakeBlock != null && p.hasPermission("dynlight.item." + hand.getId())) {
				World w = to.getExtent();
				newPos = to.getPosition().toInt();

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
		}
	}
}
