package net.runelite.cache;

import com.google.gson.Gson;
import net.runelite.cache.definitions.ObjectDefinition;
import net.runelite.cache.fs.Store;
import net.runelite.cache.region.Location;
import net.runelite.cache.region.Position;
import net.runelite.cache.region.Region;
import net.runelite.cache.region.RegionLoader;
import net.runelite.cache.util.XteaKeyManager;
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
                    for (int z = 0; z < 1; z++) {
                        final boolean isBridge = (region.getTileSetting(z + 1, dx, dy) & 2) != 0;
                        final int gameZ = isBridge ? z + 1: z;
                        final boolean isWalkable = (region.getTileSetting(gameZ, dx, dy) & 1) == 0;
                        if (isWalkable) {
                            Position pos = new Position(region.getBaseX() + dx, region.getBaseY() + dy, z);
                            walkableTiles.add(pos);
                        }
                    }
                }
            }
        }

        logger.info("Gathering obstacles");
        for (Region region : this.regions) {
            for (Location location : region.getLocations()) {
                final int localX = location.getPosition().getX() - region.getBaseX();
                final int localY = location.getPosition().getY() - region.getBaseY();
                final int gameZ = location.getPosition().getZ();

                final boolean isBlocked = (region.getTileSetting(gameZ, localX, localY) & 24) != 0;
                if (isBlocked) {
                    continue;
                }

                if(gameZ < 3) {
                    final boolean isBridgeAbove = (region.getTileSetting(gameZ + 1, localX, localY) & 2) != 0;
                    if (isBridgeAbove) {
                        continue;
                    }
                }

                final boolean isBridge = (region.getTileSetting(gameZ, localX, localY) & 2) != 0;
                final Position normalizedPosition = new Position(location.getPosition().getX(), location.getPosition().getY(), isBridge ? gameZ - 1 : gameZ);

                // Filter
                if (normalizedPosition.getZ() != 0) {
                    continue;
                }

                if (location.getType() == 0) {
                    // Lateral direction blocked
                    switch (location.getOrientation()) {
                        case 0:
                            obstaclesMap.markLeftBlocked(normalizedPosition);
                            break;
                        case 1:
                            obstaclesMap.markTopBlocked(normalizedPosition);
                            break;
                        case 2:
                            obstaclesMap.markRightBlocked(normalizedPosition);
                            break;
                        case 3:
                            obstaclesMap.markBottomBlocked(normalizedPosition);
                            break;
                    }
                } else if (location.getType() == 2) {
                    // Diagonal direction blocked, blocks both lateral ways
                    switch (location.getOrientation()) {
                        case 0:
                            obstaclesMap.markTopBlocked(normalizedPosition);
                            obstaclesMap.markLeftBlocked(normalizedPosition);
                            break;
                        case 1:
                            obstaclesMap.markTopBlocked(normalizedPosition);
                            obstaclesMap.markRightBlocked(normalizedPosition);
                            break;
                        case 2:
                            obstaclesMap.markBottomBlocked(normalizedPosition);
                            obstaclesMap.markRightBlocked(normalizedPosition);
                            break;
                        case 3:
                            obstaclesMap.markBottomBlocked(normalizedPosition);
                            obstaclesMap.markLeftBlocked(normalizedPosition);
                            break;
                    }
                } else if (location.getType() == 9) {
                    // All sides blocked
                    obstaclesMap.markAllSidesBlocked(normalizedPosition);
                } else if (location.getType() == 10) {
                    // Game object covers tiles
                    final ObjectDefinition object = this.objectManager.getObject(location.getId());
                    final int width = location.getOrientation() % 2 == 1 ? object.getSizeY() : object.getSizeX();
                    final int height = location.getOrientation() % 2 == 1 ? object.getSizeX() : object.getSizeY();

                    for (int dx = 0; dx < width; dx++) {
                        for (int dy = 0; dy < height; dy++) {
                            final Position position = new Position(normalizedPosition.getX() + dx, normalizedPosition.getY() + dy, normalizedPosition.getZ());
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

    static class ObstaclesMap {
        private final HashMap<Position, TileObstacles> map = new HashMap<>();

        public void markLeftBlocked(Position position) {
            this.getObstacle(position).leftBlocked = true;
        }

        public void markRightBlocked(Position position) {
            this.getObstacle(position).rightBlocked = true;
        }

        public void markTopBlocked(Position position) {
            this.getObstacle(position).topBlocked = true;
        }

        public void markBottomBlocked(Position position) {
            this.getObstacle(position).bottomBlocked = true;
        }

        public void markAllSidesBlocked(Position position) {
            this.markLeftBlocked(position);
            this.markRightBlocked(position);
            this.markTopBlocked(position);
            this.markBottomBlocked(position);
        }

        public MovementDump writeToMovementDump(MovementDump movementDump) {
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
            return movementDump;
        }

        private TileObstacles getObstacle(Position position) {
            if (!this.map.containsKey(position)) {
                this.map.put(position, new TileObstacles());
            }
            return this.map.get(position);
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
