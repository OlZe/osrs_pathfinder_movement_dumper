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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MovementDumper {
    private static final String CACHE_DIR = "C:\\Users\\Oli\\Desktop\\Code\\OSRS Navigator\\wiki maps pathfinding project\\movement dumper\\2023-02-15-rev211\\";
    private static final String CACHE_DIR_XTEAKEYS_FILE = "xteas_old_format.json";
    private static final String OUTPUT_FILE_ARCHIVE = "C:\\Users\\Oli\\Desktop\\Code\\OSRS Navigator\\wiki maps pathfinding project\\spring restructure\\pathfinder\\src\\main\\resources\\movement.csv.zip";
    private static final String OUTPUT_FILE_ARCHIVE_ENTRY = "movement.csv";

    private final Logger logger = LoggerFactory.getLogger(MovementDumper.class);
    private TileManager tileManager;
    private Map<Position, List<Teleport>> teleports;
    private Map<Position, List<Transport>> transports;


    @Ignore
    @Test
    public void extractAndDumpMovementData() throws IOException {
        logger.info("Init");
        this.init();
        logger.info("Init done");

        final Position start = new Position(3234, 3225, 0);


        logger.info("Starting exploration");
        final HashSet<Tile> result = this.explore(tileManager.getTile(start).get());
        logger.info("Exploration done: " + result.size() + " positions found");

        logger.info("writing dump");
        this.writeCsvDump(result);
        logger.info("done");
    }

    private HashSet<Tile> explore(final Tile startTile) {
        if (!startTile.isWalkable) {
            throw new Error("bfs: Start tile " + startTile.position + " is not walkable");
        }

        final Queue<Tile> toVisit = new LinkedList<>();
        final HashSet<Tile> visitedOrMarkedToBeVisited = new HashSet<>();
        toVisit.add(startTile);
        visitedOrMarkedToBeVisited.add(startTile);

        while (toVisit.peek() != null) {
            final Tile currentTile = toVisit.remove();

            tileManager.getDirectNeighbours(currentTile).stream()
                    .filter(n -> !visitedOrMarkedToBeVisited.contains(n))
                    .forEachOrdered(n -> {
                        toVisit.add(n);
                        visitedOrMarkedToBeVisited.add(n);
                    });

            final int amountVisited = visitedOrMarkedToBeVisited.size() - toVisit.size();
            if (amountVisited % 20000 == 0) {
                logger.info("Explored " + amountVisited + " positions. " + toVisit.size() + " left to investigate.");
            }
        }

        return visitedOrMarkedToBeVisited;
    }


    private void writeCsvDump(final Collection<Tile> tiles) throws IOException {
        try (final FileOutputStream fileOut = new FileOutputStream(OUTPUT_FILE_ARCHIVE);
             final ZipOutputStream zipOut = new ZipOutputStream(fileOut);
             final OutputStreamWriter writerOut = new OutputStreamWriter(zipOut, StandardCharsets.UTF_8);
             final BufferedWriter out = new BufferedWriter(writerOut)) {

            final ZipEntry zipEntry = new ZipEntry(OUTPUT_FILE_ARCHIVE_ENTRY);
            zipOut.putNextEntry(zipEntry);
            out.write("# x,y,z,northBlocked,eastBlocked,southBlocked,westBlocked");
            for (Tile tile : tiles) {
                assert tile.isWalkable && tile.directionalBlockers.isPresent();

                out.write('\n');
                out.write(Integer.toString(tile.position.getX()));
                out.write(',');
                out.write(Integer.toString(tile.position.getY()));
                out.write(',');
                out.write(Integer.toString(tile.position.getZ()));
                out.write(',');
                out.write(Boolean.toString(tile.directionalBlockers.get().northBlocked));
                out.write(',');
                out.write(Boolean.toString(tile.directionalBlockers.get().eastBlocked));
                out.write(',');
                out.write(Boolean.toString(tile.directionalBlockers.get().southBlocked));
                out.write(',');
                out.write(Boolean.toString(tile.directionalBlockers.get().westBlocked));
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
        final Collection<Region> regions = regionLoader.getRegions();

        final ObjectManager objectManager = new ObjectManager(cacheStore);
        objectManager.load();

        final DataDeserializer.TeleportsAndTransports teleportsAndTransports =
                new DataDeserializer().readTeleportsAndTransports();
        this.teleports = teleportsAndTransports.teleports;
        this.transports = teleportsAndTransports.transports;

        this.tileManager = new TileManager(regions, objectManager, transports);
    }
}
