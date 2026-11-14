package com.yogpc.qp.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

final class InjectorDiscovery {
    private InjectorDiscovery() {}

    static List<IInjector> find(ItemStack stack, Function<EnumFacing, TileEntity> neighbors, List<IInjector> adapters) {
        List<IInjector> result = new ArrayList<>();
        for (EnumFacing facing : EnumFacing.VALUES) {
            TileEntity tile = neighbors.apply(facing);
            if (tile == null) continue;
            for (IInjector adapter : adapters) {
                try (var candidates = adapter.getInjector(stack, tile, facing)) {
                    if (candidates != null) candidates.sequential().forEachOrdered(result::add);
                }
            }
        }
        return result;
    }
}
