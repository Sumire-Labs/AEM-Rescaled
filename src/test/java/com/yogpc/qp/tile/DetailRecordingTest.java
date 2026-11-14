package com.yogpc.qp.tile;

import java.lang.reflect.Proxy;
import net.minecraft.block.state.IBlockState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DetailRecordingTest {
    @Test void enabledRecordingKeepsTheExistingDetailsAndEnergyTotal() {
        var expected = new DetailDataCollector();
        expected.get().addData(new DetailDataCollector.Common(EnergyUsage.MOVE_HEAD, 10));
        expected.get().addData(new DetailDataCollector.Break(null, 1.5F, 20));
        expected.get().addData(new DetailDataCollector.Pump(1000, 2, 3, 30));
        var actual = new DetailDataCollector();
        actual.recordCommon(EnergyUsage.MOVE_HEAD, 10);
        actual.recordBreak(null, 1.5F, 20);
        actual.recordPump(1000, 2, 3, 30);
        assertEquals(expected.get().energy, actual.get().energy);
        assertEquals(expected.get().toString(), actual.get().toString());
    }

    @Test void disabledRecordingDoesNotFormatBlockState() throws Exception {
        var dummyType = Class.forName(DetailDataCollector.class.getName() + "$Dummy");
        var constructor = dummyType.getDeclaredConstructor();
        constructor.setAccessible(true);
        var dummy = (DetailDataCollector) constructor.newInstance();
        var state = (IBlockState) Proxy.newProxyInstance(IBlockState.class.getClassLoader(), new Class<?>[]{IBlockState.class},
                (proxy, method, arguments) -> { throw new AssertionError("Disabled recording inspected block state"); });
        dummy.recordBreak(state, 1.5F, 20);
        dummy.recordCommon(EnergyUsage.MOVE_HEAD, 10);
        dummy.recordPump(1000, 2, 3, 30);
        assertEquals(0, dummy.get().energy);
        assertEquals("DummyData", dummy.get().toString());
    }
}
