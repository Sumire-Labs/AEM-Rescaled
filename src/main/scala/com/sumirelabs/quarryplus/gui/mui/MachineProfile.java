package com.sumirelabs.quarryplus.gui.mui;

import com.yogpc.qp.QuarryPlus;
import com.yogpc.qp.QuarryPlusI;
import com.yogpc.qp.tile.IEnchantableTile;
import com.yogpc.qp.tile.TileAdvPump;
import com.yogpc.qp.tile.TileAdvQuarry;
import com.yogpc.qp.tile.TileQuarry2;
import com.yogpc.qp.tile.QuarryAction;
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
import net.minecraft.world.EnumSkyBlock;

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
    private static LightCompletion lightCompletion;

    static void run(WorldServer world) {
        try {
            Path output = Path.of(System.getProperty("quarryplus.profileOutput", "profile"));
            Files.createDirectories(output);
            List<String> results = new ArrayList<>();
            lightCompletion = new LightCompletion(world);
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
            for (int sample = 0; sample < 3; sample++) normalQuarry(world, 768, 16, 16, null);
            try (Recording recording = recording()) {
                for (int sample = 0; sample < 5; sample++) normalQuarry(world, 768, 16, 16, results);
                recording.stop();
                recording.dump(output.resolve("quarry-plus.jfr"));
            }
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
        lightCompletion.prepare(x, 0, size);
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
        lightCompletion.await(x, 0, size);
        int[] ticks = {0};
        long[] energy = {0};
        Runnable mine = () -> {
            while (!quarry.mode().is(TileAdvQuarry.NONE())) {
                quarry.setStoredEnergy(quarry.getMaxStored());
                long supplied = quarry.getStoredEnergy();
                quarry.update();
                lightCompletion.publish();
                energy[0] += supplied - quarry.getStoredEnergy();
                if (++ticks[0] > 10000) throw new IllegalStateException("Chunk Destroyer did not finish: mode="
                        + quarry.mode().toNBT() + ",target=" + quarry.target() + ",energy=" + quarry.getStoredEnergy());
            }
        };
        if (results == null) mine.run();
        else {
            measure("chunk-destroyer-complete", size * size * height, results, () -> {
                measure("chunk-destroyer", size * size * height, results, mine);
                lightCompletion.await(x, 0, size);
            });
            results.add("chunk-destroyer.light=" + lightDigest(world, x, size, height));
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

    private static void normalQuarry(WorldServer world, int x, int size, int height, List<String> results) {
        lightCompletion.prepare(x, 0, size);
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
        world.setBlockState(pos, QuarryPlusI.blockQuarry2().getDefaultState(), 2);
        TileQuarry2 quarry = (TileQuarry2) world.getTileEntity(pos);
        quarry.area_$eq(new TileQuarry2.Area(x - 1, 64 + height, -1, x + size, 64 + height, size));
        quarry.yLevel_$eq(64);
        quarry.action_$eq(new QuarryAction.BreakBlock(quarry, 63 + height, pos));
        quarry.target_$eq(quarry.action().nextTarget());
        quarry.configure(1000000000000L, 1000000000000L);
        lightCompletion.await(x, 0, size);
        int[] ticks = {0};
        long[] energy = {0}, progress = {1};
        Runnable mine = () -> {
            while (quarry.action() != QuarryAction.none()) {
                quarry.setStoredEnergy(quarry.getMaxStored());
                long supplied = quarry.getStoredEnergy();
                quarry.update();
                lightCompletion.publish();
                energy[0] += supplied - quarry.getStoredEnergy();
                progress[0] = progress[0] * 31 + quarry.target().toLong();
                if (quarry.action() instanceof QuarryAction.BreakBlock moving) {
                    progress[0] = progress[0] * 31 + Double.doubleToLongBits(moving.headX());
                    progress[0] = progress[0] * 31 + Double.doubleToLongBits(moving.headY());
                    progress[0] = progress[0] * 31 + Double.doubleToLongBits(moving.headZ());
                }
                if (++ticks[0] > 1000000) throw new IllegalStateException("QuarryPlus did not finish");
            }
        };
        if (results == null) mine.run();
        else {
            measure("quarry-plus-complete", size * size * height, results, () -> {
                measure("quarry-plus", size * size * height, results, mine);
                lightCompletion.await(x, 0, size);
            });
            results.add("quarry-plus.light=" + lightDigest(world, x, size, height));
            results.add("quarry-plus.ticks=" + ticks[0] + ",energy=" + energy[0] + ",progress=" + Long.toUnsignedString(progress[0])
                    + ",remaining=" + countSolid(world, x, 0, size, 64, 63 + height)
                    + ",storage=" + quarry.writeToNBT(new NBTTagCompound()).getCompoundTag("storage"));
        }
        quarry.clear();
        world.setBlockToAir(pos);
    }

    private static long lightDigest(WorldServer world, int x, int size, int height) {
        long digest = 1;
        for (int dx = 0; dx < size; dx++) for (int z = 0; z < size; z++) for (int y = 63; y <= 64 + height; y++) {
            BlockPos pos = new BlockPos(x + dx, y, z);
            digest = 31 * digest + world.getLightFor(EnumSkyBlock.SKY, pos) * 16 + world.getLightFor(EnumSkyBlock.BLOCK, pos);
        }
        return digest;
    }

    /** Profiling-only adapter for Pulsar 0.6.5; the public legacy flush hook is not a barrier. */
    private static final class LightCompletion {
        private final WorldServer world;
        private final Object manager;
        private final java.lang.reflect.Method publish, pending, localPending, failed;

        LightCompletion(WorldServer world) throws Exception {
            this.world = world;
            Object found;
            try { found = world.getClass().getMethod("pulsar$getLightManager").invoke(world); }
            catch (NoSuchMethodException absent) { found = null; }
            manager = found;
            if (manager == null && Boolean.getBoolean("quarryplus.requirePulsarProfile")) {
                throw new IllegalStateException("Pulsar profiling requested, but no lighting manager is available");
            }
            if (manager == null) { publish = pending = localPending = failed = null; }
            else {
                publish = manager.getClass().getMethod("publishContextualLight");
                pending = manager.getClass().getMethod("hasUpdates");
                localPending = manager.getClass().getMethod("hasChunkPendingLight", int.class, int.class);
                failed = manager.getClass().getMethod("hasFailedLightSamplesNear", int.class, int.class);
            }
        }

        void prepare(int x, int z, int size) {
            for (int cx = (x >> 4) - 2; cx <= ((x + size - 1) >> 4) + 2; cx++)
                for (int cz = (z >> 4) - 2; cz <= ((z + size - 1) >> 4) + 2; cz++) world.getChunk(cx, cz);
        }

        void publish() {
            if (manager == null) return;
            try { publish.invoke(manager); }
            catch (ReflectiveOperationException error) { throw new IllegalStateException("Light publication failed", error); }
        }

        void await(int x, int z, int size) {
            if (manager == null) return;
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(30);
            int stable = 0;
            try {
                while (stable < 2) {
                    publish();
                    boolean busy = (boolean) pending.invoke(manager);
                    for (int cx = (x >> 4) - 2; cx <= ((x + size - 1) >> 4) + 2; cx++) {
                        for (int cz = (z >> 4) - 2; cz <= ((z + size - 1) >> 4) + 2; cz++) {
                            busy |= (boolean) localPending.invoke(manager, cx, cz);
                            if ((boolean) failed.invoke(manager, cx, cz)) throw new IllegalStateException("Failed contextual light sample");
                        }
                    }
                    stable = busy ? 0 : stable + 1;
                    if (System.nanoTime() > deadline) throw new IllegalStateException("Light propagation did not settle");
                    if (stable < 2) java.util.concurrent.locks.LockSupport.parkNanos(200000);
                }
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Light completion check failed", error);
            }
        }
    }
}
