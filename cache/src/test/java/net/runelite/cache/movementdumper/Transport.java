package net.runelite.cache.movementdumper;

import net.runelite.cache.region.Position;

public class Transport {
    final Position from;
    final Position to;
    final String title;
    final byte duration;

    public Transport(final Position from, final Position to, final String title, final byte duration) {
        this.from = from;
        this.to = to;
        this.title = title;
        this.duration = duration;
    }
}
