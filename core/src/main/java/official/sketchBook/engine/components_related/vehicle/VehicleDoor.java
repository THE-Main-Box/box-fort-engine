package official.sketchBook.engine.components_related.vehicle;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.InteractableObject;
import official.sketchBook.engine.game_object_related.vehicle.VehicleSection;
import official.sketchBook.engine.util_related.enumerators.VehicleComponentType;
import official.sketchBook.engine.util_related.helper.body.FixtureData;

public class VehicleDoor extends VehicleBaseComponent implements InteractableObject {
    public static int quantity;

    /// Fixture física da porta
    public final Fixture doorFix;

    /// Flags de estado interno
    private boolean
        open;           //Se está aberta

    ///Flags de estado auxiliar
    public boolean
        broken,         //Se a porta está quebrada
        locked;         //Se a porta está trancada

    public VehicleDoor(
        VehicleSection ownerSection,
        FixtureData fixData,
        boolean broken,
        boolean locked,
        boolean open
    ) {

        super(
            "Door_id: " + quantity,
            String.valueOf(quantity),
            ownerSection,
            VehicleComponentType.PHYSICAL_INTERNAL
        );

        this.broken = broken;
        this.locked = locked;
        this.open = open;

        this.doorFix = null;
//        this.doorFix = ownerSection.getInternalBody()
//            .createFixture();
    }

    public void interact() {
        if (!canInteract()) return;

        this.open = !open;      //Abre e fecha a porta a cada chamada

        this.doorFix.setSensor(open);
    }

    public boolean canInteract() {
        return !broken && !locked;
    }
}
