package net.runelite.cache.movementdumper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DataSerializer {
    private static final String ARCHIVE_ENTRY_MOVEMENT_DATA = "movement.csv";
    private static final String ARCHIVE_ENTRY_TRANSPORTS = "transports.csv";
    private static final String ARCHIVE_ENTRY_TELEPORTS = "teleports.csv";

    public void writeDump(final Dump dump, final String file) throws IOException {
        try (final FileOutputStream fileOut = new FileOutputStream(file);
             final ZipOutputStream zipOut = new ZipOutputStream(fileOut);
             final OutputStreamWriter writerOut = new OutputStreamWriter(zipOut, StandardCharsets.UTF_8);
             final BufferedWriter out = new BufferedWriter(writerOut)) {

            zipOut.putNextEntry(new ZipEntry(ARCHIVE_ENTRY_MOVEMENT_DATA));
            this.writeMovementData(dump.tiles, out);
            out.flush();

            zipOut.putNextEntry(new ZipEntry(ARCHIVE_ENTRY_TRANSPORTS));
            writeTransports(dump.transports, out);
            out.flush();

            zipOut.putNextEntry(new ZipEntry(ARCHIVE_ENTRY_TELEPORTS));
            writeTeleports(dump.teleports, out);
            out.flush();
        }
    }

    private void writeMovementData(final Collection<Tile> tiles, final Writer out) throws IOException {
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

    private void writeTeleports(final Collection<Teleport> teleports, final Writer out) throws IOException {
        out.write("# x,y,z,duration,title");
        for (Teleport teleport : teleports) {
            out.write('\n');
            out.write(Integer.toString(teleport.destination.getX()));
            out.write(',');
            out.write(Integer.toString(teleport.destination.getY()));
            out.write(',');
            out.write(Integer.toString(teleport.destination.getZ()));
            out.write(',');
            out.write(Byte.toString(teleport.duration));
            out.write(',');
            out.write(teleport.title.replace(',', ' '));
        }
    }

    private void writeTransports(final Collection<Transport> transports, final Writer out) throws IOException {
        out.write("# fromX,fromY,fromZ,toX,toY,toZ,duration,title");
        for (Transport transport : transports) {
            out.write('\n');
            out.write(Integer.toString(transport.from.getX()));
            out.write(',');
            out.write(Integer.toString(transport.from.getY()));
            out.write(',');
            out.write(Integer.toString(transport.from.getZ()));
            out.write(',');
            out.write(Integer.toString(transport.to.getX()));
            out.write(',');
            out.write(Integer.toString(transport.to.getY()));
            out.write(',');
            out.write(Integer.toString(transport.to.getZ()));
            out.write(',');
            out.write(Byte.toString(transport.duration));
            out.write(',');
            out.write(transport.title.replace(',', ' '));
        }
    }

    public static class Dump {
        public final Collection<Tile> tiles;
        public final Collection<Teleport> teleports;
        public final Collection<Transport> transports;

        public Dump(final Collection<Tile> tiles, final Collection<Teleport> teleports, final Collection<Transport> transports) {
            this.tiles = tiles;
            this.teleports = teleports;
            this.transports = transports;
        }
    }
}
