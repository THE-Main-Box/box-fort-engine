package official.sketchBook.engine.components_related.vehicle;

import com.badlogic.gdx.physics.box2d.Fixture;

public class VehicleDoor {
    public final Fixture doorFix;
    public boolean
        broken,
        open;

    public VehicleDoor(
        Fixture doorFix,
        boolean broken,
        boolean open
    ) {
        this.doorFix = doorFix;
        this.broken = broken;
        this.open = open;
    }
}
