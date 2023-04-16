package net.runelite.cache.movementdumper;

import lombok.ToString;
import net.runelite.cache.region.Position;

import java.util.Objects;
import java.util.Optional;

@ToString
public class Tile {
    public final Position position;
    public final boolean isWalkable;
    /** Populated if isWalkable is true */
    public final Optional<DirectionalBlockers> directionalBlockers;

    public final PositionUtils.WildernessLevels wildernessLevel;

    public Tile(final Position position, final boolean isWalkable, final Optional<DirectionalBlockers> directionalBlockers, final PositionUtils.WildernessLevels wildernessLevel) {
        this.position = position;
        this.isWalkable = isWalkable;
        this.directionalBlockers = directionalBlockers;
        this.wildernessLevel = wildernessLevel;

        if (isWalkable) {
            assert directionalBlockers.isPresent();
            assert !directionalBlockers.get().allBlocked();
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Tile tile = (Tile) o;
        return position.equals(tile.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position);
    }
}
