package com.yogpc.qp.tile;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import scala.runtime.AbstractFunction0;
import static org.junit.jupiter.api.Assertions.*;

class PlayerInvCopyTest {
    @BeforeAll static void bootstrap() { Bootstrap.register(); }

    @Test void replacementStackIsSavedAndTemporaryInventoryIsCleared() {
        var placer = new InventoryBasic("placer", false, 9);
        placer.setInventorySlotContents(3, new ItemStack(Items.WATER_BUCKET));
        var inventory = new InventoryPlayer(null);
        var copy = new PlayerInvCopy(inventory, stack -> fail("Unexpected overflow"));

        String result = copy.withItems(placer, new AbstractFunction0<String>() {
            @Override public String apply() {
                inventory.currentItem = 3;
                inventory.mainInventory.set(3, new ItemStack(Items.BUCKET));
                return "used";
            }
        });

        assertEquals("used", result);
        assertEquals(Items.BUCKET, placer.getStackInSlot(3).getItem());
        assertTrue(inventory.isEmpty());
        assertEquals(0, inventory.currentItem);
    }

    @Test void consumedStackAndExtraHotbarItemsAreSavedAfterFailure() {
        var placer = new InventoryBasic("placer", false, 9);
        placer.setInventorySlotContents(2, new ItemStack(Items.APPLE));
        var inventory = new InventoryPlayer(null);
        var copy = new PlayerInvCopy(inventory, stack -> fail("Unexpected overflow"));
        var failure = new IllegalStateException("item callback failed");

        assertSame(failure, assertThrows(IllegalStateException.class, () ->
                copy.withItems(placer, new AbstractFunction0<String>() {
                    @Override public String apply() {
                        inventory.currentItem = 2;
                        inventory.mainInventory.set(2, ItemStack.EMPTY);
                        inventory.mainInventory.set(7, new ItemStack(Items.DIAMOND));
                        throw failure;
                    }
                })));

        assertTrue(placer.getStackInSlot(2).isEmpty());
        assertEquals(Items.DIAMOND, placer.getStackInSlot(7).getItem());
        assertTrue(inventory.isEmpty());
        assertEquals(0, inventory.currentItem);
    }

    @Test void savingDoesNotLoseUnchangedStacksOrTheirInPlaceChanges() {
        var placer = new InventoryBasic("placer", false, 9);
        placer.setInventorySlotContents(0, new ItemStack(Items.APPLE, 8));
        placer.setInventorySlotContents(8, new ItemStack(Items.DIAMOND, 4));
        var inventory = new InventoryPlayer(null);

        new PlayerInvCopy(inventory, stack -> fail("Unexpected overflow")).withItems(placer, new AbstractFunction0<String>() {
            @Override public String apply() {
                inventory.mainInventory.get(0).shrink(1);
                return "used";
            }
        });

        assertEquals(7, placer.getStackInSlot(0).getCount());
        assertEquals(4, placer.getStackInSlot(8).getCount());
        assertTrue(inventory.isEmpty());
    }

    @Test void callbackItemsOutsideHotbarAreReturnedBeforeCleanup() {
        var placer = new InventoryBasic("placer", false, 9);
        for (int slot = 0; slot < 9; slot++) placer.setInventorySlotContents(slot, new ItemStack(Items.APPLE, 64));
        var inventory = new InventoryPlayer(null);
        var overflow = new java.util.ArrayList<ItemStack>();

        new PlayerInvCopy(inventory, overflow::add).withItems(placer, new AbstractFunction0<String>() {
            @Override public String apply() {
                inventory.mainInventory.set(9, new ItemStack(Items.BUCKET));
                inventory.offHandInventory.set(0, new ItemStack(Items.DIAMOND));
                return "used";
            }
        });

        assertEquals(2, overflow.size());
        assertEquals(Items.BUCKET, overflow.get(0).getItem());
        assertEquals(Items.DIAMOND, overflow.get(1).getItem());
        assertEquals(64, placer.getStackInSlot(0).getCount());
        assertTrue(inventory.isEmpty());
    }
}
