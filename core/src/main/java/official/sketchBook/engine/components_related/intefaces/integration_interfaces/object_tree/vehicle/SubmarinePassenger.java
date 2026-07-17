package official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.vehicle;

import official.sketchBook.engine.components_related.physics.PhysicalMobLiquidInteractionComponent;

public interface SubmarinePassenger extends VehiclePassenger{
    PhysicalMobLiquidInteractionComponent getLiquidInteractionC();

}
