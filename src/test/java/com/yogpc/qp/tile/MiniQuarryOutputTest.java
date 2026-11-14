package com.yogpc.qp.tile;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MiniQuarryOutputTest {
    @BeforeAll static void bootstrap() { Bootstrap.register(); }

    @Test void rejectedFirstStackDoesNotBlockLaterItems() {
        var output = new ItemStackHandler(3);
        output.setStackInSlot(0, new ItemStack(Items.APPLE, 4));
        output.setStackInSlot(1, new ItemStack(Items.DIAMOND, 8));
        output.setStackInSlot(2, new ItemStack(Items.DIAMOND, 2));
        var target = new ItemStackHandler(1);
        target.setStackInSlot(0, new ItemStack(Items.DIAMOND, 56));

        TileMiniQuarry.exportOutput(output, stack -> ItemHandlerHelper.insertItemStacked(target, stack, false));

        assertEquals(4, output.getStackInSlot(0).getCount());
        assertTrue(output.getStackInSlot(1).isEmpty());
        assertEquals(2, output.getStackInSlot(2).getCount());
        assertEquals(64, target.getStackInSlot(0).getCount());
    }

    @Test void partialInsertionPreservesRemainder() {
        var output = new ItemStackHandler(1);
        output.setStackInSlot(0, new ItemStack(Items.DIAMOND, 8));
        var target = new ItemStackHandler(1);
        target.setStackInSlot(0, new ItemStack(Items.DIAMOND, 60));

        TileMiniQuarry.exportOutput(output, stack -> ItemHandlerHelper.insertItemStacked(target, stack, false));

        assertEquals(4, output.getStackInSlot(0).getCount());
        assertEquals(64, target.getStackInSlot(0).getCount());
    }

    @Test void rejectedItemsDoNotDirtyOutput() {
        int[] changes = {0};
        var output = new ItemStackHandler(1) {
            @Override protected void onContentsChanged(int slot) { changes[0]++; }
        };
        output.setStackInSlot(0, new ItemStack(Items.DIAMOND, 8));
        changes[0] = 0;

        TileMiniQuarry.exportOutput(output, ItemStack::copy);

        assertEquals(8, output.getStackInSlot(0).getCount());
        assertEquals(0, changes[0]);
    }
}
