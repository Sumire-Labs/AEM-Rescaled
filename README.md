# Additional Enchanted Miner Rescaled

This is a fork of [Additional Enchanted Miner](https://github.com/Kotori316/QuarryPlus) by kotori316, which is a fork of [QuarryPlus](https://github.com/yogpstop/QuarryPlus) by yogpstop.
This fork adds features and bug fixes, and ports the mod to [Cleanroom](https://github.com/CleanroomMC/Cleanroom) with Java 25 and Scala 3.

## Requirements

- [Cleanroom](https://github.com/CleanroomMC/Cleanroom) 0.7.14-alpha or newer
- [Scalar](https://github.com/CleanroomMC/Scalar) 3.10.0 or newer
- [ModularUI](https://github.com/CleanroomMC/ModularUI) 3.1.6 or newer

## Changes

- Port to Cleanroom with Java 25 and Scala 3.
- Rewrite the GUIs with [ModularUI](https://github.com/CleanroomMC/ModularUI), substantially improving the interface.
- Backport Mini Quarry from [upstream's Minecraft 1.20.1 version](https://github.com/Kotori316/QuarryPlus/tree/1.20.1). It mines a marker-defined area using real tools and supports tool enchantments and durability, redstone control, and mining filters.
- Backport Repeat Tick Module from [newer upstream versions](https://github.com/Kotori316/QuarryPlus/tree/v21.1.165). It increases the number of work passes for the new QuarryPlus and Chunk Destroyer. Each slot holds up to 64 modules, and each additional pass consumes its normal energy cost.
- Backport Void Module from newer upstream versions. It discards selected mined items and supports 18 filter slots, metadata and NBT matching, and multiple modules used together.
- Replace hardcoded GUI text with translation keys.
- Add Japanese, English, Russian, and Simplified Chinese translations.
- Reduce temporary allocations in QuarryPlus item export, drop handling, and enchanted-tool creation. See the [mining performance report](docs/performance/mining-and-pulsar.md).
- Reduce Chunk Destroyer allocations by reusing block-rule data when checking which blocks to mine. See the [Chunk Destroyer performance report](docs/performance/chunk-destroyer.md).
- Optimize Advanced Pump searches by changing how visited coordinates are stored, reducing temporary objects and copying. See the [Advanced Pump performance report](docs/performance/advanced-pump.md).
- Fix Workbench item duplication when an adjacent inventory accepts only part of the crafted output. Only the uninserted items are dropped.
- Fix Placer item handling to preserve replacement and overflow items, and clean up temporary state if placement throws an exception.
- Fix Mini Quarry export stalls so later stacks can be exported even when the destination rejects the first stack.
- Fix handling of large fluid quantities and fluids with NBT data. Subtract only the amount actually transferred, and preserve fluid NBT and large quantities when saving.
- Keep fake players separate for each world and update their position whenever the Placer activates.
- Fix protection mod integration so legacy Quarries and Mining Wells check for canceled block-break events before consuming energy or mining blocks.
- Reject client-authored tile NBT updates and Mover or template edit requests that do not match the player's current editing target.

## Credits

- [yogpstop](https://github.com/yogpstop): original [QuarryPlus](https://github.com/yogpstop/QuarryPlus).
- [kotori316](https://github.com/Kotori316): [Additional Enchanted Miner](https://github.com/Kotori316/QuarryPlus).
