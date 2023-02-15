package net.runelite.cache.movementdumper;

import net.runelite.cache.region.Position;

import java.util.HashMap;

class WalkableTilesMap {
    public final HashMap<Position, TileObstacles> map = new HashMap<>();

    public void addWalkableTile(Position normalizedAbsolutePosition) {
        // Filter z = 0
        if (normalizedAbsolutePosition.getZ() != 0) {
            return;
        }
        this.map.put(normalizedAbsolutePosition, new TileObstacles());
    }

    public void markLeftBlocked(Position normalizedAbsolutePosition) {
        final TileObstacles obstacle = this.map.get(normalizedAbsolutePosition);
        if (obstacle != null) {
            obstacle.westBlocked = true;
            this.removeIfAllBlocked(normalizedAbsolutePosition);
        }
    }

    public void markRightBlocked(Position normalizedAbsolutePosition) {
        final TileObstacles obstacle = this.map.get(normalizedAbsolutePosition);
        if (obstacle != null) {
            obstacle.eastBlocked = true;
            this.removeIfAllBlocked(normalizedAbsolutePosition);
        }
    }

    public void markTopBlocked(Position normalizedAbsolutePosition) {
        final TileObstacles obstacle = this.map.get(normalizedAbsolutePosition);
        if (obstacle != null) {
            obstacle.northBlocked = true;
            this.removeIfAllBlocked(normalizedAbsolutePosition);
        }
    }

    public void markBottomBlocked(Position normalizedAbsolutePosition) {
        final TileObstacles obstacle = this.map.get(normalizedAbsolutePosition);
        if (obstacle != null) {
            obstacle.southBlocked = true;
            this.removeIfAllBlocked(normalizedAbsolutePosition);
        }
    }

    public void markAllSidesBlocked(Position normalizedAbsolutePosition) {
        this.map.remove(normalizedAbsolutePosition);
    }

    private void removeIfAllBlocked(Position normalizedAbsolutePosition) {
        final TileObstacles obstacles = this.map.get(normalizedAbsolutePosition);
        if (obstacles.allDirectionsBlocked()) {
            this.map.remove(normalizedAbsolutePosition);
        }
    }
}
