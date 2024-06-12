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
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipData;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Represents a tooltip component which displays bees from a beehive.
 *
 * @author LambdAurora
 * @version 1.8.0
 * @since 1.0.0
 */
public class BeesTooltipComponent extends EntityTooltipComponent<InspecioConfig.BeeEntityConfig> {
	private static final Identifier HONEY_BAR_TEXTURE = new Identifier(Inspecio.NAMESPACE, "tooltips/honey_bar");
	private static final Identifier HONEY_LEVEL_TEXTURE = new Identifier(Inspecio.NAMESPACE, "tooltips/honey_level");

	private final List<Bee> bees = new ArrayList<>();
	private final int honeyLevel;

	public BeesTooltipComponent(InspecioConfig.BeeEntityConfig config, int honeyLevel, List<BeehiveBlockEntity.BeeData> bees) {
		super(config);
		this.honeyLevel = honeyLevel;

		bees.forEach(beeData -> {
			var bee = beeData.entityData().copyNbt();
			bee.remove("UUID");
			bee.remove("Passengers");
			bee.remove("Leash");
			var entity = EntityType.loadEntityWithPassengers(bee, this.client.world, Function.identity());
			if (entity != null) {
				this.bees.add(new Bee(beeData.ticksInHive(), entity));
			}
		});
	}

	public static Optional<TooltipData> of(ItemStack stack) {
		var config = Inspecio.getConfig().getEntitiesConfig().getBeeConfig();
		if (!config.isEnabled() && !config.shouldShowHoney())
			return Optional.empty();

		int honeyLevel = 0;

		var stateNbt = stack.get(DataComponentTypes.BLOCK_STATE);
		if (stateNbt != null) {
			Integer level = stateNbt.getValue(BeehiveBlock.HONEY_LEVEL);

			if (level != null)
				honeyLevel = level;
		}

		var nbt = stack.get(DataComponentTypes.BEES);
		if (nbt == null && !config.shouldShowHoney())
			return Optional.empty();

		if (config.shouldShowHoney())
			return Optional.of(new BeesTooltipComponent(config, honeyLevel, nbt));

		return Optional.empty();
	}

	@Override
	public int getHeight() {
		if (this.bees.isEmpty()) {
			return this.config.shouldShowHoney() ? 12 : 0;
		} else {
			return (this.shouldRenderCustomNames() ? 32 : 24) + (this.config.shouldShowHoney() ? 16 : 0);
		}
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		return Math.max(this.bees.size() * 26, (this.config.shouldShowHoney() ? 52 : 0));
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int x, int y, DrawContext graphics) {
		MatrixStack matrices = graphics.getMatrices();
		matrices.push();

		if (!this.bees.isEmpty()) {
			matrices.translate(2, 4, 0);

			int xOffset = x;
			for (var bee : this.bees) {
				this.renderEntity(matrices, xOffset, y + (this.shouldRenderCustomNames() ? 8 : 0), bee.bee(), bee.ticksInHive(),
						this.config.shouldSpin(), true);
				xOffset += 26;
			}
		}

		if (config.shouldShowHoney()) {
			matrices.translate(x, y + (this.bees.isEmpty() ? 0 : (this.shouldRenderCustomNames() ? 32 : 24)), 0);
			matrices.scale(2, 2, 1);

			graphics.drawGuiTexture(HONEY_BAR_TEXTURE, 0, 0, 26, 5);

			if (honeyLevel != 0) {
				graphics.drawGuiTexture(HONEY_LEVEL_TEXTURE, 24, 5, 0, 0, 1, 1, Math.min(24, honeyLevel * 5), 5);
			}
		}

		matrices.pop();
	}

	@Override
	protected boolean shouldRender() {
		return !this.bees.isEmpty();
	}

	@Override
	protected boolean shouldRenderCustomNames() {
		return this.bees.stream().map(bee -> bee.bee().hasCustomName()).reduce(false, (first, second) -> first || second)
				&& (this.config.shouldAlwaysShowName() || Screen.hasControlDown());
	}

	record Bee(int ticksInHive, Entity bee) {
	}
}
