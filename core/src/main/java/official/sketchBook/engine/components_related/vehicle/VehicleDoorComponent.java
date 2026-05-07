package official.sketchBook.engine.components_related.vehicle;

import official.sketchBook.engine.game_object_related.vehicle.VehicleSection;
import official.sketchBook.engine.util_related.enumerators.VehicleComponentType;

public class VehicleDoorComponent extends VehicleBaseComponent{

    private static int quantityInNode = 0;

    public VehicleDoorComponent(
        VehicleSection ownerSection,
        VehicleComponentType type
    ) {
        super(
            "door_manager_" + quantityInNode,
            String.valueOf(quantityInNode),
            ownerSection,
            type
        );

        quantityInNode ++;
    }



}
