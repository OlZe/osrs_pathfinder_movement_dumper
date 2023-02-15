package net.runelite.cache.movementdumper;

import net.runelite.cache.region.Position;
import net.runelite.cache.region.Region;

import java.util.Collection;

class PositionUtils {
    private final Collection<Region> allRegions;

    PositionUtils(final Collection<Region> allRegions) {
        this.allRegions = allRegions;
    }

    public static Position move(final Position position, int dx, int dy, int dz) {
        return new Position(
                position.getX() + dx,
                position.getY() + dy,
                position.getZ() + dz);
    }

    public static Position toAbsolute(RegionPosition regionPosition) {
        return new Position(
                (regionPosition.region.getRegionX() * Region.X) + regionPosition.relativePosition.getX(),
                (regionPosition.region.getRegionY() * Region.Y) + regionPosition.relativePosition.getY(),
                regionPosition.relativePosition.getZ());
    }

    public RegionPosition toRegionPosition(Position absolutePosition) {
        final Region region = this.findRegion(absolutePosition);
        if (region == null) {
            return null;
        }
        return new RegionPosition(
                new Position(
                        absolutePosition.getX() % Region.X,
                        absolutePosition.getY() % Region.Y,
                        absolutePosition.getZ()),
                region);
    }

    public RegionPosition move(RegionPosition position, int dx, int dy) {
        return this.toRegionPosition(move(toAbsolute(position), dx, dy, 0));
    }

    private Region findRegion(Position absolutePosition) {
        return this.allRegions.stream()
                .filter(r -> r.getRegionX() == absolutePosition.getX() / 64
                        && r.getRegionY() == absolutePosition.getY() / 64)
                .findAny()
                .orElse(null);
    }
}
