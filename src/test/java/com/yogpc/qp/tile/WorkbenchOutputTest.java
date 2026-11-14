package com.yogpc.qp.tile;

import java.util.ArrayList;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkbenchOutputTest {
    @BeforeAll static void bootstrap() { Bootstrap.register(); }

    @Test void partialInsertionDropsOnlyTheRemainder() {
        var target = new ItemStackHandler(1);
        target.setStackInSlot(0, new ItemStack(Items.DIAMOND, 60));
        var drops = new ArrayList<ItemStack>();
        var result = new ItemStack(Items.DIAMOND, 8);

        TileWorkbench.deliverResult(result,
                stack -> ItemHandlerHelper.insertItemStacked(target, stack, false), drops::add);

        assertEquals(64, target.getStackInSlot(0).getCount());
        assertEquals(1, drops.size());
        assertEquals(4, drops.get(0).getCount());
        assertEquals(8, result.getCount());
    }

    @Test void fullInsertionDoesNotDropItems() {
        var target = new ItemStackHandler(1);
        var drops = new ArrayList<ItemStack>();

        TileWorkbench.deliverResult(new ItemStack(Items.DIAMOND, 8),
                stack -> ItemHandlerHelper.insertItemStacked(target, stack, false), drops::add);

        assertEquals(8, target.getStackInSlot(0).getCount());
        assertTrue(drops.isEmpty());
    }

    @Test void rejectedInsertionDropsTheWholeResult() {
        var target = new ItemStackHandler(1);
        target.setStackInSlot(0, new ItemStack(Items.APPLE, 64));
        var drops = new ArrayList<ItemStack>();

        TileWorkbench.deliverResult(new ItemStack(Items.DIAMOND, 8),
                stack -> ItemHandlerHelper.insertItemStacked(target, stack, false), drops::add);

        assertEquals(64, target.getStackInSlot(0).getCount());
        assertEquals(1, drops.size());
        assertEquals(Items.DIAMOND, drops.get(0).getItem());
        assertEquals(8, drops.get(0).getCount());
    }
}
