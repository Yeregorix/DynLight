/*
 * Copyright (c) 2018-2019 Hugo Dupanloup (Yeregorix)
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

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.property.block.PassableProperty;
import org.spongepowered.api.item.ItemType;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@ConfigSerializable
public class Config {
	public static final TypeToken<Config> TOKEN = TypeToken.of(Config.class);

	@Setting(value = "torch", comment = "Items that should emit dynamic light when in hand (using a fake torch).")
	public Set<ItemType> torch;

	@Setting(value = "redtone_torch", comment = "Items that should emit dynamic light when in hand (using a fake redstone torch).")
	public Set<ItemType> redstone_torch;

	@Setting(value = "blacklist", comment = "Blocks that should not be replaced by a torch even if passable.")
	public Set<BlockType> blacklist;

	public Immutable toImmutable() {
		return new Immutable(this.torch, this.redstone_torch, this.blacklist);
	}

	public static class Immutable {
		public final Set<ItemType> torch, redtone_torch;
		public final Set<BlockType> blacklist;

		public Immutable(Collection<ItemType> torch, Collection<ItemType> redstone_torch, Collection<BlockType> blacklist) {
			this.torch = ImmutableSet.copyOf(torch);
			this.redtone_torch = ImmutableSet.copyOf(redstone_torch);
			this.blacklist = ImmutableSet.copyOf(blacklist);
		}

		public Optional<BlockType> getApplicableType(ItemType hand) {
			if (this.torch.contains(hand))
				return Optional.of(BlockTypes.TORCH);
			if (this.redtone_torch.contains(hand))
				return Optional.of(BlockTypes.REDSTONE_TORCH);
			return Optional.empty();
		}

		public boolean canBeReplaced(BlockType type) {
			return type.getProperty(PassableProperty.class).map(PassableProperty::getValue).orElse(false) && !this.blacklist.contains(type);
		}
	}
}
