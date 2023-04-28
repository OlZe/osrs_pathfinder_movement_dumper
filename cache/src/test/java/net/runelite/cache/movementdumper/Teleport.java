package net.runelite.cache.movementdumper;

import net.runelite.cache.region.Position;

public class Teleport {
    public final Position destination;
    public final String title;
    public final byte duration;
    public final boolean canTeleportUpTo30Wildy;

    public Teleport(final Position destination, final String title, final byte duration, final boolean canTeleportUpTo30Wildy) {
        this.destination = destination;
        this.title = title;
        this.duration = duration;
        this.canTeleportUpTo30Wildy = canTeleportUpTo30Wildy;
    }
}
