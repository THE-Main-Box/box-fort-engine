package official.sketchBook.engine.game_object_related.vehicle;

import com.badlogic.gdx.physics.box2d.Body;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.PhysicalObjectII;

public interface VehicleSection extends PhysicalObjectII {
    Vehicle getVehicle();

    Body getInternalBody();

    float getVelX();
    float getVelY();
}
