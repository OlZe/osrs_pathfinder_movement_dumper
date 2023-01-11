package net.runelite.cache.movementdumper;

import net.runelite.cache.region.Position;

import java.util.Collection;

@Deprecated
class MovementDump {
    Collection<Position> walkable;
    Collection<Position> obstaclePositions;
    Collection<Integer> obstacleValues;
}
