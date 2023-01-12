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

        final WalkableTilesMap walkableTilesMap = new WalkableTilesMap();

        logger.info("Gathering walkable tiles");
        this.gatherWalkableTiles(walkableTilesMap);

        logger.info("Gathering obstacles");
        this.gatherObstacles(walkableTilesMap);

        logger.info("writing dump");
        this.writeCsvDump(walkableTilesMap);
        logger.info("done");
    }

    private void writeCsvDump(final WalkableTilesMap walkableTilesMap) throws IOException {
        try (final FileOutputStream fileOut = new FileOutputStream(OUTPUT_FILE_ARCHIVE);
             final ZipOutputStream zipOut = new ZipOutputStream(fileOut);
             final OutputStreamWriter writerOut = new OutputStreamWriter(zipOut, StandardCharsets.UTF_8);
             final BufferedWriter out = new BufferedWriter(writerOut)) {

            final ZipEntry zipEntry = new ZipEntry(OUTPUT_FILE_ARCHIVE_ENTRY);
            zipOut.putNextEntry(zipEntry);
            out.write("# x,y,z,topBlocked,rightBlocked,bottomBlocked,leftBlocked");
            for (Map.Entry<Position, TileObstacles> entry : walkableTilesMap.map.entrySet()) {
                final Position position = entry.getKey();
                final TileObstacles tileObstacles = entry.getValue();
                out.write('\n');
                out.write(Integer.toString(position.getX()));
                out.write(',');
                out.write(Integer.toString(position.getY()));
                out.write(',');
                out.write(Integer.toString(position.getZ()));
                out.write(',');
                out.write(Boolean.toString(tileObstacles.topBlocked));
                out.write(',');
                out.write(Boolean.toString(tileObstacles.rightBlocked));
                out.write(',');
                out.write(Boolean.toString(tileObstacles.bottomBlocked));
                out.write(',');
                out.write(Boolean.toString(tileObstacles.leftBlocked));
            }
        }
    }

    private void gatherWalkableTiles(final WalkableTilesMap walkableTilesMap) {
        for (Region region : this.regions) {
            for (int dx = 0; dx < REGION_SIZE; dx++) {
                for (int dy = 0; dy < REGION_SIZE; dy++) {
                    for (int z = 0; z <= 3; z++) {
                        final Position relativePosition = new Position(dx, dy, z);
                        final TileSettings tileSettings = new TileSettings(region, relativePosition);
                        if (!tileSettings.isWalkable || tileSettings.isBlocked || tileSettings.isBridgeAbove) {
                            continue;
                        }
                        final Position normalizedAbsolutePos = PositionUtils.toNormalizedAbsolute(relativePosition, region, tileSettings.isBridge);
                        walkableTilesMap.addWalkableTile(normalizedAbsolutePos);
                    }
                }
            }
        }
    }

    private void gatherObstacles(final WalkableTilesMap walkableTilesMap) {
        for (Region region : this.regions) {
            for (Location location : region.getLocations()) {
                final Position relativePosition = PositionUtils.toRelative(region, location.getPosition());

                final TileSettings tileSettings = new TileSettings(region, relativePosition);
                if (tileSettings.isBlocked || tileSettings.isBridgeAbove) {
                    continue;
                }
                final Position normalizedAbsolutePosition = PositionUtils.toNormalizedAbsolute(relativePosition, region, tileSettings.isBridge);

                if (location.getType() == 0) {
                    // Lateral direction blocked
                    switch (location.getOrientation()) {
                        case 0:
                            walkableTilesMap.markLeftBlocked(normalizedAbsolutePosition);
                            break;
                        case 1:
                            walkableTilesMap.markTopBlocked(normalizedAbsolutePosition);
                            break;
                        case 2:
                            walkableTilesMap.markRightBlocked(normalizedAbsolutePosition);
                            break;
                        case 3:
                            walkableTilesMap.markBottomBlocked(normalizedAbsolutePosition);
                            break;
                    }
                } else if (location.getType() == 2) {
                    // Diagonal direction blocked, blocks both lateral ways
                    switch (location.getOrientation()) {
                        case 0:
                            walkableTilesMap.markTopBlocked(normalizedAbsolutePosition);
                            walkableTilesMap.markLeftBlocked(normalizedAbsolutePosition);
                            break;
                        case 1:
                            walkableTilesMap.markTopBlocked(normalizedAbsolutePosition);
                            walkableTilesMap.markRightBlocked(normalizedAbsolutePosition);
                            break;
                        case 2:
                            walkableTilesMap.markBottomBlocked(normalizedAbsolutePosition);
                            walkableTilesMap.markRightBlocked(normalizedAbsolutePosition);
                            break;
                        case 3:
                            walkableTilesMap.markBottomBlocked(normalizedAbsolutePosition);
                            walkableTilesMap.markLeftBlocked(normalizedAbsolutePosition);
                            break;
                    }
                } else if (location.getType() == 9) {
                    // All sides blocked
                    walkableTilesMap.markAllSidesBlocked(normalizedAbsolutePosition);
                } else if (location.getType() == 10) {
                    // Game object covers tiles
                    final ObjectDefinition object = this.objectManager.getObject(location.getId());
                    final int width = location.getOrientation() % 2 == 1 ? object.getSizeY() : object.getSizeX();
                    final int height = location.getOrientation() % 2 == 1 ? object.getSizeX() : object.getSizeY();

                    for (int dx = 0; dx < width; dx++) {
                        for (int dy = 0; dy < height; dy++) {
                            final Position current = PositionUtils.move(normalizedAbsolutePosition, dx, dy, 0);
                            walkableTilesMap.markAllSidesBlocked(current);
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
    private String prepareMovementDump(final WalkableTilesMap walkableTilesMap) {
        final MovementDump movementDump = new MovementDump();
        movementDump.walkable = walkableTilesMap.map.keySet();
        movementDump.obstaclePositions = new LinkedList<>();
        movementDump.obstacleValues = new LinkedList<>();
        for (Map.Entry<Position, TileObstacles> entry : walkableTilesMap.map.entrySet()) {
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
