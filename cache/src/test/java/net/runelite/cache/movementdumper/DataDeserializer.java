package net.runelite.cache.movementdumper;

import com.google.gson.Gson;
import net.runelite.cache.region.Position;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataDeserializer {
    private final static String WIKI_FILE = "transports.json";
    private final static String SKRETZO_FILE = "skretzo_data.txt";


    public TeleportsAndTransports readTeleportsAndTransports() throws IOException {
        final TransportJson[] wikiData = this.deserializeWikiData();
        final TransportJson[] skretzoData = this.deserializeSkretzoData();

        final Predicate<TransportJson> isTeleport = t -> t.start == null;
        final boolean teleportCategory = true;
        final boolean transportCategory = false;
        final Map<Boolean, List<TransportJson>> allData = Stream.concat(
                        Arrays.stream(wikiData),
                        Arrays.stream(skretzoData))
                .collect(Collectors.partitioningBy(isTeleport));

        final Map<Position, List<Teleport>> teleports =
                allData.get(teleportCategory).stream()
                        .map(t -> new Teleport(
                                new Position(t.end.x, t.end.y, t.end.z),
                                t.title,
                                t.duration))
                        .collect(Collectors.groupingBy(t -> t.destination));

        final Map<Position, List<Transport>> transports =
                allData.get(transportCategory).stream()
                        .map(t -> new Transport(
                                new Position(t.start.x, t.start.y, t.start.z),
                                new Position(t.end.x, t.end.y, t.end.z),
                                t.title,
                                t.duration))
                        .collect(Collectors.groupingBy(t -> t.from));

        return new TeleportsAndTransports(teleports, transports);
    }

    /**
     * Reads the file "transports.json"
     *
     * @return The content of "transports.json" in an Object
     * @throws IOException can't read file
     */
    private TransportJson[] deserializeWikiData() throws IOException {
        try (final InputStream file = DataDeserializer.class.getResourceAsStream(WIKI_FILE)) {
            if(file == null) {
                throw new IOException("Could not find resource: " + WIKI_FILE);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(file));
            return new Gson().fromJson(reader, TransportJson[].class);
        }
    }

    /**
     * Reads skretzo's data file and parses them into TransportJson[]
     *
     * @return The content of the file in an object. WARNING transportJson[i].duration is ALWAYS 1
     */
    private TransportJson[] deserializeSkretzoData() throws IOException {
        try (final InputStream file = DataDeserializer.class.getResourceAsStream(SKRETZO_FILE)) {
            if(file == null) {
                throw new IOException("Could not find resource: " + SKRETZO_FILE);
            }
            final BufferedReader reader = new BufferedReader(new InputStreamReader(file));

            // Filter comments
            final Stream<String> lines = reader.lines().filter(line -> !(line.startsWith("#") || line.isEmpty()));
            final Stream<TransportJson> transports = lines.map(line -> {
                final String[] parts = line.split("\t");
                final String[] parts_startCoordinate = parts[0].split(" ");
                final String[] parts_endCoordinate = parts[1].split(" ");
                final String methodOfMovement = parts[2];
                final CoordinateJson startCoordinate = new CoordinateJson();
                startCoordinate.x = Integer.parseInt(parts_startCoordinate[0]);
                startCoordinate.y = Integer.parseInt(parts_startCoordinate[1]);
                startCoordinate.z = Integer.parseInt(parts_startCoordinate[2]);
                final CoordinateJson endCoordinate = new CoordinateJson();
                endCoordinate.x = Integer.parseInt(parts_endCoordinate[0]);
                endCoordinate.y = Integer.parseInt(parts_endCoordinate[1]);
                endCoordinate.z = Integer.parseInt(parts_endCoordinate[2]);
                final TransportJson transport = new TransportJson();
                transport.start = startCoordinate;
                transport.end = endCoordinate;
                transport.title = methodOfMovement;
                transport.duration = (byte) 1; // TODO Skretzo's data does not include duration
                return transport;
            });
            return transports.toArray(TransportJson[]::new);
        }
    }

    private static class CoordinateJson {
        public int x;
        public int y;
        public int z;
    }

    private static class TransportJson {
        public CoordinateJson start;
        public CoordinateJson end;
        public String title;
        public byte duration;
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
