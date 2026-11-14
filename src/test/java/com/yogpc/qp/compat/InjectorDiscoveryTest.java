package com.yogpc.qp.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InjectorDiscoveryTest {
    @BeforeAll static void bootstrap() { Bootstrap.register(); }

    private abstract static class Adapter implements IInjector {
        @Override public ItemStack inject(ItemStack stack, World world, BlockPos pos) {
            throw new AssertionError("Discovery must finish before insertion begins");
        }
    }

    @Test void discoversEveryNeighborBeforeInsertionAndKeepsIntegrationPriority() {
        var log = new ArrayList<String>();
        TileEntity tile = new net.minecraft.tileentity.TileEntityChest();
        var first = new Adapter() {
            @Override public Stream<? extends IInjector> getInjector(ItemStack stack, TileEntity target, EnumFacing side) {
                assertSame(tile, target);
                log.add("first:" + side.name());
                return Stream.of(this).onClose(() -> log.add("close-first"));
            }
        };
        var second = new Adapter() {
            @Override public Stream<? extends IInjector> getInjector(ItemStack stack, TileEntity target, EnumFacing side) {
                log.add("second:" + side.name());
                return Stream.of(this).onClose(() -> log.add("close-second"));
            }
        };
        var found = InjectorDiscovery.find(ItemStack.EMPTY, side -> {
            log.add("neighbor:" + side.name());
            return side == EnumFacing.UP || side == EnumFacing.NORTH ? tile : null;
        }, List.of(first, second));
        assertEquals(List.of(first, second, first, second), found);
        assertEquals(List.of("neighbor:DOWN", "neighbor:UP", "first:UP", "close-first", "second:UP", "close-second",
                "neighbor:NORTH", "first:NORTH", "close-first", "second:NORTH", "close-second",
                "neighbor:SOUTH", "neighbor:WEST", "neighbor:EAST"), log);
    }

    @Test void absentNeighborsDoNotProbeAdapters() {
        int[] calls = {0};
        var adapter = new Adapter() {
            @Override public Stream<? extends IInjector> getInjector(ItemStack stack, TileEntity target, EnumFacing side) {
                throw new AssertionError("No tile to probe");
            }
        };
        assertTrue(InjectorDiscovery.find(ItemStack.EMPTY, side -> { calls[0]++; return null; }, List.of(adapter)).isEmpty());
        assertEquals(6, calls[0]);
    }

    @Test void acceptsNullStreamsAndClosesParallelCandidateStreamsInEncounterOrder() {
        boolean[] closed = {false};
        var absent = new Adapter() {
            @Override public Stream<? extends IInjector> getInjector(ItemStack stack, TileEntity tile, EnumFacing side) { return null; }
        };
        var present = new Adapter() {
            @Override public Stream<? extends IInjector> getInjector(ItemStack stack, TileEntity tile, EnumFacing side) {
                return Stream.of(absent, this).parallel().onClose(() -> closed[0] = true);
            }
        };
        var tile = new net.minecraft.tileentity.TileEntityChest();
        var found = InjectorDiscovery.find(ItemStack.EMPTY, side -> side == EnumFacing.WEST ? tile : null, List.of(absent, present));
        assertEquals(List.of(absent, present), found);
        assertTrue(closed[0]);
    }
}
