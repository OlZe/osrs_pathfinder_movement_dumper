package net.runelite.cache.movementdumper;

class TileObstacles {
    public boolean eastBlocked = false;
    public boolean westBlocked = false;
    public boolean northBlocked = false;
    public boolean southBlocked = false;

    public boolean allDirectionsBlocked() {
        return this.eastBlocked && this.westBlocked && this.northBlocked && this.southBlocked;
    }
}
