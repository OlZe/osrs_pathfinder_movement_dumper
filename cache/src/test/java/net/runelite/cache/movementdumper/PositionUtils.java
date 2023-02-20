package net.runelite.cache.movementdumper;

import net.runelite.cache.region.Position;
import net.runelite.cache.region.Region;


class PositionUtils {

    public static Position move(final Position position, int dx, int dy, int dz) {
        return new Position(
                position.getX() + dx,
                position.getY() + dy,
                position.getZ() + dz);
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
}
