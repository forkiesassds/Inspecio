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
import io.github.queerbric.inspecio.tooltip.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.tag.client.v1.ClientTags;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.component.type.SuspiciousStewEffectsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Shadow
	public abstract Item getItem();

	@Shadow public abstract ComponentMap getComponents();

	@Unique
	private final ThreadLocal<List<Text>> inspecio$tooltipList = new ThreadLocal<>();

	@Inject(
			method = "getTooltip",
			at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/item/ItemStack;contains(Lnet/minecraft/component/ComponentType;)Z"),
			locals = LocalCapture.CAPTURE_FAILHARD
	)
	private void onGetTooltipBeing(Item.TooltipContext context, PlayerEntity player, TooltipType type, CallbackInfoReturnable<List<Text>> cir, List<Text> list, MutableText mutableText) {
		this.inspecio$tooltipList.set(list);
	}

	@Inject(
			method = "getTooltip",
			at = @At(value = "RETURN")
	)
	private void onGetTooltip(Item.TooltipContext context, PlayerEntity player, TooltipType type, CallbackInfoReturnable<List<Text>> cir) {
		var tooltip = this.inspecio$tooltipList.get();
		InspecioConfig.AdvancedTooltipsConfig advancedTooltipsConfig = Inspecio.getConfig().getAdvancedTooltipsConfig();

		if (advancedTooltipsConfig.hasLodestoneCoords() && this.getItem() instanceof CompassItem && this.getComponents().contains(DataComponentTypes.LODESTONE_TRACKER)) {
			var nbt = this.getComponents();
			assert nbt != null; // Should not be null since hasLodestone returns true.

			LodestoneTrackerComponent lodestoneTrackerComponent = nbt.get(DataComponentTypes.LODESTONE_TRACKER);
            assert lodestoneTrackerComponent != null;
            Optional<GlobalPos> oGlobalPos = lodestoneTrackerComponent.target();

			if (oGlobalPos.isPresent()) {
				GlobalPos globalPos = oGlobalPos.get();

				BlockPos pos = globalPos.pos();
				var posText = Text.literal(String.format("X: %d, Y: %d, Z: %d", pos.getX(), pos.getY(), pos.getZ()))
						.formatted(Formatting.GOLD);

				tooltip.add(Text.translatable("inspecio.tooltip.lodestone_compass.target", posText).formatted(Formatting.GRAY));
				tooltip.add(Text.translatable("inspecio.tooltip.lodestone_compass.dimension",
								Text.literal(globalPos.dimension().getValue().toString()).formatted(Formatting.GOLD))
						.formatted(Formatting.GRAY));
			}
		}

		int repairCost;
		if (advancedTooltipsConfig.hasRepairCost() && (repairCost = this.getComponents().get(DataComponentTypes.REPAIR_COST)) != 0) {
			tooltip.add(Text.translatable("inspecio.tooltip.repair_cost", repairCost)
					.formatted(Formatting.GRAY));
		}
	}

	@Inject(method = "getTooltipData", at = @At("RETURN"), cancellable = true)
	private void getTooltipData(CallbackInfoReturnable<Optional<TooltipData>> info) {
		// Data is the plural and datum is the singular actually, but no one cares
		var datas = new ArrayList<TooltipData>();
		info.getReturnValue().ifPresent(datas::add);

		var config = Inspecio.getConfig();
		var stack = (ItemStack) (Object) this;

		if (stack.contains(DataComponentTypes.FOOD) &&
				//Ominous bottles are essentially a food item in a potions clothing.
				!stack.contains(DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER)) {
			var comp = stack.get(DataComponentTypes.FOOD);

			if (config.getFoodConfig().isEnabled()) {
                assert comp != null;

                datas.add(new FoodTooltipComponent(comp));
			}

			if (config.getEffectsConfig().hasPotions()) {
				if (ClientTags.isInWithLocalFallback(Inspecio.HIDDEN_EFFECTS_TAG, stack.getRegistryEntry())) {
					datas.add(new StatusEffectTooltipComponent());
				} else {
                    assert comp != null;

                    if (!comp.effects().isEmpty()) {
						datas.add(new StatusEffectTooltipComponent(comp.effects()));
					} else if (stack.getItem() instanceof SuspiciousStewItem) {
						var effects = new ArrayList<StatusEffectInstance>();
						SuspiciousStewEffectsComponent suspiciousStewEffectsComponent = stack.getOrDefault(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS, SuspiciousStewEffectsComponent.DEFAULT);

                        for (SuspiciousStewEffectsComponent.StewEffect stewEffect : suspiciousStewEffectsComponent.effects()) {
							effects.add(stewEffect.createStatusEffectInstance());
                        }

						if (!effects.isEmpty()) {
							datas.add(new StatusEffectTooltipComponent(effects, 1.f));
						}
					} else if (stack.contains(DataComponentTypes.POTION_CONTENTS)) {
						datas.add(new StatusEffectTooltipComponent(stack.get(DataComponentTypes.POTION_CONTENTS).getEffects(), 1.f));
					}
				}
			}
		}

		if (stack.getItem() instanceof ArmorItem) {
			ArmorTooltipComponent.of(stack).ifPresent(datas::add);
		}

		if (stack.getItem() instanceof DecorationItem) {
			PaintingTooltipComponent.of(stack).ifPresent(datas::add);
		}

		if (datas.size() == 1) {
			info.setReturnValue(Optional.of(datas.getFirst()));
		} else if (datas.size() > 1) {
			var comp = new CompoundTooltipComponent();
			for (var data : datas) {
				TooltipComponent component = TooltipComponentCallback.EVENT.invoker().getComponent(data);
				if (component != null)
					comp.addComponent(component);
			}
			info.setReturnValue(Optional.of(comp));
		}
	}
}
