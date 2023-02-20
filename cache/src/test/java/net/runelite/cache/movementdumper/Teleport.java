package net.runelite.cache.movementdumper;

public class Teleport {
    public final RegionPosition destination;
    public final String title;
    public final byte duration;

    public Teleport(final RegionPosition destination, final String title, final byte duration) {
        this.destination = destination;
        this.title = title;
        this.duration = duration;
    }
}
