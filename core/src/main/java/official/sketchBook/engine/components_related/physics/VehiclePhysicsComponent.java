package official.sketchBook.engine.components_related.physics;

import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.PhysicalObjectII;
import official.sketchBook.engine.game_object_related.vehicle.Vehicle;

public class VehiclePhysicsComponent extends MovableObjectPhysicsComponent {
    public VehiclePhysicsComponent(
        Vehicle object,
        int categoryBit,
        int maskBit,
        float density,
        float frict,
        float rest
    ) {
        super(
            object,
            categoryBit,
            maskBit,
            density,
            frict,
            rest
        );
    }
}
