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

import com.google.common.collect.ImmutableList;
import io.github.queerbric.inspecio.Inspecio;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.DyeColor;

import java.util.Optional;

public class BannerTooltipComponent implements InspecioTooltipData, TooltipComponent {
	private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
	private final BannerPatternsComponent pattern;
	private final ModelPart bannerField;

	private BannerTooltipComponent(BannerPatternsComponent pattern) {
		this.pattern = pattern;
		this.bannerField = CLIENT.getEntityModelLoader().getModelPart(EntityModelLayers.BANNER).getChild("flag");
	}

	public static Optional<TooltipData> of(TagKey<BannerPattern> pattern) {
		if (!Inspecio.getConfig().hasBannerPattern())
			return Optional.empty();

        assert CLIENT.world != null;
        var patternList = CLIENT.world.getRegistryManager().getOrThrow(RegistryKeys.BANNER_PATTERN)
				.getOptional(pattern).map(ImmutableList::copyOf).orElse(ImmutableList.of());
		var patterns = new BannerPatternsComponent.Builder();

		for (var p : patternList) {
			patterns.add(p, DyeColor.WHITE);
		}

		return Optional.of(new BannerTooltipComponent(patterns.build()));
	}

	@Override
	public TooltipComponent toComponent() {
		return this;
	}

	@Override
	public int getHeight(TextRenderer textRenderer) {
		return 32;
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		return 16;
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
		DiffuseLighting.disableGuiDepthLighting();
		MatrixStack matrices = context.getMatrices();
		matrices.push();
		matrices.translate(x + 8, y + 8, 0);
		matrices.push();
		matrices.translate(0.5, 16, 0);
		matrices.scale(6, -6, 1);
		matrices.scale(2, -2, -2);
		var immediate = CLIENT.getBufferBuilders().getEntityVertexConsumers();
		this.bannerField.pitch = 0.f;
		this.bannerField.pivotY = -32.f;
		BannerBlockEntityRenderer.renderCanvas(matrices, immediate, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV,
				this.bannerField, ModelBaker.BANNER_BASE, true, DyeColor.GRAY, pattern);
		matrices.pop();
		immediate.draw();
		matrices.pop();
		DiffuseLighting.enableGuiDepthLighting();
	}
}
