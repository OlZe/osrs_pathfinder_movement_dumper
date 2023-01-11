package net.runelite.cache.movementdumper;

import net.runelite.cache.region.Position;

import java.util.HashMap;

public class TilesMap {
    public final HashMap<Position, TileObstacles> map = new HashMap<>();

    public void addTile(Position absolutePosition) {
        this.map.put(absolutePosition, new TileObstacles());
    }

    public void markLeftBlocked(Position absolutePosition) {
        final TileObstacles obstacle = this.map.get(absolutePosition);
        if (obstacle != null) {
            obstacle.leftBlocked = true;
        }
    }

    public void markRightBlocked(Position absolutePosition) {
        final TileObstacles obstacle = this.map.get(absolutePosition);
        if (obstacle != null) {
            obstacle.rightBlocked = true;
        }
    }

    public void markTopBlocked(Position absolutePosition) {
        final TileObstacles obstacle = this.map.get(absolutePosition);
        if (obstacle != null) {
            obstacle.topBlocked = true;
        }
    }

    public void markBottomBlocked(Position absolutePosition) {
        final TileObstacles obstacle = this.map.get(absolutePosition);
        if (obstacle != null) {
            obstacle.bottomBlocked = true;
        }
    }

    public void markAllSidesBlocked(Position absolutePosition) {
        this.markLeftBlocked(absolutePosition);
        this.markRightBlocked(absolutePosition);
        this.markTopBlocked(absolutePosition);
        this.markBottomBlocked(absolutePosition);
    }

}
