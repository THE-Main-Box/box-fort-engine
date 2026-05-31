package official.sketchBook.engine.components_related.vehicle;

import com.badlogic.gdx.physics.box2d.Fixture;
import official.sketchBook.engine.components_related.objects.TangibleSwitchComponent;
import official.sketchBook.engine.game_object_related.vehicle_related.VehicleSection;
import official.sketchBook.engine.util_related.enumerators.ObjectType;
import official.sketchBook.engine.util_related.enumerators.VehicleComponentType;
import official.sketchBook.engine.util_related.helper.GameObjectTag;
import official.sketchBook.engine.util_related.helper.body.BodyCreatorHelper;
import official.sketchBook.engine.util_related.helper.body.FixtureData;

public class VehicleDoor extends VehicleInteractableComponent {
    public static int quantity;

    /// Flags de estado
    public boolean
        inRange,
        open,           //Se está aberta
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
            VehicleComponentType.PHYSICAL_INTERNAL,
            fixData,
            triggerFixData
        );

        this.broken = broken;
        this.locked = locked;
        this.open = open;

        initObject();

        quantity ++;
    }

    @Override
    public void initObject() {
        super.initObject();

        this.fixList.addAll(
            BodyCreatorHelper.createFixturesFromData(
                fixData,
                ownerSection.getInternalBody()
            )
        );

        for (Fixture fix : fixList) {
            fix.setUserData(
                new GameObjectTag(
                    ObjectType.VEHICLE,
                    this
                )
            );
        }

        tangibleComponent = new TangibleSwitchComponent(
            fixList.get(0).getFilterData().maskBits,
            open,
            fixList
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

    @Override
    public boolean isInRange() {
        return inRange;
    }

    public boolean canInteract() {
        return !broken && !locked;
    }

}
