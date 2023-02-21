package net.runelite.cache.movementdumper;

import net.runelite.cache.region.Position;

public class Teleport {
    public final Position destination;
    public final String title;
    public final byte duration;

    public Teleport(final Position destination, final String title, final byte duration) {
        this.destination = destination;
        this.title = title;
        this.duration = duration;
    }
}
