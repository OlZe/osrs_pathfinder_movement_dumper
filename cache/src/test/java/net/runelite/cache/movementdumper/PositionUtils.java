package net.runelite.cache.movementdumper;

import net.runelite.cache.region.Position;
import net.runelite.cache.region.Region;

class PositionUtils {
    public static Position toRelative(final Region region, final Position absolutePosition) {
        return new Position(
                absolutePosition.getX() - region.getBaseX(),
                absolutePosition.getY() - region.getBaseY(),
                absolutePosition.getZ());
    }

    public static Position toAbsolute(final Region region, final Position relativePosition) {
        return new Position(
                relativePosition.getX() + region.getBaseX(),
                relativePosition.getY() + region.getBaseY(),
                relativePosition.getZ());
    }

    public static Position normalize(final Position position, final boolean isBridge) {
        return new Position(
                position.getX(),
                position.getY(),
                isBridge ? position.getZ() - 1 : position.getZ());
    }

    public static Position toNormalizedAbsolute(final Position relativePos, final Region region, final boolean isBridge) {
        return normalize(toAbsolute(region, relativePos), isBridge);
    }

    public static Position move(final Position position, int dx, int dy, int dz) {
        return new Position(
                position.getX() + dx,
                position.getY() + dy,
                position.getZ() + dz);
    }
}
