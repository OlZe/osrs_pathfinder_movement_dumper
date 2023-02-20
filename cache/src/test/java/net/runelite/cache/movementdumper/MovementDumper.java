package net.runelite.cache.movementdumper;

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
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MovementDumper {
    public static final HashSet<Integer> LOCATION_TYPES_ALWAYS_WALKABLE = new HashSet<>(Arrays.asList(1, 3, 4, 5, 6, 7, 8));
    private static final String CACHE_DIR = "C:\\Users\\Oli\\Desktop\\Code\\OSRS Navigator\\wiki maps pathfinding project\\movement dumper\\2023-02-15-rev211\\";
    private static final String CACHE_DIR_XTEAKEYS_FILE = "xteas_old_format.json";
    private static final String OUTPUT_FILE_ARCHIVE = "C:\\Users\\Oli\\Desktop\\Code\\OSRS Navigator\\wiki maps pathfinding project\\spring restructure\\pathfinder\\src\\main\\resources\\movement.csv.zip";
    private static final String OUTPUT_FILE_ARCHIVE_ENTRY = "movement.csv";

    private final Logger logger = LoggerFactory.getLogger(MovementDumper.class);
    private ObjectManager objectManager;
    private PositionUtils positionUtils;
    private Map<RegionPosition, List<Teleport>> teleports;
    private Map<RegionPosition, List<Transport>> transports;
    private Collection<Region> regions;
    private HashSet<RegionPosition> positionsBlockedByBigObjects = new HashSet<>();


    @Ignore
    @Test
    public void extractAndDumpMovementData() throws IOException {
        logger.info("Init");
        this.init();
        logger.info("Init done");

        logger.info("Mark positions blocked by big objects");
        this.markPositionsBlockedByBigObjects();

        final RegionPosition start = this.positionUtils.toRegionPosition(new Position(3234, 3225, 0));
        logger.info("Starting exploration");
        final HashSet<RegionPosition> result = this.explore(start);
        logger.info("Exploration done: " + result.size() + " positions found");

        logger.info("writing dump");
        this.writeCsvDump(result);
        logger.info("done");
    }

    private HashSet<RegionPosition> explore(final RegionPosition startPosition) {
        if (!startPosition.isWalkable() || this.positionsBlockedByBigObjects.contains(startPosition)) {
            throw new Error("bfs: Start tile " + PositionUtils.toAbsolute(startPosition) + " is not walkable");
        }

        final Queue<RegionPosition> openList = new LinkedList<>();
        openList.add(startPosition);
        final HashSet<RegionPosition> closedList = new HashSet<>();

        while (openList.peek() != null) {
            final RegionPosition currentPos = openList.remove();

            if (closedList.contains(currentPos)) {
                continue;
            }
            closedList.add(currentPos);

            openList.addAll(this.getDirectNeighbours(currentPos));

            if (closedList.size() % 20000 == 0) {
                logger.info("Explored " + closedList.size() + " positions.");
            }
        }

        return closedList;
    }

    private void markPositionsBlockedByBigObjects() {
        this.regions.stream()
                .flatMap(r -> r.getLocations().stream())
                .filter(l -> !LOCATION_TYPES_ALWAYS_WALKABLE.contains(l.getType()))
                .filter(l -> {
                    final ObjectDefinition object = objectManager.getObject(l.getId());
                    return object.getSizeX() > 1 || object.getSizeY() > 1;
                })
                .filter(this::isBlockedByObject)
                .forEach(l -> {
                    final ObjectDefinition object = objectManager.getObject(l.getId());
                    final RegionPosition position = this.positionUtils.toRegionPosition(l.getPosition());
                    final int width = l.getOrientation() % 2 == 1 ? object.getSizeY() : object.getSizeX();
                    final int height = l.getOrientation() % 2 == 1 ? object.getSizeX() : object.getSizeY();
                    markPositionsBlockedFromTo(position, width, height);
                });
    }

    private void markPositionsBlockedFromTo(RegionPosition from, int width, int height) {
        for (int dx = 0; dx < width; dx++) {
            for (int dy = 0; dy < height; dy++) {
                final RegionPosition current = positionUtils.move(from, dx, dy);
                this.positionsBlockedByBigObjects.add(current);
            }
        }
    }

    private boolean isBlockedByObject(Location location) {
        final ObjectDefinition object = this.objectManager.getObject(location.getId());
        if (location.getType() == 22) {
            return object.getInteractType() == 1;
        } else {
            return object.getInteractType() != 0;
        }
    }

    private Collection<RegionPosition> getDirectNeighbours(RegionPosition position) {
        List<RegionPosition> neighbours = new LinkedList<>();

        // Walkable neighbours
        final RegionPosition north = getNorthIfWalkable(position);
        if (north != null) neighbours.add(north);

        final RegionPosition northEast = getNorthEastIfWalkable(position);
        if (northEast != null) neighbours.add(northEast);

        final RegionPosition east = getEastIfWalkable(position);
        if (east != null) neighbours.add(east);

        final RegionPosition southEast = getSouthEastIfWalkable(position);
        if (southEast != null) neighbours.add(southEast);

        final RegionPosition south = getSouthIfWalkable(position);
        if (south != null) neighbours.add(south);

        final RegionPosition southWest = getSouthWestIfWalkable(position);
        if (southWest != null) neighbours.add(southWest);

        final RegionPosition west = getWestIfWalkable(position);
        if (west != null) neighbours.add(west);

        final RegionPosition northWest = getNorthWestIfWalkable(position);
        if (northWest != null) neighbours.add(northWest);

//        // Transports from this position
//        this.transports.getOrDefault(position, new ArrayList<>(0))
//                .stream().map(t -> t.to)
//                .forEachOrdered(neighbours::add);


        return neighbours;
    }

    private RegionPosition getNorthIfWalkable(RegionPosition position) {
        final RegionPosition northPosition = this.positionUtils.move(position, 0, 1);
        if (northPosition != null
                && !this.positionsBlockedByBigObjects.contains(northPosition)
                && northPosition.isWalkable()
                && !northPosition.obstacles.southBlocked
                && !position.obstacles.northBlocked) {
            return northPosition;
        }
        return null;
    }

    private RegionPosition getEastIfWalkable(RegionPosition position) {
        final RegionPosition eastPosition = this.positionUtils.move(position, 1, 0);
        if (eastPosition != null
                && !this.positionsBlockedByBigObjects.contains(eastPosition)
                && eastPosition.isWalkable()
                && !eastPosition.obstacles.westBlocked
                && !position.obstacles.eastBlocked) {
            return eastPosition;
        }
        return null;
    }

    private RegionPosition getSouthIfWalkable(RegionPosition position) {
        final RegionPosition southPosition = this.positionUtils.move(position, 0, -1);
        if (southPosition != null
                && !this.positionsBlockedByBigObjects.contains(southPosition)
                && southPosition.isWalkable()
                && !southPosition.obstacles.northBlocked
                && !position.obstacles.southBlocked) {
            return southPosition;
        }
        return null;
    }

    private RegionPosition getWestIfWalkable(RegionPosition position) {
        final RegionPosition westPosition = this.positionUtils.move(position, -1, 0);
        if (westPosition != null
                && !this.positionsBlockedByBigObjects.contains(westPosition)
                && westPosition.isWalkable()
                && !westPosition.obstacles.eastBlocked
                && !position.obstacles.westBlocked) {
            return westPosition;
        }
        return null;
    }

    private RegionPosition getNorthEastIfWalkable(RegionPosition position) {
        final RegionPosition north = this.getNorthIfWalkable(position);
        final RegionPosition east = this.getEastIfWalkable(position);
        if (north != null && east != null) {
            final RegionPosition northEast = this.getEastIfWalkable(north);
            final RegionPosition eastNorth = this.getNorthIfWalkable(east);
            if (northEast != null && eastNorth != null) {
                return northEast;
            }
        }
        return null;
    }

    private RegionPosition getNorthWestIfWalkable(RegionPosition position) {
        final RegionPosition north = this.getNorthIfWalkable(position);
        final RegionPosition west = this.getWestIfWalkable(position);
        if (north != null && west != null) {
            final RegionPosition northWest = this.getWestIfWalkable(north);
            final RegionPosition westNorth = this.getNorthIfWalkable(west);
            if (northWest != null && westNorth != null) {
                return northWest;
            }
        }
        return null;
    }

    private RegionPosition getSouthEastIfWalkable(RegionPosition position) {
        final RegionPosition south = this.getSouthIfWalkable(position);
        final RegionPosition east = this.getEastIfWalkable(position);
        if (south != null && east != null) {
            final RegionPosition southEast = this.getEastIfWalkable(south);
            final RegionPosition eastSouth = this.getSouthIfWalkable(east);
            if (southEast != null && eastSouth != null) {
                return southEast;
            }
        }
        return null;
    }

    private RegionPosition getSouthWestIfWalkable(RegionPosition position) {
        final RegionPosition south = this.getSouthIfWalkable(position);
        final RegionPosition west = this.getWestIfWalkable(position);
        if (south != null && west != null) {
            final RegionPosition southWest = this.getWestIfWalkable(south);
            final RegionPosition westSouth = this.getSouthIfWalkable(west);
            if (southWest != null && westSouth != null) {
                return southWest;
            }
        }
        return null;
    }

    private void writeCsvDump(final Collection<RegionPosition> regionPositions) throws IOException {
        try (final FileOutputStream fileOut = new FileOutputStream(OUTPUT_FILE_ARCHIVE);
             final ZipOutputStream zipOut = new ZipOutputStream(fileOut);
             final OutputStreamWriter writerOut = new OutputStreamWriter(zipOut, StandardCharsets.UTF_8);
             final BufferedWriter out = new BufferedWriter(writerOut)) {

            final ZipEntry zipEntry = new ZipEntry(OUTPUT_FILE_ARCHIVE_ENTRY);
            zipOut.putNextEntry(zipEntry);
            out.write("# x,y,z,topBlocked,rightBlocked,bottomBlocked,leftBlocked");
            for (RegionPosition regionPosition : regionPositions) {
                final Position absolutePosition = PositionUtils.toAbsolute(regionPosition);

                out.write('\n');
                out.write(Integer.toString(absolutePosition.getX()));
                out.write(',');
                out.write(Integer.toString(absolutePosition.getY()));
                out.write(',');
                out.write(Integer.toString(absolutePosition.getZ()));
                out.write(',');
                out.write(Boolean.toString(regionPosition.obstacles.northBlocked));
                out.write(',');
                out.write(Boolean.toString(regionPosition.obstacles.eastBlocked));
                out.write(',');
                out.write(Boolean.toString(regionPosition.obstacles.southBlocked));
                out.write(',');
                out.write(Boolean.toString(regionPosition.obstacles.westBlocked));
            }
        }
    }

    private void init() throws IOException {
        final Store cacheStore = new Store(new File(CACHE_DIR + "\\cache"));
        cacheStore.load();

        final XteaKeyManager keyManager = new XteaKeyManager();
        try (final FileInputStream in = new FileInputStream(CACHE_DIR + "\\" + CACHE_DIR_XTEAKEYS_FILE)) {
            keyManager.loadKeys(in);
        }

        final RegionLoader regionLoader = new RegionLoader(cacheStore, keyManager);
        regionLoader.loadRegions();
        this.regions = regionLoader.getRegions();

        final ObjectManager objectManager = new ObjectManager(cacheStore);
        objectManager.load();
        this.objectManager = objectManager;

        this.positionUtils = new PositionUtils(regions, objectManager);

        final DataDeserializer.TeleportsAndTransports teleportsAndTransports =
                new DataDeserializer().readTeleportsAndTransports(this.positionUtils);
        this.teleports = teleportsAndTransports.teleports;
        this.transports = teleportsAndTransports.transports;
    }
}
