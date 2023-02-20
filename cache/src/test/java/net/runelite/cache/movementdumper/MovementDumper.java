package net.runelite.cache.movementdumper;

import net.runelite.cache.ObjectManager;
import net.runelite.cache.fs.Store;
import net.runelite.cache.region.Position;
import net.runelite.cache.region.Region;
import net.runelite.cache.region.RegionLoader;
import net.runelite.cache.util.XteaKeyManager;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class MovementDumper {
    private static final String CACHE_DIR = "C:\\Users\\Oli\\Desktop\\Code\\OSRS Navigator\\wiki maps pathfinding project\\movement dumper\\2023-02-15-rev211\\";
    private static final String CACHE_DIR_XTEAKEYS_FILE = "xteas_old_format.json";
    private static final String OUTPUT_FILE_ARCHIVE = "C:\\Users\\Oli\\Desktop\\Code\\OSRS Navigator\\wiki maps pathfinding project\\spring restructure\\pathfinder\\src\\main\\resources\\movement.csv.zip";
    private static final String OUTPUT_FILE_ARCHIVE_ENTRY = "movement.csv";

    private final Logger logger = LoggerFactory.getLogger(MovementDumper.class);
    private ObjectManager objectManager;
//    private Map<RegionPosition, List<Teleport>> teleports;
//    private Map<RegionPosition, List<Transport>> transports;
    private Collection<Region> regions;
    private TileManager tileManager;


    @Ignore
    @Test
    public void extractAndDumpMovementData() throws IOException {
        logger.info("Init");
        this.init();
        logger.info("Init done");

        final Position start = new Position(3234, 3225, 0);


        logger.info("Starting exploration");
//        final HashSet<RegionPosition> result = this.explore(start);
//        logger.info("Exploration done: " + result.size() + " positions found");
//
//        logger.info("writing dump");
//        this.writeCsvDump(result);
//        logger.info("done");
    }

    private HashSet<Tile> explore(final Tile startTile) {
        if (!startTile.isWalkable) {
            throw new Error("bfs: Start tile " + startTile.position + " is not walkable");
        }

        final Queue<Tile> openList = new LinkedList<>();
        openList.add(startTile);
        final HashSet<Tile> closedList = new HashSet<>();

        while (openList.peek() != null) {
            final Tile currentTile = openList.remove();

            if (closedList.contains(currentTile)) {
                continue;
            }
            closedList.add(currentTile);

            openList.addAll(tileManager.getDirectNeighbours(currentTile));

            if (closedList.size() % 20000 == 0) {
                logger.info("Explored " + closedList.size() + " positions.");
            }
        }

        return closedList;
    }


//    private void writeCsvDump(final Collection<RegionPosition> regionPositions) throws IOException {
//        try (final FileOutputStream fileOut = new FileOutputStream(OUTPUT_FILE_ARCHIVE);
//             final ZipOutputStream zipOut = new ZipOutputStream(fileOut);
//             final OutputStreamWriter writerOut = new OutputStreamWriter(zipOut, StandardCharsets.UTF_8);
//             final BufferedWriter out = new BufferedWriter(writerOut)) {
//
//            final ZipEntry zipEntry = new ZipEntry(OUTPUT_FILE_ARCHIVE_ENTRY);
//            zipOut.putNextEntry(zipEntry);
//            out.write("# x,y,z,topBlocked,rightBlocked,bottomBlocked,leftBlocked");
//            for (RegionPosition regionPosition : regionPositions) {
//                final Position absolutePosition = PositionUtils.toAbsolute(regionPosition);
//
//                out.write('\n');
//                out.write(Integer.toString(absolutePosition.getX()));
//                out.write(',');
//                out.write(Integer.toString(absolutePosition.getY()));
//                out.write(',');
//                out.write(Integer.toString(absolutePosition.getZ()));
//                out.write(',');
//                out.write(Boolean.toString(regionPosition.obstacles.northBlocked));
//                out.write(',');
//                out.write(Boolean.toString(regionPosition.obstacles.eastBlocked));
//                out.write(',');
//                out.write(Boolean.toString(regionPosition.obstacles.southBlocked));
//                out.write(',');
//                out.write(Boolean.toString(regionPosition.obstacles.westBlocked));
//            }
//        }
//    }

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

        this.objectManager = new ObjectManager(cacheStore);
        this.objectManager.load();

        this.tileManager = new TileManager(this.regions, this.objectManager);

//        final DataDeserializer.TeleportsAndTransports teleportsAndTransports =
//                new DataDeserializer().readTeleportsAndTransports(this.positionUtils);
//        this.teleports = teleportsAndTransports.teleports;
//        this.transports = teleportsAndTransports.transports;
    }
}
