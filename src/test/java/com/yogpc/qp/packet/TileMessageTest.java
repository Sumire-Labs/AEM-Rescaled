package com.yogpc.qp.packet;

import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TileMessageTest {
    private static MessageContext context(Side side) throws ReflectiveOperationException {
        var constructor = MessageContext.class.getDeclaredConstructor(INetHandler.class, Side.class);
        constructor.setAccessible(true);
        return constructor.newInstance(null, side);
    }

    @Test void ignoresClientSuppliedTileStateBeforeAccessingTheServerWorld() throws Exception {
        var tag = new NBTTagCompound();
        tag.setInteger("x", 1);
        tag.setInteger("y", 64);
        tag.setInteger("z", 2);
        var buffer = new PacketBuffer(Unpooled.buffer());
        try {
            buffer.writeCompoundTag(tag);
            var message = new TileMessage();
            message.fromBytes(buffer);
            assertNull(message.onReceive(message, context(Side.SERVER)));
        } finally {
            buffer.release();
        }
    }

    @Test void ignoresMissingClientSyncPayload() throws Exception {
        var message = new TileMessage();
        assertNull(message.onReceive(message, context(Side.CLIENT)));
    }

    @Test void keepsTheTileSyncWireFormat() throws Exception {
        var tag = new NBTTagCompound();
        tag.setInteger("x", -12);
        tag.setInteger("y", 64);
        tag.setInteger("z", 32);
        tag.setLong("storedEnergy", 1234);
        var input = new PacketBuffer(Unpooled.buffer());
        var output = new PacketBuffer(Unpooled.buffer());
        try {
            input.writeCompoundTag(tag);
            var message = new TileMessage();
            message.fromBytes(input);
            message.toBytes(output);
            assertEquals(tag, output.readCompoundTag());
        } finally {
            input.release();
            output.release();
        }
    }
}
