package net.runelite.cache;

import com.google.gson.Gson;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MovementDumperRewrite {
    private static final String CACHE_DIR = "C:\\Users\\Oli\\Desktop\\Code\\OSRS Navigator\\wiki maps pathfinding project\\movement dumper\\2022-12-13-rev210\\cache";
    private static final String XTEAKEYS_FILE = "C:\\Users\\Oli\\Desktop\\Code\\OSRS Navigator\\wiki maps pathfinding project\\movement dumper\\2022-12-13-rev210\\xteas_old_format.json";
    private static final String OUTPUT_FILE_ARCHIVE = "C:\\Users\\Oli\\Desktop\\Code\\OSRS Navigator\\wiki maps pathfinding project\\spring restructure\\pathfinder\\src\\main\\resources\\movement.json.zip.new";
    private static final String OUTPUT_FILE_ARCHIVE_ENTRY = "movement.json";
    private static final int REGION_SIZE = 64;

    private final Logger logger = LoggerFactory.getLogger(MovementDumperRewrite.class);

    // Init in test
    private Collection<Region> regions;
    private ObjectManager objectManager;

    @Ignore
    @Test
    public void dumpMovementData() throws IOException {
        logger.info("Start");
        this.init();
        logger.info("Init done");

        final ObstaclesMap obstaclesMap = new ObstaclesMap();
        final List<Position> walkableTiles = new LinkedList<>();

        logger.info("Gathering walkable tiles");
        for (Region region : this.regions) {
            for (int dx = 0; dx < REGION_SIZE; dx++) {
                for (int dy = 0; dy < REGION_SIZE; dy++) {
                    for (int z = 0; z <= 3; z++) {
                        final TileSettings tileSettings = new TileSettings(region, new Position(dx, dy, z));
                        if (!tileSettings.isWalkable || tileSettings.isBlocked || tileSettings.isBridgeAbove) {
                            continue;
                        }
                        final Position normalizedAbsolutePos = new Position(
                                region.getBaseX() + dx,
                                region.getBaseY() + dy,
                                tileSettings.isBridge ? z - 1 : z);

                        // Filter z = 0
                        if (normalizedAbsolutePos.getZ() != 0) {
                            continue;
                        }
                        walkableTiles.add(normalizedAbsolutePos);
                    }
                }
            }
        }

        logger.info("Gathering obstacles");
        for (Region region : this.regions) {
            for (Location location : region.getLocations()) {
                final Position relativePosition = new Position(
                        location.getPosition().getX() - region.getBaseX(),
                        location.getPosition().getY() - region.getBaseY(),
                        location.getPosition().getZ());

                final TileSettings tileSettings = new TileSettings(region, relativePosition);
                if (tileSettings.isBlocked || tileSettings.isBridgeAbove) {
                    continue;
                }
                final Position normalizedAbsolutePosition = new Position(
                        location.getPosition().getX(),
                        location.getPosition().getY(),
                        tileSettings.isBridge ? location.getPosition().getZ() - 1 : location.getPosition().getZ());

                // Filter
                if (normalizedAbsolutePosition.getZ() != 0) {
                    continue;
                }

                if (location.getType() == 0) {
                    // Lateral direction blocked
                    switch (location.getOrientation()) {
                        case 0:
                            obstaclesMap.markLeftBlocked(normalizedAbsolutePosition);
                            break;
                        case 1:
                            obstaclesMap.markTopBlocked(normalizedAbsolutePosition);
                            break;
                        case 2:
                            obstaclesMap.markRightBlocked(normalizedAbsolutePosition);
                            break;
                        case 3:
                            obstaclesMap.markBottomBlocked(normalizedAbsolutePosition);
                            break;
                    }
                } else if (location.getType() == 2) {
                    // Diagonal direction blocked, blocks both lateral ways
                    switch (location.getOrientation()) {
                        case 0:
                            obstaclesMap.markTopBlocked(normalizedAbsolutePosition);
                            obstaclesMap.markLeftBlocked(normalizedAbsolutePosition);
                            break;
                        case 1:
                            obstaclesMap.markTopBlocked(normalizedAbsolutePosition);
                            obstaclesMap.markRightBlocked(normalizedAbsolutePosition);
                            break;
                        case 2:
                            obstaclesMap.markBottomBlocked(normalizedAbsolutePosition);
                            obstaclesMap.markRightBlocked(normalizedAbsolutePosition);
                            break;
                        case 3:
                            obstaclesMap.markBottomBlocked(normalizedAbsolutePosition);
                            obstaclesMap.markLeftBlocked(normalizedAbsolutePosition);
                            break;
                    }
                } else if (location.getType() == 9) {
                    // All sides blocked
                    obstaclesMap.markAllSidesBlocked(normalizedAbsolutePosition);
                } else if (location.getType() == 10) {
                    // Game object covers tiles
                    final ObjectDefinition object = this.objectManager.getObject(location.getId());
                    final int width = location.getOrientation() % 2 == 1 ? object.getSizeY() : object.getSizeX();
                    final int height = location.getOrientation() % 2 == 1 ? object.getSizeX() : object.getSizeY();

                    for (int dx = 0; dx < width; dx++) {
                        for (int dy = 0; dy < height; dy++) {
                            final Position position = new Position(normalizedAbsolutePosition.getX() + dx, normalizedAbsolutePosition.getY() + dy, normalizedAbsolutePosition.getZ());
                            obstaclesMap.markAllSidesBlocked(position);
                        }
                    }
                }
            }
        }

        // Write result to weird format
        logger.info("writing dump");
        final MovementDump movementDump = new MovementDump();
        obstaclesMap.writeToMovementDump(movementDump);
        movementDump.walkable = walkableTiles;
        final String output = new Gson().toJson(movementDump);
        try (final FileOutputStream out = new FileOutputStream(OUTPUT_FILE_ARCHIVE);
             final ZipOutputStream zipOut = new ZipOutputStream(out)) {
            ZipEntry zipEntry = new ZipEntry(OUTPUT_FILE_ARCHIVE_ENTRY);
            zipOut.putNextEntry(zipEntry);
            zipOut.write(output.getBytes(StandardCharsets.UTF_8));
            zipOut.closeEntry();
        }
        logger.info("done");
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

    private static class TileSettings {
        public final boolean isWalkable;
        public final boolean isBlocked;
        public final boolean isBridge;
        public final boolean isBridgeAbove;

        public TileSettings(Region region, Position relativePosition) {
            final int tileSetting = region.getTileSetting(relativePosition.getZ(), relativePosition.getX(), relativePosition.getY());
            this.isWalkable = (tileSetting & 1) == 0;
            this.isBlocked = (tileSetting & 24) != 0;
            this.isBridge = (tileSetting & 2) != 0;
            if (relativePosition.getZ() < 3) {
                this.isBridgeAbove = (region.getTileSetting(relativePosition.getZ() + 1, relativePosition.getX(), relativePosition.getY()) & 2) != 0;
            } else {
                this.isBridgeAbove = false;
            }
        }
    }

    private static class ObstaclesMap {
        private final HashMap<Position, TileObstacles> map = new HashMap<>();

        public void markLeftBlocked(Position absolutePosition) {
            this.getObstacle(absolutePosition).leftBlocked = true;
        }

        public void markRightBlocked(Position absolutePosition) {
            this.getObstacle(absolutePosition).rightBlocked = true;
        }

        public void markTopBlocked(Position absolutePosition) {
            this.getObstacle(absolutePosition).topBlocked = true;
        }

        public void markBottomBlocked(Position absolutePosition) {
            this.getObstacle(absolutePosition).bottomBlocked = true;
        }

        public void markAllSidesBlocked(Position absolutePosition) {
            this.markLeftBlocked(absolutePosition);
            this.markRightBlocked(absolutePosition);
            this.markTopBlocked(absolutePosition);
            this.markBottomBlocked(absolutePosition);
        }

        public void writeToMovementDump(MovementDump movementDump) {
            movementDump.obstaclePositions = new Position[this.map.size()];
            movementDump.obstacleValues = new int[this.map.size()];
            int index = 0;
            for (Map.Entry<Position, TileObstacles> entry : this.map.entrySet()) {
                Position position = entry.getKey();
                TileObstacles obstacle = entry.getValue();
                movementDump.obstaclePositions[index] = position;
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
                movementDump.obstacleValues[index] = obstacleValue;
                index++;
            }
        }

        private TileObstacles getObstacle(Position absolutePosition) {
            if (!this.map.containsKey(absolutePosition)) {
                this.map.put(absolutePosition, new TileObstacles());
            }
            return this.map.get(absolutePosition);
        }
    }

    private static class TileObstacles {
        public boolean rightBlocked = false;
        public boolean leftBlocked = false;
        public boolean topBlocked = false;
        public boolean bottomBlocked = false;
    }

    private static class MovementDump {
        List<Position> walkable;
        Position[] obstaclePositions;
        int[] obstacleValues;
    }
}
