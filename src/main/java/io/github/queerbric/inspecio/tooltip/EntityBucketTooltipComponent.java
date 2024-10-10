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
import io.github.queerbric.inspecio.mixin.EntityAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.tooltip.TooltipData;

import java.util.Optional;

/**
 * Represents a tooltip component which displays bees from a beehive.
 *
 * @author LambdAurora
 * @version 1.8.0
 * @since 1.0.0
 */
public class EntityBucketTooltipComponent extends EntityTooltipComponent<InspecioConfig.EntityConfig> {
	private final Entity entity;

	private EntityBucketTooltipComponent(InspecioConfig.EntityConfig config, Entity entity) {
		super(config);
		this.entity = entity;
	}

	public static Optional<TooltipData> of(EntityType<?> type, NbtComponent itemNbt) {
		var entitiesConfig = Inspecio.getConfig().getEntitiesConfig();
		if (!entitiesConfig.getFishBucketConfig().isEnabled())
			return Optional.empty();

		var client = MinecraftClient.getInstance();
		var entity = type.create(client.world, SpawnReason.LOAD);
		if (entity != null) {
			EntityType.loadFromEntityNbt(client.world, null, entity, itemNbt);
			adjustEntity(entity, itemNbt.copyNbt(), entitiesConfig);
			return Optional.of(new EntityBucketTooltipComponent(entitiesConfig.getFishBucketConfig(), entity));
		}
		return Optional.empty();
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext graphics) {
		if (this.shouldRender()) {
			MatrixStack matrices = graphics.getMatrices();
			matrices.push();
			matrices.translate(2, 2, 0);
			((EntityAccessor) this.entity).setTouchingWater(true);
			this.entity.setVelocity(1.f, 1.f, 1.f);
			this.renderEntity(matrices, x + 16, y, this.entity, 0, this.config.shouldSpin(), false, 90.f);
			matrices.pop();
		}
	}

	@Override
	protected boolean shouldRender() {
		return this.entity != null;
	}

	@Override
	protected boolean shouldRenderCustomNames() {
		return false;
	}
}
