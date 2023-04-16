package net.runelite.cache.movementdumper;

import net.runelite.cache.region.Position;
import net.runelite.cache.region.Region;


class PositionUtils {

    private static final Area WILDERNESS20TO30 = new Area(2944, 3672, 3391, 3751);
    private static final Area WILDERNESSABOVE30 = new Area(2944, 3752, 3391, 3967);

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
        if (WILDERNESSABOVE30.contains(position)) {
            return WildernessLevels.ABOVE30;
        } else if (WILDERNESS20TO30.contains(position)) {
            return WildernessLevels.BETWEEN20AND30;
        } else {
            return WildernessLevels.BELOW20;
        }
    }

    public static enum WildernessLevels {
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
