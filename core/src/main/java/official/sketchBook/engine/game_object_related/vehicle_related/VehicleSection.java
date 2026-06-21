package official.sketchBook.engine.game_object_related.vehicle_related;

import com.badlogic.gdx.physics.box2d.Body;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.physics.PhysicalObjectII;

public interface VehicleSection extends PhysicalObjectII {

    Vehicle getVehicle();

    Body getInternalBody();
    Body getTriggerBody();

    float getVelX();
    float getVelY();

    boolean hasInternalArea();
}
