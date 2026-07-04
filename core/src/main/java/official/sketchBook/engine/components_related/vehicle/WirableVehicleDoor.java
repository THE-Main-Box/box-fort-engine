package official.sketchBook.engine.components_related.vehicle;

import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.interaction.ControllableObjectII;
import official.sketchBook.engine.game_object_related.vehicle_related.VehicleSection;
import official.sketchBook.engine.util_related.helper.body.FixtureData;

public class WirableVehicleDoor extends VehicleDoor implements ControllableObjectII {
    public WirableVehicleDoor(VehicleSection ownerSection, FixtureData fixData, FixtureData triggerFixData, boolean broken, boolean locked, boolean open) {
        super(ownerSection, fixData, triggerFixData, broken, locked, open);
    }

    @Override
    public void executeWiringInteraction() {
        updateDoorOpenState(!open);
    }

    @Override
    public boolean canWireInteract() {
        return true;
    }
}
