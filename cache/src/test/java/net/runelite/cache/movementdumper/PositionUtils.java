package net.runelite.cache.movementdumper;

import net.runelite.cache.ObjectManager;
import net.runelite.cache.region.Position;
import net.runelite.cache.region.Region;

import java.util.Collection;
import java.util.HashMap;

class PositionUtils {
    private final HashMap<Position, Region> allRegions;
    private final ObjectManager objectManager;

    PositionUtils(final Collection<Region> allRegions, final ObjectManager objectManager) {
        this.objectManager = objectManager;
        this.allRegions = new HashMap<>();
        allRegions.forEach(region -> {
            final Position pos = new Position(region.getRegionX(), region.getRegionY(), 0);
            this.allRegions.put(pos, region);
        });
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
                region,
                this.objectManager);
    }

    public RegionPosition move(RegionPosition position, int dx, int dy) {
        return this.toRegionPosition(move(toAbsolute(position), dx, dy, 0));
    }

    private Region findRegion(Position absolutePosition) {
        final Position regionPos = new Position(
                absolutePosition.getX() / 64,
                absolutePosition.getY() / 64,
                0
        );
        return this.allRegions.get(regionPos);
    }
}
