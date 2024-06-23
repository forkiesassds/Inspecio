package io.github.queerbric.inspecio.mixin;

import io.github.queerbric.inspecio.Inspecio;
import io.github.queerbric.inspecio.tooltip.StatusEffectTooltipComponent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.OminousBottleItem;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
@Mixin(OminousBottleItem.class)
public abstract class OminousBottleItemMixin extends Item {
	@Unique
	private final ThreadLocal<Integer> inspecio$oldTooltipLength = new ThreadLocal<>(); // ThreadLocal as REI workaround

	public OminousBottleItemMixin(Settings settings) {
		super(settings);
	}

	@Inject(method = "appendTooltip", at = @At("HEAD"))
	private void onAppendTooltipPre(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type, CallbackInfo ci) {
		this.inspecio$oldTooltipLength.set(tooltip.size());
	}

	@Inject(method = "appendTooltip", at = @At("RETURN"))
	private void onAppendTooltipPost(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type, CallbackInfo ci) {
		if (Inspecio.getConfig().getEffectsConfig().hasPotions()) {
			Inspecio.removeVanillaTooltips(tooltip, this.inspecio$oldTooltipLength.get());
		}
	}

	@Override
	public Optional<TooltipData> getTooltipData(ItemStack stack) {
		if (!Inspecio.getConfig().getEffectsConfig().hasPotions()) return super.getTooltipData(stack);
		int amplifier = stack.getOrDefault(DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER, 0);
		return Optional.of(new StatusEffectTooltipComponent(List.of(new StatusEffectInstance(StatusEffects.BAD_OMEN, 120000, amplifier)), 1.f));
	}
}
