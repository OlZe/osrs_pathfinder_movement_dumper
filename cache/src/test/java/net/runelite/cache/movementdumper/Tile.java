package net.runelite.cache.movementdumper;

import lombok.ToString;
import net.runelite.cache.region.Position;

import java.util.Optional;

@ToString
public class Tile {
    public final Position position;
    public final boolean isWalkable;
    /** Populated if isWalkable is true */
    public final Optional<DirectionalBlockers> directionalBlockers;

    public Tile(final Position position, final boolean isWalkable, final Optional<DirectionalBlockers> directionalBlockers) {
        this.position = position;
        this.isWalkable = isWalkable;
        this.directionalBlockers = directionalBlockers;

        if(isWalkable) {
            assert directionalBlockers.isPresent();
            assert !directionalBlockers.get().allBlocked();
        }
    }
}
