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

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.queerbric.inspecio.Inspecio;
import io.github.queerbric.inspecio.SignTooltipMode;
import net.minecraft.block.AbstractSignBlock;
import net.minecraft.block.WoodType;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.HangingSignBlockEntityRenderer;
import net.minecraft.client.render.block.entity.SignBlockEntityRenderer;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.HangingSignItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SignItem;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

public abstract class SignTooltipComponent<M extends Model> implements InspecioTooltipData, TooltipComponent {
	protected static final MinecraftClient CLIENT = MinecraftClient.getInstance();
	private final SignTooltipMode tooltipMode = Inspecio.getConfig().getSignTooltipMode();
	protected final WoodType type;
	private final SignText front;
	private final SignText back;
	protected final M model;

	public SignTooltipComponent(WoodType type, SignText front, SignText back, M model) {
		this.type = type;
		this.front = front;
		this.back = back;
		this.model = model;
	}

	public static Optional<TooltipData> fromItemStack(ItemStack stack) {
		if (!Inspecio.getConfig().getSignTooltipMode().isEnabled())
			return Optional.empty();

		if (stack.getItem() instanceof SignItem signItem) {
			var block = signItem.getBlock();
			var nbt = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);
			if (nbt != null)
				return Optional.ofNullable(fromTag(AbstractSignBlock.getWoodType(block), nbt.copyNbt(), stack.getItem() instanceof HangingSignItem));
		}
		return Optional.empty();
	}

	public static SignTooltipComponent<?> fromTag(WoodType type, NbtCompound nbt, boolean hanging) {
		Optional<SignText> front = Optional.empty();
		Optional<SignText> back = Optional.empty();

		if (nbt.contains("front_text")) {
			front = SignText.CODEC
					.parse(NbtOps.INSTANCE, nbt.getCompound("front_text"))
					.resultOrPartial(s -> {})
					.map(SignTooltipComponent::parseLines);
		}

		if (nbt.contains("back_text")) {
			back = SignText.CODEC
					.parse(NbtOps.INSTANCE, nbt.getCompound("back_text"))
					.resultOrPartial(s -> {})
					.map(SignTooltipComponent::parseLines);
		}

		if (front.isEmpty() && back.isEmpty()) {
			return null;
		} else if (hanging) {
			return new HangingSign(type, front.orElse(null), back.orElse(null));
		} else {
			return new Sign(type, front.orElse(null), back.orElse(null));
		}
	}

	private static SignText parseLines(SignText text) {
		for (int line = 0; line < 4; line++) {
			Text unfilteredMessage = text.getMessage(line, false);
			Text filteredMessage = text.getMessage(line, true);
			text = text.withMessage(line, unfilteredMessage, filteredMessage);
		}

		return text;
	}

	@Override
	public TooltipComponent toComponent() {
		return this;
	}

	protected boolean shouldShowBack() {
		return this.front == null || (this.back != null && Screen.hasControlDown());
	}

	private SignText getText() {
		if (this.shouldShowBack()) return this.back;
		else return this.front;
	}

	private Text[] getMessages() {
		return this.getText().getMessages(MinecraftClient.getInstance().shouldFilterText());
	}

	private OrderedText[] getOrderedMessages() {
		return this.getText().getOrderedMessages(MinecraftClient.getInstance().shouldFilterText(), Text::asOrderedText);
	}

	@Override
	public int getHeight() {
		if (this.tooltipMode == SignTooltipMode.FANCY)
			return this.getFancyHeight();
		return this.getMessages().length * 10;
	}

	protected abstract int getFancyHeight();

	@Override
	public int getWidth(TextRenderer textRenderer) {
		if (this.tooltipMode == SignTooltipMode.FANCY)
			return this.getFancyWidth();
		return Arrays.stream(this.getMessages()).map(textRenderer::getWidth).max(Comparator.naturalOrder()).orElse(94);
	}

	protected abstract int getFancyWidth();

	@Override
	public void drawText(TextRenderer textRenderer, int x, int y, Matrix4f matrix4f, VertexConsumerProvider.Immediate immediate) {
		if (this.tooltipMode != SignTooltipMode.FAST)
			return;

		this.drawTextAt(textRenderer, x, y, matrix4f, immediate, false);
	}

	public void drawTextAt(TextRenderer textRenderer, int x, int y, Matrix4f matrix4f, VertexConsumerProvider.Immediate immediate, boolean center) {
		int signColor = this.getText().getColor().getSignColor();
		var messages = this.getOrderedMessages();

		if (this.getText().isGlowing()) {
			int outlineColor;
			if (this.getText().getColor() == DyeColor.BLACK) {
				outlineColor = -988212;
			} else {
				int r = (int) (((signColor >> 24) & 255) * 0.4);
				int g = (int) (((signColor >> 16) & 255) * 0.4);
				int b = (int) (((signColor >> 8) & 255) * 0.4);

				outlineColor = (b >> 8) | (g >> 16) | (r >> 24);
			}

			for (int i = 0; i < messages.length; i++) {
				var text = messages[i];
				float textX = center ? (45 - textRenderer.getWidth(text) / 2.f) : x;
				textRenderer.drawWithOutline(text, textX, y + i * 10, signColor, outlineColor, matrix4f, immediate,
						LightmapTextureManager.MAX_LIGHT_COORDINATE
				);
			}
		} else {
			if (!center && this.getText().getColor() == DyeColor.BLACK) {
				signColor = 0xffffffff;
			}

			for (int i = 0; i < messages.length; i++) {
				var text = messages[i];
				float textX = center ? (45 - textRenderer.getWidth(text) / 2.f) : x;
				textRenderer.draw(
						text, textX, y + i * 10, signColor, false, matrix4f, immediate, TextRenderer.TextLayerType.NORMAL,
						0, LightmapTextureManager.MAX_LIGHT_COORDINATE
				);
			}
		}
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int x, int y, DrawContext graphics) {
		if (this.tooltipMode != SignTooltipMode.FANCY)
			return;

		DiffuseLighting.disableGuiDepthLighting();
		MatrixStack matrices = graphics.getMatrices();
		matrices.push();
		matrices.translate(x + 2, y, 0);

		matrices.push();
		var immediate = CLIENT.getBufferBuilders().getEntityVertexConsumers();
		var spriteIdentifier = this.getSignTextureId();
		var vertexConsumer = spriteIdentifier != null ? spriteIdentifier.getVertexConsumer(immediate, this.model::getLayer) : null;
		this.renderModel(graphics, vertexConsumer);
		immediate.draw();
		matrices.pop();

		matrices.translate(0, this.getTextOffset(), 10);

		var messages = this.getOrderedMessages();
		for (int i = 0; i < messages.length; i++) {
			var text = messages[i];
			graphics.drawText(textRenderer, text, (int) (45 - textRenderer.getWidth(text) / 2.f), i * 10,
					this.getText().getColor().getSignColor(), false
			);
		}
		matrices.pop();

		DiffuseLighting.enableGuiDepthLighting();
	}

	public abstract SpriteIdentifier getSignTextureId();

	public abstract void renderModel(DrawContext graphics, VertexConsumer vertexConsumer);

	/**
	 * {@return the vertical offset between the start of the component and where the text lines should be drawn}
	 */
	protected abstract int getTextOffset();

	public static class Sign extends SignTooltipComponent<SignBlockEntityRenderer.SignModel> {

		public Sign(WoodType type, SignText front, SignText back) {
			super(type, front, back, SignBlockEntityRenderer.createSignModel(CLIENT.getEntityModelLoader(), type));
		}

		@Override
		protected int getFancyHeight() {
			return 52;
		}

		@Override
		protected int getFancyWidth() {
			return 94;
		}

		@Override
		public SpriteIdentifier getSignTextureId() {
			return TexturedRenderLayers.getSignTextureId(this.type);
		}

		@Override
		public void renderModel(DrawContext graphics, VertexConsumer vertexConsumer) {
			graphics.getMatrices().translate(45, 56, 0);

			if (this.shouldShowBack()) {
				graphics.getMatrices().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
			}

			graphics.getMatrices().scale(65, 65, -65);
			this.model.stick.visible = false;
			this.model.root.visible = true;
			this.model.root.render(graphics.getMatrices(), vertexConsumer, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
		}

		@Override
		protected int getTextOffset() {
			return 4;
		}
	}

	public static class HangingSign extends SignTooltipComponent<HangingSignBlockEntityRenderer.HangingSignModel> {
		private final Identifier textureId = Identifier.ofVanilla("textures/gui/hanging_signs/" + this.type.name() + ".png");

		public HangingSign(WoodType type, SignText front, SignText back) {
			super(type, front, back, null);
		}

		@Override
		protected int getFancyHeight() {
			return 68;
		}

		@Override
		protected int getFancyWidth() {
			return 94;
		}

		@Override
		public SpriteIdentifier getSignTextureId() {
			return null;
		}

		@Override
		public void renderModel(DrawContext graphics, VertexConsumer vertexConsumer) {
			graphics.getMatrices().translate(44.5, 32, 0);
			RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);
			graphics.getMatrices().scale(4.f, 4.f, 1.f);
			graphics.drawTexture(this.textureId, -8, -8, 0.f, 0.f, 16, 16, 16, 16);
		}

		@Override
		protected int getTextOffset() {
			return 26;
		}
	}
}
