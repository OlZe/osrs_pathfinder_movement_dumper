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
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MovementDumper {
    private static final String CACHE_DIR = "C:\\Users\\Oli\\Desktop\\Code\\OSRS Navigator\\wiki maps pathfinding project\\movement dumper\\2023-02-15-rev211\\";
    private static final String CACHE_DIR_XTEAKEYS_FILE = "xteas_old_format.json";
    private static final String OUTPUT_FILE_ARCHIVE = "C:\\Users\\Oli\\Desktop\\Code\\OSRS Navigator\\wiki maps pathfinding project\\spring restructure\\pathfinder\\src\\main\\resources\\movement.csv.zip";
    private static final String OUTPUT_FILE_ARCHIVE_ENTRY = "movement.csv";

    private final Logger logger = LoggerFactory.getLogger(MovementDumper.class);
    private TileManager tileManager;
    private Map<Position, List<Teleport>> teleportsLeftToExplore;


    @Ignore
    @Test
    public void extractAndDumpMovementData() throws IOException {
        logger.info("Init");
        this.init();
        logger.info("Init done");

        List<Tile> allFoundTiles = new LinkedList<>();

        logger.info("Starting multiple explorations to visit every teleport.");
        while (this.teleportsLeftToExplore.size() > 0) {
            logger.info("Teleports left to visit: " + this.teleportsLeftToExplore.values().stream().mapToInt(Collection::size).sum());

            // Remove any teleport which is left to explore
            final Position startPosition = this.teleportsLeftToExplore.keySet().iterator().next();
            final List<Teleport> startTeleports = this.teleportsLeftToExplore.remove(startPosition);

            final String startTeleportTitles = teleportsToString(startTeleports);
            logger.info("Starting exploration from teleport(s): " + startTeleportTitles + " on position: " + startPosition.toString());

            final Optional<Tile> startTile = tileManager.getTile(startPosition);
            if(!startTile.isPresent() || !startTile.get().isWalkable) {
                throw new Error("Position " + startPosition + " from teleport(s): " + startTeleportTitles + " is not walkable!"
                        + " Please ensure that this teleport is correctly entered into the dataset.");
            }

            final HashSet<Tile> foundTiles = this.explore(startTile.get());
            logger.info("Exploration done: " + foundTiles.size() + " positions found from teleport(s): " + startTeleportTitles);

            allFoundTiles.addAll(foundTiles);
        }
        logger.info("Done - All teleports visited. - " + allFoundTiles.size() + " total positions found.");


        logger.info("writing dump");
        this.writeCsvDump(allFoundTiles);

        logger.info("done");
    }

    private static String teleportsToString(final List<Teleport> startTeleports) {
        return startTeleports.stream().map(tp -> "\"" + tp.title + "\"").collect(Collectors.joining(","));
    }

    private HashSet<Tile> explore(final Tile startTile) {
        final Queue<Tile> toVisit = new LinkedList<>();
        final HashSet<Tile> visitedOrMarkedToBeVisited = new HashSet<>();
        toVisit.add(startTile);
        visitedOrMarkedToBeVisited.add(startTile);

        while (toVisit.peek() != null) {
            final Tile currentTile = toVisit.remove();

            // If there are teleports to this position, mark them as visited
            final List<Teleport> teleportsToHere = this.teleportsLeftToExplore.remove(currentTile.position);
            if (teleportsToHere != null) {
                logger.info("Visited teleport(s): " + teleportsToString(teleportsToHere));
            }

            tileManager.getDirectNeighbours(currentTile).stream()
                    .filter(n -> !visitedOrMarkedToBeVisited.contains(n))
                    .forEachOrdered(n -> {
                        toVisit.add(n);
                        visitedOrMarkedToBeVisited.add(n);
                    });

            final int amountVisited = visitedOrMarkedToBeVisited.size() - toVisit.size();
            if (amountVisited % 50000 == 0) {
                logger.info("Explored " + amountVisited + " positions. ");
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


        final long amountOfTransports = teleportsAndTransports.transports.values().stream()
                .mapToLong(Collection::size)
                .sum();

        logger.info("Read " + amountOfTransports + " transports.");

        final long amountOfTeleports = teleportsAndTransports.teleports.values().stream()
                .mapToLong(List::size)
                .sum();

        logger.info("Read " + amountOfTeleports + " teleports.");

        this.teleportsLeftToExplore = new HashMap<>(teleportsAndTransports.teleports);
        this.tileManager = new TileManager(regions, objectManager, teleportsAndTransports.transports);
    }
}
