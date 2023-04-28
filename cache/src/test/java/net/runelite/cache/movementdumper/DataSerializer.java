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
        out.write("# x,y,z,northBlocked,eastBlocked,southBlocked,westBlocked,wildernessLevel\n");
        out.write("# wildernessLevel = 0: <= 20\n");
        out.write("# wildernessLevel = 1: > 20 and <= 30\n");
        out.write("# wildernessLevel = 2: > 30");

        for (Tile tile : tiles) {
            assert tile.isWalkable && tile.directionalBlockers.isPresent();

            final int wildernessLevel;
            if(tile.wildernessLevel.equals(PositionUtils.WildernessLevels.ABOVE30)) {
                wildernessLevel = 2;
            }
            else if (tile.wildernessLevel.equals(PositionUtils.WildernessLevels.BETWEEN20AND30)) {
                wildernessLevel = 1;
            }
            else {
                wildernessLevel = 0;
            }

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
            out.write(',');
            out.write(Integer.toString(wildernessLevel));
        }
    }

    private void writeTeleports(final Collection<Teleport> teleports, final Writer out) throws IOException {
        out.write("# x,y,z,duration,title,canTeleportUpTo30Wildy");
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
            out.write(',');
            out.write(Boolean.toString(teleport.canTeleportUpTo30Wildy));
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
