package net.runelite.cache.movementdumper;

import com.google.gson.Gson;
import net.runelite.cache.ObjectManager;
import net.runelite.cache.definitions.ObjectDefinition;
import net.runelite.cache.fs.Store;
import net.runelite.cache.region.Location;
import net.runelite.cache.region.Position;
import net.runelite.cache.region.Region;
import net.runelite.cache.region.RegionLoader;
import net.runelite.cache.util.XteaKeyManager;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MovementDumper {
    private static final String CACHE_DIR = "C:\\Users\\Oli\\Desktop\\Code\\OSRS Navigator\\wiki maps pathfinding project\\movement dumper\\2022-12-13-rev210\\cache";
    private static final String XTEAKEYS_FILE = "C:\\Users\\Oli\\Desktop\\Code\\OSRS Navigator\\wiki maps pathfinding project\\movement dumper\\2022-12-13-rev210\\xteas_old_format.json";
    private static final String OUTPUT_FILE_ARCHIVE = "C:\\Users\\Oli\\Desktop\\Code\\OSRS Navigator\\wiki maps pathfinding project\\spring restructure\\pathfinder\\src\\main\\resources\\movement.csv.zip";
    private static final String OUTPUT_FILE_ARCHIVE_ENTRY = "movement.csv";
    private static final int REGION_SIZE = 64;

    private final Logger logger = LoggerFactory.getLogger(MovementDumper.class);

    private Collection<Region> regions;
    private ObjectManager objectManager;

    @Ignore
    @Test
    public void extractAndDumpMovementData() throws IOException {
        logger.info("Start");
        this.init();
        logger.info("Init done");

        final TilesMap tilesMap = new TilesMap();

        logger.info("Gathering walkable tiles");
        this.gatherWalkableTiles(tilesMap);

        logger.info("Gathering obstacles");
        this.gatherObstacles(tilesMap);

        logger.info("writing dump");
        // Write result to old format
        final String output = this.prepareMovementDump(tilesMap);
        // TODO
        // final String output = this.prepareCsvDump(tilesMap);
        this.writeToZip(output);
        logger.info("done");
    }

    @Deprecated
    private String prepareMovementDump(final TilesMap tilesMap) {
        final MovementDump movementDump = new MovementDump();
        movementDump.walkable = tilesMap.map.keySet();
        movementDump.obstaclePositions = new LinkedList<>();
        movementDump.obstacleValues = new LinkedList<>();
        for (Map.Entry<Position, TileObstacles> entry : tilesMap.map.entrySet()) {
            final Position position = entry.getKey();
            final TileObstacles obstacle = entry.getValue();
            int obstacleValue = 0;
            if (obstacle.topBlocked) {
                obstacleValue += 1;
            }
            if (obstacle.rightBlocked) {
                obstacleValue += 2;
            }
            if (obstacle.bottomBlocked) {
                obstacleValue += 4;
            }
            if (obstacle.leftBlocked) {
                obstacleValue += 8;
            }
            if (obstacleValue != 0) {
                movementDump.obstaclePositions.add(position);
                movementDump.obstacleValues.add(obstacleValue);
            }
        }
        return new Gson().toJson(movementDump);
    }

    private void gatherWalkableTiles(final TilesMap tilesMap) {
        for (Region region : this.regions) {
            for (int dx = 0; dx < REGION_SIZE; dx++) {
                for (int dy = 0; dy < REGION_SIZE; dy++) {
                    for (int z = 0; z <= 3; z++) {
                        final TileSettings tileSettings = new TileSettings(region, new Position(dx, dy, z));
                        if (!tileSettings.isWalkable || tileSettings.isBlocked || tileSettings.isBridgeAbove) {
                            continue;
                        }
                        final Position normalizedAbsolutePos = new Position(region.getBaseX() + dx, region.getBaseY() + dy, tileSettings.isBridge ? z - 1 : z);

                        // Filter z = 0
                        if (normalizedAbsolutePos.getZ() != 0) {
                            continue;
                        }
                        tilesMap.addTile(normalizedAbsolutePos);
                    }
                }
            }
        }
    }

    private void writeToZip(final String output) throws IOException {
        try (final FileOutputStream out = new FileOutputStream(OUTPUT_FILE_ARCHIVE); final ZipOutputStream zipOut = new ZipOutputStream(out)) {
            ZipEntry zipEntry = new ZipEntry(OUTPUT_FILE_ARCHIVE_ENTRY);
            zipOut.putNextEntry(zipEntry);
            zipOut.write(output.getBytes(StandardCharsets.UTF_8));
            zipOut.closeEntry();
        }
    }

    private void gatherObstacles(final TilesMap tilesMap) {
        for (Region region : this.regions) {
            for (Location location : region.getLocations()) {
                final Position relativePosition = new Position(location.getPosition().getX() - region.getBaseX(), location.getPosition().getY() - region.getBaseY(), location.getPosition().getZ());

                final TileSettings tileSettings = new TileSettings(region, relativePosition);
                if (tileSettings.isBlocked || tileSettings.isBridgeAbove) {
                    continue;
                }
                final Position normalizedAbsolutePosition = new Position(location.getPosition().getX(), location.getPosition().getY(), tileSettings.isBridge ? location.getPosition().getZ() - 1 : location.getPosition().getZ());

                // Filter
                if (normalizedAbsolutePosition.getZ() != 0) {
                    continue;
                }

                if (location.getType() == 0) {
                    // Lateral direction blocked
                    switch (location.getOrientation()) {
                        case 0:
                            tilesMap.markLeftBlocked(normalizedAbsolutePosition);
                            break;
                        case 1:
                            tilesMap.markTopBlocked(normalizedAbsolutePosition);
                            break;
                        case 2:
                            tilesMap.markRightBlocked(normalizedAbsolutePosition);
                            break;
                        case 3:
                            tilesMap.markBottomBlocked(normalizedAbsolutePosition);
                            break;
                    }
                } else if (location.getType() == 2) {
                    // Diagonal direction blocked, blocks both lateral ways
                    switch (location.getOrientation()) {
                        case 0:
                            tilesMap.markTopBlocked(normalizedAbsolutePosition);
                            tilesMap.markLeftBlocked(normalizedAbsolutePosition);
                            break;
                        case 1:
                            tilesMap.markTopBlocked(normalizedAbsolutePosition);
                            tilesMap.markRightBlocked(normalizedAbsolutePosition);
                            break;
                        case 2:
                            tilesMap.markBottomBlocked(normalizedAbsolutePosition);
                            tilesMap.markRightBlocked(normalizedAbsolutePosition);
                            break;
                        case 3:
                            tilesMap.markBottomBlocked(normalizedAbsolutePosition);
                            tilesMap.markLeftBlocked(normalizedAbsolutePosition);
                            break;
                    }
                } else if (location.getType() == 9) {
                    // All sides blocked
                    tilesMap.markAllSidesBlocked(normalizedAbsolutePosition);
                } else if (location.getType() == 10) {
                    // Game object covers tiles
                    final ObjectDefinition object = this.objectManager.getObject(location.getId());
                    final int width = location.getOrientation() % 2 == 1 ? object.getSizeY() : object.getSizeX();
                    final int height = location.getOrientation() % 2 == 1 ? object.getSizeX() : object.getSizeY();

                    for (int dx = 0; dx < width; dx++) {
                        for (int dy = 0; dy < height; dy++) {
                            final Position position = new Position(normalizedAbsolutePosition.getX() + dx, normalizedAbsolutePosition.getY() + dy, normalizedAbsolutePosition.getZ());
                            tilesMap.markAllSidesBlocked(position);
                        }
                    }
                }
            }
        }
    }

    private void init() throws IOException {
        final Store cacheStore = new Store(new File(CACHE_DIR));
        cacheStore.load();

        final XteaKeyManager keyManager = new XteaKeyManager();
        try (final FileInputStream in = new FileInputStream(XTEAKEYS_FILE)) {
            keyManager.loadKeys(in);
        }

        final RegionLoader regionLoader = new RegionLoader(cacheStore, keyManager);
        regionLoader.loadRegions();

        final ObjectManager objectManager = new ObjectManager(cacheStore);
        objectManager.load();

        this.regions = regionLoader.getRegions();
        this.objectManager = objectManager;
    }

    @Deprecated
    private String prepareMovementDump(final TilesMap tilesMap) {
        final MovementDump movementDump = new MovementDump();
        movementDump.walkable = tilesMap.map.keySet();
        movementDump.obstaclePositions = new LinkedList<>();
        movementDump.obstacleValues = new LinkedList<>();
        for (Map.Entry<Position, TileObstacles> entry : tilesMap.map.entrySet()) {
            final Position position = entry.getKey();
            final TileObstacles obstacle = entry.getValue();
            int obstacleValue = 0;
            if (obstacle.topBlocked) {
                obstacleValue += 1;
            }
            if (obstacle.rightBlocked) {
                obstacleValue += 2;
            }
            if (obstacle.bottomBlocked) {
                obstacleValue += 4;
            }
            if (obstacle.leftBlocked) {
                obstacleValue += 8;
            }
            if (obstacleValue != 0) {
                movementDump.obstaclePositions.add(position);
                movementDump.obstacleValues.add(obstacleValue);
            }
        }
        return new Gson().toJson(movementDump);
    }

}
