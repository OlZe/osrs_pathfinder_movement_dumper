package net.runelite.cache.movementdumper;

import net.runelite.cache.ObjectManager;
import net.runelite.cache.definitions.ObjectDefinition;
import net.runelite.cache.region.Location;
import net.runelite.cache.region.Position;
import net.runelite.cache.region.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;

public class TileManager {
    private static final HashSet<Integer> LOCATION_TYPES_ALWAYS_WALKABLE = new HashSet<>(Arrays.asList(1, 3, 4, 5, 6, 7, 8));
    private static final int LOCATION_TYPE_WALL = 0;
    private static final int LOCATION_TYPE_WALL_CORNER = 2;
    private final Logger logger = LoggerFactory.getLogger(TileManager.class);
    private final HashSet<Position> positionsBlockedByBigObjects;
    private final HashMap<Position, Region> allRegions;
    private final ObjectManager objectManager;
    private final Map<Position, List<Transport>> transports;

    public TileManager(final Collection<Region> allRegions,
                       final ObjectManager objectManager,
                       final Map<Position, List<Transport>> transports) {
        logger.info("Init");

        this.objectManager = objectManager;
        this.transports = transports;
        this.positionsBlockedByBigObjects = new HashSet<>();
        this.allRegions = new HashMap<>();

        // Populate this.allRegions
        allRegions.forEach(region -> this.allRegions.put(PositionUtils.toRegionPosition(region), region));

        // Populate this.positionsBlockedByBigObjects
        this.markPositionsBlockedByBigObjects();
        logger.info("Marked " + positionsBlockedByBigObjects.size() + " tiles as blocked by big objects");
    }

    public Optional<Tile> getTile(Position position) {

        final Optional<Region> region = this.getRegion(position);
        if (!region.isPresent()) {
            return Optional.empty();
        }

        if (this.positionsBlockedByBigObjects.contains(position)) {
            return Optional.of(new Tile(position, false, Optional.empty()));
        }

        final Position relativePosition = PositionUtils.toRelative(position);
        TileSettings tileSettings = new TileSettings(region.get(), relativePosition);
        final boolean isBridgeAbove = tileSettings.isBridgeAbove;
        if (isBridgeAbove) {
            tileSettings = new TileSettings(region.get(), PositionUtils.move(relativePosition, 0, 0, 1));
            assert tileSettings.isBridge;
        }

        if (!tileSettings.isWalkable) {
            return Optional.of(new Tile(position, false, Optional.empty()));
        }

        final Position normalizedPos = isBridgeAbove ? PositionUtils.move(position, 0, 0, 1) : position;
        final DirectionalBlockers directionalBlockers = this.processLocations(region.get(), normalizedPos);
        if (directionalBlockers.allBlocked()) {
            return Optional.of(new Tile(position, false, Optional.empty()));
        }

        return Optional.of(new Tile(position, true, Optional.of(directionalBlockers)));
    }

    public Collection<Tile> getDirectNeighbours(Tile tile) {
        List<Tile> neighbours = new LinkedList<>();

        if (!tile.isWalkable || tile.directionalBlockers.get().allBlocked()) {
            return neighbours;
        }

        // Walkable neighbours
        getNorthIfWalkable(tile).ifPresent(neighbours::add);
        getNorthEastIfWalkable(tile).ifPresent(neighbours::add);
        getEastIfWalkable(tile).ifPresent(neighbours::add);
        getSouthEastIfWalkable(tile).ifPresent(neighbours::add);
        getSouthIfWalkable(tile).ifPresent(neighbours::add);
        getSouthWestIfWalkable(tile).ifPresent(neighbours::add);
        getWestIfWalkable(tile).ifPresent(neighbours::add);
        getNorthWestIfWalkable(tile).ifPresent(neighbours::add);

        // Transports from this position
        final List<Transport> transports = this.transports.get(tile.position);
        if(transports != null) {
            transports.stream()
                    .map(transport -> this.getTile(transport.to))
                    .filter(Optional::isPresent)
                    .filter(destination -> destination.get().isWalkable)
                    .forEachOrdered(destination -> neighbours.add(destination.get()));
        }

        return neighbours;
    }

    private DirectionalBlockers processLocations(Region region, Position position) {
        final AtomicBoolean northBlocked = new AtomicBoolean(false);
        final AtomicBoolean eastBlocked = new AtomicBoolean(false);
        final AtomicBoolean southBlocked = new AtomicBoolean(false);
        final AtomicBoolean westBlocked = new AtomicBoolean(false);

        region.getLocations().stream()
                .filter(l -> l.getPosition().equals(position))
                .filter(l -> !LOCATION_TYPES_ALWAYS_WALKABLE.contains(l.getType()))
                .forEach(location -> {
                    if (!this.objectOnLocationIsBlocking(location)) {
                        return;
                    }

                    if (location.getType() == 0) {
                        switch (location.getOrientation()) {
                            case 0:
                                westBlocked.set(true);
                                break;
                            case 1:
                                northBlocked.set(true);
                                break;
                            case 2:
                                eastBlocked.set(true);
                                break;
                            case 3:
                                southBlocked.set(true);
                                break;
                        }
                    } else if (location.getType() == 2) {
                        switch (location.getOrientation()) {
                            case 0:
                                northBlocked.set(true);
                                westBlocked.set(true);
                                break;
                            case 1:
                                northBlocked.set(true);
                                eastBlocked.set(true);
                                break;
                            case 2:
                                southBlocked.set(true);
                                eastBlocked.set(true);
                                break;
                            case 3:
                                southBlocked.set(true);
                                westBlocked.set(true);
                                break;
                        }
                    } else {
                        northBlocked.set(true);
                        eastBlocked.set(true);
                        southBlocked.set(true);
                        westBlocked.set(true);
                    }
                });

        return new DirectionalBlockers(northBlocked.get(), eastBlocked.get(), southBlocked.get(), westBlocked.get());
    }

    private Optional<Region> getRegion(Position position) {
        final Position regionPos = PositionUtils.toRegionPosition(position);
        return Optional.ofNullable(this.allRegions.get(regionPos));
    }

    private boolean objectOnLocationIsBlocking(Location location) {
        final ObjectDefinition object = objectManager.getObject(location.getId());
        if (location.getType() == 22) {
            return object.getInteractType() == 1;
        } else {
            return object.getInteractType() != 0;
        }
    }

    private void markPositionsBlockedByBigObjects() {
        final Function<ObjectDefinition, Boolean> objectIsBig = o ->
                o.getSizeX() > 1 || o.getSizeY() > 1;

        this.allRegions.values().stream()
                .flatMap(r -> r.getLocations().stream())
                .filter(l -> !(LOCATION_TYPE_WALL == l.getType()))
                .filter(l -> !(LOCATION_TYPE_WALL_CORNER == l.getType()))
                .filter(l -> !LOCATION_TYPES_ALWAYS_WALKABLE.contains(l.getType()))
                .filter(l -> objectIsBig.apply(objectManager.getObject(l.getId())))
                .filter(this::objectOnLocationIsBlocking)
                .forEachOrdered(l -> {
                    final ObjectDefinition object = objectManager.getObject(l.getId());
                    final int width = l.getOrientation() % 2 == 1 ? object.getSizeY() : object.getSizeX();
                    final int height = l.getOrientation() % 2 == 1 ? object.getSizeX() : object.getSizeY();
                    for (int dx = 0; dx < width; dx++) {
                        for (int dy = 0; dy < height; dy++) {
                            final Position current = PositionUtils.move(l.getPosition(), dx, dy, 0);
                            this.positionsBlockedByBigObjects.add(current);
                        }
                    }
                });
    }

    private Optional<Tile> getNorthIfWalkable(Tile tile) {
        if (!tile.isWalkable || tile.directionalBlockers.get().northBlocked) {
            return Optional.empty();
        }
        final Optional<Tile> northTile = this.getTile(PositionUtils.moveNorth(tile.position));
        if (northTile.isPresent()
                && northTile.get().isWalkable
                && !northTile.get().directionalBlockers.get().southBlocked) {
            return northTile;
        }
        return Optional.empty();
    }

    private Optional<Tile> getEastIfWalkable(Tile tile) {
        if (!tile.isWalkable || tile.directionalBlockers.get().eastBlocked) {
            return Optional.empty();
        }
        final Optional<Tile> eastTile = this.getTile(PositionUtils.moveEast(tile.position));
        if (eastTile.isPresent()
                && eastTile.get().isWalkable
                && !eastTile.get().directionalBlockers.get().westBlocked) {
            return eastTile;
        }
        return Optional.empty();
    }

    private Optional<Tile> getSouthIfWalkable(Tile tile) {
        if (!tile.isWalkable || tile.directionalBlockers.get().southBlocked) {
            return Optional.empty();
        }
        final Optional<Tile> southTile = this.getTile(PositionUtils.moveSouth(tile.position));
        if (southTile.isPresent()
                && southTile.get().isWalkable
                && !southTile.get().directionalBlockers.get().northBlocked) {
            return southTile;
        }
        return Optional.empty();
    }

    private Optional<Tile> getWestIfWalkable(Tile tile) {
        if (!tile.isWalkable || tile.directionalBlockers.get().westBlocked) {
            return Optional.empty();
        }
        final Optional<Tile> westTile = this.getTile(PositionUtils.moveWest(tile.position));
        if (westTile.isPresent()
                && westTile.get().isWalkable
                && !westTile.get().directionalBlockers.get().eastBlocked) {
            return westTile;
        }
        return Optional.empty();
    }

    private Optional<Tile> getNorthEastIfWalkable(Tile position) {
        final Optional<Tile> north = this.getNorthIfWalkable(position);
        final Optional<Tile> east = this.getEastIfWalkable(position);
        if (north.isPresent() && east.isPresent()) {
            final Optional<Tile> northEast = this.getEastIfWalkable(north.get());
            final Optional<Tile> eastNorth = this.getNorthIfWalkable(east.get());
            if (northEast.isPresent() && eastNorth.isPresent()) {
                return northEast;
            }
        }
        return Optional.empty();
    }

    private Optional<Tile> getNorthWestIfWalkable(Tile position) {
        final Optional<Tile> north = this.getNorthIfWalkable(position);
        final Optional<Tile> west = this.getWestIfWalkable(position);
        if (north.isPresent() && west.isPresent()) {
            final Optional<Tile> northWest = this.getWestIfWalkable(north.get());
            final Optional<Tile> westNorth = this.getNorthIfWalkable(west.get());
            if (northWest.isPresent() && westNorth.isPresent()) {
                return northWest;
            }
        }
        return Optional.empty();
    }

    private Optional<Tile> getSouthEastIfWalkable(Tile position) {
        final Optional<Tile> south = this.getSouthIfWalkable(position);
        final Optional<Tile> east = this.getEastIfWalkable(position);
        if (south.isPresent() && east.isPresent()) {
            final Optional<Tile> southEast = this.getEastIfWalkable(south.get());
            final Optional<Tile> eastSouth = this.getSouthIfWalkable(east.get());
            if (southEast.isPresent() && eastSouth.isPresent()) {
                return southEast;
            }
        }
        return Optional.empty();
    }

    private Optional<Tile> getSouthWestIfWalkable(Tile position) {
        final Optional<Tile> south = this.getSouthIfWalkable(position);
        final Optional<Tile> west = this.getWestIfWalkable(position);
        if (south.isPresent() && west.isPresent()) {
            final Optional<Tile> southWest = this.getWestIfWalkable(south.get());
            final Optional<Tile> westSouth = this.getSouthIfWalkable(west.get());
            if (southWest.isPresent() && westSouth.isPresent()) {
                return southWest;
            }
        }
        return Optional.empty();
    }
}
