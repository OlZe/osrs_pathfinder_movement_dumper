package net.runelite.cache.movementdumper;

import com.google.gson.Gson;
import net.runelite.cache.region.Position;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataDeserializer {
    private final static String WIKI_FILE = "WikiTeleportsAndTransports.json";
    private final static String SKRETZO_FILE = "SkretzoTransports.txt";


    public TeleportsAndTransports readTeleportsAndTransports() throws IOException {
        final TeleportsAndTransportsJson[] wikiData = this.deserializeWikiData();

        final Predicate<TeleportsAndTransportsJson> isTeleport = t -> t.start == null;
        final boolean teleportCategory = true;
        final boolean transportCategory = false;
        final Map<Boolean, List<TeleportsAndTransportsJson>> wikiDataSeparated = Arrays.stream(wikiData)
                .collect(Collectors.partitioningBy(isTeleport));

        final Map<Position, List<Teleport>> allTeleports =
                wikiDataSeparated.get(teleportCategory).stream()
                        .map(t -> new Teleport(
                                new Position(t.end.x, t.end.y, t.end.z),
                                t.title,
                                t.duration,
                                t.canTeleportUpTo30Wildy))
                        .collect(Collectors.groupingBy(t -> t.destination));

        final List<Transport> skretzoTransports = this.deserializeSkretzoTransports();
        final Stream<Transport> wikiTransports = wikiDataSeparated.get(transportCategory).stream()
                .map(t -> new Transport(
                        new Position(t.start.x, t.start.y, t.start.z),
                        new Position(t.end.x, t.end.y, t.end.z),
                        t.title,
                        t.duration));
        final Map<Position, List<Transport>> allTransports =
                Stream.concat(wikiTransports, skretzoTransports.stream())
                        .collect(Collectors.groupingBy(t -> t.from));

        return new TeleportsAndTransports(allTeleports, allTransports);
    }

    private TeleportsAndTransportsJson[] deserializeWikiData() throws IOException {
        try (final InputStream file = DataDeserializer.class.getResourceAsStream(WIKI_FILE)) {
            if (file == null) {
                throw new IOException("Could not find resource: " + WIKI_FILE);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(file));
            return new Gson().fromJson(reader, TeleportsAndTransportsJson[].class);
        }
    }

    private List<Transport> deserializeSkretzoTransports() throws IOException {
        try (final InputStream file = DataDeserializer.class.getResourceAsStream(SKRETZO_FILE)) {
            if (file == null) {
                throw new IOException("Could not find resource: " + SKRETZO_FILE);
            }
            final BufferedReader reader = new BufferedReader(new InputStreamReader(file));

            // Filter comments
            final Stream<String> lines = reader.lines().filter(line -> !(line.startsWith("#") || line.isEmpty()));
            final Stream<Transport> transports = lines.map(line -> {
                final String[] parts = line.split("\t");
                final String[] parts_startPos = parts[0].split(" ");
                final String[] parts_endPos = parts[1].split(" ");
                final String methodOfMovement = parts[2];
                final byte duration = (parts.length >= 7 && !parts[6].isEmpty()) ? Byte.parseByte(parts[6]) : 1;

                final Position startPos = new Position(
                        Integer.parseInt(parts_startPos[0]),
                        Integer.parseInt(parts_startPos[1]),
                        Integer.parseInt(parts_startPos[2]));
                final Position endPos = new Position(
                        Integer.parseInt(parts_endPos[0]),
                        Integer.parseInt(parts_endPos[1]),
                        Integer.parseInt(parts_endPos[2]));
                return new Transport(
                        startPos,
                        endPos,
                        methodOfMovement,
                        duration);
            });
            return transports.collect(Collectors.toList());
        }
    }

    private static class CoordinateJson {
        public int x;
        public int y;
        public int z;
    }

    private static class TeleportsAndTransportsJson {
        public CoordinateJson start;
        public CoordinateJson end;
        public String title;
        public byte duration;
        public boolean canTeleportUpTo30Wildy;
    }

    static class TeleportsAndTransports {
        public final Map<Position, List<Teleport>> teleports;
        public final Map<Position, List<Transport>> transports;

        public TeleportsAndTransports(
                final Map<Position, List<Teleport>> teleports,
                final Map<Position, List<Transport>> transports) {
            this.teleports = teleports;
            this.transports = transports;
        }
    }
}
