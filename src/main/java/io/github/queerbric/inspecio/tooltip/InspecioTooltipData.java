package io.github.queerbric.inspecio.tooltip;

import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.tooltip.TooltipData;

/**
 * Convertible {@link TooltipData}. In use with an implementation of {@link TooltipComponent} to be able to convert between types.
 */
public interface InspecioTooltipData extends TooltipData {
    /**
     * Converts {@link TooltipData} to {@link TooltipComponent}
     * @return A {@link TooltipComponent} instance.
     */
    TooltipComponent toComponent();
}
