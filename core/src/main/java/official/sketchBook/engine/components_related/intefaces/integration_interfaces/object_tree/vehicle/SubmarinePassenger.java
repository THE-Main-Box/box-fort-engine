package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.vehicle;

import official.sketchBook.engine.components_related.physics.PhysicalMobLiquidInteractionComponent;
import official.sketchBook.engine.game_object_related.vehicle_related.SubmarineNode;
import official.sketchBook.engine.game_object_related.vehicle_related.VehicleSection;

public interface SubmarinePassenger extends VehiclePassenger{
    PhysicalMobLiquidInteractionComponent getLiquidInteractionC();

    @Override
    default void onSectionChanged(VehicleSection oldSection, VehicleSection newSection) {
        if (oldSection instanceof SubmarineNode) {
            SubmarineNode oldNode = (SubmarineNode) oldSection;
            oldNode.getLiquidInteractionC().setMass(
                oldNode.getLiquidInteractionC().getMass() -
                    getLiquidInteractionC().getMass()
            );
        }

        if (newSection instanceof SubmarineNode) {
            SubmarineNode newNode = (SubmarineNode) newSection;
            newNode.getLiquidInteractionC().setMass(
                newNode.getLiquidInteractionC().getMass() +
                    getLiquidInteractionC().getMass()
            );
        }
    }

    @Override
    default void onVehicleExit(VehicleSection section) {
        if (section instanceof SubmarineNode) {
            getVehiclePassengerPhysicsC().setCurrentSection(null);
            getLiquidInteractionC().setCanInteract(true);
        }
    }

    @Override
    default void onVehicleEnter(VehicleSection newSection) {
        if (newSection instanceof SubmarineNode) {
            getVehiclePassengerPhysicsC().setCurrentSection(newSection);
            getLiquidInteractionC().setCanInteract(false);
        }
    }
}
