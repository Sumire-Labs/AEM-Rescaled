# Changelog

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added

- Add QuarryPlus traversal/head-position profiling and an optional Pulsar SCALAR comparison that waits for lighting completion.
- Add an opt-in JFR workload and comparison tools for Advanced Pump and Chunk Destroyer.

### Changed

- Reduce QuarryPlus allocations in item routing, single-item drop aggregation and enchanted-tool creation. Preserve drop normalization, routing priority and immutable enchantment snapshots.
- Avoid constructing energy-detail records when the tile's detail collector is disabled. Enabled collectors keep their existing output.
- Reduce Chunk Destroyer allocations when checking no-drop block rules by retaining an ordered rule array until configuration reload. Rule matching, harvesting events and work limits are unchanged.
- Reduce Advanced Pump search allocations by storing visited coordinates as packed longs and reusing the completed search set. Traversal order, drainage and energy costs are unchanged.
- Update the CleanroomDevEnv base to `6b24e5c`, migrate the main build and shared scripts to Kotlin DSL, and update Gradle to 9.9.0.
- Replace tokens in all compiled Mod classes and validate text constants without interpreting class-file binary data as text.
- Resolve embedded dependencies with Java runtime attributes and verify their JAR entries and manifest declarations.
- Keep AEM's Scala compilation, SRG Maven artifact, LGPL license and historical release workflows; accept both current and legacy Maven credential variables.

### Fixed

- Drop only the uninserted Workbench output when an adjacent inventory accepts part of a crafted stack.
- Preserve large fluid balances during export and retain tagged fluid variants across saves.
- Reject client-authored tile synchronization and stale Mover/template edit packets.
- Respect canceled block-break events before legacy Quarries and Mining Wells consume energy or change blocks.
- Publish portable required dependencies and expose Scala/ModularUI API types to Maven consumers; verify an independent consumer in CI.

## [12.7.4]

### Changed

- Migrate to the CleanroomDevEnv template, CleanroomGradle 0.23.0, and native Cleanroom 0.7.14-alpha userdev.
- Keep Scala 3.9 joint Java/Scala compilation and the matching Scalar runtime provider.
- Update dependency deobfuscation, development runs, and release verification for `reobfJar`; exclude MCP development JARs from releases.
- Remove stale signatures from remapped development dependencies so signed legacy mods such as IC2 and RedstoneFlux can load.

### Added

- Add a manual workflow for publishing tagged builds to CurseForge, Modrinth, and Sumire Labs Maven.
- Publish the reobfuscated distribution JAR as the main Maven artifact.

## [13.1.3]

### Added

- **Mini Quarry:** harvest a marker-defined area with real tools, including their enchantments and durability. Supports redstone pulse control, allow/deny filters, range outlines, chunk loading, and a ModularUI screen.
- **Repeat Tick Module:** stack up to 64 in one module slot to accelerate the new QuarryPlus and Chunk Destroyer. Each additional work pass consumes its normal energy; fuel generation is not accelerated.
- **Void Module:** discard selected items collected by the new QuarryPlus and Chunk Destroyer. Includes 18 non-consuming filter slots, metadata/NBT matching, and support for combining multiple modules.
- Workbench recipes and English, Japanese, Russian, and Simplified Chinese translations for the new content.

### Fixed

- Fix the creative tab displaying an untranslated key after the Rescaled rename.
- Update Japanese machine names and related messages, including the localized Chunk Destroyer name and the old QuarryPlus suffix.

## [13.1.2]

- Replace hardcoded GUI text with translation keys, including machine controls, status messages, and configuration validation errors.
- Add missing translations for English, Japanese, Russian, and Simplified Chinese.

## [13.1.1]

- Change the output name of the deliverable

## [13.1.0]

- Migrate to ModularUI

## [13.0.1]
- Update Credits
- Add ModIcon

## [13.0.0]
- Java 25
- Complete rewrite from Scalar 2 to Scalar 3
- Migrate MCP to 39-1.12

## [12.5.7]

* Fixed - JEI doesn't show more than 18 items.

## [12.5.6]

* Fixed - marker laser disappear and remain unavailable even if you get close to marker.

## [12.5.5]

* Added config option to change Solid Fuel Quarry setting. Close #142

## [12.5.4]

* Fixed - Dirt and Clay dropped bees if you have `Ex Nihilo: Creatio` installed. #121

## [12.5.3]

* Fixed - The comparator didn't respect metadata of items.

## [12.5.2]

* Fixed - Incorrect items created in WorkbenchPlus.

## [12.5.1]

* Fixed - PumpPlus recipe was invalid.

## [12.5.0]

* ~~Fixed - Incorrect items created in WorkbenchPlus.~~
* Fixed - Progress bar of WorkbenchPlus sometimes weird.
* Internal improvements.
* Upload jar to Maven repo.

## [12.4.9]

* Fixed - BC's marker(Landmark) couldn't be used for some machines.

## [12.4.8]

* Improved Workbench GUI.
  * Available recipes are correctly shown in the GUI.
  * Incorrect recipes were shown if client and server have different configs.

## [12.4.7]

* Fixed server freeze when bedrock exists where (old) quarry tries to place frame.
  * This problem doesn't happen with new quarry.
  * The frozen happens if unbreakable block(hardness = -1) is in the area of frame, yellow line of guide.
  * It seems just quarry stops, but you can't interact anything in the world. The quarry fell into infinite loop to
    search blocks to break, resulting in nothing found, and loop, loop.
  * Close [#117](https://github.com/Kotori316/QuarryPlus/issues/117)

## [12.4.6]

* Added config options
  * Tick Delays for quarries, which eventually make quarries slow.
  * DisableFillerMode, which will disable filler mode of quarry, that removes blocks very fast.

## [12.4.5]

* Catch errors of loading black list.

## [12.4.4]

* Improved filler work.
  * It can place colored wools.
  * Solar panels from IC2 and EnderIO, too

## [12.4.3]

* Removed unnecessary method of WorkbenchPlus

## [12.4.1]

* Template improved
  * Fortune Template setting will be applied to quarry correctly.

## [12.4.0]

* You can use ListEditor and Template for New QuarryPlus.
  * It's useful when quarry has both Fortune and Silktouch.
  * Changing `DisableEnchantMoverFromBook` false allows you to move both enchantments at the same time.

## [12.3.9]

* Fixed the quarry fired a harvesting event every tick.
* Use own pickaxe to break blocks. (Harvesting Level = 5 (2 level upper than diamond), Tier = DIAMOND)

## [12.3.8]

* Fixed that quarry stopped when building frames.
* Fixed the old quarry required energy before replacing fluids.
* Added a recipe to convert from Old Quarry Plus to Quarry Plus.

## [12.3.7]

* Improved quarry work.
* Reduced energy to remove fluids. (Unbreaking of machine will be applied.)
* Fixed the quarry was too faster. #85
* Log energy consumption detail.

## [12.3.6]

- Changed - Machines now accept "Insane Voltage"(up to 8192 EU/t).

## [12.3.5]

- Fixed - Quarry freezes when mining bedrock ore if you use low energy supplier(less than 1000 RF/t).

## [12.3.4]

- Removed log message of QuarryStorage.
- QuarryPlus keeps order of items to output to chest.
- QuarryPlus now push fluid every 1 second.
- Added black list for mining machines.
- Support ores added by [Bedrock Ores](https://www.curseforge.com/minecraft/mc-mods/bedrockores).

## [12.3.3]

- Softer attachment blocks. (Pump, ExpPump, Replacer)
- Make tile blocks removable when IC2 new wrench is used.
- Update dependencies. (IC2 and BC)

## [12.3.2]

- Fixed issue of energy setting.

## [12.3.1]

- Added `delete` button to adv pump.
- Improved chain breaking of quarry frame
- Added a filler. (disabled in default)

## [12.3.0]

- Working on a crash when you remove working pump.

## [12.2.9]

- Working on an issue where quarry stops when removing sea.

## [12.2.8]

- Added - Status Checker GUI for QuarryPlus. Enchantments, requiring energy and internal storage info will be shown.
  [#69](https://github.com/Kotori316/QuarryPlus/issues/69)

## [12.2.7]

- Trying to fix a crash of `IllegalArgumentException` in using new quarry. [#68](https://github.com/Kotori316/QuarryPlus/issues/68)

## [12.2.6]

- Fixed - a render issue with other mods.
- Added - git hush to jar file.

## [12.2.5]

- Fixed - Solid Fuel Quarry consumed lava bucket.
- Improvement - Workbench act as receiving energy for 10 seconds when `NoEnergy` is `true`.
- Improvement - Solid Fuel Quarry drops fuel item when removed.
- Improvement - Solid Fuel Quarry shows fuel count on its GUI.
- Improvement - gather items from wider area.
- Improvement - TorchModule place torch only once a tick.

## [12.2.4]

- Added - module system for Chunk Destroyer. [#53](https://github.com/Kotori316/QuarryPlus/issues/53)
- Improvement - You can get the y-level of machine in a tile's inventory GUI.
- Fixed - TorchModule placed torch at invalid position.
- Fixed - The state of ExpPump wasn't changed when xps are collected.
- Fixed - Now all machines respect setting option `Disabled`.

## [12.2.3]

* Added torch module and fuel module. [#61](https://github.com/Kotori316/QuarryPlus/issues/61)
* Gather drops before finishing quarry work.
* Added a predicate that prevent from inserting same kind of module.
* Unique return type
* Added tick event
* Changed field name
* Inline Symbol instance
* Send the localized name when quarry restarted
* Update lang file
* Support chain breaking of MCAssistant.
* Use BC's marker to make dig area

## [12.2.2]

* Fixed - NPE when pump module is loaded.
* Improvement - reduced energy to remove fluids.
* Fixed #60
* The new quarry connects to fluid pipe.
* Fixed - fluid in the new quarry is lost when loading
* Fixed - fluid is replacer to dummy block
* Remove nbt tag when xp is extracted.

## [12.2.1]

* Fixed - wrong reading config option

## [12.2.0]

* Changed the version to {mcVersion}.{major}.{minor}
* Added new quarry, which has module system to enhance its function.

## [1.1.3]

* The position of Quarry's dummy player is set to the point quarry is removing.
* Added support of chain break of [MCAssistant](https://www.curseforge.com/minecraft/mc-mods/mcassistant).

## [1.1.2]

* Reverted [272a28bc](https://github.com/Kotori316/QuarryPlus/commit/272a28bcf97149a24f79f3411a1b6f866b5db518) to avoid
  crash with the old forge.

## [1.1.1]

* Fixed a bug where quarry is reset when work finished if BC's pipe is connected.

## [1.1.0]

* Hide enchantment shown in the tooltip of List Template.
* Fixed a bug of saving nbt of Replacer.
* Added zh_cn.lang

## [1.0.9]

* Added List Template. Right click any block to open GUI, place block on right slot and click "Add". Then, right-click a
  machine with List Template to move setting.

## [1.0.8]

* Now you can't move enchantments to machines which is incompatible with the enchantment. (e.g., You can't move
  Silktouch to machine with Smashing or Smelting from CoFHCore, Auto-Smelt from Ender Core.)

## [1.0.7]

* PumpPlus can be enchanted with both Fortune and Silktouch is BookEnchMover is enabled.
* Update for New Wrench of IC2.
* Use ObfuscationReflectionHelper as refraction helper.

## [1.0.6]

* Internal changes. Use long instead of double to store energy.
* Allowed QuarryPlus to be enchanted with Fortune AND Silktouch at the same time. [#51](https://github.com/Kotori316/QuarryPlus/issues/51)

## [1.0.5]

* Sneak and right click with AE wrench and other mods' one to remove machine.
* Reduced log spam.

## [1.0.4]

* Add NoFrameMode to ChunkDestroyer. The mode skips building frame and start breaking blocks immediately.
* Add some tooltips to attachment machines. e.g., pumps
* Removed duplicated code.

## [1.0.3]

* Add a new item, "Quarry Y Setter", which allows you to stop quarry work at a specific y level.
* Machines with ability of changing its texture when working have comparator output. (When working 15, otherwise 0)

## [1.0.2]

* Support BuildCraft 7.99.21 and newer. Close [#47](https://github.com/Kotori316/QuarryPlus/issues/47).

## [1.0.1]

* Working to fix a bug where QuarryPlus stops its work while removing flowing liquid with PumpPlus in frame mode. If you
  have this problem, right-click the PumpPlus with a stick or wrench to change PumpPlus's mode.
* Made quarry head moving much faster. Change FastQuarryHeadMove in the config to true.

## [1.0.0]

* Now you can modify recipes of WorkbenchPlus. Copy file from `config/quarryplus/defaultrecipes`
  to `config/quarryplus/recipes` and change `UseHardCodeRecipe` in config to false.
* Made Frame mode work faster.

## [0.11.2]

* Fixed a crash when you shift-clicked item in WorkbenchPlus.
* Added a condition quarryplus:machine_enabled, which checks if a machine which has the name in "value" field is
  enabled.
* Fixed an issue where you can't craft items with WorkbenchPlus due to recreating tile every tick.

## [0.11.1]

* Now SolidQuarry doesn't push fuels to hopper. Close [#43](https://github.com/Kotori316/QuarryPlus/issues/43).

## [0.11.0]

* Remove pipes when MiningwellPlus is removed. Close [#42](https://github.com/Kotori316/QuarryPlus/issues/42)
* Fixed java.lang.NullPointerException at EnergyDebug#getTime when tile is loaded.
* Config now outputs quarryplus_noDigBlocks.json.

## [0.10.9]

* Fixed a problem where quarry didn't find a marker which implemented buildcraft.api.core.IAreaProvider.
* Changed logic to search markers.

## [0.10.8]

* Internal changes

## [0.10.7]

* Fixed a crash due to NullPointerException in FakePlayer. [#41](https://github.com/Kotori316/QuarryPlus/issues/41)
* Updated for new JEI newer than 4.12.0
