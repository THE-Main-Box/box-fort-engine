package official.sketchBook.engine.util_related.enumerators;

public enum CollisionLayers {
    ENVIRONMENT(1),
    ALLY_ENTITY(1 << 1),
    ENEMY_ENTITY(1 << 2),
    ALLY_PROJECTILE(1 << 3),
    ENEMY_PROJECTILE(1 << 4),
    SENSOR(1 << 5),
    INTERACTIVE(1 << 6),
    NONE(0),
    ENTITIES(
        ALLY_ENTITY.bit() |
        ENEMY_ENTITY.bit()
        ),
    PROJECTILES(
        ALLY_PROJECTILE.bit() |
        ENEMY_PROJECTILE.bit()
        ),
    ALL(
        ENTITIES.bit() |
        PROJECTILES.bit()|
        SENSOR.bit() |
        ENVIRONMENT.bit() |
        INTERACTIVE.bit()
        );

    private final short bit;

    CollisionLayers(int bit) {
        this.bit = (short) bit;
    }

    public short bit() {
        return bit;
    }
}
