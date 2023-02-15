package net.runelite.cache.movementdumper;

import net.runelite.cache.region.Position;
import net.runelite.cache.region.Region;

import java.util.Objects;

public class RegionPosition {
    public final Position relativePosition;
    public final Region region;
    public final TileSettings tileSettings;
    public final TileObstacles obstacles;


    public RegionPosition(Position relativePosition, Region region) {
        this.relativePosition = relativePosition;
        this.region = region;
        this.tileSettings = new TileSettings(region, relativePosition);
        this.obstacles = getObstacles();
    }

    public boolean isWalkable() {
        return !this.obstacles.allDirectionsBlocked()
                && this.tileSettings.isWalkable
                && !this.tileSettings.isBlocked
                && !this.tileSettings.isBridgeAbove;
    }

    private TileObstacles getObstacles() {
        final TileObstacles tileObstacles = new TileObstacles();
        region.getLocations().stream()
                .filter(l -> l.getPosition().equals(PositionUtils.toAbsolute(this)))
                .forEachOrdered(location -> {
                    if (location.getType() == 0) {
                        // Lateral direction blocked
                        switch (location.getOrientation()) {
                            case 0:
                                tileObstacles.westBlocked = true;
                                break;
                            case 1:
                                tileObstacles.northBlocked = true;
                                break;
                            case 2:
                                tileObstacles.eastBlocked = true;
                                break;
                            case 3:
                                tileObstacles.southBlocked = true;
                                break;
                        }
                    } else if (location.getType() == 2) {
                        // Diagonal direction blocked, blocks both lateral ways
                        switch (location.getOrientation()) {
                            case 0:
                                tileObstacles.northBlocked = true;
                                tileObstacles.westBlocked = true;
                                break;
                            case 1:
                                tileObstacles.northBlocked = true;
                                tileObstacles.eastBlocked = true;
                                break;
                            case 2:
                                tileObstacles.southBlocked = true;
                                tileObstacles.eastBlocked = true;
                                break;
                            case 3:
                                tileObstacles.southBlocked = true;
                                tileObstacles.westBlocked = true;
                                break;
                        }
                    } else if (location.getType() == 9) {
                        // All sides blocked
                        tileObstacles.westBlocked = true;
                        tileObstacles.northBlocked = true;
                        tileObstacles.eastBlocked = true;
                        tileObstacles.southBlocked = true;
                    }
                    // TODO handle game object case
                });
        return tileObstacles;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final RegionPosition that = (RegionPosition) o;
        return this.relativePosition.equals(that.relativePosition) && this.region.getRegionID() == that.region.getRegionID();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.relativePosition, this.region.getRegionID());
    }
}
