package net.runelite.cache.movementdumper;

import net.runelite.cache.ObjectManager;
import net.runelite.cache.definitions.ObjectDefinition;
import net.runelite.cache.region.Location;
import net.runelite.cache.region.Position;
import net.runelite.cache.region.Region;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * This is an abstraction which represents one game tile/square/position in the game.
 * It uses {@link Region#getTileSetting(int, int, int)} and {@link Region#getLocations()} to populate movement data.
 * It handles the "bridge-flag" under the hood.
 * {@link PositionUtils} provides useful manipulation functions.
 */
class RegionPosition {
    public final Position relativePosition;
    public final Region region;
    public final TileObstacles obstacles;
    private final TileSettings tileSettings;
    private final boolean isBridgeAbove;


    public RegionPosition(Position relativePosition, Region region, ObjectManager objectManager) {
        this.relativePosition = relativePosition;
        this.region = region;

        final TileSettings tileSettings = new TileSettings(region, relativePosition);
        /* bridge-flag means that the position data is applied on z-1.
            therefore isBridgeAbove on *this* position means that the data is on z+1.
            In that case this class will take the position data from z+1 to hide all that bridge nonsense.

            Realistically a tile with isBridge==true should never be reached, because a tile with isBridge==true
            applies to z-1. Therefore, a player would reach the bridge from the z-1 plane, which means that
            RegionPositions should only ever be created on that lower plane.

            There are however cases where the bridge-flag is, seemingly randomly, set for some specific tiles
            such as at position 2972,3456,0 where it has no effect because bridges on z=0 don't exist.
         */
        this.isBridgeAbove = tileSettings.isBridgeAbove;

        if (this.isBridgeAbove) {
            // Get the tile settings from z+1
            this.tileSettings = new TileSettings(region, PositionUtils.move(relativePosition, 0, 0, 1));
            assert this.tileSettings.isBridge;
        } else {
            this.tileSettings = tileSettings;
        }

        // Only happens if there's 2 bridges directly ontop of each other which should never happen.
        assert !this.tileSettings.isBridgeAbove;

        if(this.tileSettings.isWalkableNew()) {
            this.obstacles = getObstacleData(objectManager);
        } else {
            this.obstacles = new TileObstacles();
        }
    }

    public boolean isWalkable() {
        return this.tileSettings.isWalkableNew() && !this.obstacles.allDirectionsBlocked();
    }

    public Stream<Location> getLocations() {
        // Locations are found using absolute positions
        final Position absolutePosition;

        if (!this.isBridgeAbove) {
            absolutePosition = PositionUtils.toAbsolute(this);
        } else {
            // Get the obstacles from z+1
            absolutePosition = PositionUtils.move(PositionUtils.toAbsolute(this), 0, 0, 1);
        }
        return region.getLocations().stream()
                .filter(l -> l.getPosition().equals(absolutePosition));
    }

    private TileObstacles getObstacleData(ObjectManager objectManager) {
        final TileObstacles tileObstacles = new TileObstacles();
        this.getLocations()
                .filter(l -> !MovementDumper.LOCATION_TYPES_ALWAYS_WALKABLE.contains(l.getType()))
                .filter(l -> this.isBlockedByObject(l, objectManager))
                .forEachOrdered(location -> {
                    if(tileObstacles.allDirectionsBlocked()) {
                        return;
                    }

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
                    } else {
                        // All sides blocked
                        tileObstacles.setAllDirectionsBlocked();
                    }
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


    private boolean isBlockedByObject(Location location, ObjectManager objectManager) {
        final ObjectDefinition object = objectManager.getObject(location.getId());
        if (location.getType() == 22) {
            return object.getInteractType() == 1;
        } else {
            return object.getInteractType() != 0;
        }
    }
}
