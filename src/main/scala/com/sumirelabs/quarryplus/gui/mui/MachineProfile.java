package com.sumirelabs.quarryplus.gui.mui;

import com.yogpc.qp.QuarryPlus;
import com.yogpc.qp.QuarryPlusI;
import com.yogpc.qp.tile.IEnchantableTile;
import com.yogpc.qp.tile.TileAdvPump;
import com.yogpc.qp.tile.TileAdvQuarry;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import jdk.jfr.Configuration;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

/** Runs only in the disposable development smoke world, on its server thread. */
final class MachineProfile {
    @Name("quarryplus.MachineWork")
    @Label("AEM machine work")
    static final class Work extends Event {
        String workload;
        long allocatedBytes;
        long elapsedNanos;
        int operations;
    }

    private static final com.sun.management.ThreadMXBean THREADS =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

    static void run(WorldServer world) {
        try {
            Path output = Path.of(System.getProperty("quarryplus.profileOutput", "profile"));
            Files.createDirectories(output);
            List<String> results = new ArrayList<>();
            if (!THREADS.isThreadAllocatedMemorySupported()) throw new IllegalStateException("Allocation counters unavailable");
            THREADS.setThreadAllocatedMemoryEnabled(true);
            world.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
            world.getGameRules().setOrCreateGameRule("randomTickSpeed", "0");
            world.rand.setSeed(1);

            TileAdvPump pump = pool(world, 0, 0, 128);
            for (int i = 0; i < 12; i++) pump.buildWay();
            try (Recording recording = recording()) {
                for (int sample = 0; sample < 5; sample++) {
                    measure("pump-search", 16, results, () -> {
                        for (int i = 0; i < 16; i++) pump.buildWay();
                    });
                }
                recording.stop();
                recording.dump(output.resolve("pump-search.jfr"));
            }
            results.add("pump-search.queue=" + queueDigest(pump, "toDig"));
            world.setBlockToAir(pump.getPos());

            TileAdvPump draining = pool(world, 256, 0, 16);
            int ticks = 0;
            long energy = 0;
            draining.start();
            do {
                draining.setStoredEnergy(draining.getMaxStored());
                long before = draining.getStoredEnergy();
                draining.update();
                energy += before - draining.getStoredEnergy();
                if (++ticks > 1000) throw new IllegalStateException("Pump did not finish");
            } while (draining.isWorking());
            results.add("pump-drain.ticks=" + ticks);
            results.add("pump-drain.energy=" + energy);
            results.add("pump-drain.remaining=" + countSolid(world, 256, 0, 16, 64, 64));
            results.add("pump-drain.fluid=" + draining.writeToNBT(new NBTTagCompound()).getCompoundTag("FluidHandler"));
            world.setBlockToAir(draining.getPos());
            Files.write(output.resolve("results.txt"), results);

            quarry(world, 512, 16, 32, null); // Warm up the production mining path.
            try (Recording recording = recording()) {
                for (int sample = 0; sample < 3; sample++) quarry(world, 512, 32, 64, results);
                recording.stop();
                recording.dump(output.resolve("chunk-destroyer.jfr"));
            }
            checkIrregularPool(world, results);
            Files.write(output.resolve("results.txt"), results);
            for (String line : results) QuarryPlus.LOGGER.info("Machine profile: {}", line);
            QuarryPlus.LOGGER.info("Machine profile completed: {}", output.toAbsolutePath());
        } catch (Exception error) {
            throw new IllegalStateException("Machine profile failed", error);
        }
    }

    private static Recording recording() throws Exception {
        Recording recording = new Recording(Configuration.getConfiguration("profile"));
        recording.enable("quarryplus.MachineWork").withoutStackTrace();
        recording.enable("jdk.ObjectAllocationSample").with("throttle", "1000/s");
        recording.start();
        return recording;
    }

    private static void checkIrregularPool(WorldServer world, List<String> results) throws Exception {
        TileAdvPump pump = pool(world, -256, -32, 16);
        for (int z = -32; z < -16; z++) world.setBlockState(new BlockPos(-251, 64, z), Blocks.STONE.getDefaultState(), 2);
        for (int z = -30; z < -16; z += 3) world.setBlockState(new BlockPos(-247, 64, z),
                Blocks.FLOWING_WATER.getDefaultState().withProperty(net.minecraft.block.BlockLiquid.LEVEL, 1), 2);
        world.setBlockState(new BlockPos(-244, 64, -22), Blocks.LAVA.getDefaultState(), 2);
        pump.buildWay();
        results.add("irregular.sources=" + queueDigest(pump, "toDig"));
        results.add("irregular.flowing=" + queueDigest(pump, "toDelete"));
        pump.buildWay();
        results.add("irregular.rebuilt-sources=" + queueDigest(pump, "toDig"));
        results.add("irregular.rebuilt-flowing=" + queueDigest(pump, "toDelete"));
        // Change connectivity after discovery, before pumping the queued positions.
        for (int z = -32; z < -16; z++) world.setBlockState(new BlockPos(-249, 64, z), Blocks.STONE.getDefaultState(), 2);
        pump.start();
        int ticks = 0;
        long energy = 0;
        do {
            pump.setStoredEnergy(pump.getMaxStored());
            long before = pump.getStoredEnergy();
            pump.update();
            energy += before - pump.getStoredEnergy();
            if (++ticks > 1000) throw new IllegalStateException("Irregular pool did not finish");
        } while (pump.isWorking());
        results.add("irregular.ticks=" + ticks + ",energy=" + energy + ",remaining=" + countSolid(world, -256, -32, 16, 64, 64));
        results.add("irregular.fluid=" + pump.writeToNBT(new NBTTagCompound()).getCompoundTag("FluidHandler"));
        world.setBlockToAir(pump.getPos());
    }

    private static void measure(String name, int operations, List<String> results, Runnable action) {
        Work event = new Work();
        event.workload = name;
        event.operations = operations;
        long thread = Thread.currentThread().threadId();
        long bytes = THREADS.getThreadAllocatedBytes(thread);
        long start = System.nanoTime();
        event.begin();
        action.run();
        event.end();
        event.elapsedNanos = System.nanoTime() - start;
        event.allocatedBytes = THREADS.getThreadAllocatedBytes(thread) - bytes;
        event.commit();
        results.add(name + ".ns=" + event.elapsedNanos + ",bytes=" + event.allocatedBytes + ",operations=" + operations);
    }

    private static TileAdvPump pool(WorldServer world, int x, int z, int size) {
        for (int dx = -1; dx <= size; dx++) for (int dz = -1; dz <= size; dz++) {
            world.setBlockState(new BlockPos(x + dx, 63, z + dz), Blocks.STONE.getDefaultState(), 2);
            world.setBlockState(new BlockPos(x + dx, 64, z + dz),
                    dx >= 0 && dx < size && dz >= 0 && dz < size ? Blocks.WATER.getDefaultState() : Blocks.STONE.getDefaultState(), 2);
            world.setBlockToAir(new BlockPos(x + dx, 65, z + dz));
        }
        BlockPos pos = new BlockPos(x + size / 2, 65, z + size / 2);
        world.setBlockState(pos, QuarryPlusI.blockStandalonePump().getDefaultState(), 2);
        TileAdvPump pump = (TileAdvPump) world.getTileEntity(pos);
        pump.setEnchantment((short) IEnchantableTile.FortuneID, (short) 3);
        pump.G_ReInit();
        return pump;
    }

    private static String queueDigest(TileAdvPump pump, String name) throws Exception {
        var field = TileAdvPump.class.getDeclaredField(name);
        field.setAccessible(true);
        var queue = (scala.collection.immutable.List<?>) field.get(pump);
        long hash = 1;
        var iterator = queue.iterator();
        while (iterator.hasNext()) hash = 31 * hash + ((BlockPos) iterator.next()).toLong();
        return queue.size() + ":" + Long.toUnsignedString(hash);
    }

    private static void quarry(WorldServer world, int x, int size, int height, List<String> results) {
        for (int dx = 0; dx < size; dx++) for (int dz = 0; dz < size; dz++) for (int y = 64; y < 64 + height; y++) {
            Block block = switch (y % 4) {
                case 0 -> Blocks.STONE;
                case 1 -> Blocks.DIRT;
                case 2 -> Blocks.IRON_ORE;
                default -> Blocks.COAL_ORE;
            };
            world.setBlockState(new BlockPos(x + dx, y, dz), block.getDefaultState(), 2);
        }
        BlockPos pos = new BlockPos(x - 2, 64 + height, 0);
        world.setBlockState(pos, QuarryPlusI.blockChunkDestroyer().getDefaultState(), 2);
        TileAdvQuarry quarry = (TileAdvQuarry) world.getTileEntity(pos);
        quarry.digRange_$eq(new TileAdvQuarry.DigRange(x, 64 + height, 0, x + size - 1, 64 + height, size - 1));
        quarry.yLevel_$eq(64);
        quarry.target_$eq(quarry.digRange().min());
        quarry.mode().set(TileAdvQuarry.BREAK_BLOCK());
        quarry.configure(1000000000000L, 1000000000000L);
        quarry.setStoredEnergy(1000000000000L);
        int[] ticks = {0};
        long[] energy = {0};
        Runnable mine = () -> {
            while (!quarry.mode().is(TileAdvQuarry.NONE())) {
                quarry.setStoredEnergy(quarry.getMaxStored());
                long supplied = quarry.getStoredEnergy();
                quarry.update();
                energy[0] += supplied - quarry.getStoredEnergy();
                if (++ticks[0] > 10000) throw new IllegalStateException("Chunk Destroyer did not finish: mode="
                        + quarry.mode().toNBT() + ",target=" + quarry.target() + ",energy=" + quarry.getStoredEnergy());
            }
        };
        if (results == null) mine.run();
        else {
            measure("chunk-destroyer", size * size * height, results, mine);
            results.add("chunk-destroyer.ticks=" + ticks[0] + ",energy=" + energy[0]
                    + ",remaining=" + countSolid(world, x, 0, size, 64, 63 + height)
                    + ",items=" + quarry.cacheItems().toNBT());
        }
        quarry.clear();
        world.setBlockToAir(pos);
    }

    private static int countSolid(WorldServer world, int x, int z, int size, int low, int high) {
        int count = 0;
        for (int dx = 0; dx < size; dx++) for (int dz = 0; dz < size; dz++) for (int y = low; y <= high; y++)
            if (!world.isAirBlock(new BlockPos(x + dx, y, z + dz))) count++;
        return count;
    }
}
