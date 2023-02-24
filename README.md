# OSRS Pathfinder Movement Dumper

This project is part of the [OSRS Pathfinder Project](https://github.com/OlZe/osrs_pathfinder).

It uses the [runelite project](https://github.com/runelite/runelite) to extract walkable positions out of cache files and write them into a .zip dump. The dumped data includes: all walkable tiles with their respective directional movement blockers (eg walls on the west side of a tile), all game teleports (eg Lumbridge teleport) and game transports (eg Ladders, Stairs, Cave entrances, ...).

Transport and teleport data was gathered by hand and partially copied from [Skretzo's shortest path plugin](https://github.com/Skretzo/shortest-path). It is saved in the `cache/src/test/resources/net/runelite/cache/movementdumper/` directory.

# Usage

Download the latest [OSRS cache files](https://archive.runestats.com/osrs/) and extract them. Modify `xteas.json` such that the fields `mapsquare` and `key` are renamed into `region` and `keys` respectively.

Modify the [MovementDumper](https://github.com/OlZe/osrs_pathfinder_movement_dumper/blob/MovementDumper/cache/src/test/java/net/runelite/cache/movementdumper/MovementDumper.java) class such that the constants `CACHE_DIR`, `XTEAKEYS_FILE` and `OUTPUT_ARCHIVE` point to the right paths.

Run `net.runelite.cache.movementdumper.MovementDumper#extractAndDumpMovementData` as a unit test.