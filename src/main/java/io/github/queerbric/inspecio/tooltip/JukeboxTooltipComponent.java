/*
 * Copyright (c) 2020 LambdAurora <email@lambdaurora.dev>, Emi
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.queerbric.inspecio.tooltip;

import io.github.queerbric.inspecio.Inspecio;
import io.github.queerbric.inspecio.InspecioConfig;
import io.github.queerbric.inspecio.JukeboxTooltipMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import org.joml.Matrix4f;

import java.util.Optional;

/**
 * Represents a jukebox tooltip component. Displays the inserted disc description and an inventory slot with the disc in fancy mode.
 *
 * @author LambdAurora
 * @version 1.8.0
 * @since 1.0.0
 */
public class JukeboxTooltipComponent extends InventoryTooltipComponent {
	private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
	private final InspecioConfig config = Inspecio.getConfig();
	private final ItemStack disc;
	private final RegistryWrapper.WrapperLookup wrapperLookup;

	public JukeboxTooltipComponent(ItemStack discStack) {
		super(DefaultedList.ofSize(1, discStack), 1, null);
		wrapperLookup = CLIENT.world.getRegistryManager();

		this.disc = discStack;
	}

	public static Optional<TooltipData> of(ItemStack stack) {
		if (!Inspecio.getConfig().getJukeboxTooltipMode().isEnabled()) return Optional.empty();
		var nbt = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
		if (nbt != null && nbt.contains("RecordItem")) {
			var nbtC = nbt.copyNbt();

			var discStack = ItemStack.fromNbt(CLIENT.world.getRegistryManager(), nbtC.getCompound("RecordItem"));
			if (discStack.isPresent() && discStack.get().contains(DataComponentTypes.JUKEBOX_PLAYABLE))
				return Optional.of(new JukeboxTooltipComponent(discStack.get()));
		}
		return Optional.empty();
	}

	@Override
	public int getHeight() {
		int height = 10;
		if (this.config.getJukeboxTooltipMode() == JukeboxTooltipMode.FANCY)
			height += 20;
		return height;
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		return textRenderer.getWidth(getSongDescription());
    }

	@Override
	public void drawText(TextRenderer textRenderer, int x, int y, Matrix4f matrix4f, VertexConsumerProvider.Immediate immediate) {
		textRenderer.draw(this.getSongDescription(), x, y, 11184810, true, matrix4f, immediate, TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
	}

	public Text getSongDescription() {
		var songEntry = this.disc.get(DataComponentTypes.JUKEBOX_PLAYABLE).song().getEntry(wrapperLookup);
		return songEntry.map(s -> s.value().description()).orElse(Text.empty());
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int x, int y, DrawContext graphics) {
		if (this.config.getJukeboxTooltipMode() == JukeboxTooltipMode.FANCY)
			super.drawItems(textRenderer, x, y + 10, graphics);
	}
}
