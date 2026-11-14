package com.yogpc.qp.compat;

import java.util.Arrays;
import java.util.Objects;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class FluidStore {
    public static final boolean enabled;
    public static final long AMOUNT = 1000;

    static {
        enabled = true;//ModList.get().isLoaded("fluidtank");
    }

    public static void injectToNearTile(World world, BlockPos pos, FluidStack fluid) {
        injectToNearTile(world, pos, fluid, AMOUNT);
    }

    /**
     * @return inserted amount.
     */
    public static long injectToNearTile(World world, BlockPos pos, FluidStack fluid, long amount) {
        if (!enabled) return 0;
        return injectToNearTile_internal(world, pos, fluid, amount);
    }

    private static long injectToNearTile_internal(World world, BlockPos pos, FluidStack fluid, long amount) {
        return fillHandlers(fluid, amount, () -> Arrays.stream(EnumFacing.values()).map(facing -> {
            TileEntity entity = world.getTileEntity(pos.offset(facing));
            return entity == null ? null : entity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing.getOpposite());
        }).filter(Objects::nonNull).iterator());
    }

    static long fillHandlers(FluidStack fluid, long amount, Iterable<IFluidHandler> tanks) {
        if (amount <= 0) return 0;
        int offered = (int) Math.min(amount, Integer.MAX_VALUE);
        FluidStack remainder = new FluidStack(fluid, offered);
        for (IFluidHandler tank : tanks) {
            remainder.amount -= tank.fill(remainder, true);
            if (remainder.amount <= 0) break;
        }
        return (long) offered - remainder.amount;
    }

}
