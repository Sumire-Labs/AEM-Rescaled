package com.yogpc.qp.compat;

import java.util.List;
import net.minecraft.init.Bootstrap;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.*;

class FluidStoreTest {
    @BeforeAll static void bootstrap() { Bootstrap.register(); }

    @Test void largeStoredAmountWithNoReceiverDoesNotDisappear() {
        var water = new FluidStack(FluidRegistry.WATER, 1000);
        assertEquals(0, FluidStore.fillHandlers(water, (long) Integer.MAX_VALUE + 5000, List.of()));
    }

    @Test void largeStoredAmountSubtractsOnlyWhatFitsInTheReceivers() {
        var water = new FluidStack(FluidRegistry.WATER, 1000);
        var first = new FluidTank(1000);
        var second = new FluidTank(500);

        assertEquals(1500, FluidStore.fillHandlers(water, (long) Integer.MAX_VALUE + 5000, List.of(first, second)));
        assertEquals(1000, first.getFluidAmount());
        assertEquals(500, second.getFluidAmount());
        assertEquals(1000, water.amount);
    }

    @Test void countsTheCommittedTransferInsteadOfTheSimulation() {
        var receiver = new FluidTank(1000) {
            @Override public int fill(FluidStack resource, boolean doFill) {
                if (!doFill) return resource.amount;
                var accepted = resource.copy();
                accepted.amount = Math.min(250, accepted.amount);
                return super.fill(accepted, true);
            }
        };

        assertEquals(250, FluidStore.fillHandlers(new FluidStack(FluidRegistry.WATER, 1000), 1000, List.of(receiver)));
        assertEquals(250, receiver.getFluidAmount());
    }

    @Test void boundsEachTransferWithoutOverflowingLongStorage() {
        var receiver = new FluidTank(Integer.MAX_VALUE);
        var water = new FluidStack(FluidRegistry.WATER, 1000);
        assertEquals(Integer.MAX_VALUE, FluidStore.fillHandlers(water, Long.MAX_VALUE, List.of(receiver)));
        assertEquals(Integer.MAX_VALUE, receiver.getFluidAmount());
        assertEquals(0, FluidStore.fillHandlers(water, 0, List.of(receiver)));
        assertEquals(0, FluidStore.fillHandlers(water, -1, List.of(receiver)));
    }
}
