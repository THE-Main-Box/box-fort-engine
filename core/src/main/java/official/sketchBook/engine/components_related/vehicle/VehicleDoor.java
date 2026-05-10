package official.sketchBook.engine.components_related.vehicle;

import com.badlogic.gdx.physics.box2d.Fixture;
import official.sketchBook.engine.components_related.intefaces.integration_interfaces.object_tree.InteractableObjectII;
import official.sketchBook.engine.components_related.objects.TangibleSwitchComponent;
import official.sketchBook.engine.game_object_related.vehicle_related.VehicleSection;
import official.sketchBook.engine.util_related.enumerators.CollisionLayers;
import official.sketchBook.engine.util_related.enumerators.ObjectType;
import official.sketchBook.engine.util_related.enumerators.VehicleComponentType;
import official.sketchBook.engine.util_related.helper.GameObjectTag;
import official.sketchBook.engine.util_related.helper.body.BodyCreatorHelper;
import official.sketchBook.engine.util_related.helper.body.FixtureData;

import java.util.ArrayList;
import java.util.List;

public class VehicleDoor extends VehicleBaseComponent implements InteractableObjectII {
    public static int quantity;

    /// Lista de fixtures da porta
    public final List<Fixture> doorFixList;

    /// Dados da fixture
    public final FixtureData
        triggerFixData,
        fixData;


    /// Flags de estado interno
    private boolean
        open;           //Se está aberta

    /// Flags de estado auxiliar
    public boolean
        broken,         //Se a porta está quebrada
        locked;         //Se a porta está trancada

    private TangibleSwitchComponent tangibleComponent;

    private boolean pendingStateUpdate = false;


    public VehicleDoor(
        VehicleSection ownerSection,
        FixtureData fixData,
        FixtureData triggerFixData,
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

        this.triggerFixData = triggerFixData;
        this.fixData = fixData;

        this.doorFixList = new ArrayList<>();

        initObject();

        quantity ++;
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

        for (Fixture fix : doorFixList) {
            fix.setUserData(
                new GameObjectTag(
                    ObjectType.VEHICLE,
                    this
                )
            );
        }

        tangibleComponent = new TangibleSwitchComponent(
            doorFixList.get(0).getFilterData().maskBits,
            open,
            doorFixList
        );

    }

    @Override
    public void update(float delta) {
        super.update(delta);
    }

    public void interact() {
        if (!canInteract()) return;
        this.open = !open;
        tangibleComponent.setTangible(!open);
        pendingStateUpdate = true;

    }

    @Override
    public void postUpdate() {
        super.postUpdate();
        if (!pendingStateUpdate) return;
        tangibleComponent.updateTangibleState();
        pendingStateUpdate = false;
    }

    public boolean canInteract() {
        return !broken && !locked;
    }


    @Override
    public FixtureData getTriggerFixData() {
        return triggerFixData;
    }
}
