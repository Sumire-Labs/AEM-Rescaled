package com.yogpc.qp.tile;

import java.util.HashSet;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QuarryFluidPersistenceTest {
    @BeforeAll static void bootstrap() { Bootstrap.register(); }

    private static FluidStack variant(String name) {
        var stack = new FluidStack(FluidRegistry.WATER, 1000);
        stack.tag = new NBTTagCompound();
        stack.tag.setString("variant", name);
        return stack;
    }

    @Test void taggedVariantsRemainDistinctAfterSaveAndLoad() {
        var original = new QuarryStorage();
        original.insertFluid(variant("first"), 2000);
        original.insertFluid(variant("second"), 3000);

        var restored = new QuarryStorage();
        restored.deserializeNBT(original.serializeNBT());
        var fluids = restored.serializeNBT().getTagList("fluids", 10);

        assertEquals(2, fluids.tagCount());
        var variants = new HashSet<String>();
        long amount = 0;
        for (int i = 0; i < fluids.tagCount(); i++) {
            var entry = fluids.getCompoundTagAt(i);
            variants.add(entry.getCompoundTag("tag").getString("variant"));
            amount += entry.getLong("amount");
        }
        assertEquals(java.util.Set.of("first", "second"), variants);
        assertEquals(5000, amount);
    }

    @Test void legacyUntaggedStorageKeepsItsLongQuantity() {
        var legacy = new NBTTagCompound();
        var fluids = new NBTTagList();
        var water = new NBTTagCompound();
        water.setString("name", "water");
        water.setLong("amount", (long) Integer.MAX_VALUE + 1000);
        fluids.appendTag(water);
        legacy.setTag("fluids", fluids);

        var restored = new QuarryStorage();
        restored.deserializeNBT(legacy);
        var saved = restored.serializeNBT().getTagList("fluids", 10).getCompoundTagAt(0);
        assertEquals("water", saved.getString("name"));
        assertEquals((long) Integer.MAX_VALUE + 1000, saved.getLong("amount"));
        assertFalse(saved.hasKey("tag"));
    }

    @Test void savedTagDoesNotShareMutableFluidData() {
        var storage = new QuarryStorage();
        var fluid = variant("original");
        storage.insertFluid(fluid, 1000);
        var saved = storage.serializeNBT();
        fluid.tag.setString("variant", "changed");
        assertEquals("original", saved.getTagList("fluids", 10).getCompoundTagAt(0)
                .getCompoundTag("tag").getString("variant"));
    }
}
