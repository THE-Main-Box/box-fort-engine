package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree;

import official.sketchBook.engine.components_related.physics.PhysicalMobLiquidInteractionComponent;
import official.sketchBook.engine.components_related.physics.VehiclePassengerPhysicsComponent;

public interface VehiclePassenger extends PhysicalObjectII{
    VehiclePassengerPhysicsComponent getVehiclePassengerPhysicsC();

    PhysicalMobLiquidInteractionComponent getLiquidInteractionC();

}
