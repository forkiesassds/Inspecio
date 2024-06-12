package io.github.queerbric.inspecio.tooltip;

import io.github.queerbric.inspecio.Inspecio;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.item.TooltipData;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

import java.util.Optional;

public class CrafterTooltipComponent implements InspecioTooltipData, TooltipComponent {
    private static final Identifier CRAFTER_GUI_TEXTURE = new Identifier("textures/gui/container/crafter.png");
    private static final Identifier DISABLED_SLOT_TEXTURE = new Identifier("container/crafter/disabled_slot");

    private final DefaultedList<ItemStack> inventory;
    private final boolean[] disabledSlots;

    public CrafterTooltipComponent(DefaultedList<ItemStack> inventory, boolean[] disabledSlots) {
        this.inventory = inventory;
        this.disabledSlots = disabledSlots;
    }

    public static Optional<TooltipData> of(ItemStack stack) {
        var config = Inspecio.getConfig().getContainersConfig();
        if (!config.isCrafterEnabled()) {
            return Optional.empty();
        }

        var container = stack.get(DataComponentTypes.CONTAINER);
        if (container == null)
            return Optional.empty();

        var inventory = Inspecio.readInventory(container, 9);
        if (inventory == null)
            return Optional.empty();

        var nbt = stack.getOrDefault(DataComponentTypes.BLOCK_ENTITY_DATA, NbtComponent.DEFAULT);

        int[] disabledSlotArray = nbt.copyNbt().getIntArray("disabled_slots");
        boolean[] disabledSlots = new boolean[9];

        for (int i = 0; i < 9; ++i) {
            disabledSlots[i] = false;
        }

        for (int slot : disabledSlotArray) {
            if (canToggleSlot(inventory, slot)) {
                disabledSlots[slot] = true;
            }
        }

        return Optional.of(new CrafterTooltipComponent(inventory, disabledSlots));
    }

    private static boolean canToggleSlot(DefaultedList<ItemStack> inventory, int slot) {
        return slot > -1 && slot < 9 && inventory.get(slot).isEmpty();
    }

    @Override
    public TooltipComponent toComponent() {
        return this;
    }

    @Override
    public int getHeight() {
        return 18 * 3 + 3;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return 18 * 3;
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int xOffset, int yOffset, DrawContext graphics) {
        int x = 1;
        int y = 1;

        int slot = 0;

        graphics.drawTexture(CRAFTER_GUI_TEXTURE, xOffset, yOffset, 25, 16, 54, 54, 256, 256);

        for (var stack : this.inventory) {
            if (disabledSlots[slot])
                drawDisabledSlot(graphics, x + xOffset - 1, y + yOffset - 1, 0);

            graphics.drawItem(stack, xOffset + x, yOffset + y);
            graphics.drawItemInSlot(textRenderer, stack, xOffset + x, yOffset + y);
            x += 18;
            if (x >= 18 * 3) {
                x = 1;
                y += 18;
            }

            slot++;
        }
    }

    public static void drawDisabledSlot(DrawContext graphics, int x, int y, int z) {
        graphics.drawGuiTexture(DISABLED_SLOT_TEXTURE, x, y, z, 18, 18);
    }
}
