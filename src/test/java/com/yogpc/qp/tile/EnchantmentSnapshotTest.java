package com.yogpc.qp.tile;

import java.util.HashMap;
import net.minecraft.init.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnchantmentSnapshotTest {
    @BeforeAll static void bootstrap() { Bootstrap.register(); }

    @SuppressWarnings("unchecked")
    @Test void preservesPositiveLevelsAndOtherEnchantmentOverrides() {
        var other = (scala.collection.immutable.Map<Object, Object>) scala.collection.immutable.Map$.MODULE$.<Object, Object>empty()
                .updated(IEnchantableTile.FortuneID, 0).updated(123, 7);
        for (int efficiency = 0; efficiency <= 5; efficiency++) {
            for (int unbreaking = 0; unbreaking <= 3; unbreaking++) {
                for (boolean silk : new boolean[]{false, true}) {
                    var holder = new TileQuarry2.EnchantmentHolder(efficiency, unbreaking, 3, silk, other);
                    var expected = new HashMap<Integer, Integer>();
                    if (efficiency > 0) expected.put(IEnchantableTile.EfficiencyID, efficiency);
                    if (unbreaking > 0) expected.put(IEnchantableTile.UnbreakingID, unbreaking);
                    if (silk) expected.put(IEnchantableTile.SilktouchID, 1);
                    expected.put(123, 7);
                    assertEquals(expected, holder.visibleEnchantments());
                    assertEquals(0, TileQuarry2.getEnchantmentMap(holder).apply(IEnchantableTile.FortuneID));
                }
            }
        }
    }

    @Test void changingTheHolderKeepsPreviousViewsImmutable() {
        var empty = scala.collection.immutable.Map$.MODULE$.<Object, Object>empty();
        var previous = new TileQuarry2.EnchantmentHolder(1, 0, 0, false, empty);
        var view = previous.visibleEnchantments();
        var changed = new TileQuarry2.EnchantmentHolder(5, 0, 0, false, empty);
        assertEquals(1, view.get(IEnchantableTile.EfficiencyID));
        assertEquals(5, changed.visibleEnchantments().get(IEnchantableTile.EfficiencyID));
        assertThrows(UnsupportedOperationException.class, () -> view.put(IEnchantableTile.EfficiencyID, 4));
    }
}
