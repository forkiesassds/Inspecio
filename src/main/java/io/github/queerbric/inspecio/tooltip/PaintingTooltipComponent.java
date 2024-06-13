/*
 * Copyright (c) 2023 LambdAurora <email@lambdaurora.dev>, Emi
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
import io.github.queerbric.inspecio.mixin.DecorationItemAccessor;
import io.github.queerbric.inspecio.mixin.PaintingEntityAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.item.TooltipData;
import net.minecraft.client.texture.PaintingManager;
import net.minecraft.client.texture.Sprite;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.Optional;

/**
 * Represents a painting tooltip for painting items with a known variant.
 *
 * @param painting the painting variant
 * @author LambdAurora
 * @version 1.8.0
 * @since 1.8.0
 */
@Environment(EnvType.CLIENT)
public record PaintingTooltipComponent(PaintingVariant painting) implements InspecioTooltipData, TooltipComponent {
	public static Optional<TooltipData> of(ItemStack stack) {
		if (!Inspecio.getConfig().hasPainting())
			return Optional.empty();

		NbtComponent nbt = stack.get(DataComponentTypes.ENTITY_DATA);

		if (nbt != null
				&& stack.getItem() instanceof DecorationItemAccessor decorationItem
				&& decorationItem.getEntityType() == EntityType.PAINTING
		) {
			var entityNbt = nbt.copyNbt();

			if (entityNbt != null) {
				RegistryEntry<PaintingVariant> registryEntry = PaintingEntity.VARIANT_ENTRY_CODEC.parse(NbtOps.INSTANCE, entityNbt)
						.result()
						.orElseGet(PaintingEntityAccessor::invokeGetDefaultVariant);

				return Optional.of(new PaintingTooltipComponent(registryEntry.value()));
			}
		}

		return Optional.empty();
	}

	@Override
	public TooltipComponent toComponent() {
		return this;
	}

	@Override
	public int getHeight() {
		return this.painting.getHeight();
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		return this.painting.getWidth();
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int x, int y, DrawContext graphics) {
		PaintingManager paintingManager = MinecraftClient.getInstance().getPaintingManager();
		Sprite sprite = paintingManager.getPaintingSprite(this.painting);
		graphics.drawSprite(x, y - 2, 0, this.getWidth(textRenderer), this.getHeight(), sprite);
	}
}
