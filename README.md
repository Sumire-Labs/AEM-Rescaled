# Additional Enchanted Miner Rescaled 

## This mod is a fork of Additional Enchanted Miner, which is a fork of Quarry Plus

This mod targets Cleanroom 0.7.14-alpha or newer. It requires [Scalar 3.10.0 or newer](https://www.curseforge.com/minecraft/mc-mods/scalar) and ModularUI 3.1.6 or newer.
This mod adds automatic miner. All machines can accept RF, MJ, EU and Forge Energy.

## Machines

* **QuarryPlus** : It digs to y = 1 in the area of max 256 x 256 (blocks). Enchantments allow improving fuel efficiency
  and enchanted mining.
* **PumpPlus** : It adds QuarryPlus a function to pump liquid. You can make the Nether dry.
* **WorkbenchPlus** : You need this to make machines by this mod.
* **LaserPlus** : Use this instead of 32 BC's Laser. It improves performance of your PC and simplify wiring.
* **ChunkDestroyer** : It takes less than an hour to flatten the area of 16 x 16 (chunks).
* **Advanced Pump** : A pump faster than BC's one.
* **Spawner Controller** : Change the content of neighbor spawner.
* **Mini Quarry** : Uses real tools to harvest a marker-defined area. Redstone rising edges start/stop a scan; tool enchantments and durability apply. Insert tools or a fuel module in its five slots. The block supports Efficiency and Unbreaking enchantments.

Large mining and pumping jobs can still cause lag. See the [performance measurements](docs/performance/mining-and-pulsar.md) for tested workloads and the effect of optional Pulsar SCALAR lighting.

## Changes

- Port to Cleanroom 0.7.x with Java 25, Scala 3.10 and MCP 39-1.12 mappings.
- Replace machine and configuration screens with ModularUI.
- Backport Mini Quarry, Void Module and Repeat Tick Module from newer versions.
- Reduce temporary allocations in QuarryPlus mining, Advanced Pump searches and Chunk Destroyer block-rule checks while preserving work limits, drops and energy costs.
- Fix Workbench item duplication when adjacent inventories accept only part of the output.
- Preserve Placer replacement and overflow items, and let Mini Quarry export later stacks when the first stack cannot be inserted.
- Preserve large fluid quantities during export and keep fluid NBT variants across saves.
- Keep fake players separate for each world and respect canceled block-break events in legacy Quarries and Mining Wells.
- Reject client-authored tile state and stale editor requests.
- Fix Maven dependency metadata so other Cleanroom projects can resolve and compile against AEM.

See the [changelog](CHANGELOG.md) for release-by-release details.

## Credits

yogpstop - Original Mod 
kotori316 - Additional Enchanted Miner

Void Module and Repeat Tick Module textures from [Additional Enchanted Miner](https://github.com/Kotori316/QuarryPlus/tree/v21.1.165), licensed under LGPL-3.0.
Mini Quarry textures and the original Mini Quarry design from [Additional Enchanted Miner for 1.20.1](https://github.com/Kotori316/QuarryPlus/tree/1.20.1), licensed under LGPL-3.0.
