package net.runelite.cache.movementdumper;

public class TileObstacles {
    public boolean rightBlocked = false;
    public boolean leftBlocked = false;
    public boolean topBlocked = false;
    public boolean bottomBlocked = false;

    public boolean allDirectionsBlocked() {
        return this.rightBlocked && this.leftBlocked && this.topBlocked && this.bottomBlocked;
    }
}
