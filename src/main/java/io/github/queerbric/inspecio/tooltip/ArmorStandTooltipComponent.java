/*
 * Copyright (c) 2021 LambdAurora <email@lambdaurora.dev>, Emi
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
import io.github.queerbric.inspecio.mixin.EntityAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.tooltip.TooltipData;

import java.util.Optional;

/**
 * Represents an armor stand tooltip. Displays an armor stand and its armor.
 *
 * @author Zailer43
 * @version 1.8.0
 * @since 1.1.0
 */
public class ArmorStandTooltipComponent extends EntityTooltipComponent<InspecioConfig.EntityConfig> {
	private final Entity entity;

	public ArmorStandTooltipComponent(InspecioConfig.EntityConfig config, Entity entity) {
		super(config);
		this.entity = entity;
	}

	public static Optional<TooltipData> of(NbtComponent itemNbt) {
		var entitiesConfig = Inspecio.getConfig().getEntitiesConfig();
		var entityType = EntityType.ARMOR_STAND;
		if (!entitiesConfig.getArmorStandConfig().isEnabled())
			return Optional.empty();

		var client = MinecraftClient.getInstance();
		var entity = entityType.create(client.world, SpawnReason.LOAD);
        assert entity != null;

        itemNbt.applyToEntity(entity);
		return Optional.of(new ArmorStandTooltipComponent(entitiesConfig.getArmorStandConfig(), entity));
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext graphics) {
		if (this.shouldRender()) {
			MatrixStack matrices = graphics.getMatrices();
			matrices.push();
			matrices.translate(30, 0, 0);
			((EntityAccessor) this.entity).setTouchingWater(true);
			this.entity.setVelocity(1.f, 1.f, 1.f);
			this.renderEntity(matrices, x + 20, y + 12, this.entity, 0, this.config.shouldSpin(), true, 180.f);
			matrices.pop();
		}
	}

	@Override
	public int getHeight(TextRenderer textRenderer) {
		return super.getHeight(textRenderer) + 16;
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		return 128;
	}

	@Override
	protected boolean shouldRender() {
		return this.entity != null;
	}

	@Override
	protected boolean shouldRenderCustomNames() {
		return this.entity.hasCustomName() && (this.config.shouldAlwaysShowName() || Screen.hasControlDown());
	}
}
