package net.runelite.cache.movementdumper;

import java.util.stream.Stream;

class DirectionalBlockers {
    public final boolean northBlocked;
    public final boolean eastBlocked;
    public final boolean southBlocked;
    public final boolean westBlocked;

    DirectionalBlockers(final boolean northBlocked, final boolean eastBlocked, final boolean southBlocked, final boolean westBlocked) {
        this.northBlocked = northBlocked;
        this.eastBlocked = eastBlocked;
        this.southBlocked = southBlocked;
        this.westBlocked = westBlocked;
    }

    public boolean allBlocked() {
        return this.eastBlocked && this.westBlocked && this.northBlocked && this.southBlocked;
    }

    public boolean noneBlocked() {
        return !this.eastBlocked && !this.westBlocked && !this.northBlocked && !this.southBlocked;
    }

    @Override
    public String toString() {
        if(this.allBlocked()) {
            return "All Blocked";
        }
        else if(this.noneBlocked()) {
            return "None Blocked";
        }
        else {
            return ""
                    + (this.northBlocked ? "north," : "")
                    + (this.eastBlocked ? "east," : "")
                    + (this.southBlocked ? "south," : "")
                    + (this.westBlocked ? "west," : "")
                    + "blocked";
        }
    }
}
