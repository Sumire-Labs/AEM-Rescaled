package com.yogpc.qp.utils;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BlockMatcherTest {
    @BeforeAll static void bootstrap() { Bootstrap.register(); }

    @Test void matchesTheExistingRulesAcrossRegisteredBlockStates() {
        var rules = List.of(
                new BlockWrapper(Blocks.STONE.getStateFromMeta(1), false, false),
                new BlockWrapper(Blocks.DIRT.getDefaultState(), true, false),
                new BlockWrapper(Blocks.LOG.getDefaultState(), false, true),
                new BlockWrapper(Blocks.OAK_FENCE.getDefaultState(), true, false),
                new BlockWrapper(Blocks.SAND.getDefaultState(), true, true));
        var matcher = new BlockMatcher(rules);
        int checked = 0;
        for (Block block : Block.REGISTRY) {
            for (var state : block.getBlockState().getValidStates()) {
                assertEquals(rules.stream().anyMatch(rule -> rule.contain(state)), matcher.test(state), state.toString());
                checked++;
            }
        }
        assertTrue(checked > 100);
    }

    @Test void keepsMetadataAndPropertyMatchingDistinct() {
        var exact = new BlockMatcher(List.of(new BlockWrapper(Blocks.STONE.getStateFromMeta(1), false, false)));
        assertTrue(exact.test(Blocks.STONE.getStateFromMeta(1)));
        assertFalse(exact.test(Blocks.STONE.getDefaultState()));
        var allStone = new BlockMatcher(List.of(new BlockWrapper(Blocks.STONE.getDefaultState(), false, true)));
        assertTrue(allStone.test(Blocks.STONE.getStateFromMeta(1)));
        assertFalse(allStone.test(Blocks.DIRT.getDefaultState()));
        var fence = new BlockMatcher(List.of(new BlockWrapper(Blocks.OAK_FENCE.getDefaultState(), true, false)));
        assertTrue(fence.test(Blocks.OAK_FENCE.getDefaultState().withProperty(BlockFence.NORTH, true)));
    }

    @Test void aNewConfigurationGetsNewRulesWithoutChangingTheOldSnapshot() {
        var source = new ArrayList<BlockWrapper>();
        source.add(new BlockWrapper(Blocks.STONE.getDefaultState(), false, true));
        var oldConfig = new BlockMatcher(source);
        source.clear();
        source.add(new BlockWrapper(Blocks.DIRT.getDefaultState(), false, true));
        var newConfig = new BlockMatcher(source);
        assertTrue(oldConfig.test(Blocks.STONE.getDefaultState()));
        assertFalse(oldConfig.test(Blocks.DIRT.getDefaultState()));
        assertFalse(newConfig.test(Blocks.STONE.getDefaultState()));
        assertTrue(newConfig.test(Blocks.DIRT.getDefaultState()));
        assertFalse(new BlockMatcher(List.of()).test(Blocks.STONE.getDefaultState()));
    }

    @Test void stopsAtTheFirstMatchingRule() {
        int[] calls = {0};
        var first = new BlockWrapper(Blocks.STONE.getDefaultState(), false, true) {
            @Override public boolean contain(net.minecraft.block.state.IBlockState state) {
                calls[0]++;
                return true;
            }
        };
        var second = new BlockWrapper(Blocks.DIRT.getDefaultState(), false, true) {
            @Override public boolean contain(net.minecraft.block.state.IBlockState state) {
                fail("Rules after the first match must not run");
                return false;
            }
        };
        assertTrue(new BlockMatcher(List.of(first, second)).test(Blocks.STONE.getDefaultState()));
        assertEquals(1, calls[0]);
    }
}
