package com.yogpc.qp.utils;

import java.util.ArrayList;
import java.util.function.Predicate;
import net.minecraft.block.state.IBlockState;

/** A configuration snapshot that preserves rule order without allocating an iterator per block. */
public final class BlockMatcher implements Predicate<IBlockState> {
    private final BlockWrapper[] rules;

    public BlockMatcher(Iterable<BlockWrapper> source) {
        var copy = new ArrayList<BlockWrapper>();
        for (BlockWrapper rule : source) copy.add(rule);
        rules = copy.toArray(new BlockWrapper[0]);
    }

    @Override public boolean test(IBlockState state) {
        for (BlockWrapper rule : rules) {
            if (rule.contain(state)) return true;
        }
        return false;
    }
}
