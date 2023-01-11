package net.runelite.cache.movementdumper;

import net.runelite.cache.region.Position;
import net.runelite.cache.region.Region;

class TileSettings {
    public final boolean isWalkable;
    public final boolean isBlocked;
    public final boolean isBridge;
    public final boolean isBridgeAbove;

    public TileSettings(Region region, Position relativePosition) {
        final int tileSetting = region.getTileSetting(relativePosition.getZ(), relativePosition.getX(), relativePosition.getY());
        this.isWalkable = (tileSetting & 1) == 0;
        this.isBlocked = (tileSetting & 24) != 0;
        this.isBridge = (tileSetting & 2) != 0;
        if (relativePosition.getZ() < 3) {
            this.isBridgeAbove = (region.getTileSetting(relativePosition.getZ() + 1, relativePosition.getX(), relativePosition.getY()) & 2) != 0;
        } else {
            this.isBridgeAbove = false;
        }
    }
}
