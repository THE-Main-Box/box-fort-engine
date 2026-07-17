package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.vehicle;

import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.physics.PhysicalObjectII;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.physics.PhysicalMobLiquidInteractionComponent;
import official.sketchBook.engine.components_related.physics.VehiclePassengerPhysicsComponent;

public interface VehiclePassenger extends PhysicalObjectII {
    VehiclePassengerPhysicsComponent getVehiclePassengerPhysicsC();

    MovementComponent getMoveC();

}
