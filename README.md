# OSRS Pathfinder Movement Dumper

This project is part of the [OSRS Pathfinder Project](https://github.com/OlZe/osrs_pathfinder).

It uses the [runelite project](https://github.com/runelite/runelite) to extract map data out of cache files and write them into a file. The data for all walkable tiles as well as their directional movement blockers are extracted. Transport and teleport data must be gathered elsewhere.

# Usage

Download the latest [OSRS cache files](https://archive.runestats.com/osrs/) and extract them. Modify `xteas.json` such that the fields `mapsquare` and `key` are renamed into `region` and `keys` respectively.

Modify the [MovementDumper](https://github.com/OlZe/osrs_pathfinder_movement_dumper/blob/MovementDumper/cache/src/test/java/net/runelite/cache/MovementDumper.java) class such that the constants `CACHE_DIR`, `XTEAKEYS_FILE` and `OUTPUT_FILE_ARCHIVE` point to the right locations.

Run `net.runelite.cache.MovementDumper::extractAndDumpMovementData` as a Test.