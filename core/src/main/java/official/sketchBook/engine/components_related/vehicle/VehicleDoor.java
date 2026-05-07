package official.sketchBook.engine.components_related.vehicle;

import com.badlogic.gdx.physics.box2d.Fixture;
import official.sketchBook.engine.components_related.intefaces.base_interfaces.InteractableObject;
import official.sketchBook.engine.game_object_related.vehicle.VehicleSection;
import official.sketchBook.engine.util_related.enumerators.VehicleComponentType;
import official.sketchBook.engine.util_related.helper.body.BodyCreatorHelper;
import official.sketchBook.engine.util_related.helper.body.FixtureData;

import java.util.ArrayList;
import java.util.List;

public class VehicleDoor extends VehicleBaseComponent implements InteractableObject {
    public static int quantity;

    /// Lista de fixtures da porta
    public final List<Fixture> doorFixList;

    ///Dados da fixture
    public final FixtureData fixData;

    /// Flags de estado interno
    private boolean
        open;           //Se está aberta

    /// Flags de estado auxiliar
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

        this.fixData = fixData;
        this.doorFixList = new ArrayList<>();
    }

    @Override
    public void initObject() {
        super.initObject();

        this.doorFixList.addAll(
            BodyCreatorHelper.createFixturesFromData(
                fixData,
                ownerSection.getInternalBody()
            )
        );
    }

    public void interact() {
        this.open = !open;      //Abre e fecha a porta a cada chamada

        for (int i = 0; i < doorFixList.size(); i++) {
            this.doorFixList.get(i).setSensor(open);
        }
    }

    public boolean canInteract() {
        return !broken && !locked;
    }
}
