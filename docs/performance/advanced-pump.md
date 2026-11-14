# Advanced Pump JFR measurements

Keeping visited positions in a primitive long set reduced allocation per 128 × 128 fluid search from 10.418 MiB to 4.195 MiB (59.7%). The search order and drainage results matched the baseline.

Measurements used Minecraft 1.12.2, Cleanroom 0.7.14-alpha, Temurin 25.0.4.1, a 1 GiB heap and the development runtime dependencies in this repository on Windows 11 with an AMD Ryzen AI MAX+ 395. The baseline is commit `ebfe32aa7`, which includes the profiling workload without the optimization. Baseline A predates the additional irregular-pool checks; its timed workloads are identical. Behavior snapshots were compared against baseline B.

The workload runs on the integrated server thread in a new disposable flat world. It prepares chunks and blocks before each measurement, warms up the production methods, and records JFR with the `profile` configuration. `quarryplus.MachineWork` events delimit the measured work. Allocated bytes come from the JVM's thread allocation counter; JFR allocation samples identify the allocating classes and call sites.

Each JVM runs 12 warm-up searches followed by five batches of 16 searches over 16,384 water sources. The table shows the median time per search and mean allocated memory per search from each JVM launch.

| Run | Search time | Allocated memory |
| --- | ---: | ---: |
| Baseline A | 16.336 ms | 10.418 MiB |
| Baseline B | 6.832 ms | 10.418 MiB |
| Optimized A | 2.984 ms | 4.195 MiB |
| Optimized B | 2.832 ms | 4.195 MiB |

Elapsed time varied between launches. Allocation counts were stable. These measurements cover search rebuilds with loaded chunks; server-wide TPS and old-generation GC frequency need a longer gameplay workload.

Before the change, JFR attributed search allocations to `TileAdvPump.buildWay`, including Scala hash-set nodes, immutable set nodes and their arrays. The pump built a mutable visited set and then copied it into an immutable set. It now stores packed block coordinates in a primitive long set and retains that completed set for boundary checks. Neither traversal nor membership checks iterate the set, so its iteration order does not affect pumping.

Chunk Destroyer was also measured while removing 65,536 prepared blocks. Its samples concentrated in `TileAdvQuarry.setBlock` and Minecraft's block-update work. Its implementation was left unchanged. In baseline A, `setBlock` was the largest sampled AEM caller, appearing in 141 server-thread execution samples within mining windows. Removing block updates or changing when they run would need separate compatibility testing.

The behavior comparison checks:

- The count and ordered coordinate digest of the 16,384-source search queue.
- Drainage of a 256-source pool: seven update calls, 256,000 mB, no remaining liquid blocks, and 5,120,000,000 microMJ consumed.
- Negative coordinates, flowing water, a separate water pocket, lava, repeated searches, and connectivity changed after discovery. Queue digests, remaining blocks, fluid NBT and energy match.
- Chunk Destroyer completion: 1,058 update calls, 9,875,200,000,000 microMJ, no remaining blocks in the prepared region, and matching item NBT in all three measured repetitions.

Raw counter and behavior snapshots are in [advanced-pump/](advanced-pump/). JFR recordings stay under `run/client/profile/`, which Git ignores. The earlier incomplete setup runs named `before-1` and `before-2` are excluded from these results.

Run the baseline revision and the optimized revision with the same workload and different output names. The client opens a temporary world and exits when the workload finishes. Confirm `Machine profile completed` in `run/client/logs/latest.log`; a failed in-game assertion can still leave Gradle with exit code zero.

```powershell
.\gradlew.bat runClient '-Pextra_jvm_args=-Dquarryplus.guiSmokeTest=true -Dquarryplus.profileMachines=true -Dquarryplus.profileOutput=profile/baseline' --no-daemon --max-workers=2
.\gradlew.bat runClient '-Pextra_jvm_args=-Dquarryplus.guiSmokeTest=true -Dquarryplus.profileMachines=true -Dquarryplus.profileOutput=profile/optimized' --no-daemon --max-workers=2
java tools/JfrMachineSummary.java run/client/profile/baseline/pump-search.jfr run/client/profile/optimized/pump-search.jfr
java tools/JfrMachineSummary.java run/client/profile/baseline/chunk-destroyer.jfr
.\tools\Compare-MachineProfiles.ps1 -Before run/client/profile/baseline -After run/client/profile/optimized
```

`JfrMachineSummary` filters allocation and execution samples to the server thread and the recorded work windows. Allocation sample weights are estimates; use the thread allocation counters for the allocation totals above. The comparison script fails if the recorded behavior snapshots differ.
