# Additional Enchanted Miner Rescaled 

## This mod is a fork of Additional Enchanted Miner, which is a fork of Quarry Plus

This mod targets Cleanroom 0.7.14-alpha or newer. It requires Scala3 and ModularUI 3.1.6 or newer.
This mod adds automatic miner. All machines can accept RF, MJ, EU and Forge Energy.

## Development

The build uses [CleanroomDevEnv at `6b24e5c`](https://github.com/Sumire-Labs/CleanroomDevEnv/commit/6b24e5cc419a97a9872feed849659610d3ce6055),
adapted for AEM's Scala 3.9 sources and optional integrations. Use JDK 25 and the Gradle wrapper:

```shell
./gradlew build verifyReleaseJar
```

The main build, settings, run configurations, Mixin configuration and distribution verification use Kotlin DSL.
AEM-specific dependency, runtime, publishing and historical-release scripts remain in `gradle/scripts/*.gradle`.
Formatting is opt-in with `-Penable_spotless=true`; its configuration is in `gradle/spotless/`.
License files and source notices are maintained manually.

The unclassified Maven artifact remains the SRG distribution JAR. To verify publishing without uploading:

```shell
./gradlew publish -Ppublish_to_maven=true -Ppublish_to_local_maven=false -Pmaven_url=build/maven-verification
```

For remote publishing, both the template's `MAVEN_USERNAME` / `MAVEN_PASSWORD` and the existing
`MAVEN_USER` / `MAVEN_PASS` environment variables are supported. Keep credentials outside this repository.

### Machines:

* **QuarryPlus** : It digs to y = 1 in the area of max 256 x 256 (blocks). Enchantments allow improving fuel efficiency
  and enchanted mining.
* **PumpPlus** : It adds QuarryPlus a function to pump liquid. You can make the Nether dry.
* **WorkbenchPlus** : You need this to make machines by this mod.
* **LaserPlus** : Use this instead of 32 BC's Laser. It improves performance of your PC and simplify wiring.
* **ChunkDestroyer** : It takes less than an hour to flatten the area of 16 x 16 (chunks).
* **Advanced Pump** : A pump faster than BC's one.
* **Spawner Controller** : Change the content of neighbor spawner.
* **Mini Quarry** : Uses real tools to harvest a marker-defined area. Redstone rising edges start/stop a scan; tool enchantments and durability apply. Insert tools or a fuel module in its five slots. The block supports Efficiency and Unbreaking enchantments.

ChunkDestroyer and Advanced Pump will increase the number of old GC and lag.

## Changes

- Port to Cleanroom w/ Java 25 + Scala 3.x.
- Native Cleanroom 0.7.x
- Migrate MCP to 39-1.12
- Replace machine and configuration screens with ModularUI.
- Backported several features from newer versions, including Void Module, Repeat Tick Module, and Mini Quarry.

## Credits

yogpstop - Original Mod 
kotori316 - Additional Enchanted Miner

Void Module and Repeat Tick Module textures from [Additional Enchanted Miner](https://github.com/Kotori316/QuarryPlus/tree/v21.1.165), licensed under LGPL-3.0.
Mini Quarry textures and the original Mini Quarry design from [Additional Enchanted Miner for 1.20.1](https://github.com/Kotori316/QuarryPlus/tree/1.20.1), licensed under LGPL-3.0.
