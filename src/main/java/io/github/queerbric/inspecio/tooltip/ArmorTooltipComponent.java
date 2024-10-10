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
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.Optional;

public class ArmorTooltipComponent implements InspecioTooltipData, TooltipComponent {
	private static final Identifier ARMOR_FULL_TEXTURE = Identifier.ofVanilla("hud/armor_full");
	private static final Identifier ARMOR_HALF_TEXTURE = Identifier.ofVanilla("hud/armor_half");

	private final int prot;

	public ArmorTooltipComponent(int prot) {
		this.prot = prot;
	}

	public static Optional<ArmorTooltipComponent> of(ItemStack stack) {
		if (stack.contains(DataComponentTypes.ATTRIBUTE_MODIFIERS) && Inspecio.getConfig().hasArmor()) {
			var comp = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            assert comp != null;

            var modifiers = comp.modifiers();
			for (var modifier : modifiers) {
				var m = modifier.modifier();
				if (!m.id().getPath().startsWith("armor."))
					continue;;

				double prot = m.value();

				if (comp.showInTooltip()) {
					return Optional.of(new ArmorTooltipComponent((int) prot));
				}
			}
		}

		return Optional.empty();
	}

	@Override
	public TooltipComponent toComponent() {
		return this;
	}

	@Override
	public int getHeight(TextRenderer textRenderer) {
		return 11;
	}

	@Override
	public int getWidth(TextRenderer textRenderer) {
		return this.prot / 2 * 9;
	}

	@Override
	public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext graphics) {
		for (int i = 0; i < this.prot / 2; i++) {
			graphics.drawGuiTexture(RenderLayer::getGuiTextured, ARMOR_FULL_TEXTURE, x + i * 9, y, 9, 9);
		}
		if (this.prot % 2 == 1) {
			graphics.drawGuiTexture(RenderLayer::getGuiTextured, ARMOR_HALF_TEXTURE, x + this.prot / 2 * 9, y, 9, 9);
		}
	}
}
