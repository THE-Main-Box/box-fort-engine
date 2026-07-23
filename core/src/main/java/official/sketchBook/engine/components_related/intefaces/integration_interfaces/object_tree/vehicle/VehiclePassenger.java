package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.vehicle;

import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.physics.PhysicalObjectII;
import official.sketchBook.engine.components_related.movement.MovementComponent;
import official.sketchBook.engine.components_related.physics.VehiclePassengerPhysicsComponent;
import official.sketchBook.engine.game_object_related.vehicle_related.VehicleSection;

public interface VehiclePassenger extends PhysicalObjectII {
    VehiclePassengerPhysicsComponent getVehiclePassengerPhysicsC();

    void onSectionChanged(VehicleSection oldSection, VehicleSection newSection);

    void onVehicleEnter(VehicleSection newSection);

    void onVehicleExit(VehicleSection newSection);

    MovementComponent getMoveC();

}
