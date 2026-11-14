# Chunk Destroyer JFR measurements

Reusing an ordered array of no-drop block rules reduced the median allocation for the 65,536-block workload from about 190 MiB to 179 MiB. Elapsed time did not show a repeatable improvement.

The baseline is `a0133dc39`. The runtime and workload are the same as in [Advanced Pump profiling](advanced-pump.md): Cleanroom 0.7.14-alpha, Temurin 25.0.4.1, a 1 GiB heap, Windows 11 and an AMD Ryzen AI MAX+ 395. Each JVM warms up by mining 8,192 blocks, then mines a prepared 32 × 32 × 64 region three times. Preparation and chunk generation are outside the measured work windows. Power is replenished before each update to avoid measuring starvation.

The runs were performed in this order: baseline A, optimized A, optimized B, baseline B. Values below are medians of the three full mining passes in each JVM. Allocation totals use the server thread's JVM allocation counter.

| Run | Mining time | Allocated memory |
| --- | ---: | ---: |
| Baseline A | 414.310 ms | 189.961 MiB |
| Optimized A | 427.172 ms | 179.002 MiB |
| Optimized B | 434.144 ms | 178.937 MiB |
| Baseline B | 418.531 ms | 190.438 MiB |

Each optimized run allocated about 11 MiB less per full mining pass. The time medians were slightly higher, while individual pass timings overlapped. These runs establish an allocation reduction; they do not establish faster mining, higher server TPS or fewer old-generation collections.

JFR stacks showed allocations from `scala.collection.immutable.ChampBaseIterator` and `SetIterator` when `calcBreakEnergy` searched `Config.content.noDigBLOCKS` for each block. `Config.Content` now builds a `BlockMatcher` once from that set's iterator. The matcher evaluates the original `BlockWrapper.contain` predicates on every query, in the same order, and stops on the first match. Recreating `Config.Content` on reload also recreates the matcher.

The larger allocation stacks still come from Minecraft lighting updates through `World.getRawLight`, `World.checkLightFor` and `Chunk.relightBlock`. The change keeps those calls, block update flags, harvesting events, traversal and per-update work limits intact.

Both optimized runs matched the baseline behavior snapshots:

- 1,058 update calls and 9,875,200,000,000 microMJ consumed per mining pass.
- No blocks left in the prepared region, with identical item NBT containing 16,384 coal and 16,384 iron ore.
- Identical pump traversal and drainage snapshots from the control workloads.

Unit tests compare the matcher with the existing predicates across registered vanilla block states. They also cover metadata/property handling, overlapping flags, empty rules, short-circuit order and replacement of a configuration snapshot.

The full 34-test suite, distribution JAR checks and the in-game smoke test passed. The smoke test covers 31 screens and saving/reloading the configuration.

[Raw results](chunk-destroyer/) include every pass. JFR files remain under `run/client/profile/`. Use the workload commands from the pump report with the output names below. The stack option prints the first 12 frames of the largest sampled allocation stacks.

```powershell
java '-Daem.profile.stacks=true' tools/JfrMachineSummary.java run/client/profile/quarry-baseline/chunk-destroyer.jfr
java '-Daem.profile.stacks=true' tools/JfrMachineSummary.java run/client/profile/quarry-optimized-a/chunk-destroyer.jfr
.\tools\Compare-MachineProfiles.ps1 -Before run/client/profile/quarry-baseline -After run/client/profile/quarry-optimized-a
.\tools\Compare-MachineProfiles.ps1 -Before run/client/profile/quarry-baseline-b -After run/client/profile/quarry-optimized-b
```
