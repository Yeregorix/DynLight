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

import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.data.property.block.LightEmissionProperty;
import org.spongepowered.api.data.property.block.PassableProperty;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;

@Plugin(id = "dynlight", name = "DynLight", version = "1.0.0", authors = "Yeregorix", description = "Add dynamic light without client modifications")
public final class DynLight {
	public static final int CURRENT_CONFIG_VERSION = 1, MINIMUM_CONFIG_VERSION = 1;
	public static final Logger LOGGER = LoggerFactory.getLogger("DynLight");
	private static DynLight instance;

	@Inject
	private PluginContainer container;
	@Inject
	private Game game;
	@Inject
	@DefaultConfig(sharedRoot = true)
	private ConfigurationLoader<CommentedConfigurationNode> configLoader;
	@Inject
	@DefaultConfig(sharedRoot = true)
	private Path configFile;

	private Config.Immutable config;

	public DynLight() {
		if (instance != null)
			throw new IllegalStateException();
		instance = this;
	}

	@Listener
	public void onGamePostInit(GamePostInitializationEvent e) {
		loadConfigAndListener();
	}

	private void loadConfigAndListener() {
		boolean loadListener = this.config == null;

		try {
			loadConfig();
		} catch (Exception ex) {
			LOGGER.error("Failed to load configuration", ex);
			return;
		}

		if (loadListener)
			Sponge.getGame().getEventManager().registerListeners(this, new PlayerListener());
	}

	public void loadConfig() throws IOException, ObjectMappingException {
		LOGGER.info("Loading configuration ..");

		CommentedConfigurationNode root = this.configLoader.load();
		int version = root.getNode("Version").getInt();
		if ((version > CURRENT_CONFIG_VERSION || version < MINIMUM_CONFIG_VERSION) && backupFile(this.configFile)) {
			LOGGER.info("Your config version is not supported. A new one will be generated.");
			root = this.configLoader.createEmptyNode();
		}

		ConfigurationNode cfgNode = root.getNode("Config");
		Config cfg = cfgNode.getValue(Config.TOKEN, new Config());

		if (cfg.torch == null) {
			cfg.torch = new HashSet<>();

			for (ItemType type : Sponge.getRegistry().getAllOf(ItemType.class)) {
				int v = type.getBlock().orElse(BlockTypes.AIR).getProperty(LightEmissionProperty.class).map(LightEmissionProperty::getValue).orElse(0);
				if (v > 7)
					cfg.torch.add(type);
			}
		}

		if (cfg.redtone_torch == null) {
			cfg.redtone_torch = new HashSet<>();

			for (ItemType type : Sponge.getRegistry().getAllOf(ItemType.class)) {
				int v = type.getBlock().orElse(BlockTypes.AIR).getProperty(LightEmissionProperty.class).map(LightEmissionProperty::getValue).orElse(0);
				if (v > 2 && v <= 7)
					cfg.redtone_torch.add(type);
			}
		}

		if (cfg.blacklist == null) {
			cfg.blacklist = new HashSet<>();
			Collections.addAll(cfg.blacklist, BlockTypes.WEB, BlockTypes.LADDER, BlockTypes.VINE, BlockTypes.WATER, BlockTypes.LAVA, BlockTypes.FLOWING_WATER, BlockTypes.FLOWING_LAVA);
		}

		cfg.blacklist.removeIf(type -> !type.getProperty(PassableProperty.class).map(PassableProperty::getValue).orElse(false));

		version = CURRENT_CONFIG_VERSION;
		root.getNode("Version").setValue(version);
		cfgNode.setValue(Config.TOKEN, cfg);
		this.configLoader.save(root);

		this.config = cfg.toImmutable();
	}

	public static boolean backupFile(Path file) throws IOException {
		if (!Files.exists(file))
			return false;

		String fn = file.getFileName() + ".backup";
		Path backup = null;
		for (int i = 0; i < 100; i++) {
			backup = file.resolveSibling(fn + i);
			if (!Files.exists(backup))
				break;
		}
		Files.move(file, backup);
		return true;
	}

	@Listener
	public void onGameReload(GameReloadEvent e) {
		loadConfigAndListener();
	}

	public PluginContainer getContainer() {
		return this.container;
	}

	public Config.Immutable getConfig() {
		return this.config;
	}

	public static DynLight get() {
		if (instance == null)
			throw new IllegalStateException("Instance not available");
		return instance;
	}
}
