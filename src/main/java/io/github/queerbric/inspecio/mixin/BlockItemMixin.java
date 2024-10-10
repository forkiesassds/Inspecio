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

package io.github.queerbric.inspecio.mixin;

import io.github.queerbric.inspecio.Inspecio;
import io.github.queerbric.inspecio.InspecioConfig;
import io.github.queerbric.inspecio.api.InventoryProvider;
import io.github.queerbric.inspecio.tooltip.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
@Mixin(BlockItem.class)
public abstract class BlockItemMixin extends Item {
	@Shadow
	public abstract Block getBlock();

	public BlockItemMixin(Settings settings) {
		super(settings);
	}

	@Override
	public Optional<TooltipData> getTooltipData(ItemStack stack) {
		var inspecioConfig = Inspecio.getConfig();
		var containersConfig = inspecioConfig.getContainersConfig();
		var effectsConfig = inspecioConfig.getEffectsConfig();

		if (effectsConfig.hasBeacon() && this.getBlock() instanceof BeaconBlock) {
			var blockEntityTag = stack.getOrDefault(DataComponentTypes.BLOCK_ENTITY_DATA, NbtComponent.DEFAULT);
			var effectsList = new ArrayList<StatusEffectInstance>();
			var primary = Inspecio.getRawEffectFromTag(blockEntityTag, "primary_effect");
			var secondary = Inspecio.getRawEffectFromTag(blockEntityTag, "secondary_effect");

			if (primary != null && primary.equals(secondary)) {
				primary = new StatusEffectInstance(primary.getEffectType(), 200, 1);
				secondary = null;
			}
			if (primary != null)
				effectsList.add(primary);
			if (secondary != null)
				effectsList.add(secondary);

			return Optional.of(new StatusEffectTooltipComponent(effectsList, 1F));
		} else if (this.getBlock() instanceof BeehiveBlock) {
			var data = BeesTooltipComponent.of(stack);
			if (data.isPresent()) return data;
		} else if (this.getBlock() instanceof CampfireBlock) {
			var data = CampfireTooltipComponent.of(stack);
			if (data.isPresent()) return data;
		} else if (this.getBlock() instanceof JukeboxBlock) {
			var data = JukeboxTooltipComponent.of(stack);
			if (data.isPresent()) return data;
		} else if (this.getBlock() instanceof SpawnerBlock) {
			var data = SpawnEntityTooltipComponent.ofMobSpawner(stack);
			if (data.isPresent()) return data;
		} else if (this.getBlock() instanceof ChiseledBookshelfBlock) {
			var data = ChiseledBookshelfTooltipComponent.of(stack);
			if (data.isPresent()) return data;
		} else if (this.getBlock() instanceof CrafterBlock) {
			var data = CrafterTooltipComponent.of(stack);
			if (data.isPresent()) return data;
		} else {
			InspecioConfig.StorageContainerConfig config = containersConfig.forBlock(this.getBlock());
			InventoryProvider.Context context = InventoryProvider.searchInventoryContextOf(stack, config);

			if (config == null) {
				config = containersConfig.getStorageConfig();
			}

			if (context != null) {
				return InventoryTooltipComponent.of(stack, config.isCompact(), context);
			}
		}

		return super.getTooltipData(stack);
	}

	@Inject(method = "appendTooltip", at = @At("HEAD"), cancellable = true)
	private void onAppendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type, CallbackInfo ci) {
		var config = Inspecio.getConfig().getEntitiesConfig();

		if (this.getBlock() instanceof ShulkerBoxBlock && !Screen.hasControlDown()) {
			Inspecio.appendBlockItemTooltip(stack, this.getBlock(), tooltip);
			ci.cancel();
		}

		if (this.getBlock() instanceof BeehiveBlock && config.getBeeConfig().isEnabled() &&
				config.getBeeConfig().shouldShowHoney()) {
			ci.cancel();
		}
	}

	@Inject(method = "appendTooltip", at = @At("TAIL"))
	private void onAppendTooltipEnd(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type, CallbackInfo ci) {
		Inspecio.appendBlockItemTooltip(stack, this.getBlock(), tooltip);
	}
}
