# Mining performance and Pulsar

Pulsar covers the lighting work that dominated the Chunk Destroyer profiles. Its SCALAR engine replaces the `World.checkLightFor` and `Chunk.relightBlock` paths used during mining. AEM continues to issue its normal block updates.

The AEM changes reduce temporary work in item routing, single-item drop aggregation and enchanted-tool creation. Immutable enchantment holders reuse their read-only maps. Disabled detail collectors skip record construction; enabled collectors retain the existing data and energy totals. Item routing still discovers all destinations before insertion and preserves side and integration order.

The baseline AEM source is `ae0c176f2`. Measurements used the expanded `MachineProfile` workload on the same Windows 11 / Ryzen AI MAX+ 395 machine, Temurin 25.0.4.1, Cleanroom 0.7.14-alpha and a 1 GiB heap. Pulsar was built from `c48d30b9ebbc4f940090e7c8269847112a06cf18` as `pulsar-0.6.5-beta-mcp.jar`, SHA-256 `e682b4d16c3abb73fc53ae16ee8aad00d9df50acbac6a36f0119287698496750`.

| AEM code | Lighting engine | Chunk Destroyer, 65,536 blocks | QuarryPlus, 4,096 blocks |
| --- | --- | ---: | ---: |
| Baseline | Vanilla | 429.12 ms | 70.09 ms |
| Optimized | Vanilla | 424.95 ms | 62.76 ms |
| Baseline | Pulsar SCALAR | 76.98 ms | 39.44 ms |
| Optimized, run A | Pulsar SCALAR | 69.07 ms | 26.98 ms |
| Optimized, run B | Pulsar SCALAR | 73.00 ms | 28.00 ms |

These are per-JVM medians of three Chunk Destroyer passes and five QuarryPlus passes. The numbers include waiting for queued and in-flight lighting work to settle after mining. The large Chunk Destroyer reduction comes from Pulsar. AEM's internal changes alone did not produce a large Chunk Destroyer speedup in this workload.

The fixture drives update calls in a batch. These durations measure the processing work, not the time a machine takes to finish mining at 20 game ticks per second. The required update counts stay unchanged.

The fixture preloads a two-chunk halo and settles initial lighting before the measured work. This matters because vanilla lighting can skip work when neighboring chunks are absent. Earlier exploratory runs without that halo are not comparable to this table. All input regions use prepared stone, dirt, iron ore and coal ore; generation and preparation are outside the timed windows. Power is replenished before each update.

The Pulsar adapter runs in the disposable profiling world only. It publishes contextual changes, checks both global lighting queues and local pending work, rejects failed samples, and waits for two consecutive idle observations. It does not use `pulsar$flushLightUpdates`, which is a legacy no-op. The adapter uses Pulsar 0.6.5 internals and fails if the expected methods are unavailable. Production AEM has no Pulsar dependency or completion wait.

Recorded behavior snapshots match across the compared runs:

- Chunk Destroyer: 1,058 updates, 9,875,200,000,000 microMJ, identical drops and no remaining blocks in the region.
- QuarryPlus: 4,101 updates, 739,205,798,039 microMJ, identical target/head-position digest and stored items, with no remaining blocks.
- Matching final sky/block-light digests throughout the prepared regions, plus matching pump control cases.

The 41-test suite and distribution JAR verification passed. Regression tests cover routing order and stream closure, enabled and disabled detail recording, and immutable enchantment snapshots. The game smoke test also passed with Pulsar, including all 31 screens and configuration save/reload.

Pulsar changes when lighting becomes visible because it propagates light asynchronously. It also fixes vanilla lighting bugs. Matching these final-state tests does not establish identical intermediate lighting behavior for every modpack. Network delivery and rendering completion are outside the lighting barrier. SCALAR was tested here; RGB was not.

JFR allocation totals in the workload files count the server thread. Pulsar moves work to other threads, so those totals must not be interpreted as whole-process allocation savings. The AEM-only QuarryPlus comparison reduced median server-thread allocation from about 49.55 MiB to 31.91 MiB in the halo-loaded workload.

[Raw workload results](mining/) accompany this report. JFR recordings remain in the ignored run directories. Timing varies between JVM launches, and these batches do not establish server-wide TPS or FPS gains.

To reproduce the optional Pulsar test, build Pulsar's MCP development JAR and use a separate run directory. Copy the native test instance's `config` directory and `options.txt` there, then configure `config/pulsar.cfg` before starting Minecraft:

```text
general {
    I:configVersion=3
    S:lightingMode=SCALAR
    B:lightingModeSelected=true
}
```

```powershell
.\gradlew.bat runClient --init-script gradle/verification/pulsar-profile.init.gradle '-PpulsarProfileJar=D:/path/to/pulsar-0.6.5-beta-mcp.jar' '-Pextra_jvm_args=-Dquarryplus.guiSmokeTest=true -Dquarryplus.profileMachines=true -Dquarryplus.profileOutput=profile/scalar-complete' --no-configuration-cache --no-daemon --max-workers=2
.\tools\Compare-MachineProfiles.ps1 -Before run/client/profile/native-complete -After run/pulsar-profile/profile/scalar-complete
```

The init script defaults to `run/pulsar-profile`; `-PpulsarProfileRunDir=...` selects another directory. Confirm `Machine profile completed` in that instance's log. For a vanilla run, omit the init script and Pulsar property. Use the same profiling source and workload parameters when comparing baseline and optimized AEM revisions.
