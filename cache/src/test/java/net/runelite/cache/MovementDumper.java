package net.runelite.cache;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import net.runelite.cache.definitions.ObjectDefinition;
import net.runelite.cache.region.Location;
import java.nio.charset.Charset;
import java.util.*;

import net.runelite.cache.region.Position;
import net.runelite.cache.region.Region;
import net.runelite.cache.region.RegionLoader;
import net.runelite.cache.fs.Store;
import net.runelite.cache.util.XteaKeyManager;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovementDumper
{
    private static final Logger logger = LoggerFactory.getLogger(MovementDumper.class);

//    private static final int MAX_REGIONS = 32768;

    @Rule
    public TemporaryFolder folder = StoreLocation.getTemporaryFolder();

    private final Gson gson = new GsonBuilder().create();

    class MovementDump {
        List<Position> walkable;
        Position[] obstaclePositions;
        int[] obstacleValues;

        public MovementDump() {
            walkable = new ArrayList<>();
        }

    }
    @Test
    public void loadRegions() throws IOException
    {

        File base = new File("C:\\Users\\Oli\\Desktop\\Code\\OSRS Navigator\\wiki maps pathfinding project\\movement dumper\\2022-12-13-rev210\\cache");
        Store store = new Store(base);
        store.load();



        File xteaFile = new File("C:\\Users\\Oli\\Desktop\\Code\\OSRS Navigator\\wiki maps pathfinding project\\movement dumper\\2022-12-13-rev210\\xteas_old_format.json");
        XteaKeyManager xteaKeyManager = new XteaKeyManager();
        try (FileInputStream fin = new FileInputStream(xteaFile))
        {
            xteaKeyManager.loadKeys(fin);
        }


        RegionLoader regionLoader = new RegionLoader(store, xteaKeyManager);
        regionLoader.loadRegions();
        ObjectManager objectManager = new ObjectManager(store);
        objectManager.load();
        MovementDump dump = new MovementDump();

        int TOP = 1;
        int RIGHT = 2;
        int BOTTOM = 4;
        int LEFT = 8;
        HashMap<Position, Integer> obstacles = new HashMap<>();
        Collection<Region> regions = regionLoader.getRegions();
        for (Region region : regions)
        {
            int regionX = region.getRegionX();
            int regionY = region.getRegionY();
            for (Location location : region.getLocations()) {

                int rotation = location.getOrientation();
                int type = location.getType();

                int localX = location.getPosition().getX() - region.getBaseX();
                int localY = location.getPosition().getY() - region.getBaseY();

                boolean isBridge = (region.getTileSetting(1, localX, localY) & 2) != 0;

                if (location.getPosition().getZ() == 1) { // HOHO
                    if (!isBridge) {
                        continue;
                    }
                } else if (location.getPosition().getZ() == 0) { // HOHO
                    if (isBridge) {
                        continue;
                    }

                    if ((region.getTileSetting(0, localX, localY) & 24) != 0) { // HOHO
                        continue;
                    }
                } else {
                    continue;
                }
                Position pos = new Position(64 * regionX + localX, 64 * regionY + localY, 0); // HOHO
                int blockers = obstacles.getOrDefault(pos, 0);
                if (type == 0) {
                    if (rotation == 0) {
                        blockers |= LEFT;
                    } else if (rotation == 1) {
                        blockers |= TOP;
                    }
                    else if (rotation == 2) {
                        blockers |= RIGHT;
                    } else if (rotation == 3) {
                        blockers |= BOTTOM;
                    }
                } else if (type == 2) {
                    if (rotation == 0) {
                        blockers |= TOP + LEFT;
                    } else if (rotation == 1) {
                        blockers |= TOP + RIGHT;
                    }
                    else if (rotation == 2) {
                        blockers |= RIGHT + BOTTOM;
                    } else if (rotation == 3) {
                        blockers |= LEFT + BOTTOM;
                    }
                } else if (type == 9) {
                    blockers |= TOP + RIGHT + BOTTOM + LEFT;
                }
                if (blockers != 0) {
                    obstacles.put(pos, blockers);
                }
                if (type == 10) {
                    ObjectDefinition object = objectManager.getObject(location.getId());
                    int height = object.getSizeY();
                    int width = object.getSizeX();
                    if (rotation % 2 == 1) {
                        height = object.getSizeX();
                        width = object.getSizeY();
                    }
                    for (int a = 0; a < width; a++) {
                        for (int b = 0; b < height; b++) {
                            pos = new Position(64 * regionX + localX + a, 64 * regionY + localY + b, 0); // HOHO
                            obstacles.put(pos, TOP + RIGHT + BOTTOM + LEFT);
                        }
                    }
                }
            }
            dump.obstaclePositions = new Position[obstacles.size()];
            dump.obstacleValues = new int[obstacles.size()];

            int i = 0;
            for (Position p : obstacles.keySet()) {
                dump.obstaclePositions[i] = p;
                dump.obstacleValues[i++] = obstacles.get(p);
            }

            for (int x = 0; x < 64; x++) {
                for (int y = 0; y < 64; y++) {
                    boolean isBridge = (region.getTileSetting(1, x, y) & 2) == 2; // HOHO
                    if (isBridge) {
                        boolean walkable = (region.getTileSetting(1, x, y) & 1) == 0; // HOHO
                        if (walkable) {
                            Position pos = new Position(64 * regionX + x, 64 * regionY + y, 0); // HOHO
                            dump.walkable.add(pos);
                        }
                    } else {
                        boolean walkable = (region.getTileSetting(0, x, y) & 1) == 0; // HOHO
                        if (walkable) {
                            Position pos = new Position(64 * regionX + x, 64 * regionY + y, 0); // HOHO
                            dump.walkable.add(pos);
                        }
                    }
                }
            }
        }

        Files.write(gson.toJson(dump), new File("C:\\Users\\Oli\\Downloads",  "dump.json"), Charset.defaultCharset());


    }
}
