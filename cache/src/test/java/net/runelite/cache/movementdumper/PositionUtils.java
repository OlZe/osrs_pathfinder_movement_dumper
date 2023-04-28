package net.runelite.cache.movementdumper;

import net.runelite.cache.region.Position;
import net.runelite.cache.region.Region;

import java.util.Arrays;


class PositionUtils {

    private static final Area[] WILDERNESS20TO30 = new Area[]{
            new Area(2944, 3672, 3391, 3751), // Surface
            new Area(3136, 10072, 3263, 10151), // Revenant Caves
            new Area(3328, 10072, 3455, 10151), // Wilderness Slayer Dungeon
            new Area(3008, 10112, 3071, 10175)     // Wilderness God Wars Dungeon
    };
    private static final Area[] WILDERNESSABOVE30 = new Area[]{
            new Area(2944, 3752, 3391, 3967), // Surface
            new Area(2922, 10240, 3071, 10367), // Deep Wilderness- and Lava Maze Dungeon
            new Area(3136, 10152, 3263, 10239), // Revenant Caves
            new Area(3328, 10152, 3455, 10175), // Wilderness Slayer Dungeon
            new Area(3219,10331,3247,10352)     // Scorpia
    };

    // Disallow instantiation as this class only has static members
    private PositionUtils() {
    }

    public static Position move(final Position position, int dx, int dy, int dz) {
        return new Position(
                position.getX() + dx,
                position.getY() + dy,
                position.getZ() + dz);
    }

    public static Position moveNorth(final Position position) {
        return move(position, 0, 1, 0);
    }

    public static Position moveEast(final Position position) {
        return move(position, 1, 0, 0);
    }

    public static Position moveSouth(final Position position) {
        return move(position, 0, -1, 0);
    }

    public static Position moveWest(final Position position) {
        return move(position, -1, 0, 0);
    }

    public static Position toRelative(final Position position) {
        return new Position(
                position.getX() % 64,
                position.getY() % 64,
                position.getZ());
    }

    public static Position toRegionPosition(Region region) {
        return new Position(region.getRegionX(), region.getRegionY(), 0);
    }

    public static Position toRegionPosition(final Position position) {
        return new Position(
                position.getX() / 64,
                position.getY() / 64,
                0);
    }

    public static WildernessLevels getWildernessLevel(final Position position) {
        final boolean isAbove30 = Arrays.stream(WILDERNESSABOVE30).anyMatch(area -> area.contains(position));
        if (isAbove30) {
            return WildernessLevels.ABOVE30;
        }
        final boolean isBetween20and30 = Arrays.stream(WILDERNESS20TO30).anyMatch(area -> area.contains(position));
        if (isBetween20and30) {
            return WildernessLevels.BETWEEN20AND30;
        }
        return WildernessLevels.BELOW20;
    }

    public enum WildernessLevels {
        /**
         * Wilderness Level < 20
         */
        BELOW20,
        /**
         * Wilderness Level >= 20 and < 30
         */
        BETWEEN20AND30,
        /**
         * Wilderness Level >= 30
         */
        ABOVE30
    }

    private static class Area {
        private final int leftX;
        private final int lowerY;
        private final int rightX;
        private final int upperY;

        private Area(final int leftX, final int lowerY, final int rightX, final int upperY) {
            this.leftX = leftX;
            this.lowerY = lowerY;
            this.rightX = rightX;
            this.upperY = upperY;
        }

        public boolean contains(Position position) {
            return position.getX() >= this.leftX
                    && position.getX() <= this.rightX
                    && position.getY() >= this.lowerY
                    && position.getY() <= this.upperY;
        }
    }
}
